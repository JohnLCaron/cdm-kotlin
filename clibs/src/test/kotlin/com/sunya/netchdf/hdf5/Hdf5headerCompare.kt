package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.Netchdf
import com.sunya.netchdf.netcdfClib.NetcdfClibFile
import com.sunya.testdata.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
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

            val hdfeos5 =
                testFilesIn(testData + "devcdm/hdfeos5")
                    .withRecursion()
                    .build()

            return Stream.of( N4Files.params(),  H5Files.params()).flatMap { i -> i };
        }
    }

    @Test
    fun problem() {
        compareH5andNclib(testData + "devcdm/hdf5/dstrarr.h5")
    }

    @Test
    fun problem2() {
        compareH5andNclib(testData + "devcdm/hdf5/i32be.h5")
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