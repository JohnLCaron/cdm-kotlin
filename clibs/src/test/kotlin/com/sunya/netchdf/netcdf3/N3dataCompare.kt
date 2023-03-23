package com.sunya.netchdf.netcdf3

import com.sunya.netchdf.compareNetcdfData
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import com.sunya.netchdf.netcdfClib.NetcdfClibFile
import test.util.testData
import test.util.testFilesIn
import java.util.*
import java.util.stream.Stream

// Compare data reading for the same file with Netcdf3File and NetcdfClibFile
class N3dataCompare {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            val stream3 =
                testFilesIn(testData + "devcdm/netcdf3")
                    .build()

            val moar3 =
                testFilesIn(testData + "cdmUnitTest/formats/netcdf3")
                    .withPathFilter { p -> !p.toString().contains("exclude") }
                    .addNameFilter { name -> !name.endsWith("perverse.nc") } // too slow
                    .withRecursion()
                    .build()
            return Stream.of(stream3, moar3).flatMap { i -> i};

        }
    }

    @Test
    fun awips() {
        readDataCompareNC(testData + "cdmUnitTest/formats/netcdf3/awips.nc", "uw")
    }

    @Test
    fun problem2() {
        readDataCompareNC(testData + "devcdm/netcdf3/nctest_classic.nc", "c")
    }


    @ParameterizedTest
    @MethodSource("params")
    fun readN3dataCompareNC(filename : String) {
        readDataCompareNC(filename, null)
    }

    fun readDataCompareNC(filename : String, varname : String?) {
        val myfile = Netcdf3File(filename)
        val ncfile = NetcdfClibFile(filename)
        println(filename)
        println(myfile.cdl())
        compareNetcdfData(myfile, ncfile, varname)
        myfile.close()
        ncfile.close()
    }

}