package com.reflect.instance.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.register

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
