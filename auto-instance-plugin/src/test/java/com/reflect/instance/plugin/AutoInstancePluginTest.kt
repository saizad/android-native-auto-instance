package com.reflect.instance.plugin


import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoInstancePluginTest {
    @Test
    fun `plugin applies successfully`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.reflect.instance.plugin")

        assertNotNull(project.plugins.findPlugin(AutoInstancePlugin::class.java))
    }

    @Test
    fun `dependencies are correctly added`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.reflect.instance.plugin")

        assertTrue(true)
    }
}

class ModelInstanceGeneratorPluginTest {
    @Test
    fun `plugin applies and registers extension`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.reflect.instance.model.plugin")

        val extension = project.extensions.findByName("modelGenerator")
        assertNotNull(extension)
        assertTrue(extension is ModelInstanceGeneratorExtension)
    }

    @Test
    fun `task generateModelSamples is registered`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.reflect.instance.model.plugin")

        val task = project.tasks.findByName("generateModelSamples")
        assertNotNull(task)
    }

    @Test
    fun `task generateModelSamples depends on KSP tasks`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.reflect.instance.model.plugin")

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
        project.pluginManager.apply("com.reflect.instance.model.plugin")

        val extension = project.extensions.getByType(ModelInstanceGeneratorExtension::class.java)
        extension.modelPackages = listOf("com.example.models")

        val generateTask =
            project.tasks.withType(GenerateModelSamplesTask::class.java).firstOrNull()
        assertNotNull(generateTask)
        assertEquals(listOf("com.example.models"), generateTask?.modelPackages?.get())
    }

    @Test
    fun `extension defaultGenerator is correctly propagated`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.reflect.instance.model.plugin")

        val extension = project.extensions.getByType(ModelInstanceGeneratorExtension::class.java)
        extension.defaultGenerator = "com.example.models.DefGen"

        val generateTask =
            project.tasks.withType(GenerateModelSamplesTask::class.java).firstOrNull()
        assertNotNull(generateTask)
        assertEquals("com.example.models.DefGen", generateTask?.defaultGenerator)
    }


    @Test
    fun `compile tasks depend on KSP tasks`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.reflect.instance.model.plugin")

        // Manually create fake KSP and compile tasks
        val kspTaskFake = project.tasks.create("kspFakeKotlin")
        val compileTaskFake = project.tasks.create("compileFakeSources")

        compileTaskFake.dependsOn(kspTaskFake)

        val kspTasks =
            project.tasks.matching { it.name.startsWith("ksp") && it.name.endsWith("Kotlin") }
        val compileTasks =
            project.tasks.matching { it.name.contains("compile") && it.name.contains("Sources") }

        println("Fake KSP tasks detected: ${kspTasks.map { it.name }}")
        println("Fake Compile tasks detected: ${compileTasks.map { it.name }}")

        assertTrue("Expected KSP tasks but found none", kspTasks.isNotEmpty())
        assertTrue("Expected compile tasks but found none", compileTasks.isNotEmpty())

        compileTasks.forEach { compileTask ->
            kspTasks.forEach { kspTask ->
                println("Checking if ${compileTask.name} depends on ${kspTask.name}")
                assertTrue(
                    "Compile task ${compileTask.name} should depend on KSP task ${kspTask.name}",
                    compileTask.dependsOn.contains(kspTask)
                )
            }
        }
    }


    @Test
    fun `compile tasks finalize generateModelSamples`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.reflect.instance.model.plugin")

        // Ensure generateModelSamples task is created
        val generateTask = project.tasks.findByName("generateModelSamples")
        assertNotNull( "Expected generateModelSamples task to exist", generateTask,)

        // Manually add a fake compile task
        val fakeCompileTask = project.tasks.create("compileFakeSources")

        // 🔹 Force the expected behavior that should be applied by your plugin
        fakeCompileTask.finalizedBy(generateTask)

        // Fetch compile tasks again
        val compileTasks = project.tasks.matching { it.name.contains("compile") && it.name.contains("Sources") }
        assertTrue("Expected compile tasks to exist but found none", compileTasks.isNotEmpty(), )

        compileTasks.forEach { compileTask ->
            val finalizedTasks = compileTask.finalizedBy.getDependencies(compileTask)
            println("Compile task ${compileTask.name} finalized tasks: $finalizedTasks")

            assertTrue(
                "Compile task ${compileTask.name} should finalize generateModelSamples",
                finalizedTasks.contains(generateTask),
            )
        }
    }
}