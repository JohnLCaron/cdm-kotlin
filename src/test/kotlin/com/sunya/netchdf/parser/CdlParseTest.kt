package com.sunya.netchdf.parser

import com.github.h0tk3y.betterParse.grammar.parseToEnd
import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.Parsed
import com.github.h0tk3y.betterParse.st.SyntaxTree
import com.github.h0tk3y.betterParse.st.liftToSyntaxTreeGrammar
import com.sunya.cdm.api.Netchdf
import com.sunya.netchdf.netcdf4.normalize
import java.util.*
import java.util.stream.Stream
import kotlin.test.assertEquals

import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import test.util.testData

class CdlParseTest {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> = Stream.of(
            Arguments.of(
                testData + "devcdm/netcdf3/longOffset.nc",
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
                testData + "devcdm/netcdf4/tst_dims.nc",
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
                testData + "devcdm/netcdf3/byteArrayRecordVarPaddingTest-bad.nc",
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
                testData + "devcdm/netcdf3/tst_v2.nc",
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
                testData + "devcdm/netcdsf3/tst_pres_temp_4D_classic.nc",
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

    // @ParameterizedTest
    @MethodSource("params")
    fun testCdlParser(filename: String, cdl: String) {
        println("$cdl")
        val netcdf = CdlParser.parseToEnd(cdl)
        println("${netcdf.cdl()}")
        assertEquals(normalize(cdl), normalize(netcdf.cdl()))

        val cdlGrammer = CdlParser.liftToSyntaxTreeGrammar()
        when (val parseResult = cdlGrammer.tryParseToEnd(cdl)) {
            is ErrorResult -> println("Could not parse expression: $parseResult")
            is Parsed<SyntaxTree<Netchdf>> -> printCdlSyntaxTree(cdl, parseResult.value)
        }

       //val syntaxTree = cdlSyntaxTree.parseToEnd(expect)
      // println("\nresult = ${result.cdl(true)}")
    }

}