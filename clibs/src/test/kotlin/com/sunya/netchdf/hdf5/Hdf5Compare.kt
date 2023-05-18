package com.sunya.netchdf.hdf5

import com.sunya.netchdf.compareDataWithClib
import com.sunya.netchdf.netcdfClib.NClibFile
import com.sunya.testdata.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.*
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Compare header with Hdf5File and NetcdfClibFile
// sometime fail when they are not netcdf4 files, so nc4lib sees them as empty
class Hdf5Compare {

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
    fun testEos() {
        compareH5andNclib(testData + "cdmUnitTest/formats/hdf5/aura/MLS-Aura_L2GP-BrO_v01-52-c01_2007d029.he5")
    }

    @Test
    fun problemReferenceToPallette() {
        compareH5andNclib(testData + "netchdf/esben/level2_MSG2_8bit_VISIR_STD_20091005_0700.H5")
        compareDataWithClib(testData + "netchdf/esben/level2_MSG2_8bit_VISIR_STD_20091005_0700.H5")
    }

    @Test
    fun problem2() {
        compareH5andNclib(testData + "netchdf/rink/I3A_VHR_22NOV2007_0902_L1B_STD.h5")
        compareDataWithClib(testData + "netchdf/rink/I3A_VHR_22NOV2007_0902_L1B_STD.h5")
    }

    @Test
    fun problem3() {
        compareH5andNclib(testData + "netchdf/austin/H12007_1m_MLLW_1of6.bag")
        compareDataWithClib(testData + "netchdf/austin/H12007_1m_MLLW_1of6.bag")
    }

    @Test
    fun ok() {
        compareH5andNclib(testData + "netchdf/tomas/S3A_OL_CCDB_CHAR_AllFiles.20101019121929_1.nc4")
        compareDataWithClib(testData + "netchdf/tomas/S3A_OL_CCDB_CHAR_AllFiles.20101019121929_1.nc4")
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
        Hdf5File(filename, true).use { h5file ->
            println("${h5file.type()} $filename ")
            println("\n${h5file.cdl()}")

            NClibFile(filename).use { nclibfile ->
                println("ncfile = ${nclibfile.cdl()}")
                assertEquals(nclibfile.cdl(), h5file.cdl())
            }
        }
    }

}