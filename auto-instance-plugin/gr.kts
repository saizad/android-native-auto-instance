buildscript {
    dependencies {
        // This is needed to make the plugin classes available to the script
        classpath(files(System.getProperty("java.class.path").split(File.pathSeparator).joinToString(", ") { "'$it'" }))
    }
}


plugins {
    id("com.reflect.instance.plugin")
    id("com.reflect.instance.model.plugin")
}

// Comment out the modelGenerator block for now
modelGenerator {
    modelPackages = listOf(
        "com.example.model",
        "com.example.entity",
    )
}

repositories {
    mavenCentral()
    google()
}

// Task to verify extension configuration
tasks.register("verifyExtension") {
    doLast {
        println("MODEL PACKAGES: " + extensions.getByType(ModelGeneratorExtension::class.java).modelPackages)
    }
}

// Task to verify task creation
tasks.register("verifyTasks") {
    doLast {
        val hasGenerateTask = tasks.findByName("generateModelSamples") != null
        println("GENERATE TASK EXISTS: $hasGenerateTask")
    }
}