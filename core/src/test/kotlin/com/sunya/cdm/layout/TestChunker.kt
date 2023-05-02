package com.sunya.cdm.layout

import com.sunya.cdm.api.SectionL
import com.sunya.cdm.api.SectionP
import com.sunya.cdm.api.toLongArray
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/** Test [com.sunya.cdm.layout.Chunker]  */
class TestChunker {

    fun runChunkerTest(dataChunk: IndexSpace,
                       wantSection: SectionL,
                       expectElems : Int?,
                       expectNchunks : Int,
                       check : Boolean = true,
                       merge : Merge = Merge.all,
                       expect : (Int) -> Pair<Int, Int>) { // (srcElem, dstElem)
        println("Chunker dataChunk = ${dataChunk} wantSection = [$wantSection]")

        val layout = Chunker(dataChunk, IndexSpace(wantSection), merge)
        println("dataChunk $dataChunk")
        println("wantSection $wantSection")
        println("wantSpace ${IndexSpace(wantSection)}")
        println("layout $layout")

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
        val shape = intArrayOf(3, 12).toLongArray()
        val oneChunk = IndexSpace(shape)
        val oneSection = SectionL(shape)
        runChunkerTest(oneChunk, oneSection, null, 3, true, Merge.none) {
            count -> Pair(12 * count, 12 * count)
        }
    }

   @Test
    fun testFull2m() {
        val shape = intArrayOf(3, 12).toLongArray()
        val oneChunk = IndexSpace(shape)
        val oneSection = SectionL(shape)
        runChunkerTest(oneChunk, oneSection, null, 1, true) { Pair(0,0) }
    }

    @Test
    fun testFirstHalf() {
        val wantSection = SectionL(longArrayOf(2, 10, 20))
        val dataChunk = IndexSpace(intArrayOf(1, 10, 20))
        runChunkerTest(dataChunk, wantSection, null,10, true, Merge.none) {
            count -> Pair(20 * count, 20 * count)
        }
    }

    @Test
    fun testFirstHalfm() {
        val wantSection = SectionL(longArrayOf(2, 10, 20))
        val dataChunk = IndexSpace(intArrayOf(1, 10, 20))
        runChunkerTest(dataChunk, wantSection, null,1, true) { Pair(0,0) }
    }

    @Test
    fun testSecondHalf() {
        val wantSection = SectionL(longArrayOf(2, 10, 20))
        val dataChunk = makeChunk("1, 0:9, 0:19")
        runChunkerTest(dataChunk, wantSection, null,10, true, Merge.none) { count ->
                Pair(20 * count, 200 + 20 * count)
        }
    }

    @Test
    fun testSecondHalfm() {
        val wantSection = SectionL(longArrayOf(2, 10, 20))
        val dataChunk = makeChunk("1, 0:9, 0:19")
        runChunkerTest(dataChunk, wantSection, null,1, true) { Pair(0, 200) }
    }

    @Test
    fun testMiddleHalf() {
        val wantSection = SectionL(longArrayOf(2, 10, 20))
        val dataChunk = makeChunk("0:1, 5:9, 0:19")
        runChunkerTest(dataChunk, wantSection, null,10, true, Merge.none) { count ->
            val offset = if (count < 5) 100 else 200
            Pair(20 * count, offset + 20 * count)
        }
    }

    @Test
    fun testMiddleHalfm() {
        val wantSection = SectionL(longArrayOf(2, 10, 20))
        val dataChunk = makeChunk("0:1, 5:9, 0:19")
        runChunkerTest(dataChunk, wantSection, null,2, true) { count ->
            val offset = if (count < 1) 100 else 300
            Pair(100 * count, offset)
        }
    }

    @Test
    fun testFastIndex() {
        val wantSection = SectionL(longArrayOf(2, 10, 20))
        val dataChunk = makeChunk("0:1, 0:9, 5:14")
        runChunkerTest(dataChunk, wantSection, null,20, true, Merge.none) { count ->
            val offset = 5
            Pair(10 * count, offset + 20 * count)
        }
    }

    @Test
    fun testFastIndexm() {
        val wantSection = SectionL(longArrayOf(2, 10, 20))
        val dataChunk = makeChunk("0:1, 0:9, 5:14")
        runChunkerTest(dataChunk, wantSection, null,20, true) { count ->
            val offset = 5
            Pair(10 * count, offset + 20 * count)
        }
    }

