package com.auto.instance.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import java.io.File
import java.net.URLClassLoader
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

//import com.auto.instance.fake

data class MyTestClass(val name: String)


fun findCompiledClassDirectories(appProject: Project): List<File> {
    val outputDirs = mutableSetOf<File>()
//    val pa: Parcelable =

    appProject.tasks.withType(KotlinCompilationTask::class.java).forEach { task ->
        outputDirs.addAll(task.outputs.files.files)
    }

    appProject.tasks.withType(JavaCompile::class.java).forEach { task ->
        outputDirs.add(task.destinationDirectory.asFile.get())
    }

    return outputDirs.filter { it.exists() }
}


open class ModelInstanceGeneratorExtension {
    var modelPackages: List<String> = emptyList()
}

class ModelInstanceGeneratorPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        val extension =
            project.extensions.create("modelGenerator", ModelInstanceGeneratorExtension::class.java)

        // Register the directory where generated files will be stored
        val generatedDir = project.buildDir.resolve("generated/model-instances")

        generatedDir.resolve("src/main/kotlin").mkdirs()

        // Register the generate task
        val generateTask =
            project.tasks.register("generateModelSamples", GenerateModelSamplesTask::class.java) {
                println("xxxx -> ${generatedDir.absolutePath}")
                modelPackages.set(extension.modelPackages)
                outputDirectory.set(generatedDir)
            }

        // Register the clean task for generated files
        val cleanGeneratedModelTask = project.tasks.register("cleanGeneratedModelSamples") {
            group = "build"
            description = "Cleans the generated model sample instances"

            doLast {
                if (generatedDir.exists()) {
                    project.logger.lifecycle("Cleaning generated model samples at: ${generatedDir.absolutePath}")
                    generatedDir.deleteRecursively()
                } else {
                    project.logger.lifecycle("No generated model samples to clean at: ${generatedDir.absolutePath}")
                }
            }
        }

        // Find the app project
        val appProject = project.rootProject.findProject(":app")


        project.afterEvaluate {
            val compileTaskName = "compileDebugSources"
            val compileTask = project.tasks.findByName(compileTaskName)

            if (compileTask != null) {
                val generateTaskInstance = generateTask.get()

                generateTaskInstance.mustRunAfter(compileTask)
                project.logger.lifecycle("Configured $generateTaskInstance to run after $compileTaskName")
                project.logger.lifecycle(
                    "$compileTaskName contains = ${
                        project.gradle.startParameter.taskNames.contains(
                            compileTaskName
                        )
                    }"
                )

                compileTask.finalizedBy(generateTaskInstance)
            }
        }
    }
}

abstract class GenerateModelSamplesTask : DefaultTask() {
    @get:Input
    abstract val modelPackages: ListProperty<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

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

        val generatedDir = appProject.buildDir.resolve("generated/model-instances")
        val sourceDir = generatedDir.resolve("src/main/kotlin")

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

        val aarFile =
            project.rootProject.file("reflect-instance/build/outputs/aar/reflect-instance-debug.aar")
        val tempDir = project.rootProject.buildDir.resolve("extracted-aar")

        if (!aarFile.exists()) {
            throw GradleException("AAR file not found. Run ':reflect-instance:assembleDebug' first.")
        }

        // Extract AAR
        project.copy {
            from(project.zipTree(aarFile))
            into(tempDir)
        }

        // Load classes.jar dynamically
        val classesJar = tempDir.resolve("classes.jar")
        if (classesJar.exists()) {
            val fakeHelperClassLoader =
                URLClassLoader(arrayOf(classesJar.toURI().toURL()), javaClass.classLoader)
            val targetClass = fakeHelperClassLoader.loadClass("com.reflect.instance.FakeHelper")
            val instance = targetClass.getDeclaredConstructor().newInstance()

            println("Loaded class: ${instance.javaClass.name}")
            modelClassesByPackage.forEach { (modelPackage, classNames) ->
                appProject.generateSampleInstancesForClass(
                    modelPackage,
                    classNames,
                    instance,
                    classLoader,
                    outputDirectory.get().asFile
                )
            }
        } else {
            throw GradleException("classes.jar not found inside extracted AAR!")
        }


    }
}

private fun Project.generateSampleInstancesForClass(
    modelPackage: String,
    classNames: MutableList<String>,
    fakeHelper: Any,
    classLoader: URLClassLoader,
    outputDirectory: File
) {
    classNames.forEach { className ->
        try {
            val fullClassName = "$modelPackage.$className"
            val clazz = Class.forName(fullClassName, true, classLoader).kotlin
            val sourceDir = outputDirectory.resolve("src/main/kotlin")

            val packageDir = sourceDir.resolve(modelPackage.replace('.', '/'))
            packageDir.mkdirs()

            val sampleInstancesFile = packageDir.resolve("${className}Sample.kt")

            val fileContent = buildString {
                appendLine("package $modelPackage")
                appendLine()
                appendLine("// Auto-generated sample instances for $className")
                appendLine("// Generated on: ${java.time.LocalDateTime.now()}")
                appendLine()
                appendLine("object ${className}Sample {")

                val fakeFunction = fakeHelper::class.memberFunctions.find { it.name == "fake" }
                fakeFunction?.isAccessible = true

                val instances = fakeFunction!!.call(fakeHelper, clazz, 10) as List<Any>

                instances.forEachIndexed { index, instance ->
                    val instanceName = "sample${index + 1}"
                    appendLine("    val $instanceName: $className = ${objectToString(instance)}")
                }

                appendLine("    val allSamples: List<$className> = listOf(${instances.indices.joinToString { "sample${it + 1}" }})")
                appendLine("}")
            }

            sampleInstancesFile.writeText(fileContent)
            logger.lifecycle("Generated: ${sampleInstancesFile.absolutePath}")

        } catch (e: NoClassDefFoundError) {
            logger.warn("Skipping class $className due to missing dependency: ${e.message}")
        } catch (e: Exception) {
            logger.error("Error processing class $className: ${e.message}")
            e.printStackTrace()
        }
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