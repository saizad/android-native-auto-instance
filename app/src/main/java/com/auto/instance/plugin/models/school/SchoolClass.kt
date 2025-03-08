package com.auto.instance.plugin.models.school

data class SchoolClass(
    val id: Int,
    val name: String,
    val icon: String?,
    val sections: List<SchoolClassSection>
)
