package com.reflect.instance

import com.reflect.instance.InstanceCreator.Companion.createRandomObject
import kotlin.reflect.KClass


fun <T : Any> KClass<T>.fake(generator: DataGenerator? = null): T {
    val effectiveGenerator = generator ?: DataGeneratorRegistry.getGlobalDefaultGenerator()
    DataGeneratorRegistry.registerGenerator(effectiveGenerator, prepend = true)

    try {
        return fake(1, effectiveGenerator).first()
    } finally {
        DataGeneratorRegistry.resetToDefault()
    }
}

fun <T : Any> KClass<T>.fake(count: Int, generator: DataGenerator? = null): List<T> {
    val effectiveGenerator = generator ?: DataGeneratorRegistry.getGlobalDefaultGenerator()
    DataGeneratorRegistry.registerGenerator(effectiveGenerator, prepend = true)

    try {
        return (0 until count).map {
            createRandomObject(this)
                ?: throw IllegalArgumentException("Failed to create an instance of $this")
        }
    } finally {
        DataGeneratorRegistry.resetToDefault()
    }
}
