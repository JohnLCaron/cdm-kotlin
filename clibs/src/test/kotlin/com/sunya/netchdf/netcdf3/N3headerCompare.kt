package com.sunya.netchdf.netcdf3

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import com.sunya.netchdf.netcdfClib.NetcdfClibFile
import com.sunya.testdata.N3Files
import java.util.*
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Compare header using cdl(strict) with Netcdf3File and NetcdfClibFile
class N3headerCompare {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            return N3Files.params()
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    fun checkVersion(filename: String) {
        Netcdf3File(filename).use { ncfile ->
            println("${ncfile.type()} $filename ")
            assertTrue((ncfile.type().contains("netcdf3")))
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    fun readN3header(filename : String) {
        println(filename)
        Netcdf3File(filename).use { n3file ->
            NetcdfClibFile(filename).use { ncfile ->
                //println("actual = $root")
                //println("expect = $expect")
                assertEquals(ncfile.cdl(), n3file.cdl())
                // println(rootClib.cdlString())
            }
        }
    }

}