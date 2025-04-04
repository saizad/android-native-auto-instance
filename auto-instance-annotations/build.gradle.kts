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
            artifactId = "auto-instance-annotations"
            version = project.version.toString()
            
            from(components["java"])
            
            // Add pom information required by JitPack
            pom {
                name.set("Auto Instance Annotations")
                description.set("Annotations for auto instance generation")
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