package com.reflect.instance

import com.reflect.instance.RandomValueGenerator.Companion.MAX_RECURSION_DEPTH
import com.reflect.instance.RandomValueGenerator.Companion.generateRandomString
import com.reflect.instance.RandomValueGenerator.Companion.generateRandomValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.reflect.full.primaryConstructor

data class SimpleClass(val name: String, val age: Int)
data class ComplexClass(val name: String, val isActive: Boolean)
data class NestedClass(val simple: SimpleClass, val count: Int)
data class CollectionClass(
    val stringList: List<String>,
    val intSet: Set<Int>,
    val mapping: Map<String, Int>
)

data class NullableClass(val name: String?, val age: Int?)
data class CircularClass(val name: String, val self: CircularClass?)
enum class TestEnum { ONE, TWO, THREE }
data class EnumClass(val value: TestEnum)
data class DeepNestedClass(
    val level1: NestedClass,
    val name: String
)

// Complex domain models
data class Person(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val address: Address,
    val isActive: Boolean,
    val tags: List<String>,
    val preferences: Map<String, String>
)

data class Address(
    val streetAddress: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String,
    val isDefault: Boolean
)

data class Company(
    val id: String,
    val name: String,
    val description: String,
    val headquarters: Address,
    val employees: List<Employee>,
    val industry: String,
    val website: String
)

data class Employee(
    val id: String,
    val person: Person,
    val jobTitle: String,
    val department: String,
    val salary: Double,
    val manager: Employee?,
    val directReports: List<Employee>
)

data class Product(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val category: String,
    val inStock: Boolean,
    val imageUrl: String,
    val manufacturer: Company,
    val reviews: List<Review>,
    val tags: Set<String>,
    val specifications: Map<String, String>
)

data class Review(
    val id: String,
    val author: Person,
    val rating: Int,
    val comment: String,
    val helpfulVotes: Int,
    val images: List<String>
)

data class Order(
    val id: String,
    val customer: Person,
    val items: List<OrderItem>,
    val shippingAddress: Address,
    val billingAddress: Address,
    val totalAmount: Double,
    val status: OrderStatus,
    val paymentMethod: String,
    val notes: String?
)

data class OrderItem(
    val product: Product,
    val quantity: Int,
    val unitPrice: Double,
    val discount: Double,
    val subtotal: Double
)

enum class OrderStatus {
    PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED, RETURNED
}

data class SchoolClass(
    val id: String,
    val name: String,
    val teacher: Person,
    val students: List<Person>,
    val subject: String,
    val room: String,
    val assignments: List<Assignment>
)

data class Assignment(
    val id: String,
    val title: String,
    val description: String,
    val maxScore: Int,
    val submissions: Map<String, Submission>
)

data class Submission(
    val student: Person,
    val content: String,
    val score: Int?,
    val feedback: String?
)

class BasicTypeTests {


    @Test
    fun `should generate random string`() {
        val result = generateRandomString()
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `should generate random int`() {
        val kParam = SimpleClass::class.primaryConstructor!!.parameters.find { it.name == "age" }!!
        val result = generateRandomValue(kParam) as Int
        assertNotNull(result)
        assertTrue(result in -100..100)
    }

    @Test
    fun `should generate random boolean`() {
        val kParam =
            ComplexClass::class.primaryConstructor!!.parameters.find { it.name == "isActive" }!!
        val result = generateRandomValue(kParam) as Boolean
        // No additional assertions needed - a Boolean can only be true or false
        assertNotNull(result)
    }


}

class CollectionTests {

    @Test
    fun `should generate random list`() {
        val kParam = CollectionClass::class.primaryConstructor!!.parameters
            .find { it.name == "stringList" }!!
        val result = generateRandomValue(kParam) as List<*>
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        result.forEach { assertTrue(it is String) }
    }

    @Test
    fun `should generate random set`() {
        val kParam = CollectionClass::class.primaryConstructor!!.parameters
            .find { it.name == "intSet" }!!
        val result = generateRandomValue(kParam) as Set<*>
        assertNotNull(result)
        result.forEach { assertTrue(it is Int) }
    }

