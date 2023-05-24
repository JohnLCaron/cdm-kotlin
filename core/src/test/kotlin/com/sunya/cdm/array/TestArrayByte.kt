package com.sunya.cdm.array

import com.sunya.cdm.api.*
import com.sunya.cdm.layout.IndexND
import com.sunya.cdm.layout.IndexSpace
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import kotlin.test.*

class TestArrayByte {

    @Test
    fun testArrayByte() {
        val shape = intArrayOf(4,5,6)
        val size = shape.computeSize()
        val bb = ByteBuffer.allocate(size)
        repeat(size) { bb.put(it.toByte())}

        val testArray = ArrayByte(shape, bb)
        assertEquals(Datatype.BYTE, testArray.datatype)
        assertEquals(size, testArray.nelems)

        testArray.forEachIndexed { idx, it ->
            assertEquals(idx.toByte(), it)
        }
    }

    @Test
    fun testSection() {
        val shape = intArrayOf(4,5,6)
        val size = shape.computeSize()
        val bb = ByteBuffer.allocate(size)
        repeat(size) { bb.put(it.toByte())}
        val testArray = ArrayByte(shape, bb)

        val sectionStart = intArrayOf(1,2,3)
        val sectionLength = intArrayOf(1,2,1)
        val section = Section(sectionStart, sectionLength, shape.toLongArray())
        val sectionArray = testArray.section(section)

        assertEquals(Datatype.BYTE, sectionArray.datatype)
        assertEquals(sectionLength.computeSize(), sectionArray.nelems)

        val full = IndexND(IndexSpace(sectionStart.toLongArray(), sectionLength.toLongArray()), shape.toLongArray())
        val odo = IndexND(IndexSpace(sectionStart.toLongArray(), sectionLength.toLongArray()), shape.toLongArray())
        odo.forEachIndexed { idx, index ->
            println("$idx, ${index.contentToString()} ${full.element(index)}")
            val have = sectionArray.values.get(idx)
            val expect = testArray.values.get(full.element(index).toInt())
            println("$idx, ${expect} ${have}")
            assertEquals(expect, have)
        }
    }
}