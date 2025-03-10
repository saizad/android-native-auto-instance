package com.reflect.instance.sample

import com.reflect.instance.DataGenerator
import kotlin.reflect.KClass

class DefaultFakeDataGenerator : DataGenerator {
    override fun generateValue(
        paramName: String, paramType: Any?,
        parentClass: KClass<*>?,
        targetClass: KClass<*>?
    ): Any? {

        if (parentClass?.simpleName == "Profile") {
            when (paramName) {
                "lastName" -> return "DefaultLastNameForAllProfileClass"
            }
        }

        return null
    }
}