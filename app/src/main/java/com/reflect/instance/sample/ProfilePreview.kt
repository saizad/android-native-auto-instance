package com.reflect.instance.sample

import com.auto.instance.plugin.models.Profile
import com.auto.instance.plugin.models.school.School
import com.auto.instance.plugin.models.Token
import com.reflect.instance.annotations.AutoInject
import com.reflect.instance.annotations.InjectInstance

@InjectInstance
class ProfilePreview {

    @AutoInject()
    var refreshToken: Token? = null

} 