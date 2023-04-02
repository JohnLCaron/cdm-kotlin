package com.sunya.netchdf

import com.sunya.cdm.api.*
import com.sunya.cdm.util.Stats
import com.sunya.netchdf.netcdf4.openNetchdfFile
import com.sunya.testdata.NetchdfExtraFiles
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import com.sunya.testdata.testData
import com.sunya.testdata.testFilesIn
import org.junit.jupiter.api.AfterAll
import java.util.*
import java.util.stream.Stream

// Compare header using cdl(!strict) with Netchdf and NetcdfClibFile
// mostly fails in handling of types. nclib doesnt pass over all the types.
class NetchdfExtra {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            return NetchdfExtraFiles.params(false)
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            if (versions.size > 0) {
                versions.keys.forEach{ println("$it = ${versions[it]!!.size } files") }
            }
            Stats.show()
        }

        private val versions = mutableMapOf<String, MutableList<String>>()

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
            val paths = versions.getOrPut(ncfile.type()) { mutableListOf() }
            paths.add(filename)
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    fun readNetchdfData(filename: String) {
        readNetchdfData(filename, null)
    }

}