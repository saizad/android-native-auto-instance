package com.auto.instance.plugin.models


data class Profile(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val gender: String,
    val school: School,
    val schoolClassTeach: SchoolClassTeach,
)
