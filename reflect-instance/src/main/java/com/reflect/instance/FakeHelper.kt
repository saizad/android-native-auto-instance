package com.reflect.instance

import kotlin.reflect.KClass

class FakeHelper {

    fun <T: Any> fake(clazz: KClass<T>, count: Int, dataGenerator: DataGenerator? = null): List<T> {
        return clazz.fake(count, dataGenerator)
    }

}