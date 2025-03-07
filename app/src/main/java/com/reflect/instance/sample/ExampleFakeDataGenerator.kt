package com.reflect.instance.sample

import com.reflect.instance.DataGenerator
import kotlin.reflect.KClass

class ExampleFakeDataGenerator : DataGenerator {
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
}