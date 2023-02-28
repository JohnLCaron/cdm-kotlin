package com.sunya.cdm.iosp

import com.sunya.cdm.api.Section
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestTiling {

    @Test
    fun testTiling() {
        val varshape = intArrayOf(4, 6, 20)
        val chunk = intArrayOf(1, 3, 20)
        val tiling = TilingOld(varshape, chunk)

        // look not checking if pt is contained in varshape
        checkEquals(intArrayOf(2, 1, 1), tiling.tile(intArrayOf(2, 5, 20)))
        checkEquals(intArrayOf(2, 3, 20), tiling.index(intArrayOf(2, 1, 1)))
    }

    @Test
    fun testTilingSection() {
        val varshape = intArrayOf(4, 6, 200)
        val chunk = intArrayOf(1, 3, 20)
        val tiling = TilingOld(varshape, chunk)

        val indexSection = Section("1:2, 1:2, 0:12")
        val tiledSection = tiling.section(indexSection)
        assertEquals(Section("1:2, 0:0, 0:0"), tiledSection)
    }

    @Test
    fun testTilingSection2() {
        val varshape = intArrayOf(4, 6, 200)
        val chunk = intArrayOf(1, 3, 20)
        val tiling = TilingOld(varshape, chunk)

        val indexSection = Section("1:3, 2:4, 150:199")
        val tiledSection = tiling.section(indexSection)
        assertEquals(Section("1:3, 0:1, 7:9"), tiledSection)
    }

    @Test
    fun testTilingSection3() {
        val varshape = intArrayOf(4, 6, 200)
        val chunk = intArrayOf(1, 3, 11)
        val tiling = TilingOld(varshape, chunk)

        val indexSection = Section("1, 4:5, 33:55")
        val tiledSection = tiling.section(indexSection)
        assertEquals(Section("1:1, 1:1, 3:5"), tiledSection)
    }

    fun checkEquals(ia1 : IntArray, ia2 : IntArray) {
        if (!ia1.contentEquals(ia2)) {
            println("${ia1.contentToString()} != ${ia2.contentToString()}")
        }
        assertTrue(ia1.contentEquals(ia2))
    }

}