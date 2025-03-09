package com.reflect.instance.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.register

open class ModelInstanceGeneratorExtension {
    var modelPackages: List<String> = emptyList()
}

class AutoInstancePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val logger = Logging.getLogger(AutoInstancePlugin::class.java)
        logger.info("Applying AutoInstancePlugin")

        project.plugins.apply("com.google.devtools.ksp")
        logger.info("Applied KSP plugin")

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

            generateTask.configure {
                mustRunAfter(kspTasks)
            }

            compileTasks.configureEach {
                dependsOn(generateTask)
            }

            logger.info("Model packages configured: ${extension.modelPackages}")
        }
    }
}