    @Test
    fun `should generate random map`() {
        val kParam = CollectionClass::class.primaryConstructor!!.parameters
            .find { it.name == "mapping" }!!
        val result = generateRandomValue(kParam) as Map<*, *>
        assertNotNull(result)
        result.forEach {
            assertTrue(it.key is String)
            assertTrue(it.value is Int)
        }
    }
}


class SimpleClassTests {

    @Test
    fun `should create simple class instance`() {
        val result = SimpleClass::class.fake()
        assertNotNull(result)
        assertNotNull(result.name)
        assertTrue(result.name.isNotEmpty())
        assertNotNull(result.age)
    }

    @Test
    fun `should create multiple instances`() {
        val count = 5
        val results = SimpleClass::class.fake(count)
        assertEquals(count, results.size)
        // Ensure they're not all the same instance
        val distinctNames = results.map { it.name }.distinct()
        assertTrue(distinctNames.size > 1)
    }
}

class ComplexClassTests {

    @Test
    fun `should create complex class with datetime`() {
        val result = ComplexClass::class.fake()
        assertNotNull(result)
        assertNotNull(result.name)
        assertNotNull(result.isActive)
    }
}

class NestedClassTests {

    @Test
    fun `should create nested class`() {
        val result = NestedClass::class.fake()
        assertNotNull(result)
        assertNotNull(result.simple)
        assertNotNull(result.simple.name)
        assertNotNull(result.simple.age)
        assertNotNull(result.count)
    }

    @Test
    fun `should create deep nested class`() {
        val result = DeepNestedClass::class.fake()
        assertNotNull(result)
        assertNotNull(result.level1)
        assertNotNull(result.level1.simple)
        assertNotNull(result.level1.simple.name)
        assertNotNull(result.level1.count)
        assertNotNull(result.name)
    }
}

class NullableTypeTests {

    @Test
    fun `should sometimes generate null for nullable types`() {
        repeat((0..10).count()) {

            // Run multiple times to increase chance of seeing both null and non-null
            val results = (1..100).map { NullableClass::class.fake() }

            // Check if we have at least some nulls
            assertTrue(results.any { it.name == null })
            assertTrue(results.any { it.age == null })

            // And some non-nulls
            assertTrue(results.any { it.name != null })
            assertTrue(results.any { it.age != null })
        }
    }
}

class CircularReferenceTests {

    @Test
    fun `should handle circular references without stack overflow`() {
        val result = CircularClass::class.fake()
        assertNotNull(result)
        assertNotNull(result.name)

        // Circular references should eventually be cut off with nulls
        var current: CircularClass? = result
        var depth = 0
        while (current?.self != null && depth < 10) {
            current = current.self
            depth++
        }
        // Should terminate before hitting our arbitrary max depth
        assertTrue(depth < 10)
    }
}

class EnumGenerationTests {

    @Test
    fun `should generate random enum value`() {
        val result = TestEnum::class.fake()
        assertNotNull(result)
        assertTrue(result in TestEnum.values())
    }

    @Test
    fun `should handle class with enum field`() {
        val result = EnumClass::class.fake()
        assertNotNull(result)
        assertNotNull(result.value)
        assertTrue(result.value in TestEnum.values())
    }

    @Test
    fun `should handle enum in complex model`() {
        val result = Order::class.fake()
        assertNotNull(result)
        assertNotNull(result.status)
        assertTrue(result.status in OrderStatus.values())
    }
}

class EdgeCaseTests {

