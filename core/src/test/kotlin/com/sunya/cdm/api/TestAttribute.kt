package com.sunya.cdm.api

import org.junit.jupiter.api.Test
import kotlin.test.*

/** Test {@link dev.ucdm.core.api.Attribute} */
class TestAttribute {

    @Test
    fun testBasics() {
        val att = Attribute("name", Datatype.DOUBLE, listOf(3.14f, .0015f))
        assertEquals("name", att.name)
        assertEquals(Datatype.DOUBLE, att.datatype)
        assertEquals(2, att.values.size)
        assertEquals("3.14".toFloat(), att.values[0])
        assertEquals(".0015".toFloat(), att.values[1])
        assertFalse(att.isString)
    }

    @Test
    fun testBuilder() {
        val att: Attribute = Attribute.Builder().setName("name").setDatatype(Datatype.FLOAT)
            .setValues(listOf(3.14f, .0015f)).build()
        assertEquals("name", att.name)
        assertEquals(Datatype.FLOAT, att.datatype)
        assertEquals(2, att.values.size)
        assertEquals("3.14".toFloat(), att.values[0])
        assertEquals(".0015".toFloat(), att.values[1])
        assertEquals(3.14f, att.values[0])
        assertEquals(.0015f, att.values[1])
        assertFalse(att.isString)
    }

    @Test
    fun testStringBuilder() {
        val att = Attribute("name","valuable")
        assertEquals("name", att.name)
        assertEquals(Datatype.STRING, att.datatype)
        assertEquals(1, att.values.size)
        assertEquals("valuable", att.values[0])
        assertTrue(att.isString)
    }

    @Test
    fun testStringWithNulls() {
        val att: Attribute = Attribute.Builder().setName("name").setDatatype(Datatype.STRING)
            .setValues(listOf("what", null)).build()
        assertEquals("name", att.name)
        assertEquals(Datatype.STRING, att.datatype)
        assertEquals(2, att.values.size)
        assertEquals("what", att.values[0])
        assertEquals(null, att.values[1])
        assertNull(att.values[1])
        assertTrue(att.isString)
    }

    @Test
    fun testEmptyValues() {
        val att: Attribute = Attribute.Builder().setName("name").setDatatype(Datatype.STRING)
            .build()
        assertEquals("name", att.name)
        assertEquals(Datatype.STRING, att.datatype)
        assertEquals(0, att.values.size)
        assertTrue(att.isString)
    }

    // special case to match c library
    @Test
    fun testEmptyCharValues() {
        val att: Attribute = Attribute.Builder().setName("name").setDatatype(Datatype.CHAR)
            .build()
        assertEquals("name", att.name)
        assertEquals(Datatype.STRING, att.datatype)
        assertEquals(1, att.values.size)
        assertEquals("", att.values[0])
        assertTrue(att.isString)
    }

    @Test
    fun testCharValue() {
        val att: Attribute = Attribute.Builder().setName("name").setDatatype(Datatype.CHAR)
            .setValues(listOf('a', 'b', 'c'))
            .build()
        assertEquals("name", att.name)
        assertEquals(Datatype.CHAR, att.datatype)
        assertEquals(3, att.values.size)
        assertEquals('a', att.values[0])
        assertEquals('b', att.values[1])
        assertEquals('c', att.values[2])
        assertFalse(att.isString)
    }

    @Test
    fun testCharStringValue() {
        val att: Attribute = Attribute.Builder().setName("name").setDatatype(Datatype.CHAR)
            .setValues(listOf("abc", "def"))
            .build()
        assertEquals("name", att.name)
        assertEquals(Datatype.STRING, att.datatype)
        assertEquals(2, att.values.size)
        assertEquals("abc", att.values[0])
        assertEquals("def", att.values[1])
        assertTrue(att.isString)
    }
}