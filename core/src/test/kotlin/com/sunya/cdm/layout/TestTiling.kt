package com.sunya.cdm.layout

import com.sunya.cdm.api.TestSection
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Test [com.sunya.cdm.layout.Tiling]  */
class TestTiling {

    @Test
    fun testTiling() {
        val varshape = longArrayOf(4, 6, 20)
        val chunk = longArrayOf(1, 3, 20)
        val tiling = Tiling(varshape, chunk)

        // look not checking if pt is contained in varshape
        checkEquals(longArrayOf(2, 1, 1), tiling.tile(longArrayOf(2, 5, 20)))
        checkEquals(longArrayOf(2, 3, 20), tiling.index(longArrayOf(2, 1, 1)))
    }

    @Test
    fun testTilingSection() {
        val varshape = longArrayOf(4, 6, 200)
        val chunk = longArrayOf(1, 3, 20)
        val tiling = Tiling(varshape, chunk)

        val indexSection = IndexSpace(TestSection.fromSpec("1:2, 1:2, 0:12"))
        val tiledSection = tiling.section(indexSection)
        assertEquals(IndexSpace(TestSection.fromSpec("1:2, 0:0, 0:0")), tiledSection)
    }

    @Test
    fun testTilingSection2() {
        val varshape = longArrayOf(4, 6, 200)
        val chunk = longArrayOf(1, 3, 20)
        val tiling = Tiling(varshape, chunk)

        val indexSection = IndexSpace(TestSection.fromSpec("1:3, 2:4, 150:199"))
        val tiledSection = tiling.section(indexSection)
        assertEquals(IndexSpace(TestSection.fromSpec("1:3, 0:1, 7:9")), tiledSection)
    }

    @Test
    fun testTilingSection3() {
        val varshape = longArrayOf(4, 6, 200)
        val chunk = longArrayOf(1, 3, 11)
        val tiling = Tiling(varshape, chunk)

        val indexSection = IndexSpace(TestSection.fromSpec("1, 4:5, 33:55"))
        val tiledSection = tiling.section(indexSection)
        assertEquals(IndexSpace(TestSection.fromSpec("1:1, 1:1, 3:5")), tiledSection)
    }

    @Test
    fun testProblem() {
        val varshape = longArrayOf(60, 120)
        val chunk = longArrayOf(15, 30)
        val tiling = Tiling(varshape, chunk)

        val indexSection = IndexSpace(TestSection.fromSpec("20:39,40:79"))
        val tileSection = tiling.section(indexSection)
        assertEquals(IndexSpace(TestSection.fromSpec("1:2,1:2")), tileSection)
        
        val tileOdometer = IndexND(tileSection, tiling.tileShape) // loop over tiles we want
        for (tile in tileOdometer) {
            val key = tiling.index(tile) // convert to index "keys"
            // println("tile = ${tile.contentToString()} key = ${key.contentToString()}")
        }
    }

    @Test
    fun testProblem2() {
        val varshape = longArrayOf(8395, 781, 385)
        val chunk = longArrayOf(1, 30, 30)
        val tiling = Tiling(varshape, chunk)
        println("tiling = $tiling")

        val indexSection = IndexSpace(TestSection.fromSpec("0:9, 0:780, 0:384"))
        val tileSection = tiling.section(indexSection)
        assertEquals(IndexSpace(TestSection.fromSpec("0:9,0:26,0:12")), tileSection)

        val tileOdometer = IndexND(tileSection, tiling.tileShape) // loop over tiles we want
        var count = 0
        for (tile in tileOdometer) {
            val key = tiling.index(tile) // convert to index "keys"
            // println("tile = ${tile.contentToString()} key = ${key.contentToString()}")
            count++
        }
        println("tile count = $count")
    }

    fun checkEquals(ia1 : LongArray, ia2 : LongArray) {
        if (!ia1.contentEquals(ia2)) {
            println("${ia1.contentToString()} != ${ia2.contentToString()}")
        }
        assertTrue(ia1.contentEquals(ia2))
    }

}