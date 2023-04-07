package com.sunya.testdata

import org.junit.jupiter.params.provider.Arguments
import java.util.stream.Stream

class H5CFiles {

    // H5Files

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            val starting = Stream.of(
                Arguments.of(testData + "cdmUnitTest/formats/hdf5/grid_1_3d_xyz_aug.h5"),
                Arguments.of(testData + "cdmUnitTest/formats/hdf5/StringsWFilter.h5"),
                Arguments.of(testData + "cdmUnitTest/formats/hdf5/msg/test.h5"),
                Arguments.of(testData + "cdmUnitTest/formats/hdf5/extLink/extlink_source.h5 "),
            )

            val cdmUnitTest =
                testFilesIn(testData + "cdmUnitTest/formats/hdf5")
                    .withPathFilter { p -> !p.toString().contains("exclude") && !p.toString().contains("problem")
                            && !p.toString().contains("npoess")}
                    .addNameFilter { name -> !name.contains("OMI-Aura") }
                    .addNameFilter { name -> !name.contains("IASI") }
                    .addNameFilter { name -> !name.endsWith("groupHasCycle.h5") } // /home/all/testdata/cdmUnitTest/formats/hdf5/groupHasCycle.h5
                    .addNameFilter { name -> !name.endsWith(".xml") }
                    .withRecursion()
                    .build()

            val devcdm =
                testFilesIn(testData + "devcdm/hdf5")
                    .withRecursion()
                    .build()

            // return devcdm
            return Stream.of(cdmUnitTest, devcdm).flatMap { i -> i };
        }
    }
}