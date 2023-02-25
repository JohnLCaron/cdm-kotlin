package com.sunya.cdm.iosp

import com.sunya.cdm.api.Section
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** Test [com.sunya.cdm.iosp.IndexChunker]  */
class TestIndexChunker {
    @Test
    fun testFull() {
        val shape = intArrayOf(123, 22, 92, 12)
        val section = Section(shape)
        val index = IndexChunker(shape, section)
        assertEquals(index.totalNelems, section.computeSize())
        val chunk = index.next()
        println(" chunk = $chunk")
        assertEquals(chunk.nelems, section.computeSize().toInt())
        assertFalse(index.hasNext())
    }

    @Test
    fun testPart() {
        val full = intArrayOf(2, 10, 20)
        val part = intArrayOf(2, 5, 20)
        val section = Section(part)
        val index = IndexChunker(full, section)
        assertEquals(index.totalNelems, section.computeSize())
        var count = 0L
        index.forEach {chunk ->
            println(" chunk = $chunk")
            assertEquals(chunk.srcElem(), count * 200)
            assertEquals(chunk.nelems(), section.computeSize().toInt() / 2)
            assertEquals(chunk.destElem(), count * 100)
            count++
        }
        assertEquals(2, count)
    }

    @Test
    fun testPartCol() {
        val full = intArrayOf(2, 10, 20)
        val part = intArrayOf(2, 10, 10)
        val section = Section(part)
        val index = IndexChunker(full, section)
        assertEquals(index.totalNelems, section.computeSize())
        var count = 0L
        index.forEach {chunk ->
            println(" chunk = $chunk")
            assertEquals(chunk.srcElem(), count * 20)
            assertEquals(chunk.nelems(), 10)
            assertEquals(chunk.destElem(), count * 10)
            count++
        }
        assertEquals(20, count)
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