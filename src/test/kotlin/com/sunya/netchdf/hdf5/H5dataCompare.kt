package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.Datatype
import com.sunya.cdm.api.Range
import com.sunya.cdm.api.Section
import com.sunya.cdm.api.Variable
import com.sunya.cdm.iosp.ArrayTyped
import com.sunya.cdm.iosp.Iosp
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
import kotlin.test.assertTrue
import kotlin.test.fail

// Compare data reading for the same file with Netcdf3File and NetcdfClibFile
class H5dataCompare {
    val debug = true

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            val stream1 = Stream.of(
                Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/dimScales.h5"),
                Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/test2.nc"),
                Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/nctst_netcdf4_classic.nc4"),
                Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/attstr.h5"), // no vars haha
                Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/tst_dims.nc"), // use fill value
                )

            val stream2 =
                testFilesIn("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4")
                    .build()

            val stream3 =
                testFilesIn(oldTestDir + "formats/netcdf4")
                    .withRecursion()
                    .addNameFilter { name -> !name.endsWith("perverse.nc") } // too slow
                    .build()

            return stream2
            // return Stream.of(stream1, stream2).flatMap { i -> i};
        }
    }

    @Test
    fun problem() {
        readH5dataCompareNC("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/cdm_sea_soundings.nc4")
    }

    @Test
    fun problem2() {
        readH5dataCompareNC("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/vlenInt.nc")
    }

    @ParameterizedTest
    @MethodSource("params")
    fun readH5dataCompareNC(filename : String) {
        println("=================")
        println(filename)
        val h5file = Hdf5File(filename)
        val root = h5file.rootGroup()
        println()
        println(h5file.cdl())

        val ncfile = NetcdfClibFile(filename)
        val rootClib = ncfile.rootGroup()

        val h5vars = root.variables
        val ncvars = rootClib.variables
        if (debug) print("  OK read ")
        h5vars.forEach { h5var ->
            val h5data = h5file.readArrayData(h5var)
            val ncvar = ncvars.find { it.name == h5var.name }
            val ncdata = ncfile.readArrayData(ncvar!!)
            if (!ArrayTyped.contentEquals(ncdata, h5data)) {
                println(" *** FAIL reading data for ${ncvar.name}")
                //println(" h5data = $h5data")
                //println(" ncdata = $ncdata")
                return
            } else {
                if (debug) print(" ${ncvar.name}, ")
            }
            if (ncvar.nelems > 8 && ncvar.datatype != Datatype.CHAR) {
                testMiddleSection(h5file, h5var, ncfile, ncvar)
            }
        }
        if (debug) println()

        assertEquals(ncfile.cdl(), h5file.cdl())
        // println(rootClib.cdlString())
        h5file.close()
        ncfile.close()
    }

    fun testMiddleSection(h5file : Iosp, h5var : Variable, ncfile : Iosp, ncvar : Variable) {
        val shape = ncvar.shape
        val orgSection = Section(shape)
        val middleRanges = orgSection.ranges.map {range ->
            if (range == null) throw RuntimeException("Range is null")
                if (range.length < 9) range
            else Range(range.first + range.length/3, range.last - range.length/3)
        }
        val middleSection = Section(middleRanges)

        val h5data = h5file.readArrayData(h5var, middleSection)
        val ncdata = ncfile.readArrayData(ncvar, middleSection)
        if (!ArrayTyped.contentEquals(ncdata, h5data)) {
            println(" *** FAIL reading middle section for ${ncvar.name}")
            println(" h5data = $h5data")
            println(" ncdata = $ncdata")
            return
        } else {
            if (debug) print(" ${ncvar.name}[$middleSection], ")
        }

    }

}