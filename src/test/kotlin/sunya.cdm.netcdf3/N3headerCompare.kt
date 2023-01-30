package sunya.cdm.netcdf3

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import sunya.cdm.api.Group
import sunya.cdm.netcdfClib.NCheader
import test.util.oldTestDir
import test.util.testFilesIn
import java.util.*
import java.util.stream.Stream
import kotlin.test.assertEquals

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
        val rootb = Group.Builder()
        val ncheader = N3header(OpenFile(filename), rootb, null)
        val root = rootb.build()
        //println(root.cdlString())
        //println("________")

        val headerClib = NCheader(filename)
        val rootClib = headerClib.rootGroup.build()

        //println("actual = $root")
        //println("expect = $expect")

        assertEquals(rootClib.cdlString(), root.cdlString())
        // println(rootClib.cdlString())
    }

}