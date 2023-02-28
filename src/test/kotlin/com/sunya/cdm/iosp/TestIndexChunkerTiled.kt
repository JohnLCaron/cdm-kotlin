package com.sunya.cdm.iosp

import com.sunya.cdm.api.Section
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

private val show = false

/** Test [IndexChunkerTiled]  */
class TestIndexChunkerTiled {

    fun runIndexChunker2(dataSection: Section, wantSection: Section,
                        nelems: Int,
                        nchunks : Int,
                        check : Boolean = true,
                        srcPos : (Int) -> Long) {
        println("IndexChunkerTiled dataSection = ${dataSection} wantSection = [$wantSection]")
        val chunker = IndexChunkerTiled(dataSection, wantSection)
        var count = 0
        while (chunker.hasNext()) {
            val chunk = chunker.next()
            println(" chunk = $chunk")
            if (check) {
                assertEquals(srcPos(count), chunk.srcElem)
                assertEquals(nelems, chunk.nelems)
                assertEquals(count * nelems, chunk.destElem.toInt())
            } else if (show) {
                println("srcElem expect ${srcPos(count)}, actual ${chunk.srcElem}")
                println("nelems expect ${nelems}, actual ${chunk.nelems}")
                println("destElem expect ${count * nelems}, actual ${chunk.destElem}")
            }
            count++
        }
        if (check) {
            assertEquals( nchunks, count)
            assertEquals(wantSection.computeSize().toInt(), count * nelems)
        } else {
            println("nchunks expect ${nchunks}, actual ${count}")
        }
    }

    fun runIndexChunkerTiled(dataSection: Section, wantSection: Section, nelems: Int, srcIncr : Long, dstIncr : Long, nchunks : Int, check : Boolean = true, show : Boolean = false) {
        val index = IndexChunkerTiled(dataSection, wantSection)
        var count = 0
        while (index.hasNext()) {
            val chunk = index.next()
            println(" chunk = $chunk")
            if (check) {
                assertEquals(count * srcIncr, chunk.srcElem)
                assertEquals(nelems, chunk.nelems)
                assertEquals(count * dstIncr, chunk.destElem)
            } else if (show) {
                println("srcElem expect ${count * srcIncr}, actual ${chunk.srcElem}")
                println("nelems expect ${nelems}, actual ${chunk.nelems}")
                println("destElem expect ${count * dstIncr}, actual ${chunk.destElem}")
            }
            count++
        }
        if (check) {
            assertEquals(count, nchunks)
        } else {
            println("nchunks expect ${nchunks}, actual ${count}")
        }
    }

    // could be 1 chunk?
    @Test
    fun testChunkerTiledOne() {
        val oneSection = Section(intArrayOf(1, 20, 100))
        runIndexChunker2(oneSection, oneSection, 100, 20, true) { 100L * it }
    }

    @Test
    fun testChunkerTiledSubset() {
        val dataSection = Section(intArrayOf(1, 20, 100))
        val wantSection = Section(intArrayOf(1, 20, 200))
        runIndexChunker2(dataSection, wantSection, 100, 20, false) {
            100L * it
        }
    }

    // could be 1 chunk?
    @Test
    fun testChunkerTiledAll() {
        val dataSection = Section("0:0, 20:39,  0:1353 ")
        runIndexChunker2(dataSection, dataSection, 1354, 20, true) { 1354L * it }
    }

    @Test
    fun testChunkerTiled() {
        val dataSection = Section("0:0, 20:39,  0:1353 ") // this is the chunk
        val wantSection = Section("0:2, 22:3152, 0:1350") // this is the variable section wanted
        runIndexChunker2(dataSection, wantSection, 1351, 20, false) {
            1351L * it
        }
    }


    @Test
    fun testChunkerTiledProblem() {
        // netcdf4/Ike.egl3.SWI.tidal.nc has the chunking on the left (slowest dimensions)
        val wantSection = Section(intArrayOf(10, 20, 30))
        val dataSection = Section(intArrayOf(10, 10, 1))
        runIndexChunker2(dataSection, wantSection, 1351, 20, false) {
            1351L * it
        }
    }

    @Test
    fun testIndexChunkerTiledToString() {
        val dataSection = Section("0:0, 40:59,  0:1353  ")
        val wantSection = Section("0:2, 22:3152,0:1350")
        val index = IndexChunkerTiled(dataSection, wantSection)
        assertEquals(
            String.format(
                     "[data = 0:1353 want = 0:1350 intersect = 0:1350 ncontigElements = 1351, "
                    + "data = 40:59 want = 22:3152 intersect = 40:59 ncontigElements = 20, "
                    + "data = 0:0 want = 0:2 intersect = 0:0 ncontigElements = 1]"),
                index.toString(),
            )
    }
}
