package com.sunya.cdm.api

import org.junit.jupiter.api.Test
import kotlin.test.*

class TestDimension {

    @Test
    fun testBuilder() {
        val dim = Dimension("name", 99)
        assertEquals("name", dim.name)
        assertEquals(99L, dim.length)
        assertTrue(dim.isShared)
    }

    @Test
    fun testNotShared() {
        val dim = Dimension("dim", 9999999999999999, false)
        assertEquals("dim", dim.name)
        assertEquals(9999999999999999, dim.length)
        assertFalse(dim.isShared)
    }

    @Test
    fun testAnonInt() {
        val dim = Dimension(9)
        assertEquals("", dim.name)
        assertEquals(9, dim.length)
        assertFalse(dim.isShared)
    }

    @Test
    fun testAnonLong() {
        val dim = Dimension(999L)
        assertEquals("", dim.name)
        assertEquals(999, dim.length)
        assertFalse(dim.isShared)
    }
}