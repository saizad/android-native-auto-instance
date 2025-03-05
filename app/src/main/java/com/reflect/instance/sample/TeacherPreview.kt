package com.reflect.instance.sample

import com.auto.instance.plugin.models.SchoolClassSection
import com.auto.instance.plugin.models.SchoolClassTeach
import com.reflect.instance.annotations.AutoInject
import com.reflect.instance.annotations.InjectInstance

@InjectInstance
class TeacherPreview {

    @AutoInject
    lateinit var schoolClassTeach: SchoolClassTeach

    @AutoInject
    lateinit var section: SchoolClassSection
} 