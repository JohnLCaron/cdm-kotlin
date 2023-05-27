package com.sunya.cdm.array

import com.sunya.cdm.api.*
import com.sunya.cdm.layout.IndexND
import com.sunya.cdm.layout.IndexSpace
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import kotlin.test.*

class TestArrayOpaque {

    @Test
    fun TestArrayOpaque() {
        val osize = 10
        val shape = intArrayOf(4,5)
        val size = shape.computeSize()
        val bb = ByteBuffer.allocate(size * osize)
        repeat(size * osize) { bb.put(it.toByte())}

        val testArray = ArrayOpaque(shape, bb, osize)
        assertEquals(Datatype.OPAQUE, testArray.datatype)
        assertEquals(size, testArray.nelems)
        assertEquals(osize, testArray.size)
        assertTrue(testArray.toString().startsWith("class ArrayOpaque shape=[4, 5] data='[0, 1, 2, 3, 4, 5, 6, 7, 8, 9]','[10, 11, 12, 13, "))

        testArray.forEachIndexed { idx, nbb ->
            assertEquals(osize, nbb.limit())
            repeat(osize) { pos ->
                assertEquals( (idx * osize + pos).toByte(), nbb.get(pos),  "idx=$idx, pos=$pos")
            }
        }
    }

    @Test
    fun testSection() {
        val osize = 10
        val shape = intArrayOf(5)
        val size = shape.computeSize()
        val bb = ByteBuffer.allocate(size * osize)
        repeat(size * osize) { bb.put(it.toByte())}
        val testArray = ArrayOpaque(shape, bb, osize)

        val sectionStart = intArrayOf(1)
        val sectionLength = intArrayOf(2)
        val section = Section(sectionStart, sectionLength, shape.toLongArray())
        val sectionArray = testArray.section(section)

        assertEquals(Datatype.OPAQUE, sectionArray.datatype)
        assertEquals(sectionLength.computeSize(), sectionArray.nelems)

        val full = IndexND(IndexSpace(sectionStart.toLongArray(), sectionLength.toLongArray()), shape.toLongArray())
        val odo = IndexND(IndexSpace(sectionStart.toLongArray(), sectionLength.toLongArray()), shape.toLongArray())
        odo.forEachIndexed { idx, index ->
            println("$idx, ${index.contentToString()} ${full.element(index)}")
            val have = sectionArray.getElement(idx)
            val expect = testArray.getElement(full.element(index).toInt())
            assertEquals(expect, have)
        }
    }
}