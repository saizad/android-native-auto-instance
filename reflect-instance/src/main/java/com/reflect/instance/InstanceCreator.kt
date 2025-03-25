package com.reflect.instance

import com.reflect.instance.RandomValueGenerator.Companion.generatePrimitiveValue
import com.reflect.instance.RandomValueGenerator.Companion.generateRandomEnum
import com.reflect.instance.RandomValueGenerator.Companion.generateRandomValue
import com.reflect.instance.RandomValueGenerator.Companion.isPrimitiveOrWrapper
import com.reflect.instance.RandomValueGenerator.Companion.recursionCounters
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

class InstanceCreator {
    companion object {

        fun <T : Any> createInstance(kClass: KClass<T>, paramValues: Map<String, Any?>): T? {
            val constructor = kClass.primaryConstructor ?: return null

            val arguments = constructor.parameters.map { param ->
                paramValues[param.name]
            }

            return constructor.call(*arguments.toTypedArray())
        }

        fun <T : Any> createRandomObject(kClass: KClass<T>): T? {
            recursionCounters.clear()

            // Check if any generator provides a pre-made instance for this target class
            val preTargetInstance = DataGeneratorRegistry.preGenerateTargetClass(kClass)
            if (preTargetInstance != null) {
                @Suppress("UNCHECKED_CAST")
                return preTargetInstance as? T
            }

            val instance = when {
                isPrimitiveOrWrapper(kClass) -> {
                    @Suppress("UNCHECKED_CAST")
                    generatePrimitiveValue(kClass) as T
                }
                kClass.java.isEnum -> {
                    @Suppress("UNCHECKED_CAST")
                    generateRandomEnum(kClass) as T
                }
                else -> {
                    val constructor = kClass.primaryConstructor
                        ?: throw IllegalArgumentException("Class ${kClass.simpleName} has no primary constructor")

                    // Check if any generator provides a pre-made instance for this parent class
                    val preParentInstance = DataGeneratorRegistry.preGenerateParentClass(kClass)
                    if (preParentInstance != null) {
                        @Suppress("UNCHECKED_CAST")
                        return preParentInstance as? T
                    }

                    val paramValues = constructor.parameters.associate {
                        it.name!! to generateRandomValue(it, null, 0, kClass, kClass)
                    }
                    
                    // Apply post processing hook for parent class
                    @Suppress("UNCHECKED_CAST")
                    val parentProcessedValues = DataGeneratorRegistry.postGenerateParentClass(kClass, paramValues) as? Map<String, Any?> ?: paramValues
                    
                    createInstance(kClass, parentProcessedValues)
                }
            }

            // Apply post processing hook for target class
            @Suppress("UNCHECKED_CAST")
            return DataGeneratorRegistry.postGenerateTargetClass(kClass, instance) as? T ?: instance
        }
    }
}
