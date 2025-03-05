package com.reflect.instance.sample

fun main() {
    // Create an instance of ProfilePreview
    val profilePreview = ProfilePreview()

    // Use the generated injector to inject instances
    // After building the project, the following class will be generated:
    // ProfilePreviewInjector.inject(profilePreview)

    // For demonstration purposes, we'll manually set the properties
    // In a real application, you would use the generated injector
    profilePreview.profile = Profile(
        id = "123",
        name = "John Doe",
        email = "john.doe@example.com"
    )

    profilePreview.settings = Settings(
        darkMode = true,
        notificationsEnabled = true
    )

    // Display the profile
    profilePreview.displayProfile()
} 