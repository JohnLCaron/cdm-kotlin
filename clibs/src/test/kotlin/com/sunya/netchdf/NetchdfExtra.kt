package com.sunya.netchdf

import com.sunya.cdm.api.*
import com.sunya.netchdf.netcdf4.openNetchdfFile
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import test.util.testData
import test.util.testFilesIn
import java.util.*
import java.util.stream.Stream

// Compare header using cdl(!strict) with Netchdf and NetcdfClibFile
// mostly fails in handling of types. nclib doesnt pass over all the types.
class NetchdfExtra {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            val stream =
                testFilesIn(testData + "netchdf")
                    .withRecursion()
                    // exclude hdf4 until we fix the core dump in the C library
                    .withPathFilter { p -> !(p.toString().contains("hdf4") or p.toString().contains("exclude") or
                            p.toString().contains("gilmore/data.nc") or p.toString().contains("barrodale/test.h5") or
                            p.toString().contains("barrodale/test.h5") )}
                    .addNameFilter { name -> !name.contains("_npp_") }          // disagree with netcdf4 library - need direct hdf5 c library ??
                    .addNameFilter { name -> !name.endsWith("level2_MSG2_8bit_VISIR_STD_20091005_0700.H5") } // ditto
                    .addNameFilter { name -> !name.endsWith("I3A_VHR_22NOV2007_0902_L1B_STD.h5") }          // ditto
                    .addNameFilter { name -> !name.endsWith("H12007_1m_MLLW_1of6.bag") }                    // ditto
                    .addNameFilter { name -> !name.endsWith("S3A_OL_CCDB_CHAR_AllFiles.20101019121929_1.nc4") } // ditto
                    .addNameFilter { name -> !name.endsWith("hdf") } // core dump
                    .addNameFilter { name -> !name.endsWith(".cdl") }
                    .addNameFilter { name -> !name.endsWith(".jpg") }
                    .addNameFilter { name -> !name.endsWith(".gif") }
                    .addNameFilter { name -> !name.endsWith(".ncml") }
                    .addNameFilter { name -> !name.endsWith(".png") }
                    .addNameFilter { name -> !name.endsWith(".pdf") }
                    .addNameFilter { name -> !name.endsWith(".tif") }
                    .addNameFilter { name -> !name.endsWith(".tiff") }
                    .addNameFilter { name -> !name.endsWith(".txt") }
                    .addNameFilter { name -> !name.endsWith(".xml") }
                    .build()

            // return moar3
            return Stream.of(stream).flatMap { i -> i }
        }

        const val topdir = testData + "netchdf/"
    }

    // npp filers: superblock at file offset; reference data type
     //*** not a netchdf file = /media/twobee/netch/webb/GATRO-SATMR_npp_d20020906_t0409572_e0410270_b19646_c20090720223122943227_devl_int.h5
     //*** not a netchdf file = /media/twobee/netch/jasmine/VCBHO_npp_d20030125_t084955_e085121_b00015_c20071213022754_den_OPS_SEG.h5
     //*** not a netchdf file = /media/twobee/netch/jasmine/VCBHO_npp_d20030125_t084830_e084955_b00015_c20071213022754_den_OPS_SEG.h5
     //*** not a netchdf file = /media/twobee/netch/jasmine/SVM03_npp_d20111213_t1233213_e1234454_b00654_c20111213204856240464_noaa_ops.h5
     //*** not a netchdf file = /media/twobee/netch/jasmine/GCLDO_npp_d20030125_t084955_e085121_b00015_c20071213022754_den_OPS_SEG.h5
     //*** not a netchdf file = /media/twobee/netch/jasmine/GCLDO_npp_d20030125_t084830_e084955_b00015_c20071213022754_den_OPS_SEG.h5
     //*** not a netchdf file = /media/twobee/netch/jasmine/GMTCO_npp_d20111213_t1233213_e1234454_b00654_c20111213204123306064_noaa_ops.h5
    @Test
    fun h5npp() {
        NetchdfTest.showData = false
        readNetchdfData(topdir + "npp/VCBHO_npp_d20030125_t084955_e085121_b00015_c20071213022754_den_OPS_SEG.h5")
        readNetchdfData(topdir + "npp/GATRO-SATMR_npp_d20020906_t0409572_e0410270_b19646_c20090720223122943227_devl_int.h5")
            // , "/Data_Products/ATMS-REMAP-SDR/ATMS-REMAP-SDR_Aggr")
    }

    /*
    I wonder if npp has a group cycle?
    nc_inq_natts return -107 = NetCDF: Can't open HDF5 attribute g4.grpid= 65540
    then on close:
    HDF5: infinite loop closing library
      L,D_top,G_top,T_top,F,P,P,FD,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E,E
     */
    @Test
    @Disabled
    fun problemNPP() {
        compareCdlWithClib(testData + "netchdf/npp/VCBHO_npp_d20030125_t084955_e085121_b00015_c20071213022754_den_OPS_SEG.h5")
    }

    // this one we could probably fix
    @Test
    @Disabled
    fun unsolved2() {
        val filename = testData + "netchdf/tomas/S3A_OL_CCDB_CHAR_AllFiles.20101019121929_1.nc4"
        // showMyHeader(filename)
        showNcHeader(filename)
        // showMyData(filename)
        compareCdlWithClib(filename)
        //readDataCompareNC(filename)
    }

    ///////////////////////////////////////////////////////
    @ParameterizedTest
    @MethodSource("params")
    fun checkVersion(filename: String) {
        openNetchdfFile(filename).use { ncfile ->
            if (ncfile == null) {
                println("Not a netchdf file=$filename ")
                return
            }
            println("${ncfile.type()} $filename ")
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    fun testCompareCdlWithClib(filename: String) {
        if (filename.contains("/npp/")) {
            println("Clib cant open npp $filename")
            // return
        }
        compareCdlWithClib(filename)
    }

    @ParameterizedTest
    @MethodSource("params")
    fun readDataForProfiling(filename: String) {
        readNetchdfData(filename, null)
    }

}