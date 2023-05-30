package com.sunya.netchdf.netcdf4

import com.sunya.cdm.api.*
import com.sunya.cdm.array.ArrayUByte
import com.sunya.cdm.array.makeStringsFromBytes
import com.sunya.netchdf.openNetchdfFile
import com.sunya.netchdf.readNetchdfData
import com.sunya.testdata.N3Files
import com.sunya.testdata.testData

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.assertIs

class N4charTest {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            return N3Files.params()
        }
    }

    @Test
    fun testCharAttribute() {
        val filename = testData + "cdmUnitTest/formats/netcdf4/files/xma022032.nc5"
        openNetchdfFile(filename).use { myfile ->
            println("--- ${myfile!!.type()} $filename ")
            println(myfile.cdl())

            val gatt = myfile.rootGroup().attributes.find{ it.name == "date"}!!
            println("date = $gatt")
            assertEquals(Datatype.STRING, gatt.datatype)
            assertEquals(1, gatt.values.size)
            assertEquals("23/04/09", gatt.values[0])

            val crs = myfile.rootGroup().allVariables().find{ it.fullname() == "/xma/rtip/inp#4"}!!
            val att = crs.attributes.find{ it.name == "units"}!!
            assertEquals(Datatype.STRING, att.datatype)
            assertEquals(1, att.values.size)
            assertEquals("V", att.values[0])
        }
    }

    @Test
    fun testCharVariable() {
        val filename = testData + "cdmUnitTest/formats/netcdf4/multiDimscale.nc4"
        openNetchdfFile(filename).use { myfile ->
            println("--- ${myfile!!.type()} $filename ")
            println(myfile.cdl())
            val v = myfile.rootGroup().variables.find{ it.name == "file_date"}!!
            assertEquals(Datatype.CHAR, v.datatype)
            val data = myfile.readArrayData(v)
            println("file_date data = $data")
            assertEquals(Datatype.CHAR, data.datatype)
            assertIs<ArrayUByte>(data)

            val expect = listOf(50,48,49,48,45,48,52,45,48,52,84,48,57,58,49,55,58,52,52,46,48,48,48,48,48,48,50,48,49,48,45,48,52,45,48,52,84,48,57,58,53,52,58,49)
            data.forEachIndexed { idx, it ->
                if (idx < expect.size) {
                    assertEquals(expect[idx], it.toInt())
                }
            }
            val svalues = data.makeStringsFromBytes()
            println("svalues = $svalues")

            val expectNames = listOf("2010-04-04T09:17:44.000000","2010-04-04T09:54:18.900000","2010-04-04T09:55:51.700000","2010-04-04T09:57:23.700000","2010-04-04T09:58:56.000000","2010-04-04T10:00:29.000000","2010-04-04T10:02:01.200000","2010-04-04T10:03:33.200000","2010-04-04T10:05:09.500000","2010-04-04T10:06:41.600000")
            svalues.forEachIndexed { idx, it ->
                assertEquals(expectNames[idx], it)
            }
        }
    }

    // havent found any char members. because why would you do that?
    // should test string members, but havent found any
    @Test
    fun testStringMember() {
        val filename = testData + "cdmUnitTest/formats/netcdf4/files/c0.nc"
        openNetchdfFile(filename).use { myfile ->
            println("--- ${myfile!!.type()} $filename ")
            println(myfile.cdl())
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    fun testReadNetchdfData(filename: String) {
        readNetchdfData(filename, null, null, false, false)
    }

}