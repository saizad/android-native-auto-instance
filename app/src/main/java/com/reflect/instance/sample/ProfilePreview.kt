package com.reflect.instance.sample

import com.auto.instance.plugin.models.School
import com.reflect.instance.annotations.AutoInject
import com.reflect.instance.annotations.InjectInstance

@InjectInstance
class ProfilePreview {
    @AutoInject
    lateinit var profile: Profile
    
    @AutoInject
    lateinit var settings: Settings

    @AutoInject
    lateinit var school: School
    
    fun displayProfile() {
        println("Profile: ${profile.name} (${profile.email})")
        println("Dark Mode: ${settings.darkMode}")
    }
} 