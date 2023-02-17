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
import java.util.*
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.regex.Matcher

class CdlParseTest {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf3/longOffset.nc",
                """
netcdf longOffset {
dimensions:
    x = 6 ;
    y = 12 ;
  variables:
    int data(x, y) ;
}
"""
            ),

            Arguments.of(
                "/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/tst_dims.nc",
                """
netcdf tst_dims {
dimensions:
	latitude = 6 ;
	longitude = 12 ;
variables:
	float longitude(longitude) ;
}
"""
            ),
            Arguments.of(
                "/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf3/byteArrayRecordVarPaddingTest-bad.nc",
                """
netcdf byteArrayRecordVarPaddingTest {
dimensions:
	X = 5 ;
	D = UNLIMITED ; // (18 currently)
variables:
	double X(X) ;
	byte V(D) ;
}
"""
            ),
            Arguments.of(
                "/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf3/tst_v2.nc",
"""
netcdf tst_v2 {
dimensions:
	rise_height = 3 ;
	saltiness = 5 ;
variables:
	double wheat_loaf(rise_height, saltiness) ;
	int sourdough_wheat_loaf(rise_height, saltiness) ;
	short white_loaf(rise_height, saltiness) ;
}
    """
        ),
            Arguments.of(
                "/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdsf3/tst_pres_temp_4D_classic.nc",
                """
netcdf tst_pres_temp_4D_classic {
dimensions:
    latitude = 7 ;
variables:
	float latitude(latitude) ;
        latitude:name = "what" ;
	float longitude(latitude) ;
}
"""
            ),
        )
    }

    @ParameterizedTest
    @MethodSource("params")
    fun testCdlParser(filename: String, cdl: String) {
        println("$cdl")
        val result = CdlParser.parseToEnd(cdl)
        println("${result.cdl(true)}")
        assertEquals(normalize(cdl), normalize(result.cdl(true)))

        val cdlGrammer = CdlParser.liftToSyntaxTreeGrammar()
        when (val result = cdlGrammer.tryParseToEnd(cdl)) {
            is ErrorResult -> println("Could not parse expression: $result")
            is Parsed<SyntaxTree<Netcdf>> -> printSyntaxTree(cdl, result.value)
        }

       //val syntaxTree = cdlSyntaxTree.parseToEnd(expect)
      // println("\nresult = ${result.cdl(true)}")
    }

}