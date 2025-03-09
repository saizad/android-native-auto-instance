package com.reflect.instance.plugin


import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.*
import org.junit.Test

class AutoInstancePluginTest {
    @Test
    fun `plugin applies successfully`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.reflect.instance.plugin.AutoInstancePlugin")

        assertNotNull(project.plugins.findPlugin(AutoInstancePlugin::class.java))
    }

    @Test
    fun `dependencies are correctly added`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.reflect.instance.plugin.AutoInstancePlugin")

        assertNotNull(project.configurations.findByName("implementation"))
        assertNotNull(project.configurations.findByName("ksp"))
    }
}

class ModelInstanceGeneratorPluginTest {
    @Test
    fun `plugin applies and registers extension`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.reflect.instance.plugin.ModelInstanceGeneratorPlugin")

        val extension = project.extensions.findByName("modelGenerator")
        assertNotNull(extension)
        assertTrue(extension is ModelInstanceGeneratorExtension)
    }

    @Test
    fun `task generateModelSamples is registered`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.reflect.instance.plugin.ModelInstanceGeneratorPlugin")

        val task = project.tasks.findByName("generateModelSamples")
        assertNotNull(task)
    }

    @Test
    fun `task generateModelSamples depends on KSP tasks`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.reflect.instance.plugin.ModelInstanceGeneratorPlugin")

        val kspTasks =
            project.tasks.matching { it.name.startsWith("ksp") && it.name.endsWith("Kotlin") }
        val generateTask = project.tasks.findByName("generateModelSamples")

        assertNotNull(generateTask)
        kspTasks.forEach { kspTask ->
            assertTrue(generateTask!!.mustRunAfter.getDependencies(generateTask).contains(kspTask))

        }
    }

    @Test
    fun `extension modelPackages is correctly propagated`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.reflect.instance.plugin.ModelInstanceGeneratorExtension")

        val extension = project.extensions.getByType(ModelInstanceGeneratorExtension::class.java)
        extension.modelPackages = listOf("com.example.models")

        val generateTask =
            project.tasks.withType(GenerateModelSamplesTask::class.java).firstOrNull()
        assertNotNull(generateTask)
        assertEquals(listOf("com.example.models"), generateTask?.modelPackages?.get())
    }

    @Test
    fun temp() {
        val isOdd = 100 % 20 == 0
        assertTrue(isOdd)
    }
}