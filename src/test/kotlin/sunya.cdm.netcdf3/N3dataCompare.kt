package sunya.cdm.netcdf3

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import sunya.cdm.api.DataType
import sunya.cdm.api.Group
import sunya.cdm.netcdfClib.NCheader
import test.util.oldTestDir
import test.util.testFilesIn
import java.util.*
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class N3dataCompare {
    val debug = false

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
        readN3data("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf3/WrfTimesStrUnderscore.nc")
    }

    @ParameterizedTest
    @MethodSource("params")
    fun readN3data(filename : String) {
        println("=================")
        println(filename)
        val rootb = Group.Builder()
        val n3header = N3header(OpenFile(filename), rootb, null)
        val root = rootb.build()
        val n3iosp = n3header.getIosp()

        // println(root.cdlString())

        val ncheader = NCheader(filename)
        val rootClib = ncheader.rootGroup.build()
        val nciosp = ncheader.getIosp()

        val n3vars = root.variables
        val ncvars = rootClib.variables
        n3vars.forEach { n3var ->
            val n3data = n3iosp.readArrayData(n3var)
            val ncvar = ncvars.find { it.name == n3var.name }
            val ncdata = nciosp.readArrayData(ncvar!!)
            if (ncdata != n3data) {
                println("===============\n${ncvar.name}")
                println("n3data = $n3data")
                println("ncdata = $ncdata")
            } else {
                if (debug) println("${ncvar.name} ok")

            }
            //assertEquals(ncdata, n3data)
//
        }

        assertEquals(rootClib.cdlString(), root.cdlString())
        // println(rootClib.cdlString())
    }

}