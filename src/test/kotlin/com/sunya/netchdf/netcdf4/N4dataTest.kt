package com.sunya.netchdf.netcdf4

import com.sunya.cdm.api.Datatype
import com.sunya.cdm.api.Range
import com.sunya.cdm.api.Section
import com.sunya.cdm.api.Variable
import com.sunya.cdm.iosp.Iosp
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import com.sunya.netchdf.netcdfClib.NetcdfClibFile
import test.util.testFilesIn
import java.util.*
import java.util.stream.Stream
import kotlin.test.assertTrue

class N4dataTest {
    val debug = false

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            val stream4 =
                testFilesIn("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4")
                    .addNameFilter { name -> !name.endsWith("tst_grps.nc4") } // nested group typedefs
                    .build()

            val moar4 =
                testFilesIn("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf4")
                    .withPathFilter { p -> !p.toString().contains("exclude") }
                    .addNameFilter { name -> !name.endsWith("compound-attribute-test.nc") } // bug in clib
                    .withRecursion()
                    .build()

            // return moar3
            return Stream.of(stream4, moar4).flatMap { i -> i };
        }
    }


    @Test
    fun special() {
        readN4data("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/string_attrs.nc4")
        // readN4data("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/tst_groups.nc")
    }


    @Test
    fun problem() {
        readN4data("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/IntTimSciSamp.nc")
    }

    @ParameterizedTest
    @MethodSource("params")
    fun checkVersion(filename: String) {
        NetcdfClibFile(filename).use { ncfile ->
            println("${ncfile.type()} $filename ")
            assertTrue((ncfile.type() == "NC_FORMAT_NETCDF4") or (ncfile.type() == "NC_FORMAT_NETCDF4_CLASSIC"))
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    fun readN4data(filename: String) {
        println("=================")
        println(filename)

         NetcdfClibFile(filename).use { ncfile ->
             println(ncfile.cdl())
            val ncvars = ncfile.rootGroup().variables
            ncvars.forEach { n4var ->
                val ncdata = ncfile.readArrayData(n4var)
                println("===============\n${n4var.name}")
                if (debug) println("ncdata = $ncdata")
                if (n4var.nelems > 8 && n4var.datatype != Datatype.CHAR) {
                    readMiddleSection(ncfile, n4var)
                }
            }
         }
    }

    fun readMiddleSection(ncfile: Iosp, ncvar: Variable) {
        val shape = ncvar.shape
        val orgSection = Section(shape)
        val middleRanges = orgSection.ranges.map { range ->
            if (range == null) throw RuntimeException("Range is null")
            if (range.length < 9) range
            else Range(range.first + range.length / 3, range.last - range.length / 3)
        }
        val middleSection = Section(middleRanges)
        println(" ${ncvar.name}[$middleSection]")
        val ncdata = ncfile.readArrayData(ncvar, middleSection)
        println("===============\n${ncvar.name}")
        if (debug) println("ncdata = $ncdata")
    }

}