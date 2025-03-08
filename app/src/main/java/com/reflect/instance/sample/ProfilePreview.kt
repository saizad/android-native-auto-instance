package com.reflect.instance.sample

import com.auto.instance.plugin.models.school.School
import com.auto.instance.plugin.models.Token
import com.reflect.instance.annotations.AutoInject
import com.reflect.instance.annotations.InjectInstance

@InjectInstance
class ProfilePreview {

    @AutoInject(dataGenerator = "com.auto.instance.plugin.generator.SchoolDataGenerator")
    lateinit var school: School

    @AutoInject
    lateinit var token: Token

    @AutoInject
    lateinit var token1: Token
    @AutoInject(count = 12)
    lateinit var tkn: List<Token>
    @AutoInject(count = 5, dataGenerator = "com.auto.instance.plugin.generator.TokenDataGenerator")
    lateinit var tkn1: List<Token>
    @AutoInject(count = 2)
    lateinit var tkn2: List<Token>

    @AutoInject(dataGenerator = "com.auto.instance.plugin.generator.TokenDataGenerator")
    var refreshToken: Token? = null


} 