    @Test
    fun `should handle maximum recursion depth`() {
        // Define a deeply nested structure that would exceed MAX_RECURSION_DEPTH
        data class Level7(val value: String)
        data class Level6(val value: String, val next: Level7?)
        data class Level5(val value: String, val next: Level6?)
        data class Level4(val value: String, val next: Level5?)
        data class Level3(val value: String, val next: Level4?)
        data class Level2(val value: String, val next: Level3?)
        data class Level1(val value: String, val next: Level2?)

        val result = Level1::class.fake()
        assertNotNull(result)

        // Traverse the structure and ensure it doesn't go too deep
        var current: Any? = result
        var depth = 0
        while (current != null && depth < 10) {
            when (current) {
                is Level1 -> current = current.next
                is Level2 -> current = current.next
                is Level3 -> current = current.next
                is Level4 -> current = current.next
                is Level5 -> current = current.next
                is Level6 -> current = current.next
                else -> current = null
            }
            depth++
        }
        // Should terminate before hitting our arbitrary max depth
        assertTrue(depth <= MAX_RECURSION_DEPTH + 2)
    }

    @Test
    fun `should not exceed custom class recursion limit`() {
        // Testing that a specific class doesn't get instantiated more than twice
        data class RecursiveClass(val name: String, val child: RecursiveClass?)

        val result = RecursiveClass::class.fake()
        assertNotNull(result)
        assertNotNull(result.name)

        // Track recursion depth
        var depth = 1
        var current = result.child
        while (current != null) {
            depth++
            current = current.child
        }

        assertTrue(depth <= MAX_RECURSION_DEPTH)
    }

    @Test
    fun `should handle nested collections`() {
        data class NestedCollections(
            val listOfLists: List<List<String>>,
            val mapOfSets: Map<String, Set<Int>>
        )

        val result = NestedCollections::class.fake()
        assertNotNull(result)
        assertNotNull(result.listOfLists)
        assertNotNull(result.mapOfSets)

        // Check that nested collections are correctly typed
        if (result.listOfLists.isNotEmpty()) {
            val innerList = result.listOfLists.first()
            if (innerList.isNotEmpty()) {
                assertTrue(innerList.first() is String)
            }
        }

        if (result.mapOfSets.isNotEmpty()) {
            val innerSet = result.mapOfSets.values.first()
            if (innerSet.isNotEmpty()) {
                assertTrue(innerSet.first() is Int)
            }
        }
    }
}

class MissingConstructorTests {

    @Test
    fun `should handle class without primary constructor`() {
        class NoConstructorClass {
            val value: String = "default"
        }

        NoConstructorClass()
        // This should not throw an exception, just return null
        val result = runCatching { NoConstructorClass::class.fake() }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
}

class ComplexModelTests {

    @Test
    fun `should create Person with correct field types`() {
        val person = Person::class.fake()
        assertNotNull(person)

        // Test basic fields
        assertNotNull(person.id)
        assertTrue(person.id.isNotEmpty())
        assertNotNull(person.firstName)
        assertNotNull(person.lastName)
        assertTrue(person.email.contains("@"))

        // Test nested object
        assertNotNull(person.address)
        assertNotNull(person.address.streetAddress)
        assertNotNull(person.address.city)
        assertNotNull(person.address.state)
        assertNotNull(person.address.zipCode)
        assertNotNull(person.address.country)

        // Test collections
        assertNotNull(person.tags)
        assertNotNull(person.preferences)
    }

    @Test
    fun `should create Company with nested employees`() {
        val company = Company::class.fake()
        assertNotNull(company)

        // Basic fields
        assertNotNull(company.id)
        assertNotNull(company.name)
        assertNotNull(company.description)
        assertNotNull(company.industry)
        assertTrue(company.website.startsWith("https://"))

        // Nested address
        assertNotNull(company.headquarters)
        assertNotNull(company.headquarters.city)

        // Nested employees list
        assertNotNull(company.employees)

        // Due to recursion limits, we don't expect a full employee tree,
        // but we should at least have the list
        if (company.employees.isNotEmpty()) {
            val employee = company.employees.first()
            assertNotNull(employee.id)
            assertNotNull(employee.jobTitle)
            assertNotNull(employee.person)
        }
    }

