package com.sunya.cdm.array

import com.sunya.cdm.api.*
import com.sunya.cdm.layout.IndexND
import com.sunya.cdm.layout.IndexSpace
import com.sunya.testdata.propTestSlowConfig
import com.sunya.testdata.runTest
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.test.*

class TestArrayString {

    @Test
    fun testArrayString() {
        val shape = intArrayOf(4,5,6)
        val size = shape.computeSize()
        val values = mutableListOf<String>()
        repeat(size) { values.add("s$it")}

        val testArray = ArrayString(shape, values)
        assertEquals(Datatype.STRING, testArray.datatype)
        assertEquals(size, testArray.nelems)
        assertTrue(testArray.toString().startsWith("class ArrayString shape=[4, 5, 6] data='s0','s1','s2','s3',"))

        testArray.forEachIndexed { idx, it ->
            assertEquals("s$idx", it)
        }
    }

    // fuzz test that section() works
    @Test
    fun testSectionFuzz() {
        runTest {
            checkAll(
                propTestSlowConfig,
                Arb.int(min = 1, max = 4),
                Arb.int(min = 6, max = 8),
                Arb.int(min = 1, max = 4),
            ) { dim0, dim1, dim2 ->
                val shape = intArrayOf(dim0, dim1, dim2)
                val size = shape.computeSize()
                val values = mutableListOf<String>()
                repeat(size) { values.add("s$it")}
                val testArray = ArrayString(shape, values)

                val sectionStart = intArrayOf(dim0/2, dim1/3, dim2/2)
                val sectionLength = intArrayOf(max(1, dim0/2), max(1,dim1/3), max(1,dim2/2))
                val section = Section(sectionStart, sectionLength, shape.toLongArray())
                val sectionArray = testArray.section(section)

                assertEquals(Datatype.STRING, sectionArray.datatype)
                assertEquals(sectionLength.computeSize(), sectionArray.nelems)

                val full = IndexND(IndexSpace(sectionStart.toLongArray(), sectionLength.toLongArray()), shape.toLongArray())
                val odo = IndexND(IndexSpace(sectionStart.toLongArray(), sectionLength.toLongArray()), shape.toLongArray())
                odo.forEachIndexed { idx, index ->
                    val have = sectionArray.values.get(idx)
                    val expect = testArray.values.get(full.element(index).toInt())
                    assertEquals(expect, have)
                }
            }
        }
    }

    @Test
    fun makeStringFromBytes() {
        val shape = intArrayOf(1, 2, 3)
        val size = shape.computeSize()
        val bb = ByteBuffer.allocate(size)
        repeat(size) { bb.put((65 + it).toByte())}

        val ubyteArray = ArrayUByte(shape, bb)
        val result = ubyteArray.makeStringFromBytes()
        assertEquals("ABCDEF", result)

        bb.put( 3, 0)
        val ubyteArray2 = ArrayUByte(shape, bb)
        val result2 = ubyteArray2.makeStringFromBytes()
        assertEquals("ABC", result2)
    }

    @Test
    fun makeStringsFromBytesRank1() {
        val shape = intArrayOf(13)
        val size = shape.computeSize()
        val bb = ByteBuffer.allocate(size)
        repeat(size) { bb.put((65 + it).toByte())}

        val ubyteArray = ArrayUByte(shape, bb)
        val result = ubyteArray.makeStringsFromBytes()
        assertEquals(Datatype.STRING, result.datatype)
        assertEquals(1, result.nelems)
        assertEquals(0, result.shape.size)
        assertEquals("ABCDEFGHIJKLM", result.first())
    }

    @Test
    fun makeStringsFromBytes() {
        val shape = intArrayOf(2, 2, 3)
        val size = shape.computeSize()
        val bb = ByteBuffer.allocate(size)
        repeat(size) { bb.put((65 + it).toByte())}

        val ubyteArray = ArrayUByte(shape, bb)
        val result : ArrayString = ubyteArray.makeStringsFromBytes()
        assertEquals(Datatype.STRING, result.datatype)
        assertEquals(4, result.nelems)
        assertTrue(intArrayOf(2, 2).contentEquals(result.shape))

        result.forEachIndexed { idx, it ->
            val ba = ByteArray(3) { (65 + idx * 3 + it).toByte() }
            assertEquals(String(ba), it)
        }
    }

    @Test
    fun makeStringsFromBytesWithZeroTerminators() {
        val shape = intArrayOf(3, 6)
        val size = shape.computeSize()
        val bb = ByteBuffer.allocate(size)
        repeat(size) { bb.put((65 + it).toByte())}
        bb.put( 3, 0)
        bb.put( 17, 0)

        val ubyteArray = ArrayUByte(shape, bb)
        val result : ArrayString = ubyteArray.makeStringsFromBytes()
        assertEquals(Datatype.STRING, result.datatype)
        assertEquals(3, result.nelems)
        assertTrue(intArrayOf(3).contentEquals(result.shape))

        result.forEachIndexed { idx, it ->
            when (idx) {
                0 -> assertEquals("ABC", it)
                1 -> assertEquals("GHIJKL", it)
                2 -> assertEquals("MNOPQ", it)
            }
        }
    }
}