package com.reflect.instance.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import java.io.File
import java.net.URLClassLoader
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

fun findCompiledClassDirectories(appProject: Project): List<File> {
    val outputDirs = mutableSetOf<File>()
    appProject.tasks.withType(KotlinCompilationTask::class.java).forEach { task ->
        outputDirs.addAll(task.outputs.files.files)
    }

    appProject.tasks.withType(JavaCompile::class.java).forEach { task ->
        outputDirs.add(task.destinationDirectory.asFile.get())
    }

    return outputDirs.filter { it.exists() }
}

abstract class GenerateModelSamplesTask : DefaultTask() {
    @get:Input
    abstract val modelPackages: ListProperty<String>

    @TaskAction
    fun generate() {

        val appProject = project.rootProject.findProject(":app")
        if (appProject == null) {
            logger.error("App project not found")
            return
        }

        val classDirectories = findCompiledClassDirectories(appProject)

        if (classDirectories.isEmpty()) {
            logger.error("No class directories found. Make sure the app module is compiled.")
            logger.error("Try running './gradlew :app:compileDebugSources' first.")
            return
        }

        logger.lifecycle("Searching for classes in:")
        classDirectories.forEach { logger.lifecycle(" - ${it.absolutePath}") }

        val urls = classDirectories.map { it.toURI().toURL() }.toTypedArray()
        val classLoader = URLClassLoader(urls, this.javaClass.classLoader)

        val modelClassesByPackage = mutableMapOf<String, MutableList<String>>()


        modelPackages.get().forEach { modelPackage ->
            val packagePath = modelPackage.replace('.', '/')
            classDirectories.forEach { dir ->
                val packageDir = dir.resolve(packagePath)
                if (packageDir.exists() && packageDir.isDirectory) {
                    packageDir.listFiles()?.forEach { file ->
                        if (file.isFile && file.extension == "class") {
                            val fullClassName = "$modelPackage.${file.nameWithoutExtension}"
                            try {
                                val clazz =
                                    Class.forName(fullClassName, true, classLoader).kotlin
                                if (!clazz.isAbstract && !clazz.java.isInterface && !clazz.java.isAnonymousClass) {
                                    modelClassesByPackage.getOrPut(modelPackage) { mutableListOf() }
                                        .add(file.nameWithoutExtension)
                                }
                            } catch (e: NoClassDefFoundError) {
                                logger.warn("Skipping $fullClassName due to missing dependency: ${e.message}")
                            } catch (e: Exception) {
                                logger.error("Skipping $fullClassName due to error: ${e.message}")
                            }
                        }
                    }
                }
            }
        }

        val classesJar = findReflectInstanceJar()

        if (classesJar.exists()) {
            val fakeHelperClassLoader =
                URLClassLoader(arrayOf(classesJar.toURI().toURL()), javaClass.classLoader)
            val targetClass = fakeHelperClassLoader.loadClass("com.reflect.instance.FakeHelper")
            val instance = targetClass.getDeclaredConstructor().newInstance()

            println("Loaded class: ${instance.javaClass.name}")
            println(
                "Model Classes: ${
                    modelClassesByPackage.map { it.key.plus(":${it.value}") }.joinToString { it }
                }"
            )
            modelClassesByPackage.forEach { (modelPackage, classNames) ->
                appProject.generateSampleInstancesForClass(
                    modelPackage,
                    classNames,
                    instance,
                    classLoader,
                )
            }
        } else {
            throw GradleException("classes.jar not found inside extracted AAR!")
        }


    }

    private fun findReflectInstanceJar(): File {
        val path = "reflect-instance/build/libs/reflect-instance.jar"

        return try {
            findReflectInstanceJarFromAAR()
        } catch (e: Exception) {
            val reflectInstanceJar = project.rootProject.file(path)
            if (!reflectInstanceJar.exists()) {
                project.logger.error("Reflect Instance JAR not found at: $path")
                throw GradleException("Reflect Instance not found! Searched location: ${reflectInstanceJar.absolutePath}")
            }

            project.logger.lifecycle("Using Reflect Instance JAR from: ${reflectInstanceJar.absolutePath}")
            reflectInstanceJar
        }
    }


    @Throws(GradleException::class)
    private fun findReflectInstanceJarFromAAR(): File {
        val aarFile =
            project.rootProject.file("reflect-instance/build/outputs/aar/reflect-instance-debug.aar")
        val tempDir = project.rootProject.buildDir.resolve("extracted-aar")

        if (!aarFile.exists()) {
            throw GradleException("AAR file not found. Run ':reflect-instance:assembleDebug' first.")
        }

        project.copy {
            from(project.zipTree(aarFile))
            into(tempDir)
        }

        return tempDir.resolve("classes.jar")
    }
}

