package com.sunya.netchdf.hdf4

import com.sunya.cdm.api.*
import com.sunya.cdm.array.*
import com.sunya.netchdf.openNetchdfFile
import com.sunya.netchdf.readNetchdfData
import com.sunya.netchdf.showNetchdfHeader
import com.sunya.testdata.N3Files
import com.sunya.testdata.testData

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.assertContains
import kotlin.test.assertContentEquals
import kotlin.test.assertIs

class H4charTest {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            return N3Files.params()
        }
    }

    @Test
    fun testCharAttribute() {
        val filename = testData + "hdf4/nsidc/LAADS/MOD/MOD03.A2007001.0000.005.2007041030714.hdf"
        openNetchdfFile(filename).use { myfile ->
            println("--- ${myfile!!.type()} $filename ")
            println(myfile.cdl())

            val gatt = myfile.rootGroup().attributes.find{ it.name == "Ephemeris_Attitude_Source"}!!
            println("Ephemeris_Attitude_Source = $gatt")
            assertEquals(Datatype.STRING, gatt.datatype)
            assertEquals(1, gatt.values.size)
            assertEquals("SDP Toolkit", gatt.values[0])

            val crs = myfile.rootGroup().variables.find{ it.name == "EV_start_time"}!!
            val att = crs.attributes.find{ it.name == "units"}!!
            assertEquals(Datatype.STRING, att.datatype)
            assertEquals(1, att.values.size)
            assertEquals("seconds", att.values[0])
        }
    }

    @Test
    fun testCharVariable() {
        val filename = testData + "hdf4/nsidc/LAADS/MOD/MOD03.A2007001.0000.005.2007041030714.hdf"
        openNetchdfFile(filename).use { myfile ->
            println("--- ${myfile!!.type()} $filename ")
            println(myfile.cdl())
            val v = myfile.rootGroup().variables.find{ it.name == "Scan_Type"}!!
            assertEquals(Datatype.CHAR, v.datatype)
            val data = myfile.readArrayData(v)
            println("Scan_Type data = $data")
            assertEquals(Datatype.CHAR, data.datatype)
            assertIs<ArrayUByte>(data)

            val expect = listOf(78,105,103,104,116,0,0,0,0,0,78,105,103,104,116,0,0,0,0,0,78,105,103,104,116,0,0,0,0,0,78,105,103,104,116,0,0,0,0,0)
            data.forEachIndexed { idx, it ->
                if (idx < expect.size) {
                    assertEquals(expect[idx], it.toInt())
                }
            }
            val svalues = data.makeStringsFromBytes()
            println("svalues = $svalues")

            svalues.forEachIndexed { idx, it ->
                assertEquals(if (idx < 161) "Night" else "Day", it, "$idx")
            }
        }
    }

    @Test
    fun testCharLookupTable() {
        val filename = testData + "hdf4/eisalt/VHRR-KALPANA_20081216_070002.hdf"
        openNetchdfFile(filename).use { myfile ->
            println("--- ${myfile!!.type()} $filename ")
            println(myfile.cdl())
            val v = myfile.rootGroup().variables.find{ it.name == "Curves_at_2721.35_1298.84_lookup"}!!
            assertEquals(Datatype.CHAR, v.datatype)
            val data = myfile.readArrayData(v)
            println("Curves_at_2721.35_1298.84_lookup data = $data")
            assertEquals(Datatype.CHAR, data.datatype)
            assertIs<ArrayUByte>(data)

            val expect = listOf(0,96,150,96,0,150,0,0,255,0,150,96,96,150,0,0,255,0,150,96,0,150,0,96,255,0,0,255,255,0,10,10,10,11,11,11,12,12,12,13,13,13,14,14,14,15,15,15,16,16)
            data.forEachIndexed { idx, it ->
                if (idx < expect.size) {
                    assertEquals(expect[idx], it.toInt())
                }
            }

            // not strings - dont test
        }
    }

    @Test
    fun testCharMember() {
        val filename = testData + "hdf4/eisalt/VHRR-KALPANA_20081216_070002.hdf"
        openNetchdfFile(filename).use { myfile ->
            println("--- ${myfile!!.type()} $filename ")
            println(myfile.cdl())
            val v = myfile.rootGroup().allVariables().find{ it.name == "Historical_orbital_prediction"}!!
            assertEquals(Datatype.COMPOUND, v.datatype)
            val typedef = v.datatype.typedef as CompoundTypedef
            val member = typedef.members.find { it.name == "Date_and_time"}!!
            assertEquals(Datatype.CHAR, member.datatype)

            val sdataArray = myfile.readArrayData(v)
            println("Date_and_time data = $sdataArray")
            assertEquals(Datatype.COMPOUND, sdataArray.datatype)
            val dtypedef = v.datatype.typedef as CompoundTypedef
            assertEquals(typedef, dtypedef)

            sdataArray.forEachIndexed { idx, it ->
                val sdata = it as ArrayStructureData.StructureData
                val wtf : ArrayTyped<*> = member.values(sdata)
                println("value = $wtf")
                assertEquals(Datatype.CHAR, wtf.datatype)
                if (idx == 0) {
                    val ubarray = wtf as ArrayUByte
                    assertEquals("2008-12-14 05:30:05", ubarray.makeStringsFromBytes().first())
                    assertEquals("2008-12-14 05:30:05", ubarray.makeStringFromBytes())
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    fun testReadNetchdfData(filename: String) {
        readNetchdfData(filename, null, null, false, false)
    }

}