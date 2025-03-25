package com.auto.instance.plugin.generator

import com.reflect.instance.DataGenerator
import kotlin.reflect.KClass

class SchoolDataGenerator: DataGenerator {
    override fun generateValue(
        paramName: String, paramType: Any?,
        parentClass: KClass<*>?,
        targetClass: KClass<*>?
    ): Any? {
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

        if (parentClass?.simpleName == "School") {
            when (paramName) {
                "title" -> return topColleges.random()
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