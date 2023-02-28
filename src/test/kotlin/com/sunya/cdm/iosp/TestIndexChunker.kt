package com.sunya.cdm.iosp

import com.sunya.cdm.api.Section
import com.sunya.cdm.api.Section.Companion.computeSize
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

private val show = false

/** Test [com.sunya.cdm.iosp.IndexChunker]  */
class TestIndexChunker {

    fun runIndexChunker(srcShape: IntArray, wantSection: Section,
                        nelems: Int,
                        nchunks : Int,
                        check : Boolean = true,
                        srcPos : (Int) -> Long) {
        println("IndexChunker srcShape = ${srcShape.contentToString()} wantSection = [$wantSection]")
        val index = IndexChunker(srcShape, wantSection)
        var count = 0
        while (index.hasNext()) {
            val chunk = index.next()
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

    @Test
    fun testFull() {
        val shape = intArrayOf(123, 22, 92, 12)
        val oneSection = Section(shape)
        runIndexChunker(shape, oneSection, computeSize(shape).toInt(), 1, true) { 0 }
    }

    @Test
    fun testPart() {
        val full = intArrayOf(2, 10, 20)
        val part = intArrayOf(2, 5, 20)
        val section = Section(part)
        val size = section.computeSize().toInt()
        runIndexChunker(full, section, size / 2, 2, true) { it * 200L }
    }

    @Test
    fun testOffset1() {
        val full = intArrayOf(2, 10, 20)
        val wantSection = Section("1:1, 5:9, 0:19")
        runIndexChunker(full, wantSection, 100,  1, true) { 300L }
    }

    @Test
    fun testOffset2() {
        val full = intArrayOf(2, 10, 20)
        val wantSection = Section("0:1, 5:9, 0:11")
        runIndexChunker(full, wantSection, 12, 10, true) {
            if (it < 5) 100L + 20 * it else 200L + 20 * it
        }
    }

    @Test
    fun testOffsetStride() {
        val full = intArrayOf(4, 10, 20)
        val wantSection = Section("0:3:2, 5:9, 0:11")
        runIndexChunker(full, wantSection, 12,  10, true)  {
            if (it < 5) 100L + 20 * it else 400L + 20 * it
        }
    }

    @Test
    fun testMiddleHalf() {
        val wantSection = intArrayOf(2, 10, 20)
        val dataChunk = Section("0:1, 5:9, 0:19")
        runIndexChunker(wantSection, dataChunk, dataChunk.computeSize().toInt() / 2, 2, true) {
            if (it == 0) 100 else 300
        }
    }

    @Test
    fun testPartCol() {
        val full = intArrayOf(2, 10, 20)
        val part = intArrayOf(2, 10, 10)
        val section = Section(part)
        val size = section.computeSize().toInt()
        runIndexChunker(full, section, 10,  20,  false)  { 20L * it }
    }

    @Test
    fun testPartCol1() {
        val full = intArrayOf(2, 10, 20)
        val part = intArrayOf(2, 10, 1)
        val section = Section(part)
        val size = section.computeSize().toInt()
        runIndexChunker(full, section, 1,  20,  false)  { 20L * it }
    }

    @Test
    fun testIndexChunkerToString() {
        val full = intArrayOf(2, 10, 20)
        val part = intArrayOf(2, 5, 20)
        val section = Section(part)
        val index = IndexChunker(full, section)
        assertEquals("wantSize=[1, 2] maxSize=[200, 2] wantStride=[1, 1] stride=[20, 200]", index.toString())
    }
}