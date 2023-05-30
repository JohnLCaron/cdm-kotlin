package com.sunya.netchdf.netcdf3

import com.sunya.cdm.api.*
import com.sunya.cdm.array.ArrayStructureData
import com.sunya.cdm.array.ArrayTyped
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
import kotlin.test.assertContains
import kotlin.test.assertContentEquals
import kotlin.test.assertIs

class N3charTest {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            return N3Files.params()
        }
    }

    @Test
    fun testCharAttribute() {
        val filename = testData + "recent/cdf5/jays_DOMAIN000.nc"
        openNetchdfFile(filename).use { myfile ->
            println("--- ${myfile!!.type()} $filename ")
            println(myfile.cdl())

            val gatt = myfile.rootGroup().attributes.find{ it.name == "texture_fudging"}!!
            println("texture_fudging = $gatt")
            assertEquals(Datatype.STRING, gatt.datatype)
            assertEquals(1, gatt.values.size)
            assertEquals("No", gatt.values[0])

            val crs = myfile.rootGroup().variables.find{ it.name == "crs"}!!
            val att = crs.attributes.find{ it.name == "grid_mapping_name"}!!
            assertEquals(Datatype.STRING, att.datatype)
            assertEquals(1, att.values.size)
            assertEquals("lambert_conformal_conic", att.values[0])
        }
    }

    @Test
    fun testCharVariable() {
        val filename = testData + "cdmUnitTest/formats/netcdf3/awips.nc"
        openNetchdfFile(filename).use { myfile ->
            println("--- ${myfile!!.type()} $filename ")
            println(myfile.cdl())
            val v = myfile.rootGroup().variables.find{ it.name == "ghLevels"}!!
            assertEquals(Datatype.CHAR, v.datatype)
            val data = myfile.readArrayData(v)
            println("Char data = $data")
            assertEquals(Datatype.CHAR, data.datatype)
            assertIs<ArrayUByte>(data)

            val expect = listOf(77,66,32,49,48,48,48,32,32,32,77,66,32,57,53,48,32,32,32,32,77,66,32,57,48,48,32,32,32,32,77,66,32,56,53,48,32,32,32,32,77,66,32,56,48,48,32)
            data.forEachIndexed { idx, it ->
                if (idx < expect.size) {
                    assertEquals(expect[idx], it.toInt())
                }
            }
            val svalues = data.makeStringsFromBytes()
            println("svalues = $svalues")

            val expectNames = listOf("MB 1000   ","MB 950    ","MB 900    ","MB 850    ","MB 800    ","MB 750    ","MB 700    ","MB 650    ","MB 600    ","MB 550    ","MB 500    ","MB 450    ","MB 400    ","MB 350    ","MB 300    ","MB 250    ","MB 200    ","MB 150    ","MB 100    ","K 280     ","K 285     ","K 290     ","K 295     ","K 300     ","K 305     ","K 310     ","K 315     ","K 320     ","K 325     ","K 330     ","K 335     ","K 340     ","K 345     ","K 350     ","FRZ       ")
            svalues.forEachIndexed { idx, it ->
                assertEquals(expectNames[idx], it)
            }
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    fun testReadNetchdfData(filename: String) {
        readNetchdfData(filename, null, null, false, false)
    }

}