package com.reflect.instance

import kotlin.reflect.KClass

/**
 * Default implementation of DataGenerator using predefined values
 */
class DefaultDataGenerator : DataGenerator {
    override fun generateValue(
        paramName: String, paramType: Any?,
        parentClass: KClass<*>?,
        targetClass: KClass<*>?
    ): Any? {
        // You could use parentClass and targetClass here for more specific generation
        return SemanticDataGenerator.generateSemanticValue(paramName.lowercase(), paramType)
    }
}