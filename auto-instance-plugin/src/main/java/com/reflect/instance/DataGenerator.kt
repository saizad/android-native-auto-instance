package com.reflect.instance

import kotlin.reflect.KClass

/**
 * Interface for context-aware data generators
 */
interface DataGenerator {
    /**
     * Generate a semantically appropriate value for a parameter
     * @param paramName The name of the parameter
     * @param paramType The classifier of the parameter type
     * @param parentClass The class that contains this parameter (parameter owner)
     * @param targetClass The class being instantiated (may be different than parentClass for nested objects)
     * @return An appropriate value or null if this generator cannot handle the parameter
     */
    fun generateValue(
        paramName: String, paramType: Any?,
        parentClass: KClass<*>? = null,
        targetClass: KClass<*>? = null
    ): Any?
} 