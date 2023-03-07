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

            return Stream.of(hdf4).flatMap { i -> i};
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
        readH4header("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdf4/balloon_sonde.o3_knmi000_de.bilt_s2_20060905t112100z_002.hdf")
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

    // @ParameterizedTest
    @MethodSource("params")
    fun compareH4header(filename : String) {
        println("=================")
        println(filename)
        Hdf4File(filename).use { myfile ->
            NetcdfClibFile(filename).use { ncfile ->
                //println("actual = $root")
                //println("expect = $expect")
                assertEquals(ncfile.cdl(), myfile.cdl())
                // println(rootClib.cdlString())
            }
        }
    }

}