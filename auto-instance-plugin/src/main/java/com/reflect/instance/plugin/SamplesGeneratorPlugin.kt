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
import kotlin.reflect.full.createInstance
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
            logger.error("Try running './gradlew :app:compile{variant}Sources' first.")
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
            appProject.generateInstancesInKspInjectorFiles(
                instance,
                classLoader,
            )
        } else {
            throw GradleException("classes.jar not found inside extracted AAR!")
        }

        val allClasses = modelClassesByPackage.map { (pkg, classes) ->
            classes.map { cl -> pkg.plus(".").plus(cl) }
        }

        appProject.generateImportsInKspInjectorFiles(
            allClasses.flatten().distinct().filter { "$" !in it })


    }

    private fun findReflectInstanceJar(): File {
        return try {
            findReflectInstanceJarFromAAR()
        } catch (e: Exception) {
            val jarFile =
                findLatestJar("reflect-instance/build/libs").also { println("%%^ -> ${it?.absolutePath}") }
            jarFile ?: throw GradleException("Reflect Instance JAR not found!")
        }
    }

    @Throws(GradleException::class)
    private fun findReflectInstanceJarFromAAR(): File {
        val aarFile = findLatestAAR("reflect-instance/build/outputs/aar")
        val tempDir = project.layout.buildDirectory.get().asFile.resolve("extracted-aar")

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

private fun Project.getKspGeneratedInjectorFiles(): List<File> {
    val variant = getBuildVariant()
    val kspDir = project.layout.buildDirectory.get().asFile.resolve("generated/ksp")

    // Find the matching variant directory
    val matchedVariantDir =
        kspDir.listFiles()?.firstOrNull { it.name.equals(variant, ignoreCase = true) }
            ?: throw GradleException("Could not determine the correct KSP directory for variant: $variant")

    val kotlinDir = matchedVariantDir.resolve("kotlin")

    // Find all .kt files that end with "Injector"
    return kotlinDir.walkTopDown()
        .filter { it.isFile && it.extension == "kt" && it.name.endsWith("Injector.kt") }
        .toList()
}


private fun Project.getBuildVariant(): String {
    val defaultVariant = "debug" // Fallback option
    return project.gradle.startParameter.taskNames
        .onEach { println("Task name $it") }
        .map { it.substringAfterLast(":") }
        .onEach { println("Cleaned task name $it") }
        .firstOrNull { it.startsWith("compile") && it.endsWith("Sources") }
        ?.removePrefix("compile")
        ?.removeSuffix("Sources").also {
            println("Trimmed task name to variant $it")
        }
        ?: defaultVariant
}


private fun Project.generateInstancesInKspInjectorFiles(
    fakeHelper: Any,
    classLoader: URLClassLoader,
) {
    val generatedFiles = getKspGeneratedInjectorFiles()

    generatedFiles.forEach { generatedFile ->
        println("Generated File -> ${generatedFile.absolutePath}")

        val originalContent = generatedFile.readText()
        var modifiedContent = originalContent

        val typeRegex = Regex(
            """(?<field>\w+)\s*=\s*(?:/\*\*\s*(?<package>[\w.]+)\s*\*\*/\s*)?Any\(\)\s+as\s+(?<type>[\w.]+)"""
        )

        val listTypeRegex = Regex(
            """(?<field>\w+)\s*=\s*(?:/\*\*\s*(?<package>[\w.]+)\s*\*\*/\s*)?List\((?<size>\d+)\)\s*\{\s*Any\(\)\s+as\s+(?<type>[\w.]+)\s*}"""

        )

        val matches = typeRegex.findAll(originalContent)

        matches.forEachIndexed { index, match ->
            val field = match.groups["field"]?.value ?: ""
            val pkg = match.groups["package"]?.value
            val typeName = match.groups["type"]?.value ?: ""
            try {
                val clazz = Class.forName(typeName, true, classLoader).kotlin
                val fakeFunction = fakeHelper::class.memberFunctions.find { it.name == "fake" }
                fakeFunction?.isAccessible = true
                val generator = if (pkg != null) {
                    println("Generating generator for pkg: $pkg")
                    Class.forName(pkg, true, classLoader).kotlin.createInstance()
                        .also {
                            println("Generator for $field $it")
                        }
                } else {
                    null
                }
                val instance = (fakeFunction!!.call(fakeHelper, clazz, 1, generator) as List<*>).first()!!
                val replacement = "$field = ${objectToString(instance)}"
                modifiedContent = modifiedContent.replace(match.value, replacement)

            } catch (e: Exception) {
                logger.error("Error generating instance for type: $typeName -> ${e.message}")
            }
        }



        // Process list type replacements
        listTypeRegex.findAll(modifiedContent).forEachIndexed { index, match ->
            val field = match.groups["field"]?.value ?: ""
            val pkg = match.groups["package"]?.value ?: "No package"
            val size = match.groups["size"]?.value?.toInt() ?: 1
            val typeName = match.groups["type"]?.value ?: ""

            println("field=$field pkg=$pkg size=$size typeName=$typeName")

            try {
                val clazz = Class.forName(typeName, true, classLoader).kotlin
                val fakeFunction = fakeHelper::class.memberFunctions.find { it.name == "fake" }
                fakeFunction?.isAccessible = true

                val instances = (fakeFunction!!.call(fakeHelper, clazz, size, null) as List<*>)

                val replacement = "$field = ${objectToString(instances)}"
                modifiedContent = modifiedContent.replace(match.value, replacement)

            } catch (e: Exception) {
                logger.error("Error generating instances for list of type: $typeName -> ${e.message}")
            }
        }

        generatedFile.writeText(modifiedContent)
    }
}

private fun Project.generateImportsInKspInjectorFiles(
    imports: List<String>,
) {
    val generatedFiles = getKspGeneratedInjectorFiles()

    generatedFiles.forEach { generatedFile ->
        val originalContent = generatedFile.readText()

        val packageRegex = Regex("""^package\s+[a-zA-Z0-9_.]+""", RegexOption.MULTILINE)
        val packageMatch = packageRegex.find(originalContent)
        val packageDeclaration = packageMatch?.value ?: "package com.reflect.instance.sample"

        val contentWithoutPackage = originalContent.replace(packageRegex, "").trim()

        val newImports = imports.joinToString("\n") { "import $it" }

        val newContent = buildString {
            appendLine(packageDeclaration)
            appendLine()
            appendLine(newImports)
            appendLine()
            append(contentWithoutPackage)
        }.trim()

        generatedFile.writeText(newContent)
        println("Generated File -> ${generatedFile.absolutePath}")
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