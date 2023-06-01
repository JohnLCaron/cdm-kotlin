package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.*
import com.sunya.cdm.array.ArrayStructureData
import com.sunya.cdm.array.ArrayTyped
import com.sunya.netchdf.openNetchdfFile
import com.sunya.netchdf.readNetchdfData
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

class H5enumTest {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            return Stream.of (
                Arguments.of(testData + "devcdm/hdf5/cenum.h5"),
                Arguments.of(testData + "devcdm/hdf5/enum.h5"),
                Arguments.of(testData + "devcdm/hdf5/enumcmpnd.h5"),
                Arguments.of(testData + "devcdm/netcdf4/test_enum_type.nc"),
                Arguments.of(testData + "devcdm/netcdf4/tst_enums.nc"),
            )
        }
    }

    @Test
    fun testEnumAttribute() {
        val filename = testData + "devcdm/netcdf4/tst_enums.nc"
        openNetchdfFile(filename).use { myfile ->
            println("--- ${myfile!!.type()} $filename ")
            println(myfile.cdl())

            val att = myfile.rootGroup().attributes.find{ it.name == "brady_attribute"}!!
            println("brady_attribute = $att")
            assertEquals(Datatype.ENUM1, att.datatype)
            assertContentEquals(listOf(0.toUByte(), 3.toUByte(), 8.toUByte()), att.values)
            assertEquals(listOf("Mike", "Marsha", "Alice"), att.convertEnums())

            assertContains(myfile.cdl(), "brady_attribute = \"Mike\", \"Marsha\", \"Alice\"")
        }
    }

    @Test
    fun testEnumVariable() {
        val filename = testData + "devcdm/hdf5/enum.h5"
        openNetchdfFile(filename).use { myfile ->
            println("--- ${myfile!!.type()} $filename ")
            println(myfile.cdl())
            val v = myfile.rootGroup().variables.find{ it.name == "EnumTest"}!!
            assertEquals(Datatype.ENUM4, v.datatype)
            val data = myfile.readArrayData(v)
            println("EnumTest data = $data")
            val expect = listOf(0,1,2,3,4,0,1,2,3,4)
            data.forEachIndexed { idx, it ->
                assertEquals(expect[idx], (it as UInt).toInt())
            }

            val expectNames = listOf("RED", "GREEN", "BLUE", "WHITE", "BLACK")
            val names = data.convertEnums().toList()
            names.forEachIndexed { idx, it ->
                assertEquals(expectNames[idx % 5], it)
            }
        }
    }

    @Test
    fun testEnumMember() {
        val filename = testData + "devcdm/hdf5/enumcmpnd.h5"
        openNetchdfFile(filename).use { myfile ->
            println("--- ${myfile!!.type()} $filename ")
            println(myfile.cdl())
            val v = myfile.rootGroup().variables.find{ it.name == "EnumCmpndTest"}!!
            assertEquals(Datatype.COMPOUND, v.datatype)
            val typedef = v.datatype.typedef as CompoundTypedef
            val member = typedef.members.find { it.name == "color_name"}!!

            val mtypedef = member.datatype.typedef as EnumTypedef

            val sdataArray = myfile.readArrayData(v)
            println("EnumCmpndTest data = $sdataArray")
            assertEquals(Datatype.COMPOUND, sdataArray.datatype)
            val dtypedef = v.datatype.typedef as CompoundTypedef
            assertEquals(typedef, dtypedef)

            val expectNames = listOf("RED", "GREEN", "BLUE", "WHITE", "BLACK")
            sdataArray.forEachIndexed { idx, it ->
                val sdata = it as ArrayStructureData.StructureData
                println("sdata = $sdata")
                val wtf : ArrayTyped<*> = member.values(sdata)
                println("value = $wtf")
                assertEquals((idx % 5).toUInt(), wtf.first())
                assertEquals(expectNames[idx % 5], wtf.convertEnums().first())
            }
        }
    }

    // a compound with a member thats a type thats not a seperate typedef.
    // the obvious thing to do is to be able to add a typedef when processing the member.
    // or look for it when building H5group
    @Test
    fun compoundEnumTypedef() {
        val filename = testData + "devcdm/hdf5/enumcmpnd.h5"
        readNetchdfData(filename, null, null, true, false)
    }

    @ParameterizedTest
    @MethodSource("params")
    fun testReadNetchdfData(filename: String) {
        readNetchdfData(filename, null, null, true, true)
    }

}