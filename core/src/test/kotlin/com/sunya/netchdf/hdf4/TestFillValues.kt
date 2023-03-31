package com.sunya.netchdf.hdf4

import com.sunya.cdm.api.Datatype
import com.sunya.netchdf.netcdf4.Netcdf4
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class TestFillValues {

    @Test
    fun testNcDefaultFillValue() {
        assertEquals(Netcdf4.NC_FILL_BYTE, getNcDefaultFillValue(Datatype.BYTE))
        assertEquals(Netcdf4.NC_FILL_UBYTE, getNcDefaultFillValue(Datatype.UBYTE))
        assertEquals(Netcdf4.NC_FILL_CHAR, getNcDefaultFillValue(Datatype.CHAR))
        assertEquals(Netcdf4.NC_FILL_SHORT, getNcDefaultFillValue(Datatype.SHORT))
        assertEquals(Netcdf4.NC_FILL_USHORT, getNcDefaultFillValue(Datatype.USHORT))
        assertEquals(Netcdf4.NC_FILL_INT, getNcDefaultFillValue(Datatype.INT))
        assertEquals(Netcdf4.NC_FILL_UINT, getNcDefaultFillValue(Datatype.UINT))
        assertEquals(Netcdf4.NC_FILL_FLOAT, getNcDefaultFillValue(Datatype.FLOAT))
        assertEquals(Netcdf4.NC_FILL_DOUBLE, getNcDefaultFillValue(Datatype.DOUBLE))
        assertEquals(Netcdf4.NC_FILL_INT64, getNcDefaultFillValue(Datatype.LONG))
        assertEquals(Netcdf4.NC_FILL_UINT64, getNcDefaultFillValue(Datatype.ULONG))
        assertEquals("", getNcDefaultFillValue(Datatype.STRING))
        assertEquals(0, getNcDefaultFillValue(Datatype.OPAQUE))
    }

    // #define FILL_BYTE    ((char)-127)        /* Largest Negative value */
    //#define FILL_CHAR    ((char)0)
    //#define FILL_SHORT    ((short)-32767)
    //#define FILL_LONG    ((long)-2147483647)

    @Test
    fun testSdDefaultFillValue() {
        assertEquals((-127).toByte(), getNcDefaultFillValue(Datatype.BYTE))
        assertEquals(32769.toShort(), getNcDefaultFillValue(Datatype.SHORT))
        assertEquals(32769.toUShort(), (getNcDefaultFillValue(Datatype.SHORT) as Short).toUShort())
        assertEquals((-2147483647), getNcDefaultFillValue(Datatype.INT))
    }

    @Test
    fun testCompareFillValue() {
        assertEquals(getSDefaultFillValue(Datatype.BYTE), getNcDefaultFillValue(Datatype.BYTE))
        assertNotEquals(getSDefaultFillValue(Datatype.UBYTE), getNcDefaultFillValue(Datatype.UBYTE)) // 129 vs 255
        assertEquals(getSDefaultFillValue(Datatype.CHAR), getNcDefaultFillValue(Datatype.CHAR))
        assertEquals(getSDefaultFillValue(Datatype.SHORT), getNcDefaultFillValue(Datatype.SHORT))
        assertNotEquals(getSDefaultFillValue(Datatype.USHORT), getNcDefaultFillValue(Datatype.USHORT)) // <32769> but was: <65535>
        assertEquals(getSDefaultFillValue(Datatype.INT), getNcDefaultFillValue(Datatype.INT))
        assertNotEquals(getSDefaultFillValue(Datatype.UINT), getNcDefaultFillValue(Datatype.UINT)) // <2147483649> but was: <4294967295>
        assertEquals(getSDefaultFillValue(Datatype.FLOAT), getNcDefaultFillValue(Datatype.FLOAT))
        assertEquals(getSDefaultFillValue(Datatype.DOUBLE), getNcDefaultFillValue(Datatype.DOUBLE))
        assertEquals(getSDefaultFillValue(Datatype.LONG), getNcDefaultFillValue(Datatype.LONG))
        assertNotEquals(getSDefaultFillValue(Datatype.ULONG), getNcDefaultFillValue(Datatype.ULONG)) // <9223372036854775810> but was: <18446744073709551614>
    }

}