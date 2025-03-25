package com.reflect.instance.sample

import com.reflect.instance.DataGenerator
import kotlin.reflect.KClass

class DefaultFakeDataGenerator : DataGenerator {

    val topColleges = listOf(
        "Massachusetts Institute of Technology (MIT)",
        "Stanford University",
        "Harvard University",
        "California Institute of Technology (Caltech)",
        "University of Oxford",
        "University of Cambridge",
        "ETH Zurich",
        "Imperial College London",
        "University of Chicago",
        "Princeton University"
    )

    val subjects = listOf(
        "Mathematics",
        "Physics",
        "Chemistry",
        "Biology",
        "Computer Science",
        "History",
        "Geography",
        "Literature",
        "Philosophy",
        "Psychology",
        "Economics",
        "Political Science",
        "Sociology",
        "Anthropology",
        "Art History",
        "Music Theory",
        "Foreign Languages",
        "Statistics",
        "Engineering",
        "Business Administration",
        "Environmental Science",
        "Astronomy",
        "Geology",
        "Linguistics",
        "Law",
        "Medicine",
        "Architecture",
        "Physical Education",
        "Film Studies",
        "Media Studies"
    )

    override fun generateValue(
        paramName: String, paramType: Any?,
        parentClass: KClass<*>?,
        targetClass: KClass<*>?
    ): Any? {

        if (parentClass?.simpleName == "SchoolClassSubject") {
            when (paramName) {
                "name" -> {
                    return subjects.random()
                }
            }
        }

        if (parentClass?.simpleName == "School") {
            when (paramName) {
                "title" -> return topColleges.random()
            }
        }

        if (parentClass?.simpleName == "Profile") {
            when (paramName) {
                "lastName" -> return "DefaultLastNameForAllProfileClass"
            }
        }

        return null
    }

    override fun preGenerateTargetClass(targetClass: KClass<*>?): Any? {
        return null
    }

    override fun preGenerateParentClass(parentClass: KClass<*>?): Any? {
        return null
    }

    override fun postGenerateParentClass(parentClass: KClass<*>?, instance: Any?): Any? {
        return instance
    }

    override fun postGenerateTargetClass(targetClass: KClass<*>?, instance: Any?): Any? {
        return instance
    }
}