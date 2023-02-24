package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.*
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
import kotlin.test.assertTrue


// Compare data reading for the same file with Netcdf3File and NetcdfClibFile
class H5dataCompare {

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

            return Stream.of(stream1, stream2).flatMap { i -> i};
        }
    }

    @Test
    fun problem1() {
        readH5dataCompareNC("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/fpcs_1dwave_2.nc")
    }
    @Test
    fun problem2() {
        readDataCompareNC("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/IntTimSciSamp.nc", "tim_records")
    }

    @Test
    fun problem() {
    }

    @ParameterizedTest
    @MethodSource("params")
    fun readH5dataCompareNC(filename: String) {
        readDataCompareNC(filename, null)
    }

    fun readDataCompareNC(filename: String, varname: String?) {
        val h5file = Hdf5File(filename)
        val ncfile = NetcdfClibFile(filename)
        compareNetcdf(h5file, ncfile, varname)
        h5file.close()
        ncfile.close()
    }
}

var debugCompareNetcdf = true
var showData = false

fun compareNetcdf(myfile: Netcdf, ncfile: Netcdf, varname: String?) {
    println("=================")
    println(myfile.location())
    println(myfile.cdl())
    println()

    if (varname != null) {
        oneVar(varname, myfile as Iosp, ncfile as Iosp)
    } else {
        myfile.rootGroup().variables.forEach { oneVar(it.name, myfile as Iosp, ncfile as Iosp) }
    }
}

fun oneVar(varname: String, h5file: Iosp, ncfile: Iosp) {
    val myvar = h5file.rootGroup().variables.find { it.name == varname }
    val mydata = h5file.readArrayData(myvar!!)

    val ncvar = ncfile.rootGroup().variables.find { it.name == varname }
    val ncdata = ncfile.readArrayData(ncvar!!)

    if (!ArrayTyped.contentEquals(ncdata, mydata)) {
        println(" *** FAIL reading data for variable = ${ncvar.datatype} ${ncvar.name} ${ncvar.dimensions.map { it.name }}")
        println("\n mydata = $mydata")
        println(" ncdata = $ncdata")
        ArrayTyped.contentEquals(ncdata, mydata)
        assertTrue(false)
        return
    } else {
        if (showData) {
            print(" ${ncvar.cdl()}, ")
            print("\n mydata = $mydata")
            print(" ncdata = $ncdata")
        }
    }
    if (ncvar.nelems > 8 && ncvar.datatype != Datatype.CHAR) {
        testMiddleSection(h5file, myvar, ncfile, ncvar)
    }
    if (debugCompareNetcdf) println()
}

fun testMiddleSection(myfile: Iosp, myvar: Variable, ncfile: Iosp, ncvar: Variable) {
    val shape = ncvar.shape
    val orgSection = Section(shape)
    val middleRanges = orgSection.ranges.map { range ->
        if (range == null) throw RuntimeException("Range is null")
        if (range.length < 9) range
        else Range(range.first + range.length / 3, range.last - range.length / 3)
    }
    val middleSection = Section(middleRanges)
    // println(" ${ncvar.name}[$middleSection]")

    val mydata = myfile.readArrayData(myvar, middleSection)
    val ncdata = ncfile.readArrayData(ncvar, middleSection)
    if (!ArrayTyped.contentEquals(ncdata, mydata)) {
        println(" *** FAIL reading middle section for variable = ${ncvar}")
        println(" mydata = $mydata")
        println(" ncdata = $ncdata")
        assertTrue(false)
        return
    } else {
        if (debugCompareNetcdf) print(" ${ncvar.name}[$middleSection], ")
    }

}