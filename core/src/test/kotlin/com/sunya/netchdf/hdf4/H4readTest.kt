package com.sunya.netchdf.hdf4

import com.sunya.cdm.util.Stats
import com.sunya.netchdf.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.*
import java.util.stream.Stream

class H4readTest {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            val starter = Stream.of(
                Arguments.of(testData + "cdmUnitTest/formats/hdf4/MI1B2T_B54_O003734_AN_05.hdf"), // sds
                Arguments.of(testData + "devcdm/hdf4/TOVS_BROWSE_MONTHLY_AM_B861001.E861031_NF.HDF"), // RIG
                Arguments.of(testData + "devcdm/hdf4/17766010.hdf"), // VH struct
                Arguments.of(testData + "cdmUnitTest/formats/hdf4/f13_owsa_04010_09A.hdf"),
            )

            val hasGroups = Stream.of(
                Arguments.of(testData + "netchdf/hdf4/jeffmc/swath.hdf"),
                Arguments.of(testData + "cdmUnitTest/formats/hdf4/ncidc/AIRS.2002.09.01.L3.RetQuant_H030.v5.0.14.0.G07191213218.hdf"),
                Arguments.of(testData + "cdmUnitTest/formats/hdf4/ncidc/AMSR_E_L2_Land_T06_200801012345_A.hdf"),
                Arguments.of(testData + "cdmUnitTest/formats/hdf4/ncidc/MOD10A1.A2008001.h23v15.005.2008003161138.hdf"),
            )

            val sdsNotEos = Stream.of(
                Arguments.of(testData + "devcdm/hdf4/balloon_sonde.o3_knmi000_de.bilt_s2_20060905t112100z_002.hdf"),
                Arguments.of(testData + "devcdm/hdf4/MAC07S0.A2008230.1250.002.2008233222357.hdf"),
                Arguments.of(testData + "cdmUnitTest/formats/hdf4/c402_rp_02.diag.sfc.20020122_0130z.hdf"),
                Arguments.of(testData + "cdmUnitTest/formats/hdf4/MI1B2T_B54_O003734_AN_05.hdf"),
            )

            val hdf4 =
                testFilesIn(testData + "devcdm/hdf4")
                    .withRecursion()
                    .build()

            val hdfeos2 =
                testFilesIn(testData + "devcdm/hdfeos2")
                    .withRecursion()
                    .addNameFilter { name -> !name.endsWith("MISR_AM1_GP_GMP_P040_O003734_05.eos") } // corrupted ??
                    .build()

            val moar4 =
                testFilesIn(testData + "cdmUnitTest/formats/hdf4")
                    .withRecursion()
                    .withPathFilter { p -> !(p.toString().contains("/eos/"))}
                    .addNameFilter { name -> !name.endsWith("2006166131201_00702_CS_2B-GEOPROF_GRANULE_P_R03_E00.hdf") } // reported bug in H4Clib
                    .addNameFilter { name -> !name.endsWith("MOD021KM.A2004328.1735.004.2004329164007.hdf") } // corrupted ??
                    .addNameFilter { name -> !name.endsWith("MYD021KM.A2008349.1800.005.2009329084841.hdf") } // corrupted ??
                    .addNameFilter { name -> !name.endsWith("MOD02HKM.A2007016.0245.005.2007312120020.hdf") } // corrupted ??
                    .addNameFilter { name -> !name.endsWith("MOD02OBC.A2007001.0005.005.2007307210540.hdf") } // corrupted ??
                    .addNameFilter { name -> !name.endsWith("MOD021KM.A2001149.1030.003.2001154234131.hdf") } // corrupted ??
                    .build()

            val moarEos =
                testFilesIn(testData + "cdmUnitTest/formats/hdf4/eos")
                    .withRecursion()
                    .addNameFilter { name -> !name.endsWith("MOD021KM.A2004328.1735.004.2004329164007.hdf") } // corrupted ??
                    .addNameFilter { name -> !name.endsWith("MYD021KM.A2008349.1800.005.2009329084841.hdf") } // corrupted ??
                    .build()

            val moar42 =
                testFilesIn(testData + "netchdf/hdf4")
                    .withRecursion()
                    .addNameFilter { name -> !name.endsWith("sst.coralreef.fields.50km.n14.20010106.hdf") }
                    .addNameFilter { name -> !name.endsWith("VHRR-KALPANA_20081216_070002.hdf") }
                    .addNameFilter { name -> !name.endsWith("closest_chlora.hdf") }
                    .addNameFilter { name -> !name.endsWith(".ncml") }
                    .addNameFilter { name -> !name.endsWith(".xml") }
                    .addNameFilter { name -> !name.endsWith(".pdf") }
                    .build()

            // return Stream.of(starter, hasGroups, sdsNotEos).flatMap { i -> i}
            return Stream.of(starter, hasGroups, sdsNotEos, hdf4, moar4, moar42).flatMap { i -> i}
        }


        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            Stats.clear() // problem with concurrent tests
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            Stats.show()
        }
    }

    @Test
    fun unsolved1() { // H4 has atts n > 1
        readH4header(testData + "cdmUnitTest/formats/hdf4/ssec/2006166131201_00702_CS_2B-GEOPROF_GRANULE_P_R03_E00.hdf")
    }

    @Test
    fun chunkIterator() { // H4 has atts n > 1
        testReadIterate(testData + "cdmUnitTest/formats/hdf4/ssec/AIRS.2005.08.28.103.L1B.AIRS_Rad.v4.0.9.0.G05241172839.hdf")
    }

    //home/all/testdata/cdmUnitTest/formats/hdf4/ssec/AIRS.2005.08.28.103.L1B.AIRS_Rad.v4.0.9.0.G05241172839.hdf, radiances
    //home/all/testdata/cdmUnitTest/formats/hdf4/MYD29.A2009152.0000.005.2009153124331.hdf, Sea_Ice_by_Reflectance

    //////////////////////////////////////////////////////////////////////

    @ParameterizedTest
    @MethodSource("params")
    fun readH4header(filename : String) {
        println("=================")
        println(filename)
        Hdf4File(filename).use { myfile ->
            println(" Hdf4File = \n${myfile.cdl()}")
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    fun readData(filename: String) {
        readNetchdfData(filename, null, null, true)
        println()
    }

    @ParameterizedTest
    @MethodSource("params")
    fun testReadIterate(filename: String) {
        readNetchIterate(filename, null)
    }

}