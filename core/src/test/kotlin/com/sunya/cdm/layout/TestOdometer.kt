package com.sunya.cdm.layout

import com.sunya.cdm.api.TestSection
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestOdometer {

    @Test
    fun testFull() {
        val shape = longArrayOf(2, 3, 4)
        val dataSection = IndexSpace(shape)
        val odo = IndexND(dataSection, shape)
        var count = 0L
        for (ia in odo) {
            println("${ ia.contentToString()} = ${odo.element()}")
            assertTrue(dataSection.contains(ia))
            assertEquals(count, odo.element())
            count++
        }
        assertEquals(dataSection.totalElements, count.toLong())
    }

    @Test
    fun testSlice() {
        val shape = longArrayOf(3, 4, 5)
        val dataSection = IndexSpace(TestSection.fromSpec("1:2, 2:3, 3:4"))
        val odo = IndexND(dataSection, shape)
        var count = 0
        for (ia in odo) {
            println("${ ia.contentToString()} = ${odo.element()}")
            assertTrue(dataSection.contains(ia))
            count++
        }
        assertEquals(dataSection.totalElements.toInt(), count)
    }

    @Test
    fun testSlice2() {
        val shape = longArrayOf(3, 4, 50)
        val dataSection = IndexSpace(TestSection.fromSpec("1:2, 2:3, 47:47"))
        val odo = IndexND(dataSection, shape)
        var count = 0
        for (ia in odo) {
            println("${ ia.contentToString()} = ${odo.element()}")
            assertTrue(dataSection.contains(ia))
            count++
        }
        assertEquals(dataSection.totalElements.toInt(), count)
    }
}