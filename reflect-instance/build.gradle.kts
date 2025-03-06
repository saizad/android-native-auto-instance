plugins {
    kotlin("jvm")
}


java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}


dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation("junit:junit:4.13.2")

}