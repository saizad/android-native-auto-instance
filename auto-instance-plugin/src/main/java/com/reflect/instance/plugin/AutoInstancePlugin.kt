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
        // Apply KSP plugin
        project.plugins.apply("com.google.devtools.ksp")
        
        // Add dependencies
        project.dependencies {
            add("implementation", project.project(":auto-instance-annotations"))
            add("ksp", project.project(":auto-instance-processor"))
            add("implementation", project.project(":reflect-instance"))
        }

        val extension =
            project.extensions.create("modelGenerator", ModelInstanceGeneratorExtension::class.java)

        // Register the directory where generated files will be stored
        val generatedDir = project.buildDir.resolve("generated/model-instances")

        generatedDir.resolve("src/main/kotlin").mkdirs()

        project.afterEvaluate {
            project.afterEvaluate {
                project.tasks.matching { it.name == "compileDebugSources" }.configureEach {
                    dependsOn("kspDebugKotlin")
                    mustRunAfter("kspDebugKotlin")
                }
            }
        }

        val generateTask =
            project.tasks.register("generateModelSamples", GenerateModelSamplesTask::class.java) {
                modelPackages.set(extension.modelPackages)
            }

        project.afterEvaluate {
            val compileTaskName = "compileDebugSources"
            val compileTask = project.tasks.findByName(compileTaskName)
            if (compileTask != null) {
                val generateTaskInstance = generateTask.get()
                generateTaskInstance.mustRunAfter(compileTask)
                compileTask.finalizedBy(generateTaskInstance)
            }
        }
    }
}

// Extension function to make the dependencies block more readable
fun DependencyHandler.ksp(dependencyNotation: Any) {
    add("ksp", dependencyNotation)
} 