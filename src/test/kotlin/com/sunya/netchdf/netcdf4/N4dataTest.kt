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

class N4dataTest {
    val debug = false

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            val stream1 = Stream.of(
                Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/tst_dims.nc"),
                Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/attstr.h5"),
                Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/dimScales.h5"),
            )
            val stream2 =
                testFilesIn("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4")
                    .withRecursion()
                    .build()

            return Stream.of(stream1, stream2).flatMap { i -> i };
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
    fun readN4data(filename: String) {
        println("=================")
        println(filename)

         NetcdfClibFile(filename).use { ncfile ->
             println(ncfile.cdl())
            val ncvars = ncfile.rootGroup().variables
            ncvars.forEach { n4var ->
                val ncdata = ncfile.readArrayData(n4var)
                println("===============\n${n4var.name}")
                println("ncdata = $ncdata")
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
        println("ncdata = $ncdata")
    }

}