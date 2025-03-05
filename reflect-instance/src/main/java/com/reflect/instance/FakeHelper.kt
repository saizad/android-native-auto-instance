package com.reflect.instance

import kotlin.reflect.KClass

class FakeHelper {

    fun <T: Any> fake(clazz: KClass<T>, count: Int): List<T> {
        return clazz.fake(count)
    }

}