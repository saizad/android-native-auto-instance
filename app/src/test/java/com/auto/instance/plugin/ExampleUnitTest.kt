package com.auto.instance.plugin

import com.auto.instance.plugin.generator.TokenDataGenerator
import com.auto.instance.plugin.models.Profile
import com.auto.instance.plugin.models.Token
import com.auto.instance.plugin.models.school.School
import com.reflect.instance.fake
import com.reflect.instance.sample.DefaultFakeDataGenerator
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
        println(Token::class.fake(TokenDataGenerator()))
    }
}