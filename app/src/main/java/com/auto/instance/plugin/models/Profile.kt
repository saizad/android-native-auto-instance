package com.auto.instance.plugin.models

import com.auto.instance.plugin.models.school.School
import com.auto.instance.plugin.models.school.SchoolClassTeach


data class Profile(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val gender: String,
    val school: School,
    val schoolClassTeach: SchoolClassTeach,
)
