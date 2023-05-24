package com.sunya.testdata

import org.junit.jupiter.params.provider.Arguments
import java.util.stream.Stream

class NetchdfExtraFiles {

    companion object {
        @JvmStatic
        fun params(excludeClibFails: Boolean): Stream<Arguments> {
            val builder = testFilesIn(testData + "netchdf")
                .withRecursion()
                .withPathFilter { p ->
                    !(p.toString().contains("exclude") or
                            p.toString().contains("gilmore/data.nc") or
                            p.toString().contains("barrodale/test.h5"))
                }
                .addNameFilter { name -> !name.endsWith(".cdl") }
                .addNameFilter { name -> !name.endsWith(".jpg") }
                .addNameFilter { name -> !name.endsWith(".gif") }
                .addNameFilter { name -> !name.endsWith(".ncml") }
                .addNameFilter { name -> !name.endsWith(".png") }
                .addNameFilter { name -> !name.endsWith(".pdf") }
                .addNameFilter { name -> !name.endsWith(".tif") }
                .addNameFilter { name -> !name.endsWith(".tiff") }
                .addNameFilter { name -> !name.endsWith(".txt") }
                .addNameFilter { name -> !name.endsWith(".xml") }

            /*
            /home/all/testdata/netchdf/esben/level2_MSG2_8bit_VISIR_STD_20091005_0700.H5
            /home/all/testdata/netchdf/rink/I3A_VHR_22NOV2007_0902_L1B_STD.h5
            /home/all/testdata/netchdf/austin/H12007_1m_MLLW_1of6.bag
            /home/all/testdata/netchdf/tomas/S3A_OL_CCDB_CHAR_AllFiles.20101019121929_1.nc4
             */
            if (excludeClibFails) {
                builder.addNameFilter { name -> !name.lowercase().contains("_npp_") }          // disagree with C library
                    .addNameFilter { name -> !name.endsWith("level2_MSG2_8bit_VISIR_STD_20091005_0700.H5") } // ditto
                    .addNameFilter { name -> !name.endsWith("I3A_VHR_22NOV2007_0902_L1B_STD.h5") }          // ditto
                    .addNameFilter { name -> !name.endsWith("H12007_1m_MLLW_1of6.bag") }                    // ditto
                    // .addNameFilter { name -> !name.endsWith("S3A_OL_CCDB_CHAR_AllFiles.20101019121929_1.nc4") } // ditto
            }

            return builder.build()
        }
    }
}