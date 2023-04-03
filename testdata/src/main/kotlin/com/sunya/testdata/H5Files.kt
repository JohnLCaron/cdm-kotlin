package com.sunya.testdata

import org.junit.jupiter.params.provider.Arguments
import java.util.stream.Stream

class H5Files {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            val devcdm =
                testFilesIn(testData + "devcdm/hdf5")
                    .withRecursion()
                    .build()

            val cdmUnitTest =
                testFilesIn(testData + "cdmUnitTest/formats/hdf5")
                    .withPathFilter { p -> !p.toString().contains("exclude") and !p.toString().contains("problem") }
                    .addNameFilter { name -> !name.endsWith("groupHasCycle.h5") } // /home/all/testdata/cdmUnitTest/formats/hdf5/groupHasCycle.h5
                    .addNameFilter { name -> !name.endsWith(".xml") }
                    .withRecursion()
                    .build()

            return Stream.of(devcdm, cdmUnitTest).flatMap { i -> i };
        }
    }
}