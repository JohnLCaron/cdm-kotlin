package com.sunya.cdm.api

import org.junit.jupiter.api.Test
import kotlin.test.*

class TestDatatype {

    @Test
    fun testBasics() {
        assertFalse(Datatype.STRING.isVlenString)
        assertFalse(Datatype.CHAR.isVlenString)
        assertFalse(Datatype.INT.isVlenString)

        assertTrue(Datatype.FLOAT.isNumeric)
        assertTrue(Datatype.FLOAT.isNumber)
        assertTrue(Datatype.UINT.isNumeric)
        assertFalse(Datatype.UINT.isNumber)
        assertFalse(Datatype.STRING.isNumeric)
        assertFalse(Datatype.COMPOUND.isNumeric)

        assertFalse(Datatype.FLOAT.isIntegral)
        assertTrue(Datatype.UINT.isIntegral)
        assertFalse(Datatype.STRING.isIntegral)
        assertTrue(Datatype.FLOAT.isFloatingPoint)
        assertFalse(Datatype.UINT.isFloatingPoint)
        assertFalse(Datatype.STRING.isFloatingPoint)

        assertFalse(Datatype.FLOAT.isEnum)
        assertFalse(Datatype.UINT.isEnum)
        assertFalse(Datatype.STRING.isEnum)
        assertTrue(Datatype.ENUM1.isEnum)
        assertTrue(Datatype.ENUM2.isEnum)
        assertTrue(Datatype.ENUM4.isEnum)

        assertFalse(Datatype.FLOAT.isUnsigned)
        assertFalse(Datatype.BYTE.isUnsigned)
        assertTrue(Datatype.UBYTE.isUnsigned)
        assertFalse(Datatype.SHORT.isUnsigned)
        assertTrue(Datatype.USHORT.isUnsigned)
        assertFalse(Datatype.INT.isUnsigned)
        assertTrue(Datatype.UINT.isUnsigned)
        assertFalse(Datatype.LONG.isUnsigned)
        assertTrue(Datatype.ULONG.isUnsigned)
        assertFalse(Datatype.STRING.isUnsigned)
        assertTrue(Datatype.ENUM1.isUnsigned)
        assertTrue(Datatype.ENUM2.isUnsigned)
        assertTrue(Datatype.ENUM4.isUnsigned)
    }

    @Test
    fun testWithSigndedness() {
        assertEquals(Datatype.UINT.withSignedness(false), Datatype.UINT)
        assertEquals(Datatype.UINT.withSignedness(true), Datatype.INT)
        assertEquals(Datatype.INT.withSignedness(false), Datatype.UINT)
        assertEquals(Datatype.INT.withSignedness(true), Datatype.INT)
        assertEquals(Datatype.USHORT.withSignedness(false), Datatype.USHORT)
        assertEquals(Datatype.USHORT.withSignedness(true), Datatype.SHORT)
        assertEquals(Datatype.SHORT.withSignedness(false), Datatype.USHORT)
        assertEquals(Datatype.SHORT.withSignedness(true), Datatype.SHORT)
        assertEquals(Datatype.BYTE.withSignedness(false), Datatype.UBYTE)
        assertEquals(Datatype.BYTE.withSignedness(true), Datatype.BYTE)
        assertEquals(Datatype.UBYTE.withSignedness(false), Datatype.UBYTE)
        assertEquals(Datatype.UBYTE.withSignedness(true), Datatype.BYTE)
        assertEquals(Datatype.LONG.withSignedness(false), Datatype.ULONG)
        assertEquals(Datatype.LONG.withSignedness(true), Datatype.LONG)
        assertEquals(Datatype.ULONG.withSignedness(false), Datatype.ULONG)
        assertEquals(Datatype.ULONG.withSignedness(true), Datatype.LONG)
        assertEquals(Datatype.STRING.withSignedness(false), Datatype.STRING)
    }

    @Test
    fun testToString() {
        for (t in listOf(Datatype.UINT, Datatype.INT, Datatype.STRING, Datatype.COMPOUND)) {
            assertEquals(t.cdlName, t.toString())
        }
        val tvlen = Datatype.VLEN.withTypedef(VlenTypedef("viva", Datatype.UINT))
        assertEquals("vlen uint", tvlen.toString())
    }

    @Test
    fun testVlen() {
        var tvlen = Datatype.VLEN.withTypedef(VlenTypedef("viva", Datatype.UINT))
        assertNull(tvlen.isVlen)

        tvlen = Datatype.VLEN.withTypedef(VlenTypedef("viva", Datatype.UINT)).withVlen(false)
        assertNotNull(tvlen.isVlen)
        assertFalse(tvlen.isVlen?: true)

        tvlen = Datatype.VLEN.withTypedef(VlenTypedef("viva", Datatype.UINT)).withVlen(true)
        assertTrue(tvlen.isVlen?: false)
    }

    @Test
    fun testStringVlen() {
        val tvlen = Datatype.VLEN.withTypedef(VlenTypedef("viva", Datatype.UINT)).withVlen(true)
        assertFalse(tvlen.isVlenString)

        val tvlen1 = Datatype.STRING.withVlen(false)
        assertFalse(tvlen1.isVlen?: true)

        val tvlen2 = Datatype.STRING.withVlen(true)
        assertTrue(tvlen2.isVlen?: false)
    }

    @Test
    fun testEquals() {
        assertEquals(Datatype.STRING, Datatype.STRING)
        assertEquals(Datatype.STRING.withVlen(true), Datatype.STRING.withVlen(true))
        assertEquals(Datatype.STRING, Datatype.STRING.withVlen(true))
        assertEquals(Datatype.STRING.withVlen(false), Datatype.STRING.withVlen(true))

        var tvlen1 = Datatype.VLEN.withTypedef(VlenTypedef("viva", Datatype.UINT))
        var tvlen2 = Datatype.VLEN.withTypedef(VlenTypedef("voova", Datatype.UINT))
        assertEquals(tvlen1, tvlen2)

        assertEquals(Datatype.UBYTE.withSignedness(false), Datatype.UBYTE)
        assertNotEquals(Datatype.UBYTE.withSignedness(true), Datatype.UBYTE)
    }

}