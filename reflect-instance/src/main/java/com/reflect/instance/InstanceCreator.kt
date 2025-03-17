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

            return when {
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

                    val paramValues = constructor.parameters.associate {
                        it.name!! to generateRandomValue(it, null, 0, kClass, kClass)
                    }
                    createInstance(kClass, paramValues)
                }
            }
        }
    }
}
