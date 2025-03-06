package com.reflect.instance

import org.junit.Assert.*
import org.junit.Test
import kotlin.reflect.KClass

class SemanticDataGeneratorTest {

    // Helper function to simulate parameter with name and type
    private data class TestParameter(val name: String, val type: KClass<*>)

    private fun testParam(name: String, type: KClass<*>): TestParameter {
        return TestParameter(name, type)
    }

    private fun generateValue(param: TestParameter): Any? {
        return SemanticDataGenerator.generateSemanticValue(param.name, param.type)
    }

    @Test
    fun `test first name generation`() {
        val firstNameVariations = listOf(
            "firstName", "firstname", "fname", "first"
        )

        for (name in firstNameVariations) {
            val value = generateValue(testParam(name, String::class))
            assertNotNull("Should generate value for $name", value)
            assertTrue(
                "Value for $name should be a first name",
                value is String && RandomDataValues.firstNames.contains(value)
            )
        }
    }

    @Test
    fun `test last name generation`() {
        val lastNameVariations = listOf(
            "lastName", "lastname", "lname", "last"
        )

        for (name in lastNameVariations) {
            val value = generateValue(testParam(name, String::class))
            assertNotNull("Should generate value for $name", value)
            assertTrue(
                "Value for $name should be a last name",
                value is String && RandomDataValues.lastNames.contains(value)
            )
        }
    }

    @Test
    fun `test full name generation`() {
        val fullNameVariations = listOf(
            "fullName", "fullname", "name"
        )

        for (name in fullNameVariations) {
            val value = generateValue(testParam(name, String::class))
            assertNotNull("Should generate value for $name", value)
            assertTrue("Value for $name should be a full name", value is String)

            val nameParts = (value as String).split(" ")
            assertEquals(
                "Full name(para $name) should have first and last name parts",
                2,
                nameParts.size
            )
            assertTrue(
                "First part of full name should be a valid first name",
                RandomDataValues.firstNames.contains(nameParts[0])
            )
            assertTrue(
                "Second part of full name should be a valid last name",
                RandomDataValues.lastNames.contains(nameParts[1])
            )
        }
    }

    @Test
    fun `test email generation`() {
        val emailVariations = listOf(
            "email", "emailAddress", "userEmail"
        )

        for (name in emailVariations) {
            val value = generateValue(testParam(name, String::class))
            assertNotNull("Should generate value for $name", value)
            assertTrue("Value for $name should be an email", value is String)

            val email = value as String
            assertTrue("Email should contain @", email.contains("@"))
            assertTrue("Email should have a domain part after @",
                RandomDataValues.emailDomains.any { email.endsWith("@$it") })
        }
    }

    @Test
    fun `test phone number generation`() {
        val phoneVariations = listOf(
            "phone", "phoneNumber", "mobilePhone", "cellPhone"
        )

        for (name in phoneVariations) {
            val value = generateValue(testParam(name, String::class))
            assertNotNull("Should generate value for $name", value)
            assertTrue("Value for $name should be a phone number", value is String)

            val phone = value as String

            assertTrue(
                "Phone should match format",
                phone.matches(Regex(".*[0-9].*")) &&
                        (phone.contains("-") || phone.contains("(") || phone.contains(" ") || phone.contains(
                            "."
                        ))
            )

        }
    }

    @Test
    fun `test address generation`() {
        // Test street address
        val streetValue = generateValue(testParam("street", String::class))
        assertNotNull("Should generate street address", streetValue)
        assertTrue(
            "Street should be a string with number and name",
            streetValue is String && streetValue.contains(Regex("[0-9]+"))
        )

        // Test city
        val cityValue = generateValue(testParam("city", String::class))
        assertNotNull("Should generate city", cityValue)
        assertTrue(
            "City should be a valid city name",
            cityValue is String && RandomDataValues.cities.contains(cityValue)
        )

        // Test state
        val stateValue = generateValue(testParam("state", String::class))
        assertNotNull("Should generate state", stateValue)
        assertTrue(
            "State should be a valid state abbreviation",
            stateValue is String && RandomDataValues.states.contains(stateValue)
        )

        // Test zip code
        val zipCodeVariations = listOf("zip", "zipCode", "postalCode", "pincode")
        for (name in zipCodeVariations) {
            val value = generateValue(testParam(name, String::class))
            assertNotNull("Should generate value for $name", value)
            assertTrue(
                "Value for $name should be a zip code",
                value is String && value.matches(Regex("[0-9]{5}"))
            )
        }

        // Test full address
        val addressValue = generateValue(testParam("address", String::class))
        assertNotNull("Should generate full address", addressValue)
        assertTrue(
            "Address should be a complete address",
            addressValue is String &&
                    addressValue.contains(Regex("[0-9]+")) &&
                    addressValue.contains(",")
        )
    }

