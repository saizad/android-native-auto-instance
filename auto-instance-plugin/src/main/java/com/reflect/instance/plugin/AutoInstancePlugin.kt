package com.reflect.instance.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.dependencies

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
    }
}

// Extension function to make the dependencies block more readable
fun DependencyHandler.ksp(dependencyNotation: Any) {
    add("ksp", dependencyNotation)
} 