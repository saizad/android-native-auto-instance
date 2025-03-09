package com.reflect.instance.plugin

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Integration tests for GenerateModelSamplesTask that test the actual Gradle task execution.
 *
 * This test requires setting up a complete Gradle project structure.
 */
class GenerateModelSamplesTaskIntegrationTest {

    @Rule
    @JvmField
    val testProjectDir = TemporaryFolder()

    @Test
    fun `test task execution with sample project`() {
        // Set up project structure
        setupSampleProject()

        // Run the Gradle task
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("generateModelSamples", "--stacktrace")
            .withPluginClasspath()
            .build()

        // Verify task execution
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateModelSamples")?.outcome)

        // Verify that injector files were modified
        val injectorFile = File(testProjectDir.root, "app/build/generated/ksp/debug/kotlin/SampleInjector.kt")
        val content = injectorFile.readText()

        // Check for imports
        assertTrue(content.contains("import com.example.model.UserModel"))
        assertTrue(content.contains("import com.example.model.ItemModel"))

        // Check that Any() instances were replaced
        assertTrue(!content.contains("Any() as com.example.model.UserModel"))
        assertTrue(!content.contains("Any() as com.example.model.ItemModel"))
    }

    private fun setupSampleProject() {
        // Create build.gradle.kts
        val buildFile = testProjectDir.newFile("build.gradle.kts")
        buildFile.writeText("""
            plugins {
                id("com.reflect.instance.plugin")
            }
            
            tasks.register<com.reflect.instance.plugin.GenerateModelSamplesTask>("generateModelSamples") {
                modelPackages.set(listOf("com.example.model"))
            }
        """)

        // Create settings.gradle.kts
        val settingsFile = testProjectDir.newFile("settings.gradle.kts")
        settingsFile.writeText("""
            rootProject.name = "test-project"
            include(":app")
        """)

        // Create app module
        val appDir = testProjectDir.newFolder("app")
        val appBuildFile = File(appDir, "build.gradle.kts")
        appBuildFile.writeText("""
            plugins {
                id("kotlin")
                id("com.google.devtools.ksp")
            }
        """)

        // Create model classes
        val modelDir = File(appDir, "src/main/kotlin/com/example/model")
        modelDir.mkdirs()

        File(modelDir, "UserModel.kt").writeText("""
            package com.example.model
            
            data class UserModel(
                val name: String,
                val email: String,
                val age: Int
            )
        """)

        File(modelDir, "ItemModel.kt").writeText("""
            package com.example.model
            
            data class ItemModel(
                val id: String,
                val name: String,
                val price: Double
            )
        """)

        // Create KSP output structure
        val kspDir = File(appDir, "build/generated/ksp/debug/kotlin")
        kspDir.mkdirs()

        // Create injector file
        File(kspDir, "SampleInjector.kt").writeText("""
            package com.example.generated
            
            class SampleInjector {
                fun provideModels() {
                    val user = Any() as com.example.model.UserModel
                    val items = List(2) { Any() as com.example.model.ItemModel }
                }
            }
        """)

        // Create compiled classes directories
        val classesDir = File(appDir, "build/classes/kotlin/main/com/example/model")
        classesDir.mkdirs()

        // Add mock class files
        File(classesDir, "UserModel.class").createNewFile()
        File(classesDir, "ItemModel.class").createNewFile()

        // Create mock AAR file
        val aarDir = File(testProjectDir.root, "reflect-instance/build/outputs/aar")
        aarDir.mkdirs()
        File(aarDir, "reflect-instance-debug.aar").createNewFile()

        // Create mock classes.jar inside extracted AAR location
        val extractedDir = File(testProjectDir.root, "build/extracted-aar")
        extractedDir.mkdirs()
        File(extractedDir, "classes.jar").createNewFile()
    }

    @Test
    fun `test task handles missing app project gracefully`() {
        // Create a minimal project without :app module
        val buildFile = testProjectDir.newFile("build.gradle.kts")
        buildFile.writeText("""
            plugins {
                id("com.reflect.instance.plugin")
            }
            
            tasks.register<com.reflect.instance.plugin.GenerateModelSamplesTask>("generateModelSamples") {
                modelPackages.set(listOf("com.example.model"))
            }
        """)

        val settingsFile = testProjectDir.newFile("settings.gradle.kts")
        settingsFile.writeText("""
            rootProject.name = "test-project"
            // No app module included
        """)

        // Run the task - it should not fail but log a warning
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("generateModelSamples", "--stacktrace")
            .withPluginClasspath()
            .build()

        // Task should still succeed, just not do anything
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateModelSamples")?.outcome)
        assertTrue(result.output.contains("App project not found"))
    }

