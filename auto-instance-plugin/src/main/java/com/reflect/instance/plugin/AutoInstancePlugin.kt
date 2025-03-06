package com.reflect.instance.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.dependencies

open class ModelInstanceGeneratorExtension {
    var modelPackages: List<String> = emptyList()
}

class AutoInstancePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("com.google.devtools.ksp")
        
        project.dependencies {
            add("implementation", project.project(":auto-instance-annotations"))
            add("ksp", project.project(":auto-instance-processor"))
            add("implementation", project.project(":reflect-instance"))
        }

        val extension =
            project.extensions.create("modelGenerator", ModelInstanceGeneratorExtension::class.java)

        val generateTask =
            project.tasks.register("generateModelSamples", GenerateModelSamplesTask::class.java) {
                modelPackages.set(extension.modelPackages)
            }

        project.afterEvaluate {
            val compileTasks = project.tasks.matching { it.name.contains("compile") && it.name.contains("Sources") }
            val kspTasks = project.tasks.matching { it.name.startsWith("ksp") && it.name.endsWith("Kotlin") }

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
        }

    }
}