package com.sunya.testdata

import org.junit.jupiter.params.provider.Arguments
import java.util.stream.Stream

class N4Files {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            val stream4 =
                testFilesIn(testData + "devcdm/netcdf4")
                    .addNameFilter { name -> !name.endsWith("tst_grps.nc4") } // nested group typedefs
                    .build()

            val moar4 =
                testFilesIn(testData + "cdmUnitTest/formats/netcdf4")
                    .addNameFilter { name -> !name.endsWith("compound-attribute-test.nc") } // bug in clib
                    .withRecursion()
                    .build()

            return Stream.of(stream4, moar4).flatMap { i -> i }
        }
    }
}