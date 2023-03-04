package com.sunya.netchdf

import com.sunya.cdm.api.*
import com.sunya.cdm.api.Section.Companion.computeSize
import com.sunya.cdm.iosp.Iosp
import com.sunya.netchdf.netcdf4.openNetchdfFile
import com.sunya.netchdf.netcdfClib.NetcdfClibFile
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import test.util.testFilesIn
import java.util.*
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Compare header using cdl(!strict) with Netchdf and NetcdfClibFile
class NetchdfExtra {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            val stream =
                testFilesIn("/media/twobee/netch")
                    .withRecursion()
                    .withPathFilter { p -> !p.toString().contains("hdf4") }
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
            return Stream.of(stream).flatMap { i -> i };
        }

        const val topdir = "/media/twobee/netch/"
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
        readData(topdir + "jasmine/VCBHO_npp_d20030125_t084955_e085121_b00015_c20071213022754_den_OPS_SEG.h5")
//)
        readData(topdir + "webb/GATRO-SATMR_npp_d20020906_t0409572_e0410270_b19646_c20090720223122943227_devl_int.h5")
            // , "/Data_Products/ATMS-REMAP-SDR/ATMS-REMAP-SDR_Aggr")
    }

    val showCdl= false

    //@ParameterizedTest
    @MethodSource("params")
    fun compareCdlWithClib(filename: String) {
        val netchdf: Netcdf? = openNetchdfFile(filename)
        if (netchdf == null) {
            println("*** not a netchdf file = $filename")
            return
        }
        println("filename $filename type ${netchdf.type()}")
        if (showCdl) println("\nnetchdf = ${netchdf.cdl()}")
        val nclibfile: Netcdf = NetcdfClibFile(filename)
        assertEquals(nclibfile.cdl(), netchdf.cdl())

        netchdf.close()
        nclibfile.close()
    }

    //@ParameterizedTest
    @MethodSource("params")
    fun readDataForProfiling(filename: String) {
        println(filename)
        readData(filename)
        println()
    }
}