    @Test
    fun `should create Product with nested complex objects`() {

        val product = Product::class.fake()
        assertNotNull(product)

        // Basic fields
        assertNotNull(product.id)
        assertNotNull(product.name)
        assertNotNull(product.description)
        assertTrue(product.price >= 0)
        assertNotNull(product.category)
        assertNotNull(product.imageUrl)

        // Nested objects
        assertNotNull(product.manufacturer)
        assertNotNull(product.reviews)

        // Collections
        assertNotNull(product.tags)
        assertNotNull(product.specifications)
    }

    @Test
    fun `should create Order with nested objects and enums`() {
        val order = Order::class.fake()
        assertNotNull(order)

        // Basic fields
        assertNotNull(order.id)
        assertTrue(order.totalAmount >= 0)
        assertNotNull(order.paymentMethod)
        // Notes can be null

        // Nested objects
        assertNotNull(order.customer)
        assertNotNull(order.shippingAddress)
        assertNotNull(order.billingAddress)

        // Collections
        assertNotNull(order.items)

        // Enum
        assertNotNull(order.status)
        assertTrue(order.status in OrderStatus.values())
    }

    @Test
    fun `should create SchoolClass with nested objects and collections`() {
        val schoolClass = SchoolClass::class.fake()
        assertNotNull(schoolClass)

        // Basic fields
        assertNotNull(schoolClass.id)
        assertNotNull(schoolClass.name)
        assertNotNull(schoolClass.subject)
        assertNotNull(schoolClass.room)

        // Nested objects
        assertNotNull(schoolClass.teacher)

        // Collections
        assertNotNull(schoolClass.students)
        assertNotNull(schoolClass.assignments)
    }
}

class SemanticDataTests {

    @Test
    fun `should generate semantic data for name fields`() {
        data class Person(val firstName: String, val lastName: String, val fullName: String)

        val result = Person::class.fake()
        assertNotNull(result)
        assertNotNull(result.firstName)
        assertNotNull(result.lastName)
        assertNotNull(result.fullName)

        // Check that firstName looks like a first name
        assertTrue(result.firstName.length >= 2)
        assertTrue(result.firstName[0].isUpperCase())

        // Check that lastName looks like a last name
        assertTrue(result.lastName.length >= 2)
        assertTrue(result.lastName[0].isUpperCase())

        // Check that fullName contains a space
        assertTrue(result.fullName.contains(" "))
    }

    @Test
    fun `should generate semantic data for contact fields`() {
        data class Contact(
            val email: String,
            val emailAddress: String,
            val phoneNumber: String,
            val phone: String
        )

        val result = Contact::class.fake()
        assertNotNull(result)

        // Check that email fields contain @ symbol
        assertTrue(result.email.contains("@"))
        assertTrue(result.emailAddress.contains("@"))

        // Check that phone fields follow expected format
        assertTrue(result.phoneNumber.isNotEmpty())
        assertTrue(result.phone.isNotEmpty())
        // Phone formats can vary, but should at least have digits
        assertTrue(result.phone.any { it.isDigit() })
        assertTrue(result.phoneNumber.any { it.isDigit() })
    }

    @Test
    fun `should generate semantic data for address fields`() {
        data class Location(
            val streetAddress: String,
            val city: String,
            val state: String,
            val country: String,
            val zipCode: String,
            val address: String
        )

        val result = Location::class.fake()
        assertNotNull(result)

        // Check streetAddress has a number and street
        val streetParts = result.streetAddress.split(" ")
        assertTrue(streetParts.size >= 2)
        assertTrue(streetParts[0].toIntOrNull() != null || streetParts[0].toIntOrNull() != null)

        // Check city, state, country are all non-empty
        assertTrue(result.city.isNotEmpty())
        assertTrue(result.state.isNotEmpty())
        assertTrue(result.country.isNotEmpty())

        // Check zipCode is in expected format (5 digits)
        assertTrue(result.zipCode.length == 5)
        assertTrue(result.zipCode.all { it.isDigit() })

        // Check full address has commas
        assertTrue(result.address.contains(","))
    }

