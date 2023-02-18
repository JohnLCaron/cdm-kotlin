package com.sunya.netchdf.netcdf3

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import com.sunya.netchdf.netcdfClib.NetcdfClibFile
import test.util.oldTestDir
import test.util.testFilesIn
import java.util.*
import java.util.stream.Stream
import kotlin.test.assertEquals

// Compare header using cdl(strict) with Netcdf3File and NetcdfClibFile
class N3headerCompare {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            val stream1 = Stream.of(
                    Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf3/simple_xy.nc"),
                    Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf3/longOffset.nc"),
                    Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf3/WMI_Lear-2003-05-28-212817.nc"),
                    Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf3/nctest_64bit_offset.nc"),
                    Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf3/WrfTimesStrUnderscore.nc"),
                    Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf3/testSpecialChars.nc"),
                )
            val stream2 =
                testFilesIn(oldTestDir + "formats/netcdf3")
                    .withRecursion()
                    .build()

            return Stream.of(stream1, stream2).flatMap { i -> i};
        }
    }

    @Test
    fun special() {
        readN3header("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf3/files/nc_test_classic.nc4")
    }

    @ParameterizedTest
    @MethodSource("params")
    fun readN3header(filename : String) {
        println("=================")
        println(filename)
        Netcdf3File(filename).use { n3file ->
            NetcdfClibFile(filename).use { ncfile ->
                //println("actual = $root")
                //println("expect = $expect")
                assertEquals(ncfile.cdl(), n3file.cdl())
                // println(rootClib.cdlString())
            }
        }
    }

}