package com.sunya.testdata

import org.junit.jupiter.params.provider.Arguments
import java.util.stream.Stream

class N3Files {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            val stream3 =
                testFilesIn(testData + "devcdm/netcdf3")
                    .build()

            val moar3 =
                testFilesIn(testData + "cdmUnitTest/formats/netcdf3")
                    .withPathFilter { p -> !p.toString().contains("exclude") }
                    .addNameFilter { name -> !name.endsWith("perverse.nc") } // too slow
                    .withRecursion()
                    .build()

            return Stream.of(stream3, moar3).flatMap { i -> i };
        }
    }
}