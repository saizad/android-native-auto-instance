package com.reflect.instance

import org.junit.Assert.*
import org.junit.Test
import kotlin.reflect.KClass

class FakeHelperTest{

    data class SampleData(val id: Int, val name: String)

    @Test
    fun `fake should return generated instances`() {
        val fakeHelper = FakeHelper()
        val sampleClass: KClass<SampleData> = SampleData::class
        val result = fakeHelper.fake(sampleClass, 3)
        assertEquals(3, result.size)
    }


}