package com.sunya.netchdf.netcdf3

import com.sunya.netchdf.compareNetcdfData
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import com.sunya.netchdf.netcdfClib.NetcdfClibFile
import test.util.testFilesIn
import java.util.*
import java.util.stream.Stream

// Compare data reading for the same file with Netcdf3File and NetcdfClibFile
class N3dataCompare {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            val stream3 =
                testFilesIn("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf3")
                    .build()

            val moar3 =
                testFilesIn(test.util.oldTestDir + "formats/netcdf3")
                    .withPathFilter { p -> !p.toString().contains("exclude") }
                    .withRecursion()
                    .build()
            return Stream.of(stream3, moar3).flatMap { i -> i};

        }
    }

    @Test
    fun awips() {
        readDataCompareNC("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf3/awips.nc", "uw")
    }

    @Test
    fun problem2() {
        readDataCompareNC("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf3/WMI_Lear-2003-05-28-212817.nc", "time")
    }


    @ParameterizedTest
    @MethodSource("params")
    fun readN3dataCompareNC(filename : String) {
        readDataCompareNC(filename, null)
    }

    fun readDataCompareNC(filename : String, varname : String?) {
        val myfile = Netcdf3File(filename)
        val ncfile = NetcdfClibFile(filename)
        compareNetcdfData(myfile, ncfile, varname)
        myfile.close()
        ncfile.close()
    }

}