package com.sunya.cdm.iosp

import com.sunya.cdm.api.Section
import com.sunya.cdm.layout.Chunker
import com.sunya.cdm.layout.IndexSpace
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/** Test [com.sunya.cdm.iosp.IndexChunker]  */
class TestChunker {

    fun runChunkerTest(dataChunk: IndexSpace, wantSection: Section,
                       expectElems : Int?,
                       expectNchunks : Int,
                       check : Boolean = true,
                       merge : Boolean = true,
                       expect : (Int) -> Pair<Int, Int>) {
        println("Chunker dataChunk = ${dataChunk.makeSection()} wantSection = [$wantSection]")

        val layout = Chunker(dataChunk, 4, wantSection, merge)
        val expectNelems = expectElems ?: (dataChunk.totalElements.toInt() / expectNchunks)
        var count = 0
        var totalNelems = 0
        while (layout.hasNext()) {
            val chunk = layout.next()
            println(" chunk = $chunk")
            if (check) {
                val (srcElem, dstElem) = expect(count)
                assertEquals(srcElem, chunk.srcElem.toInt(), "srcPos ")
                assertEquals(expectNelems, chunk.nelems, "nelems ")
                assertEquals(dstElem, chunk.destElem.toInt(), "destElem ")
            }
            totalNelems += chunk.nelems
            count++
        }
        if (check) {
            assertEquals(expectNchunks, count, "nchunks ")
            assertEquals(expectNelems * expectNchunks, totalNelems, "totalNelems ")
        }
        println("nchunks expect ${expectNchunks}, actual ${count}")
        println("totalElements expect ${expectNelems * expectNchunks}, actual ${totalNelems}")
    }

    @Test
    fun testFull2() {
        val shape = intArrayOf(3, 12)
        val oneChunk = IndexSpace(shape)
        val oneSection = Section(shape)
        runChunkerTest(oneChunk, oneSection, null, 3, true, false) {
            count -> Pair(12 * count, 12 * count)
        }
    }

   @Test
    fun testFull2m() {
        val shape = intArrayOf(3, 12)
        val oneChunk = IndexSpace(shape)
        val oneSection = Section(shape)
        runChunkerTest(oneChunk, oneSection, null, 1, true, true) { Pair(0,0) }
    }

    @Test
    fun testFirstHalf() {
        val wantSection = Section(intArrayOf(2, 10, 20))
        val dataChunk = IndexSpace(intArrayOf(1, 10, 20))
        runChunkerTest(dataChunk, wantSection, null,10, true, false) {
            count -> Pair(20 * count, 20 * count)
        }
    }

    @Test
    fun testFirstHalfm() {
        val wantSection = Section(intArrayOf(2, 10, 20))
        val dataChunk = IndexSpace(intArrayOf(1, 10, 20))
        runChunkerTest(dataChunk, wantSection, null,1, true, true) { Pair(0,0) }
    }

    @Test
    fun testSecondHalf() {
        val wantSection = Section(intArrayOf(2, 10, 20))
        val dataChunk = IndexSpace(Section("1, 0:9, 0:19"))
        runChunkerTest(dataChunk, wantSection, null,10, true, false) { count ->
                Pair(20 * count, 200 + 20 * count)
        }
    }

    @Test
    fun testSecondHalfm() {
        val wantSection = Section(intArrayOf(2, 10, 20))
        val dataChunk = IndexSpace(Section("1, 0:9, 0:19"))
        runChunkerTest(dataChunk, wantSection, null,1, true, true) { Pair(0, 200) }
    }

    @Test
    fun testMiddleHalf() {
        val wantSection = Section(intArrayOf(2, 10, 20))
        val dataChunk = IndexSpace(Section("0:1, 5:9, 0:19"))
        runChunkerTest(dataChunk, wantSection, null,10, true, false) { count ->
            val offset = if (count < 5) 100 else 200
            Pair(20 * count, offset + 20 * count)
        }
    }

    @Test
    fun testMiddleHalfm() {
        val wantSection = Section(intArrayOf(2, 10, 20))
        val dataChunk = IndexSpace(Section("0:1, 5:9, 0:19"))
        runChunkerTest(dataChunk, wantSection, null,2, true, true) { count ->
            val offset = if (count < 1) 100 else 300
            Pair(100 * count, offset)
        }
    }

