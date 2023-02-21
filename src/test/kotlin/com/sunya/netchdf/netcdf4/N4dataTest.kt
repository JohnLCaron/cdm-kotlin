package com.sunya.netchdf.netcdf4

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
    }

    @ParameterizedTest
    @MethodSource("params")
    fun readN4data(filename: String) {
        println("=================")
        println(filename)
         NetcdfClibFile(filename).use { ncfile ->
             println(ncfile.cdl())
             /*
        val ncvars = rootClib.variables
        ncvars.forEach { n4var ->
            val ncdata = nciosp.readArrayData(n4var)
            println("===============\n${n4var.name}")
            println("ncdata = $ncdata")
        }

         */
         }
    }

}