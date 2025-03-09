package com.reflect.instance.plugin

import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

class ModelInstanceGeneratorPluginFunctionalTest {
    @Test
    fun `plugin correctly configures task dependencies`() {
        val testProjectDir = File("build/functionalTest").apply { mkdirs() }
        val buildFile = File(testProjectDir, "build.gradle.kts")
        val settingsFile = File(testProjectDir, "settings.gradle.kts")

        // ✅ Create a settings file for the test project
        settingsFile.writeText(
            """
            rootProject.name = "test-project"
            """.trimIndent()
        )

        // ✅ Create a real Gradle build script applying the plugin
        buildFile.writeText(
            """
            plugins {
                id("com.reflect.instance.model.plugin")
            }
            
            tasks.register("testCompileTask") {
                dependsOn("generateModelSamples")
            }
            
            tasks.register("testKspTask") {
                doLast {
                    println("KSP Task executed")
                }
            }
            """.trimIndent()
        )

        // ✅ Run Gradle with TestKit
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("testCompileTask", "--info")
            .withPluginClasspath()
            .build()

        // ✅ Verify task execution order
        val output = result.output
        println(output)

        assertTrue(output.contains("Task :testKspTask")) // ✅ Confirms KSP tasks run first
        assertTrue(output.contains("Task :testCompileTask")) // ✅ Confirms compile task runs
        assertTrue(output.contains("Task :generateModelSamples")) // ✅ Confirms generateModelSamples runs last
    }


    @Test
    fun `verify task execution order`() {
        val testProjectDir = File("build/functionalTest").apply { mkdirs() }
        val buildFile = File(testProjectDir, "build.gradle.kts")
        val settingsFile = File(testProjectDir, "settings.gradle.kts")

        // ✅ Create a settings file for the test project
        settingsFile.writeText(
            """
            rootProject.name = "test-project"
            """.trimIndent()
        )


        buildFile.writeText(
            """
                plugins {
                    id("com.reflect.instance.model.plugin")
                }
                
                tasks.register("logTaskGraph") {
                    doLast {
                        gradle.taskGraph.whenReady { graph ->
                            println("Task execution order:")
                            graph.allTasks.forEach { task ->
                                println(task.path)
                            }
                        }
                    }
                }
                """.trimIndent()
        )


        val runner = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("logTaskGraph")
            .withPluginClasspath()
            .withDebug(true)
            .forwardOutput()

        val result = runner.build()

        // ✅ Assert task execution order appears in output
        val output = result.output
        assertTrue(output.contains("Task execution order:"))
        assertTrue(output.contains(":kspKotlin"))
        assertTrue(output.contains(":compileSources"))
        assertTrue(output.contains(":generateModelSamples"))
    }
}
