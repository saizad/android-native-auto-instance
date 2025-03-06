package com.reflect.instance.plugin

import org.gradle.api.Project
import org.gradle.api.internal.project.DefaultProject
import org.gradle.internal.impldep.org.testng.Assert.assertEquals
import org.gradle.internal.impldep.org.testng.Assert.assertNotNull
import org.gradle.internal.impldep.org.testng.Assert.assertTrue
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test

class AutoInstancePluginTest {
    private lateinit var project: Project

    @Before
    fun setup() {
        project = ProjectBuilder.builder().build()
        // Setup required projects that are referenced in the plugin
        project.allprojects {
            it.group = "com.reflect.instance.plugin"
        }
        val annotationsProject = ProjectBuilder.builder()
            .withName("auto-instance-annotations")
            .withParent(project)
            .build()
        val processorProject = ProjectBuilder.builder()
            .withName("auto-instance-processor")
            .withParent(project)
            .build()
        val reflectInstanceProject = ProjectBuilder.builder()
            .withName("reflect-instance")
            .withParent(project)
            .build()
    }

    @Test
    fun `plugin applies KSP plugin`() {
        // Apply the plugin
        project.plugins.apply(AutoInstancePlugin::class.java)

        // Verify KSP plugin is applied
        assertTrue(project.plugins.hasPlugin("com.google.devtools.ksp"),
            "KSP plugin should be applied")
    }

    @Test
    fun `plugin adds required dependencies`() {
        // Apply the plugin
        project.plugins.apply(AutoInstancePlugin::class.java)

        // Get implementation configuration
        val implementationConfig = project.configurations.findByName("implementation")
        assertNotNull(implementationConfig, "Implementation configuration should exist")

        // Get KSP configuration
        val kspConfig = project.configurations.findByName("ksp")
        assertNotNull(kspConfig, "KSP configuration should exist")

        // Check the dependencies were added correctly
        val dependencies = project.configurations.flatMap { it.dependencies }

        // Verify project dependencies
        assertTrue(dependencies.any {
            it.group == project.group.toString() &&
                    it.name == "auto-instance-annotations"
        }, "auto-instance-annotations dependency should be added")

        assertTrue(dependencies.any {
            it.group == project.group.toString() &&
                    it.name == "auto-instance-processor"
        }, "auto-instance-processor dependency should be added")

        assertTrue(dependencies.any {
            it.group == project.group.toString() &&
                    it.name == "reflect-instance"
        }, "reflect-instance dependency should be added")
    }

    @Test
    fun `plugin creates model generator extension`() {
        // Apply the plugin
        project.plugins.apply(AutoInstancePlugin::class.java)

        // Verify extension is created
        val extension = project.extensions.findByName("modelGenerator")
        assertNotNull(extension, "modelGenerator extension should be created")
        assertTrue(extension is ModelInstanceGeneratorExtension,
            "Extension should be of type ModelInstanceGeneratorExtension")
    }

    @Test
    fun `plugin registers generate model samples task`() {
        // Apply the plugin
        project.plugins.apply(AutoInstancePlugin::class.java)

        // Verify task is registered
        val generateTask = project.tasks.findByName("generateModelSamples")
        assertNotNull(generateTask, "generateModelSamples task should be registered")
        assertTrue(generateTask is GenerateModelSamplesTask,
            "Task should be of type GenerateModelSamplesTask")
    }

    @Test
    fun `plugin configures model packages from extension`() {
        // Apply the plugin
        project.plugins.apply(AutoInstancePlugin::class.java)

        // Configure extension
        val extension = project.extensions.getByType(ModelInstanceGeneratorExtension::class.java)
        extension.modelPackages = listOf("com.example.model", "com.example.data")

        // Force afterEvaluate actions to execute
        (project as? DefaultProject)?.evaluate()

        // Verify task configuration
        val generateTask = project.tasks.findByName("generateModelSamples") as GenerateModelSamplesTask
        assertEquals(listOf("com.example.model", "com.example.data"), generateTask.modelPackages.get(),
            "Task should be configured with model packages from extension")
    }

    @Test
    fun `plugin configures task dependencies correctly`() {
        // Apply Java plugin to create compile tasks
        project.plugins.apply("java")

        // Apply the plugin
        project.plugins.apply(AutoInstancePlugin::class.java)

        // Create a KSP task
        val kspTask = project.tasks.create("kspKotlin")

        // Force afterEvaluate actions to execute
        (project as? DefaultProject)?.evaluate()

        // Get compile tasks
        val compileKotlinTask = project.tasks.findByName("compileKotlin")
        assertNotNull(compileKotlinTask, "compileKotlin task should exist")

        // Verify task dependencies
        val compileDependsOn = compileKotlinTask!!.dependsOn
        assertTrue(compileDependsOn.contains(kspTask),
            "Compile task should depend on KSP task")

        // Verify mustRunAfter relationship
        val generateTask = project.tasks.findByName("generateModelSamples")
        assertNotNull(generateTask, "generateModelSamples task should exist")

        // Check finalizedBy relationship
        val finalizerTasks = compileKotlinTask.finalizedBy.getDependencies(compileKotlinTask)
        assertTrue(finalizerTasks.contains(generateTask),
            "Compile task should be finalized by generate task")
    }

    @Test
    fun `creates generated directory structure`() {
        // Apply the plugin
        project.plugins.apply(AutoInstancePlugin::class.java)

        // Verify directory structure
        val generatedDir = project.buildDir.resolve("generated/model-instances/src/main/kotlin")
        assertTrue(generatedDir.exists(), "Generated directory structure should be created")
    }
}