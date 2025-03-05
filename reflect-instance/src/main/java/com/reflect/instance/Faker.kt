package com.reflect.instance

import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor


// Maximum depth to prevent infinite recursion with circular references
const val MAX_RECURSION_DEPTH = 5
private val recursionCounters = mutableMapOf<KClass<*>, Int>()

fun <T : Any> createInstance(kClass: KClass<T>, paramValues: Map<String, Any?>): T? {
    // Get the primary constructor
    val constructor = kClass.primaryConstructor ?: return null

    // Map the constructor parameters to a list of arguments
    val arguments = constructor.parameters.map { param ->
        paramValues[param.name]  // Get the value for the parameter from the map
    }

    // Call the constructor with the arguments
    return constructor.call(*arguments.toTypedArray())
}

// Enhanced function to generate values based on parameter name and type
fun generateRandomValue(param: KParameter, kType: KType? = null, depth: Int = 0): Any? {
    val type = kType ?: param.type
    val classifier = type.classifier

    // Handle null for nullable types
    if (type.isMarkedNullable && Random.nextInt(0, 10) < 2) { // 20% chance of null for nullable types
        return null
    }

    // Try to generate semantically appropriate value based on parameter name
    val paramName = param.name ?: ""
    val semanticValue = SemanticDataGenerator.generateSemanticValue(paramName, classifier)
    if (semanticValue != null) {
        return semanticValue
    }

    // Fall back to type-based generation
    return when {
        // Handle primitive types
        classifier == String::class -> generateRandomString()
        classifier == Int::class -> Random.nextInt(-100, 100)
        classifier == Long::class -> Random.nextLong(-100, 100)
        classifier == Short::class -> Random.nextInt(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        classifier == Byte::class -> Random.nextBytes(1)[0]
        classifier == Double::class -> Random.nextDouble(1.0, 1000.0)
        classifier == Float::class -> Random.nextFloat() * 100f
        classifier == Boolean::class -> Random.nextBoolean()
        classifier == Char::class -> ('A'..'Z').random()

        // Handle Joda DateTime

        // Handle collections
        classifier == List::class -> generateRandomList(param, depth)
        classifier == Set::class -> generateRandomSet(param, depth)
        classifier == Map::class -> generateRandomMap(param, depth)

        // Handle enums
        (classifier as? KClass<*>)?.java?.isEnum == true -> generateRandomEnum(classifier)

        // Handle other classes
        else -> generateRandomClassInstance(param, type, depth)
    }
}

// Generate a random enum value
fun generateRandomEnum(kClass: KClass<*>): Any {
    val enumConstants = kClass.java.enumConstants
    return enumConstants[Random.nextInt(enumConstants.size)]
}

// Generate a random string
fun generateRandomString(length: Int = 8): String {
    val words = RandomDataValues.sentences.random().split(" ")
    return if (words.isNotEmpty() && Random.nextBoolean()) {
        words.random()
    } else {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        (1..length).map { chars.random() }.joinToString("")
    }
}

// Function to generate a random list of elements for List<T>
fun generateRandomList(param: KParameter, depth: Int): List<Any?> {
    // Check recursion depth to prevent stack overflow
    if (depth > MAX_RECURSION_DEPTH) {
        return emptyList()
    }

    val elementType = param.type.arguments.firstOrNull()?.type
    val size = Random.nextInt(1, 3) // Reduced size to minimize recursion
    return List(size) { generateRandomValue(param, elementType, depth + 1) }
}

// Function to generate a random set of elements for Set<T>
fun generateRandomSet(param: KParameter, depth: Int): Set<Any?> {
    // Check recursion depth to prevent stack overflow
    if (depth > MAX_RECURSION_DEPTH) {
        return emptySet()
    }

    val elementType = param.type.arguments.firstOrNull()?.type
    val size = Random.nextInt(1, 3) // Reduced size to minimize recursion
    return (0 until size).map { generateRandomValue(param, elementType, depth + 1) }.toSet()
}

// Function to generate a random map for Map<K, V>
fun generateRandomMap(param: KParameter, depth: Int): Map<Any?, Any?> {
    // Check recursion depth to prevent stack overflow
    if (depth > MAX_RECURSION_DEPTH) {
        return emptyMap()
    }

    val keyType = param.type.arguments.getOrNull(0)?.type
    val valueType = param.type.arguments.getOrNull(1)?.type
    val size = Random.nextInt(1, 3) // Reduced size to minimize recursion

    return (0 until size).associate {
        generateRandomValue(param, keyType, depth + 1) to generateRandomValue(param, valueType, depth + 1)
    }
}

// Function to generate a random instance of any class (for custom classes)
fun generateRandomClassInstance(param: KParameter, kType: KType?, depth: Int): Any? {
    if (depth > MAX_RECURSION_DEPTH) {
        // If we've gone too deep, return null for nullable types or a default value
        return if (kType?.isMarkedNullable == true) null else generateDefaultValue(kType)
    }

    val type = kType ?: param.type
    val kClass = type.classifier as? KClass<*> ?: throw IllegalArgumentException("Unsupported type: $type")

    // Check and update recursion counter for this class
    val currentCount = recursionCounters.getOrDefault(kClass, 0)
    if (currentCount >= 2) {  // Prevent more than 2 levels of recursion for the same class
        recursionCounters[kClass] = 0  // Reset counter
        return if (type.isMarkedNullable) null else generateDefaultValue(type)
    }
    recursionCounters[kClass] = currentCount + 1

    // Handle primitive types
    if (isPrimitiveOrWrapper(kClass)) {
        val result = generatePrimitiveValue(kClass)
        recursionCounters[kClass] = currentCount  // Restore counter
        return result
    }

    try {
        val constructor = kClass.primaryConstructor
            ?: throw IllegalArgumentException("No primary constructor found for $kClass")

        // Generate random values for each constructor parameter
        val paramValues = constructor.parameters.associate {
            it.name!! to generateRandomValue(it, null, depth + 1)
        }

        // Create the instance
        val result = createInstance(kClass, paramValues)
            ?: throw IllegalArgumentException("Failed to create an instance of $kClass")

        // Restore recursion counter after successful creation
        recursionCounters[kClass] = currentCount

        return result
    } catch (e: Exception) {
        // If something goes wrong, reset counter and return null for nullable or default for non-nullable
        recursionCounters[kClass] = currentCount
        return if (type.isMarkedNullable) null else generateDefaultValue(type)
    }
}

// Generate a default value for a type when we need to break recursion
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

// Function to check if a type is a primitive or wrapper
fun isPrimitiveOrWrapper(classifier: Any?): Boolean {
    return when (classifier) {
        Byte::class, Short::class, Int::class, Long::class,
        Float::class, Double::class, Boolean::class, Char::class,
        String::class -> true
        else -> false
    }
}

// Function to generate primitive values
fun generatePrimitiveValue(classifier: Any?): Any {
    return when (classifier) {
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

// Function to create an object dynamically with random values
fun <T : Any> createRandomObject(kClass: KClass<T>): T? {
    recursionCounters.clear() // Clear counters for a fresh start

    // Special handling for primitive types and their wrappers
    if (isPrimitiveOrWrapper(kClass)) {
        @Suppress("UNCHECKED_CAST")
        return generatePrimitiveValue(kClass) as T
    }

    // Special handling for enums
    if (kClass.java.isEnum) {
        @Suppress("UNCHECKED_CAST")
        return generateRandomEnum(kClass) as T
    }

    // Get the primary constructor of the class
    val constructor = kClass.primaryConstructor ?: return null

    // Create a map with random values for each constructor parameter
    val paramValues = constructor.parameters.associate {
        it.name!! to generateRandomValue(it, null, 0)
    }

    // Use the createInstance function to instantiate the object
    return createInstance(kClass, paramValues)
}

fun <T: Any> KClass<T>.fake(): T {
    return fake(1).first()
}

fun <T: Any> KClass<T>.fake(count: Int): List<T> {
    return (0 until count).map {
        createRandomObject(this) ?: throw IllegalArgumentException("Failed to create an instance of $this")
    }
}