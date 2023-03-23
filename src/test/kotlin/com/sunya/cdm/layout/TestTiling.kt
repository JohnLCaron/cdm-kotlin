package com.sunya.cdm.layout

import com.sunya.cdm.api.Section
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Test [com.sunya.cdm.layout.Tiling]  */
class TestTiling {

    @Test
    fun testTiling() {
        val varshape = intArrayOf(4, 6, 20)
        val chunk = intArrayOf(1, 3, 20)
        val tiling = Tiling(varshape, chunk)

        // look not checking if pt is contained in varshape
        checkEquals(intArrayOf(2, 1, 1), tiling.tile(intArrayOf(2, 5, 20)))
        checkEquals(intArrayOf(2, 3, 20), tiling.index(intArrayOf(2, 1, 1)))
    }

    @Test
    fun testTilingSection() {
        val varshape = intArrayOf(4, 6, 200)
        val chunk = intArrayOf(1, 3, 20)
        val tiling = Tiling(varshape, chunk)

        val indexSection = IndexSpace(Section("1:2, 1:2, 0:12"))
        val tiledSection = tiling.section(indexSection)
        assertEquals(IndexSpace(Section("1:2, 0:0, 0:0")), tiledSection)
    }

    @Test
    fun testTilingSection2() {
        val varshape = intArrayOf(4, 6, 200)
        val chunk = intArrayOf(1, 3, 20)
        val tiling = Tiling(varshape, chunk)

        val indexSection = IndexSpace(Section("1:3, 2:4, 150:199"))
        val tiledSection = tiling.section(indexSection)
        assertEquals(IndexSpace(Section("1:3, 0:1, 7:9")), tiledSection)
    }

    @Test
    fun testTilingSection3() {
        val varshape = intArrayOf(4, 6, 200)
        val chunk = intArrayOf(1, 3, 11)
        val tiling = Tiling(varshape, chunk)

        val indexSection = IndexSpace(Section("1, 4:5, 33:55"))
        val tiledSection = tiling.section(indexSection)
        assertEquals(IndexSpace(Section("1:1, 1:1, 3:5")), tiledSection)
    }

    @Test
    fun testProblem() {
        val varshape = intArrayOf(60, 120)
        val chunk = intArrayOf(15, 30)
        val tiling = Tiling(varshape, chunk)

        val indexSection = IndexSpace(Section("20:39,40:79"))
        val tileSection = tiling.section(indexSection)
        assertEquals(IndexSpace(Section("1:2,1:2")), tileSection)
        
        val tileOdometer = Odometer(tileSection, tiling.tileShape) // loop over tiles we want
        while (!tileOdometer.isDone()) {
            val tile = tileOdometer.current
            val key = tiling.index(tile) // convert to index "keys"
            // println("tile = ${tile.contentToString()} key = ${key.contentToString()}")
            tileOdometer.incr()
        }

    }

    @Test
    fun testProblem2() {
        val varshape = intArrayOf(8395, 781, 385)
        val chunk = intArrayOf(1, 30, 30)
        val tiling = Tiling(varshape, chunk)
        println("tiling = $tiling")

        val indexSection = IndexSpace(Section("0:9, 0:780, 0:384"))
        val tileSection = tiling.section(indexSection)
        assertEquals(IndexSpace(Section("0:9,0:26,0:12")), tileSection)

        val tileOdometer = Odometer(tileSection, tiling.tileShape) // loop over tiles we want
        var count = 0
        while (!tileOdometer.isDone()) {
            val tile = tileOdometer.current
            val key = tiling.index(tile) // convert to index "keys"
            // println("tile = ${tile.contentToString()} key = ${key.contentToString()}")
            tileOdometer.incr()
            count++
        }
        println("tile count = $count")
    }

    fun checkEquals(ia1 : IntArray, ia2 : IntArray) {
        if (!ia1.contentEquals(ia2)) {
            println("${ia1.contentToString()} != ${ia2.contentToString()}")
        }
        assertTrue(ia1.contentEquals(ia2))
    }

}