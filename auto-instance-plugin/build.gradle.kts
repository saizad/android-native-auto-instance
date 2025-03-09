plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("maven-publish")
}

group = "com.github.saizad"
version = "1.0.0"

// For JitPack compatibility
val gitVersionTag: String? = System.getenv("VERSION")
if (gitVersionTag != null) {
    version = gitVersionTag
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(gradleApi())
    implementation("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.0.21-1.0.27")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    testImplementation("junit:junit:4.13.2")
    testImplementation(gradleTestKit()) // Enables Gradle functional testing
    testImplementation(kotlin("test")) // Standard Kotlin test lib

}

gradlePlugin {
    plugins {
        create("autoInstancePlugin") {
            id = "com.reflect.instance.plugin"
            implementationClass = "com.reflect.instance.plugin.AutoInstancePlugin"
        }

        create("modelInstanceGeneratorPlugin") {
            id = "com.reflect.instance.model.plugin"
            implementationClass = "com.reflect.instance.plugin.ModelInstanceGeneratorPlugin"
        }
    }
    // Use the plugin maven publication
    isAutomatedPublishing = true
}

// Configure publishing for JitPack
publishing {
    publications {
        // Create a publication for JitPack
        create<MavenPublication>("jitpack") {
            groupId = "com.github.saizad.android-native-auto-instance"
            artifactId = "auto-instance-plugin"
            version = project.version.toString()
            
            from(components["java"])
            
            // Add pom information required by JitPack
            pom {
                name.set("Auto Instance Plugin")
                description.set("Gradle plugin for auto instance generation")
                url.set("https://github.com/saizad/android-native-auto-instance")
                
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                
                developers {
                    developer {
                        id.set("saizad")
                        name.set("Sa Zad")
                    }
                }
            }
        }
    }
    
    repositories {
        mavenLocal()
    }
}

// Add a task to explicitly publish the auto-instance-plugin to JitPack
tasks.register("publishPluginToJitPack") {
    dependsOn("publishJitpackPublicationToMavenLocal")
    doLast {
        println("Published auto-instance-plugin to JitPack")
    }
}

tasks.withType<Test> {
    testLogging {
        events("passed", "skipped", "failed")
    }
}

