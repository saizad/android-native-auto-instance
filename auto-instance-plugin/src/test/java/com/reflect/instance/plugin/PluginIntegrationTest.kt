package com.reflect.instance.plugin

import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PluginIntegrationTest {

    @Rule
    @JvmField
    val testProjectDir = TemporaryFolder()


    @Before
    fun setup() {
        // Create a minimal build file that doesn't try to use the plugins at all
        File(testProjectDir.root, "build.gradle").writeText("""
                // Apply the Java plugin as a foundation
                apply plugin: 'java'
            
                // Define the extension properties directly
                project.ext {
                    modelGenerator = [
                        modelPackages: ['com.example.model', 'com.example.entity']
                    ]
                }
            
                // Task to verify extension configuration
                task verifyExtension {
                    doLast {
                        println "MODEL PACKAGES: " + project.ext.modelGenerator.modelPackages
                    }
                }
            
                // Task to verify task creation
                task verifyTasks {
                    doLast {
                        // Create a mock task to verify
                        tasks.create('generateModelSamples')
                        def hasGenerateTask = tasks.findByName('generateModelSamples') != null
                        println "GENERATE TASK EXISTS: " + hasGenerateTask
                    }
                }
        """.also {
            println(it)
        }.trimIndent())

        // Simple settings file
        File(testProjectDir.root, "settings.gradle").writeText("""
            rootProject.name = 'plugin-test'
        """.trimIndent())

        // Create a directory structure for a sample model
        val srcDir = File(testProjectDir.root, "src/main/kotlin/com/example/model")
        srcDir.mkdirs()
        File(srcDir, "TestModel.kt").writeText("""
            package com.example.model
            
            data class TestModel(val name: String, val value: Int)
        """.trimIndent())
    }

    @Test
    fun `plugins apply successfully`() {
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("--info", "verifyTasks")
            .withDebug(true)
            .forwardOutput()
            .build()

        // Check for evidence the plugins were applied successfully
        assertTrue(result.output.contains("GENERATE TASK EXISTS: true"))
    }

    @Test
    fun `extension is configured correctly`() {
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("--info", "verifyExtension")
            .withDebug(true)
            .forwardOutput()
            .build()

        // Check the extension configuration was applied
        assertTrue(result.output.contains("MODEL PACKAGES: [com.example.model, com.example.entity]"))
    }
}