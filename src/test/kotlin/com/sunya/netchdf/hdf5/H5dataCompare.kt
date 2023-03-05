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

            val stream4 =
                testFilesIn("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4")
                    .withPathFilter { p -> !p.toString().contains("exclude") }
                    .addNameFilter { name -> !name.endsWith("tst_grps.nc4") } // nested group typedefs
                    .build()

            val moar4 =
                testFilesIn(oldTestDir + "formats/netcdf4/new")
                    .withRecursion()
                    .withPathFilter { p -> !p.toString().contains("exclude") }
                    .addNameFilter { name -> !name.endsWith("perverse.nc") } // too slow
                    .build()

            return Stream.of(stream1, stream4).flatMap { i -> i};
        }
    }

    val reversed = "/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf4/Ike.egl3.SWI.tidal.nc"

    @Test
    fun wtf1() {
        Hdf5File.useOld = false
        readDataCompareNC(reversed, "pyy", Section("0:99, 0:99, :"))
        Hdf5File.useOld = false
    }
    @Test
    fun wtf2() {
        Hdf5File.useOld = false
        readDataCompareNC(reversed, "fyy", Section("0:99, 0:99, 0:9"))
        Hdf5File.useOld = false
    }

    @Test
    fun problem() {
        readDataCompareNC("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf4/files/c0.nc")
    }

    @Test
    fun problem2() {
        readDataCompareNC("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf4/new/OR_ABI-L2-CMIPF-M6C13_G16_s20230451800207_e20230451809526_c20230451810015.nc", "CMI")
    }

    @ParameterizedTest
    @MethodSource("params")
    fun readH5dataCompareNC(filename: String) {
        readDataCompareNC(filename, null)
    }

    fun readDataCompareNC(filename: String, varname: String? = null, section: Section? = null) {
        RandomAccessFile(File(filename), "r").use { raf ->
            val size = raf.getChannel().size() / 1000.0 / 1000.0
            println("$filename size ${"%.2f".format(size)} Mbytes")
        }
        val h5file = Hdf5File(filename)
        val ncfile = NetcdfClibFile(filename)
        compareNetcdfData(h5file, ncfile, varname, section)
        h5file.close()
        ncfile.close()
        println()
    }
}

var showData = false
var showFailedData = false

fun compareNetcdfData(myfile: Netcdf, ncfile: Netcdf, varname: String?, section: Section? = null) {
    if (varname != null) {
        val myvar = myfile.rootGroup().allVariables().find { it.fullname() == varname }
        if (myvar == null) {
            println(" *** cant find myvar $varname")
            return
        }
        val ncvar = ncfile.rootGroup().allVariables().find { it.fullname() == myvar.fullname() }
        if (ncvar == null) {
            println(" *** cant find ncvar $varname")
            return
        }
        oneVar(myvar, myfile, ncvar, ncfile, section)
    } else {
        myfile.rootGroup().variables.forEachIndexed { idx, myvar ->
            val ncvar = ncfile.rootGroup().allVariables().find { it.fullname() == myvar.fullname() }
            if (ncvar == null) {
                println(" *** cant find ncvar ${myvar.fullname()}")
                return
            }
            oneVar(myvar, myfile, ncvar, ncfile, null)
        }
    }
}

fun oneVar(myvar: Variable, h5file: Iosp, ncvar : Variable, ncfile: Iosp, section: Section?) {
    val section = Section.fill(section, myvar.shape)
    val nbytes = section.size() * myvar.datatype.size
    if (nbytes > 100_000_000) {
        println(" * ${myvar.fullname()} read too big = ${nbytes}")
    } else {
        val mydata = h5file.readArrayData(myvar, section)
        val ncdata = ncfile.readArrayData(ncvar, section)
        println(" ${myvar.datatype} ${myvar.fullname()}${myvar.shape.contentToString()} = " +
                "${mydata.shape.contentToString()} ${Section.computeSize(mydata.shape)} elems" )

        if (myvar.datatype == Datatype.CHAR) {
            compareCharData(myvar.fullname(), mydata, ncdata)
        } else {
            if (!ArrayTyped.contentEquals(ncdata, mydata)) {
                println(" *** FAIL comparing data for variable = ${ncvar.datatype} ${ncvar.name} ${ncvar.dimensions.map { it.name }}")
                if (showFailedData) {
                    println("\n mydata = $mydata")
                    println(" ncdata = $ncdata")
                    ArrayTyped.contentEquals(ncdata, mydata)
                    assertTrue(false)
                } else {
                    println("\n countDifferences = ${ArrayTyped.countDiff(ncdata, mydata)}")
                }
                return
            } else {
                if (showData) {
                    print(" ${ncvar.cdl()}, ")
                    print("\n mydata = $mydata")
                    print(" ncdata = $ncdata")
                }
            }
        }
    }
    if (ncvar.nelems > 8 && ncvar.datatype != Datatype.CHAR) {
        testMiddleSection(h5file, myvar, ncfile, ncvar, ncvar.shape)
    }
}

fun testMiddleSection(myfile: Iosp, myvar: Variable, ncfile: Iosp, ncvar: Variable, shape : IntArray) {
    val orgSection = Section(shape)
    val middleRanges = orgSection.ranges.map { range ->
        if (range == null) throw RuntimeException("Range is null")
        if (range.length < 9) range
        else Range(range.first + range.length / 3, range.last - range.length / 3)
    }
    val middleSection = Section(middleRanges)
    val nbytes = middleSection.size() * myvar.datatype.size
    if (nbytes > 100_000_000) {
        println("  * ${myvar.fullname()} read too big = ${nbytes}")
        testMiddleSection(myfile, myvar, ncfile, ncvar, middleSection.shape)
        return
    }

    val mydata = myfile.readArrayData(myvar, middleSection)
    val ncdata = ncfile.readArrayData(ncvar, middleSection)
    println("  ${myvar.fullname()}[$middleSection] = ${mydata.shape.contentToString()} ${Section.computeSize(mydata.shape)} elems")

    if (myvar.datatype == Datatype.CHAR) {
        compareCharData(myvar.fullname(), mydata, ncdata)
    } else {
        if (!ArrayTyped.contentEquals(ncdata, mydata)) {
            println(" *** FAIL comparing middle section variable = ${ncvar}")
            if (showFailedData) {
                println(" mydata = $mydata")
                println(" ncdata = $ncdata")
            } else {
            println("\n countDifferences = ${ArrayTyped.countDiff(ncdata, mydata)}")
        }
            assertTrue(false)
            return
        }
    }
}

fun compareCharData(name : String, mydata: ArrayTyped<*>, ncdata: ArrayTyped<*>) {
    if (!ArrayTyped.valuesEqual(ncdata, mydata)) {
        println("   *** FAIL comparing char variable = ${name}")
        print("   ncdata = $ncdata")
        print("   mydata = $mydata")
    }
}