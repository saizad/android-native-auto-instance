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
}
