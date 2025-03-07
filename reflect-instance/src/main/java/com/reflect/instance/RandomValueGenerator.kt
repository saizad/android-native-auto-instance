package com.reflect.instance

import com.reflect.instance.InstanceCreator.Companion.createInstance
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor

class RandomValueGenerator {
    companion object {
        const val MAX_RECURSION_DEPTH = 5
        val recursionCounters = mutableMapOf<KClass<*>, Int>()

        fun generateRandomValue(
            param: KParameter, kType: KType? = null, depth: Int = 0,
            parentClass: KClass<*>? = null, targetClass: KClass<*>? = null
        ): Any? {
            val type = kType ?: param.type
            val classifier = type.classifier
            if (type.isMarkedNullable && Random.nextInt(0, 10) < 2) {
                return null
            }

            val paramName = param.name ?: ""
            val semanticValue = DataGeneratorRegistry.generateValue(paramName, classifier, parentClass, targetClass)
            if (semanticValue != null) return semanticValue

            return when (classifier) {
                String::class -> generateRandomString()
                Int::class -> Random.nextInt(-100, 100)
                Long::class -> Random.nextLong(-100, 100)
                Short::class -> Random.nextInt(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                Byte::class -> Random.nextBytes(1)[0]
                Double::class -> Random.nextDouble(1.0, 1000.0)
                Float::class -> Random.nextFloat() * 100f
                Boolean::class -> Random.nextBoolean()
                Char::class -> ('A'..'Z').random()
                List::class -> generateRandomList(param, depth, parentClass, targetClass)
                Set::class -> generateRandomSet(param, depth, parentClass, targetClass)
                Map::class -> generateRandomMap(param, depth, parentClass, targetClass)
                is KClass<*> -> {

                    if (classifier.java.isEnum) {
                        generateRandomEnum(classifier)
                    } else {
                        generateRandomClassInstance(param, type, depth, parentClass, targetClass ?: classifier)
                    }
                }
                else -> null
            }
        }

        fun generateRandomEnum(kClass: KClass<*>): Any = kClass.java.enumConstants.random()

        fun generateRandomString(length: Int = 8): String {
            val words = RandomDataValues.sentences.random().split(" ")
            return if (words.isNotEmpty() && Random.nextBoolean()) words.random()
            else (1..length).map { "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".random() }.joinToString("")
        }

        fun generateRandomList(param: KParameter, depth: Int, parentClass: KClass<*>?, targetClass: KClass<*>?): List<Any?> {
            if (depth > MAX_RECURSION_DEPTH) return emptyList()
            val elementType = param.type.arguments.firstOrNull()?.type
            return List(Random.nextInt(1, 3)) { generateRandomValue(param, elementType, depth + 1, parentClass, targetClass) }
        }

        fun generateRandomSet(param: KParameter, depth: Int, parentClass: KClass<*>?, targetClass: KClass<*>?): Set<Any?> {
            if (depth > MAX_RECURSION_DEPTH) return emptySet()
            val elementType = param.type.arguments.firstOrNull()?.type
            return (0 until Random.nextInt(1, 3)).map { generateRandomValue(param, elementType, depth + 1, parentClass, targetClass) }.toSet()
        }

        fun generateRandomMap(param: KParameter, depth: Int, parentClass: KClass<*>?, targetClass: KClass<*>?): Map<Any?, Any?> {
            if (depth > MAX_RECURSION_DEPTH) return emptyMap()
            val keyType = param.type.arguments.getOrNull(0)?.type
            val valueType = param.type.arguments.getOrNull(1)?.type
            return (0 until Random.nextInt(1, 3)).associate {
                generateRandomValue(param, keyType, depth + 1, parentClass, targetClass) to
                        generateRandomValue(param, valueType, depth + 1, parentClass, targetClass)
            }
        }

        fun generateRandomClassInstance(param: KParameter, kType: KType?, depth: Int, parentClass: KClass<*>?, targetClass: KClass<*>?): Any? {
            if (depth > MAX_RECURSION_DEPTH) return generateDefaultValue(kType)
            val type = kType ?: param.type
            val kClass = type.classifier as? KClass<*> ?: return null

            // Check if we've already created too many instances of this class (to prevent infinite recursion)
            val currentCount = recursionCounters.getOrDefault(kClass, 0)
            if (currentCount >= 2) {
                // For circular references, return null for nullable types or a default value
                if (type.isMarkedNullable) {
                    return null
                }
                return generateDefaultValue(type)
            }
            
            // Increment the counter for this class
            recursionCounters[kClass] = currentCount + 1

            if (isPrimitiveOrWrapper(kClass)) {
                // Reset counter before returning for primitive types
                recursionCounters[kClass] = currentCount
                return generatePrimitiveValue(kClass)
            }

            return try {
                val constructor = kClass.primaryConstructor
                if (constructor == null) {
                    // Reset counter and throw an exception for classes without a primary constructor
                    recursionCounters[kClass] = currentCount
                    throw IllegalArgumentException("Class ${kClass.simpleName} has no primary constructor")
                }
                
                // Create parameter values map
                val paramValues = constructor.parameters.associate { parameter ->
                    // For self-referential parameters that would cause circular references,
                    // use null if the parameter is nullable
                    val value = if (parameter.type.classifier == kClass && parameter.type.isMarkedNullable && currentCount > 0) {
                        null
                    } else {
                        generateRandomValue(parameter, null, depth + 1, kClass, targetClass)
                    }
                    parameter.name!! to value
                }
                
                // Create the instance and reset the counter
                val result = createInstance(kClass, paramValues)
                // Reset counter after successful creation
                recursionCounters[kClass] = currentCount
                result
            } catch (e: Exception) {
                // Reset counter if there was an exception
                recursionCounters[kClass] = currentCount
                if (type.isMarkedNullable) {
                    null
                } else {
                    generateDefaultValue(type)
                }
            }
        }

        fun generateDefaultValue(type: KType?): Any {
            if (type == null) return ""
            return when (type.classifier) {
                String::class -> ""
                Int::class -> 0
                Long::class -> 0L
                Float::class -> 0f
                Double::class -> 0.0
                Boolean::class -> false
                Char::class -> ' '
                List::class -> emptyList<Any>()
                Set::class -> emptySet<Any>()
                Map::class -> emptyMap<Any, Any>()
                else -> {
                    val kClass = type.classifier as? KClass<*>
                    if (kClass?.java?.isEnum == true) {
                        kClass.java.enumConstants.first()
                    } else {
                        // For complex types we can't handle, return an empty string as a last resort
                        ""
                    }
                }
            }
        }

        fun isPrimitiveOrWrapper(classifier: Any?): Boolean = classifier in setOf(
            Byte::class, Short::class, Int::class, Long::class,
            Float::class, Double::class, Boolean::class, Char::class, String::class
        )

        fun generatePrimitiveValue(classifier: Any?): Any = when (classifier) {
            Byte::class -> Random.nextBytes(1)[0]
            Short::class -> Random.nextInt(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            Int::class -> Random.nextInt(-100, 100)
            Long::class -> Random.nextLong(-100, 100)
            Float::class -> Random.nextFloat() * 100f
            Double::class -> Random.nextDouble(1.0, 1000.0)
            Boolean::class -> Random.nextBoolean()
            Char::class -> ('A'..'Z').random()
            String::class -> generateRandomString()
            else -> throw IllegalArgumentException("Unsupported primitive type: $classifier")
        }
    }
}
