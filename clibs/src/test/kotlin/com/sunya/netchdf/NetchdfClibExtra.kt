package com.sunya.netchdf

import com.sunya.cdm.api.*
import com.sunya.cdm.util.Stats
import com.sunya.testdata.NetchdfExtraFiles
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import com.sunya.testdata.testData
import org.junit.jupiter.api.AfterAll
import java.util.*
import java.util.stream.Stream

// Compare header using cdl(!strict) with Netchdf and NetcdfClibFile
// mostly fails in handling of types. nclib doesnt pass over all the types.
class NetchdfClibExtra {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            return NetchdfExtraFiles.params(true)
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
            val paths = versions.getOrPut(ncfile.type()) { mutableListOf() }
            paths.add(filename)
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    fun testCompareCdlWithClib(filename: String) {
        compareCdlWithClib(filename)
    }

    @ParameterizedTest
    @MethodSource("params")
    fun readNetchdfData(filename: String) {
        readNetchdfData(filename, null)
    }


    @ParameterizedTest
    @MethodSource("params")
    fun testCompareDataWithClib(filename: String) {
        compareDataWithClib(filename)
    }

}