plugins {
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":auto-instance-annotations"))
    implementation(project(":reflect-instance"))

    // Update this line with the compatible KSP version
    implementation("com.google.devtools.ksp:symbol-processing-api:2.0.21-1.0.27")
    
    // KotlinPoet for code generation
    implementation("com.squareup:kotlinpoet:1.14.2")
    implementation("com.squareup:kotlinpoet-ksp:1.14.2")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

}