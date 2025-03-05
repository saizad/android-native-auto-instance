package com.auto.instance.plugin.models


data class ExelaTeacherAuth(
    val id: Int,
    val refresh: String,
    val access: String,
    val user: ExelaTeacherUser,
)
