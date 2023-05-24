package com.sunya.cdm.api

import com.sunya.cdm.layout.IndexND
import com.sunya.cdm.layout.IndexSpace
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TestSection {

    @Test
    fun testSectionP() {
        val ranges = listOf(
            LongProgression.fromClosedRange(0, 10, 1),
            LongProgression.fromClosedRange(0, 10, 1),
            LongProgression.fromClosedRange(0, 10, 1),
        )
        val sectionp = SectionPartial(ranges)

        val have = SectionPartial.fromSpec("101:101,0:1919")

    }

    @Test
    fun sectionToIndexND() {
        val ranges = listOf(
            LongProgression.fromClosedRange(0, 10, 1),
            LongProgression.fromClosedRange(5, 10, 1),
            LongProgression.fromClosedRange(10, 10, 1),
        )
        // SectionL(val ranges : List<LongProgression>, val varShape : LongArray)
        val section = Section(ranges, longArrayOf(11, 11, 11))

        // IndexND(val section : IndexSpace, val datashape : LongArray)
        val indexND = IndexND(section)
        // class IndexND(val section : IndexSpace, val datashape : LongArray) : Iterable<LongArray> {
        val space : IndexSpace = indexND.section

        // IndexSpace is the same as List<LongProgression>, except for strides
        assertEquals(ranges, space.ranges)

        // sectionL is the same as IndexND, except for strides
        val rt2 = Section(indexND.section.ranges, indexND.datashape)
        assertEquals(section, rt2)
    }

    @Test
    fun sectionToIndexNDwithStride() {
        val ranges = listOf(
            LongProgression.fromClosedRange(0, 10, 1),
            LongProgression.fromClosedRange(5, 10, 2),
            LongProgression.fromClosedRange(10, 10, 1),
        )
        // SectionL(val ranges : List<LongProgression>, val varShape : LongArray)
        val section = Section(ranges, longArrayOf(11, 11, 11))

        // IndexND(val section : IndexSpace, val datashape : LongArray)
        val indexND = IndexND(section)
        // class IndexND(val section : IndexSpace, val datashape : LongArray) : Iterable<LongArray> {
        val space : IndexSpace = indexND.section

        // IndexSpace is the same as List<LongProgression>, except for strides
        assertNotEquals(ranges, space.ranges)

        // sectionL is the same as IndexND, except for strides
        val rt2 = Section(indexND.section.ranges, indexND.datashape)
        assertNotEquals(section, rt2)
    }

    //          main/test/clibs
    // SectionP   18/15/56
    // SectionL   72/21/16
    // IndexSpace 44/27/2
    // IndexND    14/7/0
    // Chunker    17/2/0
    // Tiling      7/7/0

    companion object {
        // for testing only, assumes varshape is last() + 1
        fun fromSpec(spec : String) : Section {
            val sp = SectionPartial.fromSpec(spec)
            val varShape = sp.ranges.map { it!!.last + 1 }.toLongArray()
            return SectionPartial.fill(sp, varShape)
        }
    }
}
