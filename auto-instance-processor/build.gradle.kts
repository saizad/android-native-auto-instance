plugins {
    kotlin("jvm")
    id("maven-publish")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":auto-instance-annotations"))
    implementation(project(":reflect-instance"))

    implementation("com.google.devtools.ksp:symbol-processing-api:2.0.21-1.0.27")
    
    // KotlinPoet for code generation
    implementation("com.squareup:kotlinpoet:1.14.2")
    implementation("com.squareup:kotlinpoet-ksp:1.14.2")

    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.5.0")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:1.5.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-reflect:1.8.0")
}

// Set group and version explicitly for this project
group = "com.github.saizad"
version = "1.0.0"

// For JitPack compatibility
val gitVersionTag: String? = System.getenv("VERSION")
if (gitVersionTag != null) {
    version = gitVersionTag
}

// Configure publishing for JitPack
publishing {
    publications {
        create<MavenPublication>("jitpack") {
            groupId = "com.github.saizad.android-native-auto-instance"
            artifactId = "auto-instance-processor"
            version = project.version.toString()
            
            from(components["java"])
            
            // Add pom information required by JitPack
            pom {
                name.set("Auto Instance Processor")
                description.set("Processor for auto instance generation")
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