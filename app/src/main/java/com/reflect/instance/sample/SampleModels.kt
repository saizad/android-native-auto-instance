package com.reflect.instance.sample

data class Profile(
    val id: String,
    val name: String,
    val email: String
)

data class Settings(
    val darkMode: Boolean,
    val notificationsEnabled: Boolean
) 