plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("maven-publish")
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
}

gradlePlugin {
    plugins {
        create("autoInstancePlugin") {
            id = "com.reflect.instance.plugin"
            implementationClass = "com.reflect.instance.plugin.AutoInstancePlugin"
        }

        create("ModelInstanceGeneratorPlugin") {
            id = "com.reflect.instance.plugin"
            implementationClass = "com.reflect.instance.plugin.ModelInstanceGeneratorPlugin"
        }
    }
}

