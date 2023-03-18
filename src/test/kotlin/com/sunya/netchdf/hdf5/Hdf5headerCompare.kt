package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.Netchdf
import com.sunya.netchdf.netcdfClib.NetcdfClibFile
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import test.util.testFilesIn
import java.util.*
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Compare header using cdl(!strict) with Hdf5File and NetcdfClibFile
// sometime fail when they are not netcdf4 files, so nc4lib sees them as empty
class Hdf5headerCompare {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            // 10 of 114 fail, because we compare with netcdf4 instead of hdf5 c library

            val moar4 =
                testFilesIn("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf4")
                    .withPathFilter { p -> !p.toString().contains("exclude") }
                    .withRecursion()
                    .build()

            val hdf5 =
                testFilesIn("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdf5")
                    .withRecursion()
                    .build()

            val moar5 =
                testFilesIn("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/hdf5")
                    .withPathFilter { p -> !p.toString().contains("exclude") }
                    .addNameFilter { name -> !name.endsWith(".xml") } // bug in clib
                    .withRecursion()
                    .build()

            val hdfeos5 =
                testFilesIn("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdfeos5")
                    .withRecursion()
                    .build()

            // return hdfeos5
            return Stream.of(moar4, hdf5, moar5).flatMap { i -> i };
        }
    }

    @Test
    fun problem() {
        compareH5andNclib("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdf5/dstrarr.h5")
    }

    @Test
    fun problem2() {
        compareH5andNclib("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdf5/i32be.h5")
    }

    @ParameterizedTest
    @MethodSource("params")
    fun checkVersion(filename: String) {
        Hdf5File(filename).use { ncfile ->
            println("${ncfile.type()} $filename ")
            assertTrue(ncfile.type().contains("hdf5") or (ncfile.type().contains("netcdf4")))
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    fun compareH5andNclib(filename: String) {
        println("=================")
        val h5file = Hdf5File(filename, true)
        println("${h5file.type()} $filename ")
        println("\n${h5file.cdl()}")

        val nclibfile : Netchdf = NetcdfClibFile(filename)
        println("ncfile = ${nclibfile.cdl()}")

        assertEquals(nclibfile.cdl(), h5file.cdl())

        h5file.close()
        nclibfile.close()
    }

}