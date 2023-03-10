package com.sunya.netchdf.cdl

import com.github.h0tk3y.betterParse.grammar.parseToEnd
import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.lexer.RegexToken
import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.Parsed
import com.github.h0tk3y.betterParse.st.SyntaxTree
import com.github.h0tk3y.betterParse.st.liftToSyntaxTreeGrammar
import com.sunya.cdm.api.Netcdf
import com.sunya.netchdf.netcdf4.normalize
import com.sunya.netchdf.netcdf4.openNetchdfFile
import java.util.*
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.regex.Matcher

class CdlReadAndParseTest {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> = Stream.of(
            // Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf3/longOffset.nc"),
            Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/tst_dims.nc"),
            // LOOK Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf3/testWriteFill.nc"),
            Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf3/tst_v2.nc"),
            Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf3/tst_pres_temp_4D_classic.nc"),
        )
    }

    @ParameterizedTest
    @MethodSource("params")
    fun testCdlParser(filename: String) {
        println("=================")
        println(filename)
        val netchdf : Netcdf? = openNetchdfFile(filename)
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