package com.sunya.cdm.iosp

import com.sunya.cdm.api.Section
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/** Test [IndexChunkerTiled]  */
class TestIndexChunkerTiled {

    @Test
    fun testChunkerTiledProblem() {
        // netcdf4/Ike.egl3.SWI.tidal.nc has the chunking on the left (slowest dimensions)
        val wantSection = Section(intArrayOf(191, 242, 589))
        val dataSection = Section(intArrayOf(191, 242, 1))
        val index = IndexChunkerTiled(dataSection, wantSection)
        var count = 0L
        val chuckSize = 191 * 242L
        while (index.hasNext()) {
            val chunk = index.next()
            println(" chunk = $chunk")
            //assertEquals(count, chunk.srcElem)
            //assertEquals(1, chunk.nelems)
            //assertEquals(count * chuckSize, chunk.destElem)
            count++
        }
        assertEquals(chuckSize, count)
    }

    @Test
    fun testChunkerTiledAll() {
        val dataSection = Section("0:0, 20:39,  0:1353 ")
        val index = IndexChunkerTiled(dataSection, dataSection)
        var count = 0L
        while (index.hasNext()) {
            val chunk = index.next()
            println(" chunk = $chunk")
            assertEquals(chunk.srcElem, 1354 * count)
            assertEquals(chunk.nelems, 1354)
            assertEquals(chunk.destElem, 1354 * count)
            count++
        }
        assertEquals(count, 20)
    }

    @Test
    fun testChunkerTiled() {
        val chunkSection = Section("0:0, 20:39,  0:1353 ") // this is the chunk
        val wantSection = Section("0:2, 22:3152, 0:1350") // this is the variable section wanted
        val index = IndexChunkerTiled(chunkSection, wantSection)
        var count = 0L
        while (index.hasNext()) {
            val chunk = index.next()
            println(" chunk = $chunk")
            assertEquals(chunk.srcElem, 2708 + 1354 * count)
            assertEquals(chunk.nelems, 1351)
            assertEquals(chunk.destElem, 1351 * count)
            count++
        }
    }

    @Test
    fun testChunkerTiled2() {
        val chunkSection = Section("0:0, 40:59,  0:1353  ")
        val wantSection = Section("0:2, 22:3152,0:1350")
        val index = IndexChunkerTiled(chunkSection, wantSection)
        var count = 0L
        while (index.hasNext()) {
            val chunk = index.next()
            System.out.printf(" %s%n", chunk)
            assertEquals(chunk.srcElem, 1354 * count)
            assertEquals(chunk.nelems, 1351)
            assertEquals(chunk.destElem, 24318 + 1351 * count)
            count++
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
