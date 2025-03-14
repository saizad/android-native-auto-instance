package com.reflect.instance

import kotlin.reflect.KClass

/**
 * Registry and strategy selector for data generators
 */
object DataGeneratorRegistry {
    private var globalDefaultGenerator: DataGenerator = DefaultDataGenerator()
    private val defaultGenerator: DataGenerator = DefaultDataGenerator() // Singleton instance
    private val generators = mutableListOf<DataGenerator>(globalDefaultGenerator)

    /**
     * Set a global default generator. This will be used when no custom generator is provided.
     * @param generator The generator to be used as the global default
     */
    fun setGlobalDefaultGenerator(generator: DataGenerator) {
        globalDefaultGenerator = generator
    }

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
     * Clear all registered generators except the global default one
     */
    fun resetToDefault() {
        generators.clear()
        generators.add(globalDefaultGenerator)
    }

    /**
     * Get the currently active global default generator
     */
    fun getGlobalDefaultGenerator(): DataGenerator {
        return globalDefaultGenerator
    }

    /**
     * Try to generate a value using registered generators
     * If all generators return null, fall back to a shared instance of DefaultDataGenerator
     */
    fun generateValue(
        paramName: String, paramType: Any?,
        parentClass: KClass<*>? = null,
        targetClass: KClass<*>? = null
    ): Any? {
        // Try all registered generators
        for (generator in generators) {
            val value = generator.generateValue(paramName, paramType, parentClass, targetClass)
            if (value != null) {
                return value
            }
        }

        // If no generator provided a value, use the shared instance of DefaultDataGenerator
        return defaultGenerator.generateValue(paramName, paramType, parentClass, targetClass)
    }
}
