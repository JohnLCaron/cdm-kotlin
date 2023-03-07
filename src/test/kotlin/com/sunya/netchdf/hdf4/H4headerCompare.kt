package com.sunya.netchdf.hdf4

import com.sunya.netchdf.NetchdfTest
import com.sunya.netchdf.netcdfClib.NetcdfClibFile
import com.sunya.netchdf.readData
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import test.util.testFilesIn
import java.util.*
import java.util.stream.Stream
import kotlin.test.assertEquals

// Compare header using cdl(strict) with Netcdf3File and NetcdfClibFile
class H4headerCompare {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
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

            return Stream.of(hdf4, hdfeos2, moar4).flatMap { i -> i};
        }
    }

    @Test
    fun hasStruct() {
        NetchdfTest.showData = true
        readData("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdf4/17766010.hdf",
            "Sea_Ice_Motion_Vectors_-_17766010")
        NetchdfTest.showData = false
    }

    @Test
    fun special() {
        compareH4header("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdf4/balloon_sonde.o3_knmi000_de.bilt_s2_20060905t112100z_002.hdf")
    }

    @Test
    fun readData() {
        readData("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdf4/balloon_sonde.o3_knmi000_de.bilt_s2_20060905t112100z_002.hdf",
            "O3.CONCENTRATION_INSITU", null, false)
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
    fun readDataForProfiling(filename: String) {
        println(filename)
        readData(filename)
        println()
    }

    // The netCDF-4 library can read HDF4 data files, if they were created with the SD (Scientific Data) API.
    // https://docs.unidata.ucar.edu/nug/current/getting_and_building_netcdf.html#build_hdf4
    @ParameterizedTest
    @MethodSource("params")
    fun compareH4header(filename : String) {
        println("=================")
        println(filename)
        Hdf4File(filename, true).use { myfile ->
            NetcdfClibFile(filename).use { ncfile ->
                //println("actual = $root")
                //println("expect = $expect")
                assertEquals(ncfile.cdl(true), myfile.cdl(true))
                println(myfile.cdl())
            }
        }
    }

}