package com.sunya.netchdf.hdf4

import com.sunya.cdm.util.Stats
import com.sunya.netchdf.*
import com.sunya.netchdf.hdf4Clib.Hdf4ClibFile
import com.sunya.netchdf.netcdf4.openNetchdfFile
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import test.util.testData
import test.util.testFilesIn
import java.util.*
import java.util.stream.Stream
import kotlin.test.assertEquals

class HCcompare {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            val hdfeos2 =
                testFilesIn(testData + "devcdm/hdfeos2")
                    .withRecursion()
                    .build()

            val devcdm = testFilesIn(testData + "devcdm/hdf4")
                .withRecursion()
                .build()

            // remove files that core dump
            val hdf4NoCore =
                testFilesIn(testData + "hdf4")
                    .withRecursion()
                //    .withPathFilter { p -> !(p.toString().contains("/eos/"))}
                    .addNameFilter { name -> !name.endsWith("VHRR-KALPANA_20081216_070002.hdf") }
                    .addNameFilter { name -> !name.endsWith("MOD01.A2007303.0325.005.2007306182401.hdf") }
                    .addNameFilter { name -> !name.endsWith("MOD02OBC.A2007001.0005.005.2007307210540.hdf") }
                    .addNameFilter { name -> !name.endsWith("MYD01.A2007001.0440.005.2007311085701.hdf") }
                    .build()

            return Stream.of(devcdm, hdfeos2, hdf4NoCore).flatMap { i -> i}
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            if (versions.size > 0) {
                versions.keys.forEach{ println("$it = ${versions[it]!!.size } files") }
            }
            Stats.show()
        }

        private val versions = mutableMapOf<String, MutableList<String>>()
    }

    @Test
    @Disabled
    fun coreDump() { // fakeDims, HC coredump "double free or corruption (out)"
        compareH4header(testData + "hdf4/nsidc/LAADS/MOD/MOD01.A2007303.0325.005.2007306182401.hdf")
    }

    @Test
    fun problemDuplicateTypedefs() { // duplicate typedefs, look for ones that match before adding
        val filename = testData + "/hdf4/nasa/eos-grid-1d/MISR_AM1_CGAS_FIRSTLOOK_AUG_30_2007_F11_0027.hdf"
        compareH4header(filename)
    }

    //////////////////////////////////////////////////////////////////////

    @ParameterizedTest
    @MethodSource("params")
    fun checkVersion(filename: String) {
        openNetchdfFile(filename).use { ncfile ->
            if (ncfile == null) {
                println("Not a netchdf file=$filename ")
                return
            }
            println("${ncfile.type()} $filename ")
            val paths = versions.getOrPut(ncfile.type()) { mutableListOf() }
            paths.add(filename)
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    fun readH4header(filename : String) {
        println("=================")
        println(filename)
        Hdf4File(filename).use { myfile ->
            println(" Hdf4File = \n${myfile.cdl()}")
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    fun readHCheader(filename : String) {
        println("=================")
        println(filename)
        Hdf4ClibFile(filename).use { myfile ->
            println(" Hdf4ClibFile = \n${myfile.cdl()}")
        }
    }

    // The netCDF-4 library can read HDF4 data files, if they were created with the SD (Scientific Data) API.
    // https://docs.unidata.ucar.edu/nug/current/getting_and_building_netcdf.html#build_hdf4
    @ParameterizedTest
    @MethodSource("params")
    fun compareH4header(filename : String) {
        println("=================")
        println(filename)
        Hdf4File(filename).use { myfile ->
            println("Hdf4File = \n${myfile.cdl()}")
            Hdf4ClibFile(filename).use { ncfile ->
                //println("actual = $root")
                //println("expect = $expect")
                assertEquals(ncfile.cdl(), myfile.cdl())
                println(myfile.cdl())
            }
        }
    }

    fun readHCdata(filename : String, varname : String? = null) {
        println("=================")
        println(filename)
        Hdf4ClibFile(filename).use { hcfile ->
            readMyData(hcfile, varname)
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    fun readH4Data(filename: String) {
        readNetchdfData(filename, null, null, true)
        println()
    }

    @ParameterizedTest
    @MethodSource("params")
    fun compareData(filename: String) {
        compareData(filename, null)
    }

    fun compareData(filename: String, varname : String?) {
        println("=================")
        println(filename)
        Hdf4File(filename).use { myfile ->
            // println("Hdf4File = \n${myfile.cdl()}")
            Hdf4ClibFile(filename).use { ncfile ->
                compareNetcdfData(myfile, ncfile, varname)
            }
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    fun testIterateWithClib(filename: String) {
        compareIterateWithClib(filename)
    }

}