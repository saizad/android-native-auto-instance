package com.auto.instance.plugin.models.school


data class SchoolClassTeach(
    val schoolClass: SchoolClass,
    val section: SchoolClassSection,
    val schoolClassSubjects: List<SchoolClassSubject>
)
