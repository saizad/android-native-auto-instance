plugins {
    `kotlin-dsl`
}

group = "com.auto.instance.plugin"
version = "1.0.0"

gradlePlugin {
    plugins {
        create("ModelInstanceGeneratorPlugin") {
            id = "com.auto.instance.plugin"
            implementationClass = "com.auto.instance.plugin.ModelInstanceGeneratorPlugin"
        }
    }
}

dependencies {
    compileOnly(gradleApi())
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.0.21")
    implementation("com.android.tools.build:gradle:8.5.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlin:kotlin-parcelize-runtime:2.0.21")
}

