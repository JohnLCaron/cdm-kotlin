package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.Range
import com.sunya.cdm.api.Section
import com.sunya.cdm.api.Variable
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

// Compare data reading for the same file with Netcdf3File and NetcdfClibFile
class H5dataCompare {
    val debug = true

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            val stream1 = Stream.of(
                Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/attstr.h5"), // no vars haha
                // Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/tst_dims.nc"),
                )
            val stream2 =
                testFilesIn(oldTestDir + "formats/netcdf3")
                    .withRecursion()
                    .addNameFilter { name -> !name.endsWith("perverse.nc") } // too slow
                    .build()

            return stream1
            // return Stream.of(stream1, stream2).flatMap { i -> i};
        }
    }

    //@Test
    fun problem1() {
        readH5dataCompareNC("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf3/files/c0tmp.nc")
    }


    @ParameterizedTest
    @MethodSource("params")
    fun readH5dataCompareNC(filename : String) {
        println("=================")
        println(filename)
        val h5file = Hdf5File(filename)
        val root = h5file.rootGroup()
        println(h5file.cdl())

        val ncfile = NetcdfClibFile(filename)
        val rootClib = ncfile.rootGroup()

        val n3vars = root.variables
        val ncvars = rootClib.variables
        if (debug) print("  OK")
        n3vars.forEach { n3var ->
            val n3data = h5file.readArrayData(n3var)
            val ncvar = ncvars.find { it.name == n3var.name }
            val ncdata = ncfile.readArrayData(ncvar!!)
            if (ncdata != n3data) {
                println(" *** FAIL reading data for ${ncvar.name}")
                println(" n3data = $n3data")
                println(" ncdata = $ncdata")
            } else {
                if (debug) print(" ${ncvar.name}")
            }
            assertEquals(ncdata, n3data)

            if (ncvar.nelems > 8) {
                testMiddleSection(h5file, n3var, ncfile, ncvar)
            }
        }
        if (debug) println()

        assertEquals(ncfile.cdl(), h5file.cdl())
        // println(rootClib.cdlString())
        h5file.close()
        ncfile.close()
    }

    fun testMiddleSection(h5file : Iosp, n3var : Variable, ncfile : Iosp, ncvar : Variable) {
        val shape = ncvar.shape
        val orgSection = Section(shape)
        val middleRanges = orgSection.ranges.map {range ->
            if (range == null) throw RuntimeException("Range is null")
                if (range.length < 9) range
            else Range(range.first + range.length/3, range.last - range.length/3)
        }
        val middleSection = Section(middleRanges)

        val n3data = h5file.readArrayData(n3var, middleSection)
        val ncdata = ncfile.readArrayData(ncvar, middleSection)
        if (ncdata != n3data) {
            println(" *** FAIL reading data for ${ncvar.name}")
            println(" n3data = $n3data")
            println(" ncdata = $ncdata")
        } else {
            if (debug) print(" ${ncvar.name}[$middleSection]")
        }
        assertEquals(ncdata, n3data)

    }

}