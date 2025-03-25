package com.reflect.instance

import com.reflect.instance.InstanceCreator.Companion.createInstance
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor

class RandomValueGenerator {

    companion object {
        internal const val MAX_RECURSION_DEPTH = 20
        private const val NULL_PROBABILITY = 0.2
        private const val DEFAULT_STRING_LENGTH = 8
        private const val DEFAULT_COLLECTION_SIZE = 2

        internal val recursionCounters = mutableMapOf<KClass<*>, Int>()
        private val random = Random.Default

        private val primitiveGenerators = mapOf<KClass<*>, () -> Any>(
            String::class to { generateRandomString() },
            Int::class to { random.nextInt(-100, 100) },
            Long::class to { random.nextLong(-100, 100) },
            Short::class to { random.nextInt(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort() },
            Byte::class to { random.nextBytes(1)[0] },
            Double::class to { random.nextDouble(1.0, 1000.0) },
            Float::class to { random.nextFloat() * 100f },
            Boolean::class to { random.nextBoolean() },
            Char::class to { ('A'..'Z').random(random) }
        )

        private val defaultValues = mapOf<KClass<*>, Any>(
            String::class to "",
            Int::class to 0,
            Long::class to 0L,
            Short::class to 0.toShort(),
            Byte::class to 0.toByte(),
            Float::class to 0f,
            Double::class to 0.0,
            Boolean::class to false,
            Char::class to ' ',
            List::class to emptyList<Any>(),
            Set::class to emptySet<Any>(),
            Map::class to emptyMap<Any, Any>()
        )

        fun generateRandomValue(
            param: KParameter,
            kType: KType? = null,
            depth: Int = 0,
            parentClass: KClass<*>? = null,
            targetClass: KClass<*>? = null
        ): Any? {
            val type = kType ?: param.type
            val classifier = type.classifier

            // Early return for nullability check
            if (type.isMarkedNullable && random.nextDouble() < NULL_PROBABILITY) {
                return null
            }

            // Check for semantic value generation first
            val paramName = param.name ?: ""
            val semanticValue = DataGeneratorRegistry.generateValue(paramName, classifier, parentClass, targetClass)
            if (semanticValue != null) return semanticValue

            // Handle primitive types efficiently
            primitiveGenerators[classifier]?.let { return it() }

            // Handle collection types
            return when (classifier) {
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

        fun generateRandomEnum(kClass: KClass<*>): Any =
            kClass.java.enumConstants.random(random)

        fun generateRandomString(length: Int = DEFAULT_STRING_LENGTH): String {
            val words = RandomDataValues.sentences.random(random).split(" ")
            return if (words.isNotEmpty() && random.nextBoolean()) {
                words.random(random)
            } else {
                generateRandomChars(length)
            }
        }

        private fun generateRandomChars(length: Int): String {
            val charPool = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
            return (1..length)
                .map { charPool.random(random) }
                .joinToString("")
        }

        fun generateRandomList(param: KParameter, depth: Int, parentClass: KClass<*>?, targetClass: KClass<*>?): List<Any?> {
            if (depth > MAX_RECURSION_DEPTH) return emptyList()
            val elementType = param.type.arguments.firstOrNull()?.type
            val size = random.nextInt(1, DEFAULT_COLLECTION_SIZE + 1)

            return List(size) {
                generateCollectionElement(elementType, depth, parentClass, targetClass)
            }
        }

        fun generateRandomSet(param: KParameter, depth: Int, parentClass: KClass<*>?, targetClass: KClass<*>?): Set<Any?> {
            if (depth > MAX_RECURSION_DEPTH) return emptySet()
            val elementType = param.type.arguments.firstOrNull()?.type
            val size = random.nextInt(1, DEFAULT_COLLECTION_SIZE + 1)

            return (0 until size).map {
                generateCollectionElement(elementType, depth, parentClass, targetClass)
            }.toSet()
        }

        fun generateRandomMap(param: KParameter, depth: Int, parentClass: KClass<*>?, targetClass: KClass<*>?): Map<Any?, Any?> {
            if (depth > MAX_RECURSION_DEPTH) return emptyMap()
            val keyType = param.type.arguments.getOrNull(0)?.type
            val valueType = param.type.arguments.getOrNull(1)?.type
            val size = random.nextInt(1, DEFAULT_COLLECTION_SIZE + 1)

            return (0 until size).associate {
                val key = generateValueFromType(keyType, depth + 1, parentClass, targetClass)
                val value = generateCollectionElement(valueType, depth, parentClass, targetClass)
                key to value
            }
        }

        private fun generateCollectionElement(elementType: KType?, depth: Int, parentClass: KClass<*>?, targetClass: KClass<*>?): Any? {
            if (elementType == null) return null

            val newDepth = depth + 1
            return when (elementType.classifier) {
                List::class -> generateNestedCollection(elementType, depth, parentClass, targetClass) { innerType ->
                    generateListWithElementType(innerType, newDepth, parentClass, targetClass)
                }
                Set::class -> generateNestedCollection(elementType, depth, parentClass, targetClass) { innerType ->
                    generateSetWithElementType(innerType, newDepth, parentClass, targetClass)
                }
                Map::class -> {
                    val keyType = elementType.arguments.getOrNull(0)?.type
                    val valueType = elementType.arguments.getOrNull(1)?.type
                    generateMapWithElementTypes(keyType, valueType,
                        newDepth, parentClass, targetClass)
                }
                else -> {
                    generateValueFromType(elementType, newDepth, parentClass, targetClass)
                }
            }
        }

        private fun <T> generateNestedCollection(
            elementType: KType,
            depth: Int,
            parentClass: KClass<*>?,
            targetClass: KClass<*>?,
            collectionGenerator: (KType?) -> T
        ): T {
            val innerElementType = elementType.arguments.firstOrNull()?.type
            return collectionGenerator(innerElementType)
        }

        fun generateRandomClassInstance(param: KParameter, kType: KType?, depth: Int, parentClass: KClass<*>?, targetClass: KClass<*>?): Any? {
            if (depth > MAX_RECURSION_DEPTH) throw Exception("Max depth at $MAX_RECURSION_DEPTH only allowed")
            val type = kType ?: param.type
            val kClass = type.classifier as? KClass<*> ?: return null

            // Check if any generator provides a pre-made instance for this target class
            val preTargetInstance = DataGeneratorRegistry.preGenerateTargetClass(kClass)
            if (preTargetInstance != null) {
                return preTargetInstance
            }

            // Handle recursion detection
            val currentCount = recursionCounters.getOrDefault(kClass, 0)
            if (currentCount >= 2) {
                return if (type.isMarkedNullable) null else generateDefaultValue(type)
            }

            // Track instance creation count
            recursionCounters[kClass] = currentCount + 1

            try {
                if (isPrimitiveOrWrapper(kClass)) {
                    return generatePrimitiveValue(kClass)
                }

                val constructor = kClass.primaryConstructor ?: throw IllegalArgumentException(
                    "Class ${kClass.simpleName} has no primary constructor"
                )

                // Check if any generator provides a pre-made instance for this parent class
                val preParentInstance = DataGeneratorRegistry.preGenerateParentClass(kClass)
                if (preParentInstance != null) {
                    return preParentInstance
                }

                // Create parameter values map
                val paramValues = constructor.parameters.associate { parameter ->
                    val value = if (parameter.type.classifier == kClass && parameter.type.isMarkedNullable && currentCount > 0) {
                        null
                    } else {
                        generateRandomValue(parameter, null, depth + 1, kClass, targetClass)
                    }
                    parameter.name!! to value
                }

                // Apply post processing hook for parent class
                @Suppress("UNCHECKED_CAST")
                val parentProcessedValues = DataGeneratorRegistry.postGenerateParentClass(kClass, paramValues) as? Map<String, Any?> ?: paramValues

                val instance = createInstance(kClass, parentProcessedValues)
                
                // Apply post processing hook for target class
                return DataGeneratorRegistry.postGenerateTargetClass(kClass, instance)
            } catch (e: Exception) {
                return if (type.isMarkedNullable) null else generateDefaultValue(type)
            } finally {
                // Always reset the recursion counter
                recursionCounters[kClass] = currentCount
            }
        }

        fun generateDefaultValue(type: KType?): Any {
            if (type == null) return "" // TODO: "Don't pass string for any type"
            val classifier = type.classifier as? KClass<*> ?: return ""

            return defaultValues[classifier] ?: when {
                classifier.java.isEnum -> classifier.java.enumConstants.first()
                else -> ""
            }
        }

        fun isPrimitiveOrWrapper(classifier: Any?): Boolean =
            classifier in primitiveGenerators.keys

        fun generatePrimitiveValue(classifier: Any?): Any =
            primitiveGenerators[classifier]?.invoke() ?:
            throw IllegalArgumentException("Unsupported primitive type: $classifier")

        // Helper functions for collections
        private fun generateListWithElementType(elementType: KType?, depth: Int, parentClass: KClass<*>?, targetClass: KClass<*>?): List<Any?> {
            if (depth > MAX_RECURSION_DEPTH) return emptyList()
            val size = random.nextInt(1, DEFAULT_COLLECTION_SIZE + 1)
            return List(size) {
                generateValueFromType(elementType, depth + 1, parentClass, targetClass)
            }
        }

        private fun generateSetWithElementType(elementType: KType?, depth: Int, parentClass: KClass<*>?, targetClass: KClass<*>?): Set<Any?> {
            if (depth > MAX_RECURSION_DEPTH) return emptySet()
            val size = random.nextInt(1, DEFAULT_COLLECTION_SIZE + 1)
            return (0 until size).map {
                generateValueFromType(elementType, depth + 1, parentClass, targetClass)
            }.toSet()
        }

        private fun generateMapWithElementTypes(keyType: KType?, valueType: KType?, depth: Int, parentClass: KClass<*>?, targetClass: KClass<*>?): Map<Any?, Any?> {
            if (depth > MAX_RECURSION_DEPTH) return emptyMap()
            val size = random.nextInt(1, DEFAULT_COLLECTION_SIZE + 1)
            return (0 until size).associate {
                val key = generateValueFromType(keyType, depth + 1, parentClass, targetClass)
                val value = generateValueFromType(valueType, depth + 1, parentClass, targetClass)
                key to value
            }
        }

        // Helper function to generate a value from a KType
        private fun generateValueFromType(type: KType?, depth: Int, parentClass: KClass<*>?, targetClass: KClass<*>?): Any? {
            if (type == null) return null

            // Create a dummy parameter to use with generateRandomValue
            val dummyParam = object : KParameter {
                override val annotations: List<Annotation> = emptyList()
                override val index: Int = 0
                override val isOptional: Boolean = false
                override val isVararg: Boolean = false
                override val kind: KParameter.Kind = KParameter.Kind.VALUE
                override val name: String = "dummy"
                override val type: KType = type
            }
            return generateRandomValue(dummyParam, type, depth, parentClass, targetClass)
        }
    }
}