    @Test
    fun testSectionOffset() {
        val wantSection = SectionL.fromSpec("5:6, 20:29, 5:25")
        val dataChunk = makeChunk("5:5, 20:29, 5:25")
        runChunkerTest(dataChunk, wantSection,null,10, true, Merge.none) { count ->
            Pair(21 * count, 21 * count)
        }
    }

    @Test
    fun testSectionOffsetm() {
        val wantSection = SectionL.fromSpec("5:6, 20:29, 5:25")
        val dataChunk = makeChunk("5:5, 20:29, 5:25")
        runChunkerTest(dataChunk, wantSection,null,1, true) { Pair(0, 0) }
    }

    @Test
    fun testOffsetLastQuarter() {
        val wantSection = SectionL.fromSpec("5:6, 20:29, 5:25")
        val dataChunk = makeChunk("5:5, 25:29, 5:25")
        runChunkerTest(dataChunk, wantSection, null,5, true, Merge.none) { count ->
            val offset = 105
            Pair(21 * count, offset + 21 * count)
        }
    }

    @Test
    fun testOffsetLastQuarterm() {
        val wantSection = SectionL.fromSpec("5:6, 20:29, 5:25")
        val dataChunk = makeChunk("5:5, 25:29, 5:25")
        runChunkerTest(dataChunk, wantSection, null,1, true) { Pair(0, 105) }
    }

    @Test
    fun testOffsetHalf() {
        val wantSection = SectionL.fromSpec("5:6, 20:29, 5:25")
        val dataChunk = makeChunk("5:6, 25:29, 5:25")
        runChunkerTest(dataChunk, wantSection, null,10, true, Merge.none) { count ->
            val offset = if (count < 5) 105 else 210
            Pair(21 * count, offset + 21 * count)
        }
    }

    @Test
    fun testOffsetHalfm() {
        val wantSection = SectionL.fromSpec("5:6, 20:29, 5:25")
        val dataChunk = makeChunk("5:6, 25:29, 5:25")
        runChunkerTest(dataChunk, wantSection, null,2, true) { count ->
            val offset = if (count < 1) 105 else 315
            Pair(105 * count, offset)
        }
    }

    @Test
    fun testOffsetFastIndex() {
        val wantSection = SectionL.fromSpec("5:6, 20:29, 5:25")
        val dataChunk = makeChunk("5:6, 20:29, 15:24")
        runChunkerTest(dataChunk, wantSection, null,20, true, Merge.none) { count ->
            Pair(10 * count, 10 + 21 * count)
        }
    }

    @Test
    fun testOffsetFastIndexm() {
        val wantSection = SectionL.fromSpec("5:6, 20:29, 5:25")
        val dataChunk = makeChunk("5:6, 20:29, 15:24")
        runChunkerTest(dataChunk, wantSection, null,20, true) { count ->
            Pair(10 * count, 10 + 21 * count)
        }
    }

    @Test
    fun testOffset212() {
        val wantSection = SectionL.fromSpec("5:8, 20:29, 5:25")
        val dataChunk = makeChunk("6:7, 20:29, 15:24")
        runChunkerTest(dataChunk, wantSection, null,20, true, Merge.none)  { count ->
            Pair(10 * count, 220 + 21 * count)
        }
    }

    @Test
    fun testOffset212m() {
        val wantSection = SectionL.fromSpec("5:8, 20:29, 5:25")
        val dataChunk = makeChunk("6:7, 20:29, 15:24")
        runChunkerTest(dataChunk, wantSection, null,20, false)  { count ->
            Pair(10 * count, 220 + 21 * count)
        }
    }

    @Test
    fun testChunkUpper() {
        val wantSection = SectionL.fromSpec("5:8, 20:29, 5:25")
        val dataChunk = makeChunk("4:5, 20:29, 5:25")
        runChunkerTest(dataChunk, wantSection, 21, 10, true, Merge.none) { count ->
            Pair(210 + 21 * count, 21 * count)
        }
    }

    @Test
    fun testChunkUpperm() {
        val wantSection = SectionL.fromSpec("5:8, 20:29, 5:25")
        val dataChunk = makeChunk("4:5, 20:29, 5:25")
        runChunkerTest(dataChunk, wantSection, 210, 1, true) { count ->
            Pair(210 + 21 * count, 21 * count)
        }
    }

