package com.reflect.instance

import kotlin.reflect.KClass

/**
 * Registry and strategy selector for data generators
 */
object DataGeneratorRegistry {
    private val generators = mutableListOf<DataGenerator>(DefaultDataGenerator())

    /**
     * Register a custom data generator
     * @param generator The generator to register
     * @param prepend If true, the generator is given priority over existing generators
     */
    fun registerGenerator(generator: DataGenerator, prepend: Boolean = false) {
        if (prepend) {
            generators.add(0, generator)
        } else {
            generators.add(generator)
        }
    }

    /**
     * Clear all registered generators except the default one
     */
    fun resetToDefault() {
        generators.clear()
        generators.add(DefaultDataGenerator())
    }

    /**
     * Try to generate a value using registered generators
     * @param paramName The name of the parameter
     * @param paramType The classifier of the parameter type
     * @param parentClass The class containing the parameter
     * @param targetClass The class being instantiated
     * @return The first non-null value generated, or null if no generator can handle the parameter
     */
    fun generateValue(
        paramName: String, paramType: Any?,
        parentClass: KClass<*>? = null,
        targetClass: KClass<*>? = null
    ): Any? {
        for (generator in generators) {
            val value = generator.generateValue(paramName, paramType, parentClass, targetClass)
            if (value != null) {
                return value
            }
        }
        return null
    }
}