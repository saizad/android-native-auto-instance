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

    /**
     * Hook called before generating an instance of the parent class
     * @param parentClass The class that contains the parameter (parameter owner)
     * @return An instance to use (skipping normal generation) or null to continue with normal generation
     */
    fun preGenerateParentClass(parentClass: KClass<*>?): Any?

    /**
     * Hook called after generating an instance of the parent class
     * @param parentClass The class that contains the parameter (parameter owner)
     * @param instance The generated instance
     * @return The potentially modified instance or null to use the default
     */
    fun postGenerateParentClass(parentClass: KClass<*>?, instance: Any?): Any?

    /**
     * Hook called before generating an instance of the target class
     * @param targetClass The class being instantiated
     * @return An instance to use (skipping normal generation) or null to continue with normal generation
     */
    fun preGenerateTargetClass(targetClass: KClass<*>?): Any?

    /**
     * Hook called after generating an instance of the target class
     * @param targetClass The class being instantiated
     * @param instance The generated instance
     * @return The potentially modified instance or null to use the default
     */
    fun postGenerateTargetClass(targetClass: KClass<*>?, instance: Any?): Any?
}