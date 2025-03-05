package com.reflect.instance

import kotlin.reflect.KClass

class FakeHelper {

    fun fake(clazz: KClass<Any>, count: Int): List<Any> {
        return clazz.fake(count)
    }

}