package com.reflect.instance.sample

import com.auto.instance.plugin.models.School
import com.auto.instance.plugin.models.Token
import com.reflect.instance.annotations.AutoInject
import com.reflect.instance.annotations.InjectInstance

@InjectInstance
class ProfilePreview {

    @AutoInject
    lateinit var school: School

    @AutoInject
    lateinit var token: Token

    @AutoInject
    lateinit var token1: Token
    @AutoInject(count = 12)
    lateinit var tkn: List<Token>
    @AutoInject(count = 5)
    lateinit var tkn1: List<Token>
    @AutoInject(count = 2)
    lateinit var tkn2: List<Token>


} 