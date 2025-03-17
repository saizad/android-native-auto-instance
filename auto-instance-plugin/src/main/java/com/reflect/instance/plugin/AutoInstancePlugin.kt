package com.reflect.instance.plugin

import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

open class ModelInstanceGeneratorExtension {
    var modelPackages: List<String> = emptyList()
    var defaultGenerator: String? = null
}

class AutoInstancePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val logger = Logging.getLogger(AutoInstancePlugin::class.java)
        logger.info("Applying AutoInstancePlugin")

        project.plugins.apply("com.google.devtools.ksp")
        logger.info("Applied KSP plugin")

        project.plugins.withId("com.google.devtools.ksp") {
            project.extensions.configure<KspExtension> {
                arg("gradleTaskName", project.gradle.startParameter.taskNames.firstOrNull() ?: "")
                arg("rootDir", project.rootDir.absolutePath)
            }
        }

        project.dependencies {
            if (project.findProject(":auto-instance-annotations") != null) {
                add("implementation", project.project(":auto-instance-annotations"))
            }
            if (project.findProject(":auto-instance-processor") != null) {
                add("ksp", project.project(":auto-instance-processor"))
            }
            if (project.findProject(":reflect-instance") != null) {
                add("implementation", project.project(":reflect-instance"))
            }
        }
        logger.info("Dependencies added successfully")
    }
}

class ModelInstanceGeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val logger = Logging.getLogger(ModelInstanceGeneratorPlugin::class.java)
        logger.info("Applying ModelInstanceGeneratorPlugin")

        val extension = project.extensions.create<ModelInstanceGeneratorExtension>("modelGenerator")
        logger.info("ModelInstanceGeneratorExtension registered")

        val generateTask = project.tasks.register<GenerateModelSamplesTask>("generateModelSamples") {
            modelPackages.set(extension.modelPackages)
            defaultGenerator = extension.defaultGenerator
        }
        logger.info("Task generateModelSamples registered")

        project.afterEvaluate {
            setPluginRunSequence(generateTask)
        }

    }
}

fun Project.setPluginRunSequence(generateTask: TaskProvider<GenerateModelSamplesTask>) {

    val kspGeneratedDir = project.layout.buildDirectory.get().asFile.resolve("generated/ksp")
    val backupDir = file("$rootDir/ksp_backup") // Store outside `build/`
    println("kspDir=${kspGeneratedDir.absolutePath}")
    logger.lifecycle("backupDir=${backupDir.absolutePath}")
    // Make sure KSP processor only runs for compile sources
    project.tasks.forEach {
        val cmd = project.gradle.startParameter.taskNames.firstOrNull() ?: ""
        val compileTask =
            cmd.contains("compile")
                    && cmd.contains("Sources")
                    && !cmd.contains("Test")
        val regex = Regex("ksp(\\w+)(AndroidTest|UnitTest)?Kotlin")

        if (regex.containsMatchIn(it.name) && !compileTask) {
            logger.lifecycle("Matched Task: ${it.name} calledBy=$cmd")
            if (kspGeneratedDir.exists() && cmd != "clean" && cmd != "build" && cmd.isNotEmpty()) {
                backupDir.deleteRecursively()
                kspGeneratedDir.copyRecursively(backupDir, overwrite = true)
                kspGeneratedDir.deleteRecursively()
                logger.lifecycle("♻️ Cleaned for recycle")
            }
        }
    }

    val compileTasks = project.tasks
        .matching { it.name.contains("compile") && it.name.contains("Sources") }
        .filter { it.name.contains(getBuildVariant()) }
        .filter { !it.name.contains("Test") }

    val kspTasks = project.tasks
        .matching { it.name.startsWith("ksp") && it.name.endsWith("Kotlin") }
        .filter { it.name.contains(getBuildVariant()) }
        .filter { !it.name.contains("Test") }


    val generateTaskInstance = generateTask.get()

    compileTasks.filterNotNull().forEach { compileTask ->
        // Ensure all KSP tasks run before compiling sources
        kspTasks.forEach { kspTask ->
            compileTask.dependsOn(kspTask)
            compileTask.mustRunAfter(kspTask)
        }

        // Ensure generateTask runs after compilation
        generateTaskInstance.mustRunAfter(compileTask)
        compileTask.finalizedBy(generateTaskInstance)
    }

}