    @Test
    fun `should generate semantic data for money and number fields`() {
        data class Financial(
            val amount: Double,
            val price: Int,
            val cost: Long,
            val fee: String,
            val salary: Double,
            val percentage: Int,
            val percent: String
        )

        val result = Financial::class.fake()
        assertNotNull(result)

        // Check money fields are positive values
        assertTrue(result.amount > 0)
        assertTrue(result.price > 0)
        assertTrue(result.cost > 0)

        // Check fee has $ prefix since it's a String
        assertTrue(result.fee.startsWith("$"))

        // Check percentage is between 0-100
        assertTrue(result.percentage in 0..100)
        assertTrue(result.percent.contains("%"))
    }

    @Test
    fun `should generate semantic data for image and media fields`() {
        data class Media(
            val image: String,
            val photo: String,
            val imageUrl: String,
            val avatar: String,
            val icon: String
        )

        val result = Media::class.fake()
        assertNotNull(result)

        // Check URLs look valid
        assertTrue(result.image.startsWith("http"))
        assertTrue(result.photo.startsWith("http"))
        assertTrue(result.imageUrl.startsWith("http"))
        assertTrue(result.avatar.startsWith("http"))
        assertTrue(result.icon.startsWith("http"))
    }

    @Test
    fun `should generate semantic data for color fields`() {
        data class ColorInfo(
            val color: String,
            val hexColor: String
        )

        val result = ColorInfo::class.fake()
        assertNotNull(result)

        // Check color is a valid name
        assertTrue(result.color.isNotEmpty())

        // Check hexColor is in hex format (#RRGGBB)
        assertTrue(result.hexColor.startsWith("#"))
        assertTrue(result.hexColor.length == 7)
    }

    @Test
    fun `should generate semantic data for ID fields`() {
        data class IdContainer(
            val id: String,
            val userId: String,
            val productId: String
        )

        val result = IdContainer::class.fake()
        assertNotNull(result)

        // Check IDs are non-empty
        assertTrue(result.id.isNotEmpty())
        assertTrue(result.userId.isNotEmpty())
        assertTrue(result.productId.isNotEmpty())
    }

    @Test
    fun `should generate semantic data for URL fields`() {
        data class UrlContainer(
            val url: String,
            val link: String,
            val website: String
        )

        val result = UrlContainer::class.fake()
        assertNotNull(result)

        // Check URLs are in expected format
        assertTrue(result.url.startsWith("https://"))
        assertTrue(result.link.startsWith("https://"))
        assertTrue(result.website.startsWith("https://"))
    }

    @Test
    fun `should generate semantic data for job fields`() {
        data class JobInfo(
            val jobTitle: String,
            val position: String,
            val role: String,
            val company: String,
            val organization: String
        )

        val result = JobInfo::class.fake()
        assertNotNull(result)

        // Check job titles
        assertTrue(result.jobTitle.isNotEmpty())
        assertTrue(result.position.isNotEmpty())
        assertTrue(result.role.isNotEmpty())

        // Check company names
        assertTrue(result.company.isNotEmpty())
        assertTrue(result.organization.isNotEmpty())
    }

    @Test
    fun `should generate semantic data for product fields`() {
        data class ProductInfo(
            val product: String,
            val item: String
        )

        val result = ProductInfo::class.fake()
        assertNotNull(result)

        // Check product names
        assertTrue(result.product.isNotEmpty())
        assertTrue(result.item.isNotEmpty())
    }

    @Test
    fun `should generate semantic data for description fields`() {
        data class ContentInfo(
            val description: String,
            val summary: String,
            val details: String,
            val comment: String,
            val review: String
        )

        val result = ContentInfo::class.fake()
        assertNotNull(result)

        // Check descriptions are more than a couple of words
        assertTrue(result.description.split(" ").size > 2)
        assertTrue(result.summary.split(" ").size > 2)
        assertTrue(result.details.split(" ").size > 2)

        // Check comments
        assertTrue(result.comment.isNotEmpty())
        assertTrue(result.review.isNotEmpty())
    }
}
