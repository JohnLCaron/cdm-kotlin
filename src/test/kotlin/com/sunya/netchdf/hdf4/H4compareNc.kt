package com.sunya.netchdf.hdf4

import com.sunya.cdm.api.Section
import com.sunya.netchdf.NetchdfTest
import com.sunya.netchdf.netcdfClib.NetcdfClibFile
import com.sunya.netchdf.readDataCompareHC
import com.sunya.netchdf.readMyData
import com.sunya.netchdf.readDataCompareNC
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import test.util.testFilesIn
import java.util.*
import java.util.stream.Stream
import kotlin.test.assertEquals

class H4compareNc {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            val sds = Stream.of(
                Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdf4/balloon_sonde.o3_knmi000_de.bilt_s2_20060905t112100z_002.hdf"),
                Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdf4/MAC07S0.A2008230.1250.002.2008233222357.hdf"),
                Arguments.of("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/hdf4/c402_rp_02.diag.sfc.20020122_0130z.hdf"),
                Arguments.of("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/hdf4/MI1B2T_B54_O003734_AN_05.hdf"),
            )

            val hdf4 =
                testFilesIn("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdf4")
                    .withRecursion()
                    .build()

            val hdfeos2 =
                testFilesIn("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdfeos2")
                    .withRecursion()
                    .build()

            val moar4 =
                testFilesIn("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/hdf4")
                    .withRecursion()
                    .build()

            val moar42 =
                testFilesIn("/media/twobee/netch/hdf4")
                    .withRecursion()
                    .addNameFilter { name -> !name.endsWith(".ncml") }
                    .addNameFilter { name -> !name.endsWith(".xml") }
                    .addNameFilter { name -> !name.endsWith(".pdf") }
                    .build()

            return Stream.of(sds, hdf4, hdfeos2, moar4, moar42).flatMap { i -> i};
        }
    }

    @Test
    fun hasStruct() {
        NetchdfTest.showData = true
        readMyData("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdf4/17766010.hdf",
            "Sea_Ice_Motion_Vectors_-_17766010")
        NetchdfTest.showData = false
    }

    @Test
    fun problem() {
        readDataCompareHC("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/hdf4/c402_rp_02.diag.sfc.20020122_0130z.hdf")
    }

    // compress_type = 0
    @Test
    fun compressProblem() {
        readDataCompareNC("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/hdf4/MYD021KM.A2008349.1800.005.2009329084841.hdf")
    }

    @Test
    fun linkedNotCompressed() {
        readDataCompareNC("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/hdf4/c402_rp_02.diag.sfc.20020122_0130z.hdf",
            "ALBEDO")
    }

    @Test
    fun chunkedCompressed() {
        // readH4header("/media/twobee/netch/hdf4/chlora/MODSCW_P2009168_C4_1805_1810_1940_1945_GM03_closest_chlora.hdf")
        readDataCompareHC("/media/twobee/netch/hdf4/chlora/MODSCW_P2009168_C4_1805_1810_1940_1945_GM03_closest_chlora.hdf",
            "chlor_a", Section("0:1533,0:1000"))
    }

    @ParameterizedTest
    @MethodSource("params")
    fun readH4header(filename : String) {
        println("=================")
        println(filename)
        Hdf4File(filename).use { myfile ->
            println("actual = ${myfile.cdl()}")
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    fun readData(filename: String) {
        readMyData(filename)
        println()
    }

    @ParameterizedTest
    @MethodSource("params")
    fun readDataCompareWithNC(filename: String) {
        readDataCompareNC(filename)
        println()
    }


    @ParameterizedTest
    @MethodSource("params")
    fun readDataCompareWithHC(filename: String) {
        readDataCompareHC(filename)
        println()
    }

    @Test
    fun problemHeader() {
        compareH4header("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/hdf4/ssec/CAL_LID_L1-Launch-V1-06.2006-07-07T21-20-40ZD.hdf")
    }

    @Test
    fun problemData() {
        readDataCompareNC("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/hdf4/ssec/CAL_LID_L1-Launch-V1-06.2006-07-07T21-20-40ZD.hdf")
    }

    // The netCDF-4 library can read HDF4 data files, if they were created with the SD (Scientific Data) API.
    // https://docs.unidata.ucar.edu/nug/current/getting_and_building_netcdf.html#build_hdf4
    // @ParameterizedTest
    @MethodSource("params")
    fun compareH4header(filename : String) {
        println("=================")
        println(filename)
        Hdf4File(filename, true).use { myfile ->
            NetcdfClibFile(filename).use { ncfile ->
                //println("actual = $root")
                //println("expect = $expect")
                assertEquals(ncfile.cdl(), myfile.cdl())
                println(myfile.cdl())
            }
        }
    }

}