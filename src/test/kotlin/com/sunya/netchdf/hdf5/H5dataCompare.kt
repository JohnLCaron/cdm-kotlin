package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.*
import com.sunya.cdm.array.ArrayTyped
import com.sunya.cdm.iosp.Iosp
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import com.sunya.netchdf.netcdfClib.NetcdfClibFile
import test.util.oldTestDir
import test.util.testFilesIn
import java.io.File
import java.io.RandomAccessFile
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
                    .addNameFilter { name -> !name.endsWith("tst_grps.nc4") } // nested group typedefs
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
        readH5dataCompareNC("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/attributeStruct.nc")
    }
    @Test
    fun problem2() {
        readDataCompareNC("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/simple_xy_nc4.nc", "data")
    }

    @Test
    fun problem() {
        readDataCompareNC("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf4/files/tst_string_data.nc")
    }

    @Test // strangely slow
    fun problem3() {
        readDataCompareNC("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf4/Ike.egl3.SWI.tidal.nc", "pyy")
    }

    @Test // strangely slow
    fun problem4() {
        val chunked2 = "/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf4/UpperDeschutes_t4p10_swemelt.nc"
        readDataCompareNC(chunked2, "UpperDeschutes_t4p10_swemelt", Section("5:9, :, :"))
    }

    @ParameterizedTest
    @MethodSource("params")
    fun readH5dataCompareNC(filename: String) {
        readDataCompareNC(filename, null)
    }

    fun readDataCompareNC(filename: String, varname: String? = null, section: Section? = null) {
        RandomAccessFile(File(filename), "r").use { raf ->
            val size = raf.getChannel().size() / 1000.0 / 1000.0
            println(" $filename size ${"%.2f".format(size)} Mbytes")
        }
        println("=================")
        val h5file = Hdf5File(filename)
        val ncfile = NetcdfClibFile(filename)
        compareNetcdf(h5file, ncfile, varname, section)
        h5file.close()
        ncfile.close()
    }
}

var debugCompareNetcdf = true
var showData = false

fun compareNetcdf(myfile: Netcdf, ncfile: Netcdf, varname: String?, section: Section? = null) {
    println(myfile.cdl())
    println()

    if (varname != null) {
        oneVar(varname, myfile, ncfile, section)
    } else {
        myfile.rootGroup().variables.forEachIndexed { idx, it ->
            oneVar(it.name, myfile, ncfile, null)
        }
    }
    println()
}

fun oneVar(varname: String, h5file: Iosp, ncfile: Iosp, section: Section?) {
    val myvar = h5file.rootGroup().variables.find { it.name == varname }
    if (myvar == null) {
        println("cant find $varname")
        return
    }
    val section = Section.fill(section, myvar.shape)
    val nbytes = section.size() * myvar.datatype.size
    if (nbytes > 100_000_000) {
        println("$varname read too big = ${nbytes}")
        return
    }
    val mydata = h5file.readArrayData(myvar, section)
    val ncvar = ncfile.rootGroup().variables.find { it.name == varname }
    val ncdata = ncfile.readArrayData(ncvar!!, section)
    if (debugCompareNetcdf) println(" ${ncvar.name}, ")

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
    val nbytes = middleSection.size() * myvar.datatype.size
    if (nbytes > 100_000_000) {
        println("${myvar.name} read too big = ${nbytes}")
        return
    }

    if (debugCompareNetcdf) println(" myfile ${ncvar.name}[$middleSection],")
    val mydata = myfile.readArrayData(myvar, middleSection)
    if (debugCompareNetcdf) println(" ncfile ${ncvar.name}[$middleSection],")
    val ncdata = ncfile.readArrayData(ncvar, middleSection)

    if (!ArrayTyped.contentEquals(ncdata, mydata)) {
        println(" *** FAIL reading middle section for variable = ${ncvar}")
        println(" mydata = $mydata")
        println(" ncdata = $ncdata")
        assertTrue(false)
        return
    }

}