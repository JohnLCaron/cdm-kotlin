package com.sunya.netchdf.parser

import com.github.h0tk3y.betterParse.grammar.parseToEnd
import com.sunya.cdm.api.Netchdf
import com.sunya.netchdf.netcdf4.normalize
import com.sunya.netchdf.netcdf4.openNetchdfFile
import java.util.*
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import test.util.testData

class CdlReadAndParseTest {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> = Stream.of(
            // Arguments.of(testData + "devcdm/netcdf3/longOffset.nc"),
            Arguments.of(testData + "devcdm/netcdf4/tst_dims.nc"),
            // LOOK Arguments.of(testData + "devcdm/netcdf3/testWriteFill.nc"),
            Arguments.of(testData + "devcdm/netcdf3/tst_v2.nc"),
            Arguments.of(testData + "devcdm/netcdf3/tst_pres_temp_4D_classic.nc"),
        )
    }

    // @ParameterizedTest
    @MethodSource("params")
    fun testCdlParser(filename: String) {
        println("=================")
        println(filename)
        val netchdf : Netchdf? = openNetchdfFile(filename)
        if (netchdf == null) {
            println("*** not a netchdf file = $filename")
            return
        }

        val cdl = netchdf.cdl()
        println("org = $cdl")
        val netcdfCdl = CdlParser.parseToEnd(cdl)
        println("parse = ${netcdfCdl.cdl()}")
        assertEquals(normalize(cdl), normalize(netcdfCdl.cdl()))
        assertTrue(netchdf.rootGroup() == netcdfCdl.rootGroup())
        assertTrue(netchdf.rootGroup().hashCode() == netcdfCdl.rootGroup().hashCode())
    }

}