    @Test
    fun testChunkLower() {
        val wantSection = SectionL.fromSpec("5:8, 20:29, 5:24")
        val dataChunk = makeChunk("7:12, 20:29, 5:24")
        runChunkerTest(dataChunk, wantSection, 20, 20, true, Merge.none) { count ->
            Pair(20 * count, 400 + 20 * count)
        }
    }


    @Test
    fun testChunkLowerm() {
        val wantSection = SectionL.fromSpec("5:8, 20:29, 5:24")
        val dataChunk = makeChunk("7:12, 20:29, 5:24")
        runChunkerTest(dataChunk, wantSection, 400, 1, true) { count ->
            Pair(20 * count, 400 + 20 * count)
        }
    }

    @Test
    fun testChunkUpper3() {
        val wantSection = SectionL.fromSpec("5:8, 20:29, 5:24")
        val dataChunk = makeChunk("2:5, 15:25, 0:30")
        runChunkerTest(dataChunk, wantSection, 20, 6, true, Merge.none) { count ->
            Pair(1183 + 31 * count, 20 * count)
        }
    }

    @Test
    fun testChunkUpper3m() {
        val wantSection = SectionL.fromSpec("5:8, 20:29, 5:24")
        val dataChunk = makeChunk("2:5, 15:25, 0:30")
        runChunkerTest(dataChunk, wantSection, 20, 6, true) { count ->
            Pair(1183 + 31 * count, 20 * count)
        }
    }

    @Test
    fun testChunkLower215() {
        val wantSection = SectionL.fromSpec("5:8, 20:29, 5:24")
        val dataChunk = makeChunk("7:12, 29:30, 20:29")
        runChunkerTest(dataChunk, wantSection, 5, 2, true, Merge.none) { count ->
            Pair(20 * count, 595 + 200 * count)
        }
    }

    @Test
    fun testChunkLower215m() {
        val wantSection = SectionL.fromSpec("5:8, 20:29, 5:24")
        val dataChunk = makeChunk("7:12, 29:30, 20:29")
        runChunkerTest(dataChunk, wantSection, 5, 2, true) { count ->
            Pair(20 * count, 595 + 200 * count)
        }
    }

    @Test
    fun testChunkLower225() {
        val wantSection = SectionL.fromSpec("5:8, 20:29, 5:24")
        val dataChunk = makeChunk("7:12, 28:30, 20:29")
        runChunkerTest(dataChunk, wantSection, 5, 4, true, Merge.none) { count ->
            val src = if (count < 2) 0 else 10
            val dst = if (count < 2) 575 else 735
            Pair(src + 10 * count, dst + 20 * count)
        }
    }

    @Test
    fun testChunkLower225m() {
        val wantSection = SectionL.fromSpec("5:8, 20:29, 5:24")
        val dataChunk = makeChunk("7:12, 28:30, 20:29")
        runChunkerTest(dataChunk, wantSection, 5, 4, true) { count ->
            val src = if (count < 2) 0 else 10
            val dst = if (count < 2) 575 else 735
            Pair(src + 10 * count, dst + 20 * count)
        }
    }

    @Test
    fun testSegmented() {
        val shape = intArrayOf(1, 6, 12)
        val varShape = IndexSpace(shape)
        val wantSection = SectionL.fromSpec("0:0,0:5,4:7")
        runChunkerTest(varShape, wantSection, 4, 6, true, Merge.notFirst) {
                count -> Pair(4 + 12 * count, 4 * count)
        }
    }

    @Test
    fun testSegmented2() {
        val shape = intArrayOf(3)
        val varShape = IndexSpace(shape)
        val wantSection = SectionL.fromSpec("0:2")
        runChunkerTest(varShape, wantSection, 1, 3, true, Merge.notFirst) {
                count -> Pair(count, count)
        }
    }

    @Test
    fun testProblem() {
        val varshape = longArrayOf(6, 12)
        val wantSection = SectionP.fill(SectionP.fromSpec("0:5, 4:7"), varshape)
        val dataChunk = IndexSpace(varshape)
        runChunkerTest(dataChunk, wantSection, 4, 6, true) { count ->
            Pair(4 + 12 * count, 4 * count)
        }
    }

    internal fun makeChunk(spec : String) : IndexSpace {
        val sp = SectionL.fromSpec(spec)
        return IndexSpace(sp)
    }
}