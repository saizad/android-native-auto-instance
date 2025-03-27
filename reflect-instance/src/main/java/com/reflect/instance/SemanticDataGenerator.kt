package com.reflect.instance

import com.reflect.instance.RandomValueGenerator.Companion.generateRandomString
import java.util.Calendar
import kotlin.random.Random

/**
 * Generates semantically meaningful random data based on parameter names
 */
object SemanticDataGenerator {

    val calendar = Calendar.getInstance()

    //    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    fun Calendar.toDateTimeString(): String {
        val year = get(Calendar.YEAR)
        val month = get(Calendar.MONTH) + 1
        val day = get(Calendar.DAY_OF_MONTH)
        val hour = get(Calendar.HOUR_OF_DAY)
        val minute = get(Calendar.MINUTE)
        val second = get(Calendar.SECOND)
        val millis = get(Calendar.MILLISECOND)

        // Get timezone offset
        val timeZoneOffsetMinutes = timeZone.rawOffset / 60000
        val offsetHours = Math.abs(timeZoneOffsetMinutes) / 60
        val offsetMinutes = Math.abs(timeZoneOffsetMinutes) % 60
        val sign = if (timeZoneOffsetMinutes >= 0) "+" else "-"

        // Format to match DateTime.now().toString() format
        return "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}T" +
                "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}:" +
                "${second.toString().padStart(2, '0')}.${millis.toString().padStart(3, '0')}" +
                "$sign${offsetHours.toString().padStart(2, '0')}:${
                    offsetMinutes.toString().padStart(2, '0')
                }"
    }

    fun generateSemanticValue(paramName: String, paramType: Any?): Any? {
        val name = paramName.lowercase()

        return when {
            name.contains("gender") -> listOf("Male", "Female").random()

            name.contains("username") -> {
                when (paramType) {
                    String::class -> "${RandomDataValues.firstNames.random().lowercase()}_${RandomDataValues.lastNames.random().lowercase()}"
                    else -> null
                }
            }

            name.contains("fullname") || name == "name" -> {
                when (paramType) {
                    String::class -> "${RandomDataValues.firstNames.random()} ${RandomDataValues.lastNames.random()}"
                    else -> null
                }
            }

            name.contains("firstname") || name.contains("fname") ||
                    (name == "first" && paramType == String::class) -> {
                when (paramType) {
                    String::class -> RandomDataValues.firstNames.random()
                    else -> null
                }
            }

            name.contains("lastname") || name.contains("lname") ||
                    (name == "last" && paramType == String::class) -> {
                when (paramType) {
                    String::class -> RandomDataValues.lastNames.random()
                    else -> null
                }
            }

            name.contains("email") -> {
                when (paramType) {
                    String::class -> "${RandomDataValues.firstNames.random().lowercase()}.${RandomDataValues.lastNames.random().lowercase()}@${RandomDataValues.emailDomains.random()}"
                    else -> null
                }
            }

            name.contains("phone") -> {
                when (paramType) {
                    String::class -> generatePhoneNumber()
                    else -> null
                }
            }

            name.contains("city") -> {
                when (paramType) {
                    String::class -> RandomDataValues.cities.random()
                    else -> null
                }
            }

            name.contains("state") -> {
                when (paramType) {
                    String::class -> RandomDataValues.states.random()
                    else -> null
                }
            }

            name.contains("country") -> {
                when (paramType) {
                    String::class -> RandomDataValues.countries.random()
                    else -> null
                }
            }

            name.contains("zip") || name.contains("zipcode") || name.contains("postal") || name.contains("pincode") -> {
                when (paramType) {
                    String::class -> String.format("%05d", Random.nextInt(10000, 99999))
                    Int::class -> Random.nextInt(10000, 99999)
                    else -> null
                }
            }

            name.contains("street") || (name.contains("address") && name.contains("line1")) -> {
                when (paramType) {
                    String::class -> "${Random.nextInt(1, 9999)} ${RandomDataValues.streetNames.random()}"
                    else -> null
                }
            }

            name.contains("address") && !name.contains("line") -> {
                when (paramType) {
                    String::class -> generateFullAddress()
                    else -> null
                }
            }

            name.contains("comment") || name.contains("feedback") || name.contains("review") -> {
                when (paramType) {
                    String::class -> RandomDataValues.sentences.random()
                    else -> null
                }
            }

            name.contains("rating") -> {
                when (paramType) {
                    Int::class -> Random.nextInt(1, 5)
                    Double::class -> Random.nextDouble(1.0, 5.0).round(1)
                    String::class -> "${Random.nextDouble(1.0, 5.0).round(1)}/5"
                    else -> null
                }
            }

            name.contains("date") || name.contains("time") || name.contains("timestamp") -> {
                when (paramType) {
                    String::class -> calendar.toDateTimeString()
                    Long::class -> System.currentTimeMillis()
                    else -> null
                }
            }

            name.contains("amount") || name.contains("price") || name.contains("cost") ||
                    name.contains("fee") || name.contains("salary") -> {
                when (paramType) {
                    Int::class -> Random.nextInt(1, 10000)
                    Long::class -> Random.nextLong(1, 10000)
                    Double::class -> Random.nextDouble(1.0, 10000.0).round(2)
                    Float::class -> Random.nextFloat() * 10000f
                    String::class -> "$${Random.nextDouble(1.0, 10000.0).round(2)}"
                    else -> null
                }
            }

            name.contains("percentage") || name.contains("percent") -> {
                when (paramType) {
                    Int::class -> Random.nextInt(0, 100)
                    Double::class -> Random.nextDouble(0.0, 100.0).round(2)
                    Float::class -> Random.nextFloat() * 100f
                    String::class -> "${Random.nextInt(0, 100)}%"
                    else -> null
                }
            }

            name.contains("currency") -> {
                when (paramType) {
                    String::class -> RandomDataValues.currencies.random()
                    else -> null
                }
            }

            name.contains("boolean") || name.startsWith("is") || name.endsWith("enabled") ||
                    name.contains("active") || name.contains("verified") -> {
                when (paramType) {
                    Boolean::class -> Random.nextBoolean()
                    String::class -> listOf("true", "false").random()
                    else -> null
                }
            }

            name.contains("image") || name.contains("photo") || name.contains("picture") -> {
                when (paramType) {
                    String::class -> RandomDataValues.imageUrls.random()
                    else -> null
                }
            }

            name.contains("icon") -> {
                when (paramType) {
                    String::class -> RandomDataValues.iconUrls.random()
                    else -> null
                }
            }

            name.contains("color") -> {
                when (paramType) {
                    String::class -> if (name.contains("hex")) RandomDataValues.hexColors.random() else RandomDataValues.colors.random()
                    else -> null
                }
            }

            name.contains("id") -> {
                when (paramType) {
                    String::class -> generateRandomId()
                    Int::class -> Random.nextInt(1000, 9999)
                    Long::class -> Random.nextLong(100000, 999999)
                    else -> null
                }
            }

            name.contains("url") || name.contains("link") || name.contains("website") -> {
                when (paramType) {
                    String::class -> "https://www.${generateRandomString(8).lowercase()}.com"
                    else -> null
                }
            }

            name.contains("job") || name.contains("title") || name.contains("position") || name.contains("role") -> {
                when (paramType) {
                    String::class -> RandomDataValues.jobTitles.random()
                    else -> null
                }
            }

            name.contains("company") || name.contains("employer") || name.contains("business") || name.contains("organization") -> {
                when (paramType) {
                    String::class -> RandomDataValues.companyNames.random()
                    else -> null
                }
            }

            name.contains("product") || (name == "item") -> {
                when (paramType) {
                    String::class -> RandomDataValues.productNames.random()
                    else -> null
                }
            }

            name.contains("description") || name.contains("summary") || name.contains("details") -> {
                when (paramType) {
                    String::class -> RandomDataValues.paragraphs.random()
                    else -> null
                }
            }

            else -> null
        }
    }


    // Helper methods
    private fun generatePhoneNumber(): String {
        val format = RandomDataValues.phoneFormats.random()
        var phoneNumber = ""
        for (char in format) {
            phoneNumber += if (char == '#') {
                Random.nextInt(0, 10).toString()
            } else {
                char
            }
        }
        return phoneNumber
    }

    private fun generateFullAddress(): String {
        val streetNumber = Random.nextInt(1, 9999)
        val street = RandomDataValues.streetNames.random()
        val city = RandomDataValues.cities.random()
        val state = RandomDataValues.states.random()
        val zip = String.format("%05d", Random.nextInt(10000, 99999))
        return "$streetNumber $street, $city, $state $zip"
    }

    private fun generateRandomId(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..10).map { chars.random() }.joinToString("")
    }

    // Function to round Double to n decimal places
    private fun Double.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return kotlin.math.round(this * multiplier) / multiplier
    }
}