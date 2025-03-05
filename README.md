# Auto Instance Plugin

A Kotlin Symbol Processing (KSP) plugin that automatically injects instances into classes marked with `@InjectInstance` annotation.

## Setup

1. Add the plugin to your project's `settings.gradle.kts` file:

```kotlin
pluginManagement {
    repositories {
        // ... other repositories
        includeBuild("path/to/auto-instance-plugin")
    }
}
```

2. Apply the plugin in your app's `build.gradle.kts` file:

```kotlin
plugins {
    // ... other plugins
    id("com.reflect.instance.plugin")
}
```

## Usage

1. Mark your class with `@InjectInstance` annotation:

```kotlin
@InjectInstance
class ProfilePreview {
    @AutoInject
    lateinit var profile: Profile
    
    @AutoInject
    lateinit var settings: Settings
}
```

2. Use the generated injector to inject instances:

```kotlin
val profilePreview = ProfilePreview()
ProfilePreviewInjector.inject(profilePreview)

// Now profilePreview.profile and profilePreview.settings are initialized
```

## How it works

The plugin uses KSP to generate injector classes for each class marked with `@InjectInstance`. The injector uses `FakeHelper` to create instances of the properties marked with `@AutoInject`.

## Project Structure

- `auto-instance-annotations`: Contains the annotations used to mark classes and properties for injection
- `auto-instance-processor`: Contains the KSP processor that generates the injector classes
- `auto-instance-plugin`: Contains the Gradle plugin that applies the KSP processor to your project
- `reflect-instance`: Contains the `FakeHelper` class that creates instances of the properties 