    @Test
    fun `test amount and price generation`() {
        val amountVariations = listOf(
            "amount", "price", "cost", "fee", "salary"
        )

        // Test Int amounts
        for (name in amountVariations) {
            val intValue = generateValue(testParam(name, Int::class))
            assertNotNull("Should generate Int value for $name", intValue)
            assertTrue(
                "Value for $name should be a positive Int",
                intValue is Int && intValue > 0
            )
        }

        // Test Double amounts
        for (name in amountVariations) {
            val doubleValue = generateValue(testParam(name, Double::class))
            assertNotNull("Should generate Double value for $name", doubleValue)
            assertTrue(
                "Value for $name should be a positive Double",
                doubleValue is Double && doubleValue > 0
            )
        }

        // Test String amounts (formatted with currency symbol)
        for (name in amountVariations) {
            val stringValue = generateValue(testParam(name, String::class))
            assertNotNull("Should generate String value for $name", stringValue)
            assertTrue(
                "Value for $name should be a formatted amount",
                stringValue is String && stringValue.startsWith("$")
            )
        }
    }

    @Test
    fun `test currency generation`() {
        val value = generateValue(testParam("currency", String::class))
        assertNotNull("Should generate currency code", value)
        assertTrue(
            "Currency should be a valid currency code",
            value is String && RandomDataValues.currencies.contains(value)
        )
    }

    @Test
    fun `test percentage generation`() {
        val percentageVariations = listOf(
            "percentage", "percent"
        )

        // Test Int percentages
        for (name in percentageVariations) {
            val intValue = generateValue(testParam(name, Int::class))
            assertNotNull("Should generate Int value for $name", intValue)
            assertTrue(
                "Value for $name should be an Int between 0-100",
                intValue is Int && intValue >= 0 && intValue <= 100
            )
        }

        // Test Double percentages
        for (name in percentageVariations) {
            val doubleValue = generateValue(testParam(name, Double::class))
            assertNotNull("Should generate Double value for $name", doubleValue)
            assertTrue(
                "Value for $name should be a Double between 0-100",
                doubleValue is Double && doubleValue >= 0 && doubleValue <= 100
            )
        }

        // Test String percentages
        for (name in percentageVariations) {
            val stringValue = generateValue(testParam(name, String::class))
            assertNotNull("Should generate String value for $name", stringValue)
            assertTrue(
                "Value for $name should be a formatted percentage",
                stringValue is String && stringValue.endsWith("%")
            )
        }
    }

    @Test
    fun `test image and icon URL generation`() {
        val imageVariations = listOf(
            "image", "photo", "picture", "avatar"
        )

        for (name in imageVariations) {
            val value = generateValue(testParam(name, String::class))
            assertNotNull("Should generate value for $name", value)
            assertTrue(
                "Value for $name should be an image URL",
                value is String && (value.startsWith("http") || value.startsWith("https"))
            )
        }

        val iconValue = generateValue(testParam("icon", String::class))
        assertNotNull("Should generate icon URL", iconValue)
        assertTrue(
            "Icon should be a valid URL",
            iconValue is String && iconValue.startsWith("https")
        )
    }

    @Test
    fun `test color generation`() {
        // Test named colors
        val colorValue = generateValue(testParam("color", String::class))
        assertNotNull("Should generate color name", colorValue)
        assertTrue(
            "Color should be a valid color name",
            colorValue is String && RandomDataValues.colors.contains(colorValue)
        )

        // Test hex colors
        val hexColorValue = generateValue(testParam("hexColor", String::class))
        assertNotNull("Should generate hex color", hexColorValue)
        assertTrue(
            "Hex color should be a valid hex code",
            hexColorValue is String && hexColorValue.startsWith("#") &&
                    hexColorValue.matches(Regex("#[0-9A-F]{6}"))
        )
    }

    @Test
    fun `test ID generation`() {
        val idVariations = listOf(
            "id", "userId", "accountId", "customerId"
        )

        for (name in idVariations) {
            val value = generateValue(testParam(name, String::class))
            assertNotNull("Should generate value for $name", value)
            assertTrue(
                "Value for $name should be an ID string",
                value is String && value.isNotEmpty()
            )
        }
    }

    @Test
    fun `test URL generation`() {
        val urlVariations = listOf(
            "url", "link", "website"
        )

        for (name in urlVariations) {
            val value = generateValue(testParam(name, String::class))
            assertNotNull("Should generate value for $name", value)
            assertTrue(
                "Value for $name should be a URL",
                value is String && value.startsWith("https://www.") && value.endsWith(".com")
            )
        }
    }

    @Test
    fun `test job title generation`() {
        val jobVariations = listOf(
            "job", "jobTitle", "title", "position", "role"
        )

        for (name in jobVariations) {
            val value = generateValue(testParam(name, String::class))
            assertNotNull("Should generate value for $name", value)
            assertTrue(
                "Value for $name should be a job title",
                value is String && RandomDataValues.jobTitles.contains(value)
            )
        }
    }

    @Test
    fun `test company name generation`() {
        val companyVariations = listOf(
            "company", "employer", "business", "organization"
        )

        for (name in companyVariations) {
            val value = generateValue(testParam(name, String::class))
            assertNotNull("Should generate value for $name", value)
            assertTrue(
                "Value for $name should be a company name",
                value is String && RandomDataValues.companyNames.contains(value)
            )
        }
    }

