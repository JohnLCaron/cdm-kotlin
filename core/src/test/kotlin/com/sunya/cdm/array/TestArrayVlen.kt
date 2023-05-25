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
import kotlin.math.max
import kotlin.test.*

class TestArrayVlen {

    @Test
    fun testArrayVlen() {
        val shape = intArrayOf(4,2)
        val size = shape.computeSize()
        val listOfVlen = mutableListOf<Array<Int>>()
        repeat(size) {
            val vlen = Array(2 * it + 1) { it*it }
            listOfVlen.add(vlen)
        }

        val testArray = ArrayVlen(shape, listOfVlen, Datatype.INT)
        assertEquals(Datatype.VLEN, testArray.datatype)
        assertEquals(size, testArray.nelems)
        assertTrue(testArray.toString().startsWith("class ArrayVlen shape=[4, 2] data=[0],[0, 1, 4],[0, 1, 4, 9, 16],[0, 1, 4, 9,"))

        testArray.forEachIndexed { idx, vlen ->
            val expected = Array(2 * idx + 1) { it * it }
            assertTrue(vlen.contentEquals(expected))
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
            ) { dim0, dim1 ->
                val shape = intArrayOf(dim0, dim1)
                val size = shape.computeSize()
                val listOfVlen = mutableListOf<Array<Int>>()
                repeat(size) {
                    val vlen = Array(2 * it + 1) { it*it }
                    listOfVlen.add(vlen)
                }
                val testArray = ArrayVlen(shape, listOfVlen, Datatype.INT)

                val sectionStart = intArrayOf(dim0/2, dim1/3)
                val sectionLength = intArrayOf(max(1, dim0/2), max(1,dim1/3))
                val section = Section(sectionStart, sectionLength, shape.toLongArray())
                val sectionArray = testArray.section(section)

                assertEquals(Datatype.VLEN, sectionArray.datatype)
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
    fun testEquals() {
        val shape = intArrayOf(4,2)
        val size = shape.computeSize()
        val listOfVlen = mutableListOf<Array<Int>>()
        repeat(size) {
            val vlen = Array(2 * it + 1) { it*it }
            listOfVlen.add(vlen)
        }
        val testArray = ArrayVlen(shape, listOfVlen, Datatype.INT)
        assertEquals(testArray, testArray)

        val testArray2 = ArrayVlen(shape, listOfVlen, Datatype.INT)
        assertEquals(testArray, testArray2)
        assertEquals(testArray.hashCode(), testArray2.hashCode())

        val testArrayShape = ArrayVlen(intArrayOf(4,2,1), listOfVlen, Datatype.INT)
        assertNotEquals(testArray, testArrayShape)

        val listOfVlen4 = mutableListOf<Array<Int>>()
        repeat(size) {
            val vlen = Array(2 * it + 1) { it*it*it }
            listOfVlen4.add(vlen)
        }

        val testArray4 = ArrayVlen(shape, listOfVlen4, Datatype.INT)
        assertNotEquals(testArray, testArray4)
    }

    @Test
    fun testFromArray() {
        for (datatype in Datatype.values()) {
            testFromArray(datatype)
        }
    }

    fun testFromArray(datatype : Datatype) {
        val shape = intArrayOf(4,5,6)
        val size = shape.computeSize()
        val listOfVlen = mutableListOf<Array<*>>()
        var test = true
        repeat(size) {
            val vlen = when ( datatype) {
                Datatype.BYTE -> Array(2 * it + 1) { it.toByte() }
                Datatype.UBYTE -> Array(2 * it + 1) { it.toUByte() }
                Datatype.SHORT -> Array(2 * it + 1) { it.toShort()}
                Datatype.USHORT -> Array(2 * it + 1) { it.toUShort() }
                Datatype.INT -> Array(2 * it + 1) { it }
                Datatype.UINT -> Array(2 * it + 1) { it.toUInt() }
                Datatype.FLOAT -> Array(2 * it + 1) { it.toFloat() }
                Datatype.DOUBLE -> Array(2 * it + 1) { it.toDouble() }
                Datatype.LONG -> Array(2 * it + 1) { it.toLong() }
                Datatype.ULONG -> Array(2 * it + 1) { it.toULong() }
                else -> {
                    test = false
                    Array(0) {}
                }
            }
            listOfVlen.add(vlen)
        }
        if (!test) return
        val testArray = ArrayVlen.fromArray(shape, listOfVlen, datatype)
        assertEquals(Datatype.VLEN, testArray.datatype)
        assertEquals(size, testArray.nelems)

        testArray.forEachIndexed { idx, it ->
            val expect = listOfVlen[idx]
            val have = testArray.values.get(idx)
            assertEquals(expect, have)
        }
    }
}