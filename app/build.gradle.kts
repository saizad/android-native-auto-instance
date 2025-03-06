plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
    id("com.reflect.instance.plugin")
    id("com.reflect.instance.model.plugin")
    alias(libs.plugins.kotlin.compose)
}

// Apply the plugin manually
// apply(plugin = "com.reflect.instance.plugin")

// Comment out the modelGenerator block for now
modelGenerator {
    modelPackages = listOf(
        "com.auto.instance.plugin.models",
    )
}

android {
    namespace = "com.reflect.instance.sample"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.reflect.instance.sample"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    flavorDimensions += "default"

    productFlavors {
        create("fakeData") {
            versionName = "fake-data"
            applicationIdSuffix = ".fake.data"
        }

        create("dev") {
            versionName = "dev"
            applicationIdSuffix = ".dev"
        }
    }

}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlin.parcelize.runtime)

    testImplementation(libs.kotlin.reflect)
    implementation(libs.jetbrains.kotlin.reflect)
}