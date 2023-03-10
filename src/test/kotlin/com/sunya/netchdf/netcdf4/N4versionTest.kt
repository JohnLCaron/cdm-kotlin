package com.sunya.netchdf.netcdf4

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import com.sunya.netchdf.netcdfClib.NetcdfClibFile
import test.util.testFilesIn
import java.util.*
import java.util.stream.Stream
import kotlin.test.assertTrue

class N4versionTest {

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

    @ParameterizedTest
    @MethodSource("params")
    fun checkVersion(filename: String) {
        NetcdfClibFile(filename).use { ncfile ->
            println("${ncfile.type()} $filename ")
            assertTrue((ncfile.type() == "NC_FORMAT_NETCDF4") or (ncfile.type() == "NC_FORMAT_NETCDF4_CLASSIC"))
        }
    }

}