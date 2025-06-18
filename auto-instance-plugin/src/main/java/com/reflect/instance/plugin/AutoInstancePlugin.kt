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

        val generateTask =
            project.tasks.register<GenerateModelSamplesTask>("generateModelSamples") {
                defaultGenerator = extension.defaultGenerator
            }
        logger.info("Task generateModelSamples registered")

        project.afterEvaluate {
            setPluginRunSequence(generateTask)
        }

    }

    fun Project.setPluginRunSequence(generateTask: TaskProvider<GenerateModelSamplesTask>) {
        val compileKotlinTasks = tasks.matching {
            it.name.startsWith("compile") && it.name.contains("Kotlin")
        }

        val kspTasks = tasks.matching {
            it.name.startsWith("ksp") && it.name.endsWith("Kotlin")
        }

        val generateTaskInstance = generateTask.get()

        generateTaskInstance.mustRunAfter(kspTasks)
        generateTaskInstance.mustRunAfter(compileKotlinTasks)

        tasks.matching { it.name.contains("compile") && it.name.contains("Sources") }
            .forEach { compileSourcesTask ->
                compileSourcesTask.dependsOn(generateTaskInstance)
            }
    }

}
