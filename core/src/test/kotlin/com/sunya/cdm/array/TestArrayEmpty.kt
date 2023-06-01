package com.sunya.cdm.array

import com.sunya.cdm.api.*
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import kotlin.test.*

class TestArrayEmpty {

    @Test
    fun testArrayEmpty() {
        val shape = intArrayOf(1,2,3)
        val size = shape.computeSize()
        val empty = ArrayEmpty<UShort>(shape, Datatype.USHORT)
        assertEquals(Datatype.USHORT, empty.datatype)
        assertEquals(size, empty.nelems)

        var count = 0
        empty.forEach {
            assertEquals(42.toUShort(), it) // doesnt happen
            count++
        }
        assertEquals(0, count)

        val section = longArrayOf(41)
        val sectionArray = empty.section(Section(section))
        assertEquals(Datatype.USHORT, sectionArray.datatype)
        assertEquals(section.computeSize(), sectionArray.nelems.toLong())

        count = 0
        sectionArray.forEach {
            assertEquals(42.toUShort(), it) // doesnt happen
            count++
        }
        assertEquals(0, count)
    }

}