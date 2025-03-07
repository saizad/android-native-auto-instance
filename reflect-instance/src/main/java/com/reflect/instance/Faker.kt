package com.reflect.instance

import com.reflect.instance.InstanceCreator.Companion.createRandomObject
import kotlin.reflect.KClass


// Updated fake extensions to accept custom generators
fun <T : Any> KClass<T>.fake(generator: DataGenerator? = null): T {
    if (generator != null) {
        DataGeneratorRegistry.registerGenerator(generator, true)
    }
    try {
        return fake(1).first()
    } finally {
        if (generator != null) {
            DataGeneratorRegistry.resetToDefault()
        }
    }
}

fun <T : Any> KClass<T>.fake(count: Int, generator: DataGenerator? = null): List<T> {
    if (generator != null) {
        DataGeneratorRegistry.registerGenerator(generator, true)
    }
    try {
        return (0 until count).map {
            try {
                createRandomObject(this)
                    ?: throw IllegalArgumentException("Failed to create an instance of $this")
            } catch (e: IllegalArgumentException) {
                // Rethrow the exception for classes without primary constructors
                throw e
            }
        }
    } finally {
        if (generator != null) {
            DataGeneratorRegistry.resetToDefault()
        }
    }
}