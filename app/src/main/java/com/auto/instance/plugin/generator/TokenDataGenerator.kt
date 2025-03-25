package com.auto.instance.plugin.generator

import com.reflect.instance.DataGenerator
import java.util.UUID
import kotlin.reflect.KClass

class TokenDataGenerator: DataGenerator {
    override fun generateValue(
        paramName: String, paramType: Any?,
        parentClass: KClass<*>?,
        targetClass: KClass<*>?
    ): Any? {


        if (parentClass?.simpleName == "Token") {
            when (paramName) {
                "token" -> return UUID.randomUUID().toString()
            }
        }

        return null
    }

    override fun preGenerateTargetClass(targetClass: KClass<*>?): Any? {
        return null
    }

    override fun preGenerateParentClass(parentClass: KClass<*>?): Any? {
        return null
    }

    override fun postGenerateParentClass(parentClass: KClass<*>?, instance: Any?): Any? {
        return instance
    }

    override fun postGenerateTargetClass(targetClass: KClass<*>?, instance: Any?): Any? {
        return instance
    }

}