private fun Project.generateSampleInstancesForClass(
    modelPackage: String,
    classNames: MutableList<String>,
    fakeHelper: Any,
    classLoader: URLClassLoader,
) {
    logger.lifecycle(classNames.joinToString { it })
    val generatedDir = File(project.buildDir, "generated/ksp/debug/kotlin/com/reflect/instance/sample")

    val generatedFiles = generatedDir.listFiles()?.filter { it.isFile && it.extension == "kt" } ?: emptyList()

    // Step 1: Modify instances in the files
    classNames.forEach { className ->
        try {
            val fullClassName = "$modelPackage.$className"
            val clazz = Class.forName(fullClassName, true, classLoader).kotlin
            val fakeFunction = fakeHelper::class.memberFunctions.find { it.name == "fake" }
            fakeFunction?.isAccessible = true
            val instances = fakeFunction!!.call(fakeHelper, clazz, 10) as List<Any>
            val instance = instances.first()

            logger.lifecycle("~~ $fullClassName")

            generatedFiles.forEach { generatedFile ->
                val originalContent = generatedFile.readText()
                val instanceTypeName = instance::class.simpleName!!

                val typeRegex = Regex("""val\s+(\w+)\s*:\s*($instanceTypeName)\s*=\s*(?:.*\s+as\s+\2)?""")
                val modifiedContent = originalContent.replace(typeRegex) { matchResult ->
                    val valName = matchResult.groupValues[1]
                    val typeName = matchResult.groupValues[2]
                    "val $valName: $typeName = ${objectToString(instance)}"
                }

                generatedFile.writeText(modifiedContent)
                println("Modified: ${generatedFile.absolutePath}")
            }
        } catch (e: NoClassDefFoundError) {
            logger.warn("Skipping class $className due to missing dependency: ${e.message}")
        } catch (e: Exception) {
            logger.error("Error processing class $className: ${e.message}")
            e.printStackTrace()
        }
    }

    // Step 2: Remove all existing imports
    generatedFiles.forEach { generatedFile ->
        val contentWithoutImports = generatedFile.readText().replace(Regex("""import\s+[a-zA-Z0-9_.]+"""), "").trim()
        generatedFile.writeText(contentWithoutImports)
    }

    // Step 3: Re-add package declaration and imports
    generatedFiles.forEach { generatedFile ->
        val originalContent = generatedFile.readText()

        val packageRegex = Regex("""^package\s+[a-zA-Z0-9_.]+""", RegexOption.MULTILINE)
        val packageMatch = packageRegex.find(originalContent)
        val packageDeclaration = packageMatch?.value ?: "package com.reflect.instance.sample"

        val contentWithoutPackage = originalContent.replace(packageRegex, "").trim()

        val newImports = classNames.joinToString("\n") { "import $modelPackage.$it" }

        val newContent = buildString {
            appendLine(packageDeclaration)
            appendLine()
            appendLine(newImports)
            appendLine()
            append(contentWithoutPackage)
        }.trim()

        generatedFile.writeText(newContent)
        println("Updated imports: ${generatedFile.absolutePath}")
    }
}



fun objectToString(obj: Any?): String {
    if (obj == null) return "null"

    return when (obj) {
        is String -> "\"$obj\""
        is Number, is Boolean -> obj.toString()
        is Char -> "'$obj'"
        is List<*> -> obj.joinToString(prefix = "listOf(", postfix = ")") { objectToString(it) }
        is Array<*> -> obj.joinToString(prefix = "Array(", postfix = ")") { objectToString(it) }
        is IntArray -> obj.joinToString(prefix = "intArrayOf(", postfix = ")")
        is LongArray -> obj.joinToString(prefix = "longArrayOf(", postfix = ")")
        is ShortArray -> obj.joinToString(prefix = "shortArrayOf(", postfix = ")")
        is ByteArray -> obj.joinToString(prefix = "byteArrayOf(", postfix = ")")
        is DoubleArray -> obj.joinToString(prefix = "doubleArrayOf(", postfix = ")")
        is FloatArray -> obj.joinToString(prefix = "floatArrayOf(", postfix = ")")
        is BooleanArray -> obj.joinToString(prefix = "booleanArrayOf(", postfix = ")")
        is CharArray -> obj.joinToString(prefix = "charArrayOf(", postfix = ")") { "'$it'" }
        is Map<*, *> -> obj.entries.joinToString(
            prefix = "mapOf(",
            postfix = ")"
        ) { "${objectToString(it.key)} to ${objectToString(it.value)}" }

        is Enum<*> -> obj.name
        else -> {
            try {
                val kClass = obj::class
                val properties = kClass.memberProperties
                if (properties.isEmpty()) return obj.toString()
                properties.joinToString(
                    prefix = "${kClass.simpleName}(",
                    postfix = ")"
                ) { "${it.name} = ${objectToString((it as KProperty1<Any, *>).get(obj))}" }
            } catch (e: Exception) {
                obj.toString()
            }
        }
    }
}