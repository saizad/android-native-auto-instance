package com.auto.instance.plugin.models


data class SchoolClassTeach(
    val schoolClass: SchoolClass,
    val section: SchoolClassSection,
    val schoolClassSubjects: List<SchoolClassSubject>
)
