package com.sunya.cdm.layout

import com.sunya.cdm.api.Section
import com.sunya.cdm.api.computeSize
import com.sunya.cdm.api.toIntArray
import com.sunya.cdm.array.ArrayByte
import com.sunya.cdm.array.ArrayInt
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class TestIndexFn {

    @Test
    fun testFlip1x6() {
        val shape = intArrayOf(1, 6)

        val flipper = IndexFn(shape)
        assertContentEquals(intArrayOf(6, 1), flipper.flippedShape())

        val size = shape.computeSize().toInt()
        val bb = ByteBuffer.allocate(size)
        repeat(size) { bb.put(it.toByte())}


        val flippedBB = flipper.flip(bb, 1)
        assertContentEquals(byteArrayOf(0, 1, 2, 3, 4, 5), ByteArray(size) { flippedBB.get(it) } )
    }

    @Test
    fun testFlip2x3() {
        val shape = intArrayOf(2, 3)

        val flipper = IndexFn(shape)
        assertContentEquals(intArrayOf(3, 2), flipper.flippedShape())

        val size = shape.computeSize()
        val bb = ByteBuffer.allocate(size)
        repeat(size) { bb.put(it.toByte())}

        val flippedBB = flipper.flip(bb, 1)
        assertContentEquals(byteArrayOf(0, 3, 1, 4, 2, 5), ByteArray(size) { flippedBB.get(it) } )
    }

    @Test
    fun testFlipInt() {
        val shape = intArrayOf(2, 3)

        val flipper = IndexFn(shape)
        assertContentEquals(intArrayOf(3, 2), flipper.flippedShape())

        val size = shape.computeSize()
        val bb = ByteBuffer.allocate(size*4)
        val ibb = bb.asIntBuffer()
        repeat(size) { ibb.put(it)}

        val flippedBB = flipper.flip(bb, 4)
        val iflippedBB = flippedBB.asIntBuffer()
        assertContentEquals(intArrayOf(0, 3, 1, 4, 2, 5), IntArray(size) { iflippedBB.get(it) } )
    }

    @Test
    fun testFlipMiddleSection() {
        val varshape = longArrayOf(3, 5)
        val wantSection = Section(intArrayOf(1, 1), intArrayOf(2, 3), varshape)
        assertContentEquals(longArrayOf(2, 3), wantSection.shape )
        val sectionSize = wantSection.totalElements.toInt()

        // fill the entire array
        val size = varshape.computeSize().toInt()
        val bb = ByteBuffer.allocate(size)
        repeat(size) { bb.put(it.toByte())}

        // grab only the middle section
        val ba = ArrayByte(varshape.toIntArray(), bb)
        val middle : ByteBuffer = ba.section(wantSection).values
        assertEquals( sectionSize, middle.capacity())
        assertContentEquals(byteArrayOf(6, 7, 8, 11, 12, 13 ), ByteArray(sectionSize) { middle.get(it) } )

        assertContentEquals(longArrayOf(2, 3), wantSection.shape )
        val flipper = IndexFn(wantSection.shape.toIntArray())
        assertContentEquals(intArrayOf(3, 2), flipper.flippedShape())

        val flippedBB = flipper.flip(middle, 1)
        assertContentEquals(byteArrayOf(6, 11, 7, 12, 8, 13), ByteArray(sectionSize) { flippedBB.get(it) } )
    }

    @Test
    fun testFlipMiddleSectionInt() {
        val varshape = longArrayOf(5, 5)
        val wantSection = Section(intArrayOf(1, 1), intArrayOf(3, 3), varshape)
        assertContentEquals(longArrayOf(3, 3), wantSection.shape )
        val sectionSize = wantSection.totalElements.toInt()
        assertEquals( 9, sectionSize)

        // fill the entire array
        val size = varshape.computeSize().toInt()
        val bb = ByteBuffer.allocate(size * 4)
        val ibb = bb.asIntBuffer()
        repeat(size) { ibb.put(it)}

        // grab only the middle section
        val ba = ArrayInt(varshape.toIntArray(), bb)
        val middle : ByteBuffer = ba.section(wantSection).bb
        val imiddle = ba.section(wantSection).values
        assertEquals( sectionSize, imiddle.capacity())
        assertContentEquals(intArrayOf(6, 7, 8, 11, 12, 13, 16, 17, 18 ), IntArray(sectionSize) { imiddle.get(it) } )

        val flipper = IndexFn(wantSection.shape.toIntArray())
        assertContentEquals(intArrayOf(3, 3), flipper.flippedShape())

        val flippedBB = flipper.flip(middle, 4)
        val iflippedBB = flippedBB.asIntBuffer()
        assertContentEquals(intArrayOf(6, 11, 16, 7, 12, 17, 8, 13, 18), IntArray(sectionSize) { iflippedBB.get(it) } )
    }

}