package com.reflect.instance.plugin

import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ModelInstanceGeneratorPluginTest1 {
    @Test
    fun `plugin applies and registers extension`() {
        val project = ProjectBuilder.builder().build()

        // ✅ Apply the plugin
        project.pluginManager.apply("com.reflect.instance.model.plugin")

        // ✅ Verify the extension is registered
        val extension = project.extensions.findByName("modelGenerator")
        assertNotNull(extension, "Expected modelGenerator extension to be registered")
        assertTrue(extension is ModelInstanceGeneratorExtension)
    }

    @Test
    fun `plugin registers generateModelSamples task`() {
        val project = ProjectBuilder.builder().build()

        // ✅ Apply the plugin
        project.pluginManager.apply("com.reflect.instance.model.plugin")

        // ✅ Verify the task exists
        val generateTask = project.tasks.findByName("generateModelSamples")
        assertNotNull(generateTask, "Expected generateModelSamples task to be registered")
    }

//    @Test
    fun `plugin correctly configures task dependencies`() {
        println("~~~~~~~~~~~~")

        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.reflect.instance.model.plugin")

        // Manually create fake KSP and compile tasks
        val kspTaskFake = project.tasks.create("kspFakeKotlin")
        val compileTaskFake = project.tasks.create("compileFakeSources")


        compileTaskFake.dependsOn(kspTaskFake)
        // ✅ Fetch registered tasks
        val kspTasks = project.tasks.matching { it.name.startsWith("ksp") && it.name.endsWith("Kotlin") }
        val compileTasks = project.tasks.matching { it.name.contains("compile") && it.name.contains("Sources") }
        val generateTask = project.tasks.findByName("generateModelSamples")

        project.tasks.forEach {
            println(it.name.prependIndent(">>> "))
        }
        assertNotNull(generateTask, "Expected generateModelSamples task to be registered")
        assertTrue(kspTasks.isNotEmpty(), "Expected at least one KSP task")
        assertTrue(compileTasks.isNotEmpty(), "Expected at least one compile task")

        // ✅ KSP tasks must complete before Compile tasks
        compileTasks.forEach { compileTask ->
            kspTasks.forEach { kspTask ->
                assertTrue(
                    compileTask.mustRunAfter.getDependencies(compileTask).contains(kspTask),
                    "Compile task ${compileTask.name} should mustRunAfter KSP task ${kspTask.name}"
                )
            }
        }

        // ✅ Compile tasks finalize Generate task
        compileTasks.forEach { compileTask ->
            assertTrue(
                compileTask.finalizedBy.getDependencies(compileTask).contains(generateTask),
                "Compile task ${compileTask.name} should finalize generateModelSamples"
            )
        }

        // ✅ Generate task must run after Compile tasks
        compileTasks.forEach { compileTask ->
            assertTrue(
                generateTask!!.mustRunAfter.getDependencies(generateTask).contains(compileTask),
                "generateModelSamples should mustRunAfter compile task ${compileTask.name}"
            )
        }
    }
}
