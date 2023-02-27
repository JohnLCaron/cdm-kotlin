package com.sunya.cdm.iosp

import com.sunya.cdm.api.Section
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestOdometer {

    @Test
    fun testFull() {
        val dataSection = Section(intArrayOf(2, 3, 4))
        val odo = Odometer(dataSection)
        var count = 0
        for (ia in odo) {
            println(ia.contentToString())
            assertTrue(dataSection.contains(ia))
            count++
        }
        assertEquals(dataSection.size().toInt(), count)
    }

    @Test
    fun testSlice() {
        val dataSection = Section("1:2, 2:3, 3:4")
        val odo = Odometer(dataSection)
        var count = 0
        for (ia in odo) {
            println(ia.contentToString())
            assertTrue(dataSection.contains(ia))
            count++
        }
        assertEquals(dataSection.size().toInt(), count)
    }

    @Test
    fun testSlice2() {
        val dataSection = Section("1:2, 2:3, 47:47")
        val odo = Odometer(dataSection)
        var count = 0
        for (ia in odo) {
            println(ia.contentToString())
            assertTrue(dataSection.contains(ia))
            count++
        }
        assertEquals(dataSection.size().toInt(), count)
    }

    @Test
    fun testTiling() {
        val tileSection = Section("1:2, 2:3, 0:12")
        val tileOdometer = Odometer(tileSection)
        val incrDigit = 1
        var count = 0
        while (!tileOdometer.isDone()) {
            val tile = tileOdometer.current
            println(tile.contentToString())
            assertTrue(tileSection.contains(tile))
            count++

            if (incrDigit >= 0) {
                tileOdometer.incr(incrDigit)
            } else {
                break
            }
        }
        assertEquals(4, count)
    }

    @Test
    fun testTilingRank1() {
        val tileSection = Section("0:12")
        val tileOdometer = Odometer(tileSection)
        val incrDigit = -1
        var count = 0
        while (!tileOdometer.isDone()) {
            val tile = tileOdometer.current
            println(tile.contentToString())
            assertTrue(tileSection.contains(tile))
            count++

            if (incrDigit >= 0) {
                tileOdometer.incr(incrDigit)
            } else {
                break
            }
        }
        assertEquals(1, count)
    }
}