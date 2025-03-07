package com.reflect.instance

import com.reflect.instance.RandomValueGenerator.Companion.generateRandomString
import kotlin.random.Random

/**
 * Generates semantically meaningful random data based on parameter names
 */
object SemanticDataGenerator {

    // Helper function to analyze parameter name and generate appropriate value
    fun generateSemanticValue(paramName: String, paramType: Any?): Any? {
        val name = paramName.lowercase()
        
        // Check if we can determine the type from the parameter name
        return when {
            // Names
            name.contains("fullname") || name == "name" ->
                "${RandomDataValues.firstNames.random()} ${RandomDataValues.lastNames.random()}"

            name.contains("firstname") || name.contains("fname") ||
                (name == "first" && paramType == String::class) ->
                RandomDataValues.firstNames.random()

            name.contains("lastname") || name.contains("lname") ||
                (name == "last" && paramType == String::class) -> {
                    RandomDataValues.lastNames.random()
                }


            // Contact information    
            name.contains("email") -> 
                "${RandomDataValues.firstNames.random().lowercase()}.${RandomDataValues.lastNames.random().lowercase()}@${RandomDataValues.emailDomains.random()}"
                
            name.contains("phone") -> generatePhoneNumber()
            
            // Addresses
            name.contains("street") || (name.contains("address") && name.contains("line1")) -> 
                "${Random.nextInt(1, 9999)} ${RandomDataValues.streetNames.random()}"
                
            name.contains("city") -> RandomDataValues.cities.random()
            
            name.contains("state") -> RandomDataValues.states.random()
            
            name.contains("country") -> RandomDataValues.countries.random()
            
            name.contains("zip") || name.contains("zipcode") || name.contains("postal") || name.contains("pincode") -> 
                String.format("%05d", Random.nextInt(10000, 99999))
            
            name.contains("address") && !name.contains("line") && paramType == String::class -> generateFullAddress()

            (name.contains("comment") || name.contains("feedback") || name.contains("review")) && paramType == String::class -> {
                val sentence = RandomDataValues.sentences.random()
                if (sentence.endsWith(".")) sentence else "$sentence."
            }

            // Money and numbers
            name.contains("amount") || name.contains("price") || name.contains("cost") || 
                name.contains("fee") || name.contains("salary") -> {
                when (paramType) {
                    Int::class -> Random.nextInt(1, 10000)
                    Long::class -> Random.nextLong(1, 10000)
                    Double::class -> Random.nextDouble(1.0, 10000.0).round(2)
                    Float::class -> Random.nextFloat() * 10000f
                    String::class -> "$${Random.nextDouble(1.0, 10000.0).round(2)}"
                    else -> Random.nextDouble(1.0, 10000.0).round(2)
                }
            }
            
            name.contains("currency") -> RandomDataValues.currencies.random()
            
            name.contains("percentage") || name.contains("percent") -> {
                when (paramType) {
                    Int::class -> Random.nextInt(0, 100)
                    Double::class -> Random.nextDouble(0.0, 100.0).round(2)
                    Float::class -> Random.nextFloat() * 100f
                    String::class -> "${Random.nextInt(0, 100)}%"
                    else -> Random.nextInt(0, 100)
                }
            }
            
            // Images and icons
            name.contains("image") || name.contains("photo") || name.contains("picture") -> 
                RandomDataValues.imageUrls.random()
                
            name.contains("icon") -> RandomDataValues.iconUrls.random()
            
            name.contains("avatar") -> RandomDataValues.imageUrls.random()
            
            // Colors
            name.contains("color") && paramType == String::class -> {
                if (name.contains("hex")) {
                    RandomDataValues.hexColors.random()
                } else {
                    RandomDataValues.colors.random()
                }
            }
            
            // IDs
            name.contains("id") && paramType == String::class -> 
                generateRandomId()
            
            // URLs
            name.contains("url") || name.contains("link") || name.contains("website") -> 
                "https://www.${generateRandomString(8).lowercase()}.com"
            
            // Job related
            name.contains("job") || name.contains("title") || 
                name.contains("position") || name.contains("role") -> 
                RandomDataValues.jobTitles.random()
            
            name.contains("company") || name.contains("employer") || 
                name.contains("business") || name.contains("organization") -> 
                RandomDataValues.companyNames.random()
            
            // Products
            name.contains("product") || (name == "item") -> 
                RandomDataValues.productNames.random()
            
            // Text content
            name.contains("description") || name.contains("summary") || name.contains("details") -> 
                RandomDataValues.paragraphs.random()
            
            // Try to interpret generic names based on type
            (name == "name" || name == "title") -> {
                when (paramType) {
                    String::class -> "${RandomDataValues.firstNames.random()} ${RandomDataValues.lastNames.random()}"
                    else -> generateRandomString()
                }
            }
            
            // Default behavior - use existing generation logic
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