    @Test
    fun testFastIndex() {
        val wantSection = Section(intArrayOf(2, 10, 20))
        val dataChunk = IndexSpace(Section("0:1, 0:9, 5:14"))
        runChunkerTest(dataChunk, wantSection, null,20, true, false) { count ->
            val offset = 5
            Pair(10 * count, offset + 20 * count)
        }
    }

    @Test
    fun testFastIndexm() {
        val wantSection = Section(intArrayOf(2, 10, 20))
        val dataChunk = IndexSpace(Section("0:1, 0:9, 5:14"))
        runChunkerTest(dataChunk, wantSection, null,20, true, true) { count ->
            val offset = 5
            Pair(10 * count, offset + 20 * count)
        }
    }

    @Test
    fun testSectionOffset() {
        val wantSection = Section("5:6, 20:29, 5:25")
        val dataChunk = IndexSpace(Section("5:5, 20:29, 5:25"))
        runChunkerTest(dataChunk, wantSection,null,10, true, false) { count ->
            Pair(21 * count, 21 * count)
        }
    }

    @Test
    fun testSectionOffsetm() {
        val wantSection = Section("5:6, 20:29, 5:25")
        val dataChunk = IndexSpace(Section("5:5, 20:29, 5:25"))
        runChunkerTest(dataChunk, wantSection,null,1, true, true) { Pair(0, 0) }
    }

    @Test
    fun testOffsetLastQuarter() {
        val wantSection = Section("5:6, 20:29, 5:25")
        val dataChunk = IndexSpace(Section("5:5, 25:29, 5:25"))
        runChunkerTest(dataChunk, wantSection, null,5, true, false) { count ->
            val offset = 105
            Pair(21 * count, offset + 21 * count)
        }
    }

    @Test
    fun testOffsetLastQuarterm() {
        val wantSection = Section("5:6, 20:29, 5:25")
        val dataChunk = IndexSpace(Section("5:5, 25:29, 5:25"))
        runChunkerTest(dataChunk, wantSection, null,1, true, true) { Pair(0, 105) }
    }

    @Test
    fun testOffsetHalf() {
        val wantSection = Section("5:6, 20:29, 5:25")
        val dataChunk = IndexSpace(Section("5:6, 25:29, 5:25"))
        runChunkerTest(dataChunk, wantSection, null,10, true, false) { count ->
            val offset = if (count < 5) 105 else 210
            Pair(21 * count, offset + 21 * count)
        }
    }

    @Test
    fun testOffsetHalfm() {
        val wantSection = Section("5:6, 20:29, 5:25")
        val dataChunk = IndexSpace(Section("5:6, 25:29, 5:25"))
        runChunkerTest(dataChunk, wantSection, null,2, true, true) { count ->
            val offset = if (count < 1) 105 else 315
            Pair(105 * count, offset)
        }
    }

    @Test
    fun testOffsetFastIndex() {
        val wantSection = Section("5:6, 20:29, 5:25")
        val dataChunk = IndexSpace(Section("5:6, 20:29, 15:24"))
        runChunkerTest(dataChunk, wantSection, null,20, true, false) { count ->
            Pair(10 * count, 10 + 21 * count)
        }
    }

    @Test
    fun testOffsetFastIndexm() {
        val wantSection = Section("5:6, 20:29, 5:25")
        val dataChunk = IndexSpace(Section("5:6, 20:29, 15:24"))
        runChunkerTest(dataChunk, wantSection, null,20, true, true) { count ->
            Pair(10 * count, 10 + 21 * count)
        }
    }

    @Test
    fun testOffset212() {
        val wantSection = Section("5:8, 20:29, 5:25")
        val dataChunk = IndexSpace(Section("6:7, 20:29, 15:24"))
        runChunkerTest(dataChunk, wantSection, null,20, true, false)  { count ->
            Pair(10 * count, 220 + 21 * count)
        }
    }

    @Test
    fun testOffset212m() {
        val wantSection = Section("5:8, 20:29, 5:25")
        val dataChunk = IndexSpace(Section("6:7, 20:29, 15:24"))
        runChunkerTest(dataChunk, wantSection, null,20, false, true)  { count ->
            Pair(10 * count, 220 + 21 * count)
        }
    }

