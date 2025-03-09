package com.reflect.instance.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.kotlin.dsl.dependencies

open class ModelInstanceGeneratorExtension {
    var modelPackages: List<String> = emptyList()
}

class AutoInstancePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val logger = Logging.getLogger(AutoInstancePlugin::class.java)
        logger.info("Applying AutoInstancePlugin")

        project.plugins.apply("com.google.devtools.ksp")
        logger.info("Applied KSP plugin")

        try {
            project.dependencies {
                add("implementation", project.project(":auto-instance-annotations"))
                add("ksp", project.project(":auto-instance-processor"))
                add("implementation", project.project(":reflect-instance"))
            }
            logger.info("Dependencies added successfully")
        } catch (e: Exception) {
            logger.error("Failed to add dependencies", e)
        }
    }
}

class ModelInstanceGeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val logger = Logging.getLogger(ModelInstanceGeneratorPlugin::class.java)
        logger.info("Applying ModelInstanceGeneratorPlugin")

        try {
            val extension = project.extensions.create(
                "modelGenerator",
                ModelInstanceGeneratorExtension::class.java
            )
            logger.info("ModelInstanceGeneratorExtension registered")

            val generateTask =
                project.tasks.register("generateModelSamples", GenerateModelSamplesTask::class.java) {
                    modelPackages.set(extension.modelPackages)
                }
            logger.info("Task generateModelSamples registered")

            project.afterEvaluate {
                logger.info("After evaluate: configuring tasks")

                val compileTasks = project.tasks.matching { it.name.contains("compile") && it.name.contains("Sources") }
                val kspTasks = project.tasks.matching { it.name.startsWith("ksp") && it.name.endsWith("Kotlin") }

                if (compileTasks.isEmpty()) {
                    logger.warn("No compile tasks found. Ensure the project is correctly set up.")
                }

                if (kspTasks.isEmpty()) {
                    logger.warn("No KSP tasks found. Ensure KSP is correctly configured.")
                }

                compileTasks.filterNotNull().forEach { compileTask ->
                    val generateTaskInstance = generateTask.get()

                    // Ensure all KSP tasks run before compiling sources
                    kspTasks.forEach { kspTask ->
                        compileTask.dependsOn(kspTask)
                        compileTask.mustRunAfter(kspTask)
                    }

                    // Ensure generateModelSamples runs after compilation
                    generateTaskInstance.mustRunAfter(compileTask)
                    compileTask.finalizedBy(generateTaskInstance)
                }

                logger.info("Model packages configured: ${extension.modelPackages}")
            }
        } catch (e: Exception) {
            logger.error("Error applying ModelInstanceGeneratorPlugin", e)
        }
    }
}
