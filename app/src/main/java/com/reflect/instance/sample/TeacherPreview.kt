package com.reflect.instance.sample

import com.auto.instance.plugin.models.ExelaTeacherAuth
import com.auto.instance.plugin.models.Profile
import com.auto.instance.plugin.models.Token
import com.auto.instance.plugin.models.school.SchoolClassSection
import com.auto.instance.plugin.models.school.SchoolClassSubject
import com.auto.instance.plugin.models.school.SchoolClassTeach
import com.reflect.instance.annotations.AutoInject
import com.reflect.instance.annotations.InjectInstance

@InjectInstance
class TeacherPreview {

    @AutoInject
    lateinit var schoolClassTeach: SchoolClassTeach

    @AutoInject
    lateinit var section: SchoolClassSection

    @AutoInject(dataGenerator = "com.auto.instance.plugin.generator.TokenDataGenerator")
    lateinit var tok: Token

    @AutoInject
    lateinit var profile: Profile


    @AutoInject
    lateinit var schoolClassSubject: SchoolClassSubject
} 