    @Test
    fun testChunkUpper() {
        val wantSection = Section("5:8, 20:29, 5:25")
        val dataChunk = IndexSpace(Section("4:5, 20:29, 5:25"))
        runChunkerTest(dataChunk, wantSection, 21, 10, true, false) { count ->
            Pair(210 + 21 * count, 21 * count)
        }
    }

    @Test
    fun testChunkUpperm() {
        val wantSection = Section("5:8, 20:29, 5:25")
        val dataChunk = IndexSpace(Section("4:5, 20:29, 5:25"))
        runChunkerTest(dataChunk, wantSection, 210, 1, true, true) { count ->
            Pair(210 + 21 * count, 21 * count)
        }
    }

    @Test
    fun testChunkLower() {
        val wantSection = Section("5:8, 20:29, 5:24")
        val dataChunk = IndexSpace(Section("7:12, 20:29, 5:24"))
        runChunkerTest(dataChunk, wantSection, 20, 20, true, false) { count ->
            Pair(20 * count, 400 + 20 * count)
        }
    }


    @Test
    fun testChunkLowerm() {
        val wantSection = Section("5:8, 20:29, 5:24")
        val dataChunk = IndexSpace(Section("7:12, 20:29, 5:24"))
        runChunkerTest(dataChunk, wantSection, 400, 1, true, true) { count ->
            Pair(20 * count, 400 + 20 * count)
        }
    }

    @Test
    fun testChunkUpper3() {
        val wantSection = Section("5:8, 20:29, 5:24")
        val dataChunk = IndexSpace(Section("2:5, 15:25, 0:30"))
        runChunkerTest(dataChunk, wantSection, 20, 6, true, false) { count ->
            Pair(1183 + 31 * count, 20 * count)
        }
    }


    @Test
    fun testChunkUpper3m() {
        val wantSection = Section("5:8, 20:29, 5:24")
        val dataChunk = IndexSpace(Section("2:5, 15:25, 0:30"))
        runChunkerTest(dataChunk, wantSection, 20, 6, true, true) { count ->
            Pair(1183 + 31 * count, 20 * count)
        }
    }

    @Test
    fun testChunkLower215() {
        val wantSection = Section("5:8, 20:29, 5:24")
        val dataChunk = IndexSpace(Section("7:12, 29:30, 20:29"))
        runChunkerTest(dataChunk, wantSection, 5, 2, true, false) { count ->
            Pair(20 * count, 595 + 200 * count)
        }
    }

    @Test
    fun testChunkLower215m() {
        val wantSection = Section("5:8, 20:29, 5:24")
        val dataChunk = IndexSpace(Section("7:12, 29:30, 20:29"))
        runChunkerTest(dataChunk, wantSection, 5, 2, true, true) { count ->
            Pair(20 * count, 595 + 200 * count)
        }
    }

    @Test
    fun testChunkLower225() {
        val wantSection = Section("5:8, 20:29, 5:24")
        val dataChunk = IndexSpace(Section("7:12, 28:30, 20:29"))
        runChunkerTest(dataChunk, wantSection, 5, 4, true, false) { count ->
            val src = if (count < 2) 0 else 10
            val dst = if (count < 2) 575 else 735
            Pair(src + 10 * count, dst + 20 * count)
        }
    }

    @Test
    fun testChunkLower225m() {
        val wantSection = Section("5:8, 20:29, 5:24")
        val dataChunk = IndexSpace(Section("7:12, 28:30, 20:29"))
        runChunkerTest(dataChunk, wantSection, 5, 4, true, true) { count ->
            val src = if (count < 2) 0 else 10
            val dst = if (count < 2) 575 else 735
            Pair(src + 10 * count, dst + 20 * count)
        }
    }

    /*
    @Test
    fun testOffsetStride() {
        val full = intArrayOf(4, 10, 20)
        val wantSection = Section("0:3:2, 5:9, 0:11")
        runLayoutTransfer(full, wantSection, 12,  10, true)  {
            if (it < 5) 100L + 20 * it else 400L + 20 * it
        }
    }

     */
}