    @Test
    fun `test task handles no class directories gracefully`() {
        // Setup project structure without compiled classes
        val buildFile = testProjectDir.newFile("build.gradle.kts")
        buildFile.writeText("""
            plugins {
                id("com.reflect.instance.plugin")
            }
            
            tasks.register<com.reflect.instance.plugin.GenerateModelSamplesTask>("generateModelSamples") {
                modelPackages.set(listOf("com.example.model"))
            }
        """)

        val settingsFile = testProjectDir.newFile("settings.gradle.kts")
        settingsFile.writeText("""
            rootProject.name = "test-project"
            include(":app")
        """)

        // Create app module without any compiled classes
        val appDir = testProjectDir.newFolder("app")
        val appBuildFile = File(appDir, "build.gradle.kts")
        appBuildFile.writeText("""
            plugins {
                id("kotlin")
            }
        """)

        // Run the task - it should not fail but log a warning
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("generateModelSamples", "--stacktrace")
            .withPluginClasspath()
            .build()

        // Task should still succeed, just not do anything useful
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateModelSamples")?.outcome)
        assertTrue(result.output.contains("No class directories found"))
    }

    @Test
    fun `test task handles missing AAR file gracefully`() {
        // Setup basic project structure without the AAR file
        setupSampleProject()

        // Delete the AAR file to simulate it missing
        val aarFile = File(testProjectDir.root, "reflect-instance/build/outputs/aar/reflect-instance-debug.aar")
        aarFile.delete()

        // Run the task - should fail with a useful error
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("generateModelSamples", "--stacktrace")
            .withPluginClasspath()
            .buildAndFail() // We expect failure here

        // Verify the failure message
        assertTrue(result.output.contains("AAR file not found"))
    }

    @Test
    fun `test task handles various model types`() {
        // Setup project with different model types
        setupProjectWithVariousModels()

        // Run the Gradle task
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("generateModelSamples", "--stacktrace")
            .withPluginClasspath()
            .build()

        // Verify task execution
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateModelSamples")?.outcome)

        // Verify that injector files were modified
        val injectorFile = File(testProjectDir.root, "app/build/generated/ksp/debug/kotlin/ComplexInjector.kt")
        val content = injectorFile.readText()

        // Check that all model types were handled
        assertTrue(content.contains("import com.example.model.SimpleModel"))
        assertTrue(content.contains("import com.example.model.ComplexModel"))
        assertTrue(content.contains("import com.example.model.GenericModel"))
    }

    private fun setupProjectWithVariousModels() {
        // Basic project setup similar to setupSampleProject()
        val buildFile = testProjectDir.newFile("build.gradle.kts")
        buildFile.writeText("""
            plugins {
                id("com.reflect.instance.plugin")
            }
            
            tasks.register<com.reflect.instance.plugin.GenerateModelSamplesTask>("generateModelSamples") {
                modelPackages.set(listOf("com.example.model"))
            }
        """)

        val settingsFile = testProjectDir.newFile("settings.gradle.kts")
        settingsFile.writeText("""
            rootProject.name = "test-project"
            include(":app")
        """)

        // Create app module
        val appDir = testProjectDir.newFolder("app")
        val appBuildFile = File(appDir, "build.gradle.kts")
        appBuildFile.writeText("""
            plugins {
                id("kotlin")
                id("com.google.devtools.ksp")
            }
        """)

        // Create model classes with different types
        val modelDir = File(appDir, "src/main/kotlin/com/example/model")
        modelDir.mkdirs()

        File(modelDir, "SimpleModel.kt").writeText("""
            package com.example.model
            
            data class SimpleModel(val name: String, val value: Int)
        """)

        File(modelDir, "ComplexModel.kt").writeText("""
            package com.example.model
            
            data class ComplexModel(
                val id: String,
                val nestedMap: Map<String, List<Int>>,
                val flags: Set<String>
            )
        """)

        File(modelDir, "GenericModel.kt").writeText("""
            package com.example.model
            
            data class GenericModel<T>(
                val data: T,
                val metadata: Map<String, String>
            )
        """)

        // Create KSP output structure
        val kspDir = File(appDir, "build/generated/ksp/debug/kotlin")
        kspDir.mkdirs()

        // Create injector file with different model types
        File(kspDir, "ComplexInjector.kt").writeText("""
            package com.example.generated
            
            class ComplexInjector {
                fun provideModels() {
                    val simple = Any() as com.example.model.SimpleModel
                    val complex = Any() as com.example.model.ComplexModel
                    val generic = Any() as com.example.model.GenericModel<String>
                    val simpleList = List(3) { Any() as com.example.model.SimpleModel }
                }
            }
        """)

        // Create compiled classes directories
        val classesDir = File(appDir, "build/classes/kotlin/main/com/example/model")
        classesDir.mkdirs()

        // Add mock class files
        File(classesDir, "SimpleModel.class").createNewFile()
        File(classesDir, "ComplexModel.class").createNewFile()
        File(classesDir, "GenericModel.class").createNewFile()

        // Create mock AAR file
        val aarDir = File(testProjectDir.root, "reflect-instance/build/outputs/aar")
        aarDir.mkdirs()
        File(aarDir, "reflect-instance-debug.aar").createNewFile()

        // Create mock classes.jar inside extracted AAR location
        val extractedDir = File(testProjectDir.root, "build/extracted-aar")
        extractedDir.mkdirs()
        File(extractedDir, "classes.jar").createNewFile()
    }
}