    @Test
    fun `test product name generation`() {
        val productVariations = listOf(
            "product", "item", "productName"
        )

        for (name in productVariations) {
            val value = generateValue(testParam(name, String::class))
            assertNotNull("Should generate value for $name", value)
            assertTrue(
                "Value for $name should be a product name",
                value is String && RandomDataValues.productNames.contains(value)
            )
        }
    }

    @Test
    fun `test text content generation`() {
        val descriptionVariations = listOf(
            "description", "summary", "details"
        )

        for (name in descriptionVariations) {
            val value = generateValue(testParam(name, String::class))
            assertNotNull("Should generate value for $name", value)
            assertTrue(
                "Value for $name should be descriptive text",
                value is String && value.length > 50
            )
        }

        val commentVariations = listOf(
            "comment", "feedback", "review"
        )

        for (name in commentVariations) {
            val value = generateValue(testParam(name, String::class))
            assertNotNull("Should generate value for $name", value)
            assertTrue(
                "Value for $name should be a sentence",
                value is String && value.endsWith(".")
            )
        }
    }

    @Test
    fun `test generic name interpretation`() {
        // For generic "name" parameter, should create a full name
        val nameValue = generateValue(testParam("name", String::class))
        assertNotNull("Should generate name", nameValue)
        assertTrue(
            "Name should be a full name",
            nameValue is String && nameValue.toString().contains(" ")
        )
    }

    @Test
    fun `test null return for unsupported parameter names`() {
        // Parameters that don't match any semantic pattern should return null
        val randomParam = generateValue(testParam("somethingRandom", String::class))
        assertNull("Should return null for unmatched parameter name", randomParam)

        val anotherRandomParam = generateValue(testParam("xyz123", String::class))
        assertNull("Should return null for unmatched parameter name", anotherRandomParam)
    }

    @Test
    fun `test integration with data model`() {
        // Test with a sample data class to verify real-world usage

        data class User(
            val id: String,
            val firstName: String,
            val lastName: String,
            val email: String,
            val phoneNumber: String,
            val address: String,
            val salary: Double,
            val jobTitle: String,
            val isActive: Boolean
        )

        // Create an instance using the fake extension
        val user = User::class.fake()

        // Verify semantic values were used for the appropriate fields
        assertTrue("ID should be a non-empty string", user.id.isNotEmpty())
        assertTrue(
            "First name should be from first names list",
            RandomDataValues.firstNames.contains(user.firstName)
        )
        assertTrue(
            "Last name should be from last names list",
            RandomDataValues.lastNames.contains(user.lastName)
        )
        assertTrue("Email should contain @ and domain",
            user.email.contains("@") && RandomDataValues.emailDomains.any { user.email.endsWith(it) })
        assertTrue(
            "Phone number should have correct format",
            user.phoneNumber.contains(Regex("[0-9]"))
        )
        assertTrue(
            "Address should be complete",
            user.address.contains(",") && user.address.contains(Regex("[0-9]+"))
        )
        assertTrue("Salary should be positive", user.salary > 0)
        assertTrue(
            "Job title should be from job titles list",
            RandomDataValues.jobTitles.contains(user.jobTitle)
        )
    }

    @Test
    fun `test complex nested data model`() {
        // Test with nested data classes

        data class Address(
            val street: String,
            val city: String,
            val state: String,
            val zipCode: String,
            val country: String
        )

        data class Contact(
            val email: String,
            val phoneNumber: String,
            val alternatePhone: String?
        )

        data class Customer(
            val id: String,
            val firstName: String,
            val lastName: String,
            val address: Address,
            val contact: Contact,
            val totalSpent: Double,
            val isActive: Boolean
        )


        // Create instances using the fake extension
        val customers = Customer::class.fake(5)

        // Check that we got 5 customers
        assertEquals(5, customers.size)

        // Verify a sample customer
        val customer = customers.first()

        // Verify primary fields
        assertTrue("ID should be non-empty", customer.id.isNotEmpty())
        assertTrue(
            "First name should be from first names list",
            RandomDataValues.firstNames.contains(customer.firstName)
        )

        // Verify nested address
        val address = Address::class.fake()
        assertTrue(
            "Street should contain number and name",
            address.street.contains(Regex("[0-9]+"))
        )
        assertTrue(
            "City should be from cities list",
            RandomDataValues.cities.contains(address.city)
        )
        assertTrue(
            "State should be from states list",
            RandomDataValues.states.contains(address.state)
        )
        assertTrue(
            "Zip code should be 5 digits",
            address.zipCode.matches(Regex("[0-9]{5}"))
        )
        assertTrue(
            "Country should be from countries list",
            RandomDataValues.countries.contains(address.country)
        )

        // Verify nested contact
        val contact = customer.contact
        assertTrue("Email should contain @ and domain",
            contact.email.contains("@") &&
                    RandomDataValues.emailDomains.any { contact.email.endsWith(it) })
        assertTrue(
            "Phone number should have correct format",
            contact.phoneNumber.contains(Regex("[0-9]"))
        )
    }
}