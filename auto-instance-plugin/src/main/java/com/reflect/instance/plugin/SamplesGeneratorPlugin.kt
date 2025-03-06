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

            modelClassesByPackage.forEach { (modelPackage, classNames) ->
                appProject.generateSampleInstancesForClass(
                    modelPackage,
                    classNames.distinct(),
                    instance,
                    classLoader,
                )
            }
        } else {
            throw GradleException("classes.jar not found inside extracted AAR!")
        }
    }

    private fun findReflectInstanceJar(): File {
        return try {
            findReflectInstanceJarFromAAR()
        } catch (e: Exception) {
            val jarFile = findLatestJar("reflect-instance/build/libs").also { println("%%^ -> ${it?.absolutePath}") }
            jarFile ?: throw GradleException("Reflect Instance JAR not found!")
        }
    }

    @Throws(GradleException::class)
    private fun findReflectInstanceJarFromAAR(): File {
        val aarFile = findLatestAAR("reflect-instance/build/outputs/aar")
        val tempDir = project.rootProject.buildDir.resolve("extracted-aar")

        if (aarFile == null || !aarFile.exists()) {
            throw GradleException("AAR file not found. Run ':reflect-instance:assembleDebug' first.")
        }

        project.copy {
            from(project.zipTree(aarFile))
            into(tempDir)
        }

        return tempDir.resolve("classes.jar").takeIf { it.exists() }
            ?: throw GradleException("Extracted classes.jar not found in AAR")
    }

    private fun findLatestJar(dir: String): File? {
        return project.rootProject.file(dir)
            .takeIf { it.exists() }
            ?.listFiles { file -> file.extension == "jar" }
            ?.minByOrNull { it.lastModified() }
    }

    private fun findLatestAAR(dir: String): File? {
        return project.rootProject.file(dir)
            .takeIf { it.exists() }
            ?.listFiles { file -> file.extension == "aar" }
            ?.maxByOrNull { it.lastModified() }
    }

}

private fun Project.getGeneratedKspDir(): File {
    val variant = getBuildVariant()
    val kspDir = project.buildDir.resolve("generated/ksp")

    // Find a directory that matches the variant dynamically
    val matchedVariantDir =
        kspDir.listFiles()?.firstOrNull { it.name.contains(variant, ignoreCase = true) }

    return matchedVariantDir?.resolve("kotlin/com/reflect/instance/sample")
        ?: throw GradleException("Could not determine the correct KSP directory for variant: $variant")
}

private fun Project.getBuildVariant(): String {
    val defaultVariant = "debug" // Fallback option
    return project.gradle.startParameter.taskNames
        .onEach { println("Build Variant $it") }
        .map { it.substringAfterLast(":") }
        .firstOrNull { it.startsWith("compile") && it.endsWith("Sources") }
        ?.removePrefix("compile")
        ?.removeSuffix("Sources")
        ?: defaultVariant
}


private fun Project.generateSampleInstancesForClass(
    modelPackage: String,
    classNames: List<String>,
    fakeHelper: Any,
    classLoader: URLClassLoader,
) {
    val generatedDir = getGeneratedKspDir()

    println("generatedDir -> $generatedDir")
    val generatedFiles =
        generatedDir.listFiles()?.filter { it.isFile && it.extension == "kt" } ?: emptyList()

    // Step 1: Modify instances in the files
    generatedFiles.forEach { generatedFile ->
        println("Generated File -> ${generatedFile.absolutePath}")

        classNames.forEach { className ->
            try {
                val fullClassName = "$modelPackage.$className"
                logger.lifecycle("~~ $fullClassName")
                val clazz = Class.forName(fullClassName, true, classLoader).kotlin
                val fakeFunction = fakeHelper::class.memberFunctions.find { it.name == "fake" }
                fakeFunction?.isAccessible = true
                var instance = (fakeFunction!!.call(fakeHelper, clazz, 1) as List<Any>).first()


                val originalContent = generatedFile.readText()
                val instanceTypeName = instance::class.simpleName!!

                val typeRegex =
                    Regex("""val\s+(\w+)\s*:\s*($instanceTypeName)\s*=\s*(?:.*\s+as\s+\2)?""")


                val matches = typeRegex.findAll(originalContent)
                var modifiedContent = originalContent

                matches.forEachIndexed { index, match ->
                    val valName = match.groupValues[1]
                    val typeName = match.groupValues[2]
                    instance = if(index > 0) {
                        (fakeFunction.call(fakeHelper, clazz, 1) as List<Any>).first()
                    } else {
                        instance
                    }
                    val replacement =
                        "val $valName: $typeName = ${objectToString(instance)}"
                    modifiedContent = modifiedContent.replace(match.value, replacement)
                }


                val listTypeRegex = Regex(
                    """val\s+(\w+)\s*:\s*List<($instanceTypeName)>\s*=\s*List\((\d+)\)\s*\{\s*Any\(\)\s+as\s+(\2)\s*}"""
                )
                val listMatches = listTypeRegex.findAll(originalContent)

                listMatches.forEachIndexed { index, match ->
                    val valName = match.groupValues[1]
                    val typeName = match.groupValues[2]
                    val listSize = match.groupValues[3].toInt()

                    val replacement = "val $valName: List<$typeName> = ${objectToString(fakeFunction.call(fakeHelper, clazz, listSize) as List<Any>)}"

                    modifiedContent = modifiedContent.replace(match.value, replacement)
                }

                generatedFile.writeText(modifiedContent)

            } catch (e: NoClassDefFoundError) {
                logger.warn("Skipping class $className due to missing dependency: ${e.message}")
            } catch (e: Exception) {
                logger.error("Error processing class $className: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // Step 2: Remove all existing imports
    generatedFiles.forEach { generatedFile ->
        val contentWithoutImports =
            generatedFile.readText().replace(Regex("""import\s+[a-zA-Z0-9_.]+"""), "").trim()
        generatedFile.writeText(contentWithoutImports)
    }

    // Step 3: Re-add package declaration and imports
    generatedFiles.forEach { generatedFile ->
        val originalContent = generatedFile.readText()

        val packageRegex = Regex("""^package\s+[a-zA-Z0-9_.]+""", RegexOption.MULTILINE)
        val packageMatch = packageRegex.find(originalContent)
        val packageDeclaration = packageMatch?.value ?: "package com.reflect.instance.sample"

        val contentWithoutPackage = originalContent.replace(packageRegex, "").trim()

        val newImports = classNames
            .filter { "$" !in it }  // Exclude nested/companion/serializer classes
            .joinToString("\n") { "import $modelPackage.$it" }

        val newContent = buildString {
            appendLine(packageDeclaration)
            appendLine()
            appendLine(newImports)
            appendLine()
            append(contentWithoutPackage)
        }.trim()

        generatedFile.writeText(newContent)
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