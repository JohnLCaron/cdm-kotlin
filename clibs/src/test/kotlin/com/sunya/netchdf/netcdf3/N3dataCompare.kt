package com.sunya.netchdf.netcdf3

import com.sunya.netchdf.compareNetcdfData
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import com.sunya.netchdf.netcdfClib.NetcdfClibFile
import com.sunya.testdata.N3Files
import com.sunya.testdata.testData
import java.util.*
import java.util.stream.Stream

// Compare data reading for the same file with Netcdf3File and NetcdfClibFile
class N3dataCompare {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            return N3Files.params()
        }
    }

    @Test
    fun awips() {
        readDataCompareNC(testData + "cdmUnitTest/formats/netcdf3/awips.nc", "uw")
    }

    @ParameterizedTest
    @MethodSource("params")
    fun readN3dataCompareNC(filename : String) {
        readDataCompareNC(filename, null)
    }

    fun readDataCompareNC(filename : String, varname : String?) {
        val myfile = Netcdf3File(filename)
        val ncfile = NetcdfClibFile(filename)
        println(filename)
        println(myfile.cdl())
        compareNetcdfData(myfile, ncfile, varname)
        myfile.close()
        ncfile.close()
    }

}