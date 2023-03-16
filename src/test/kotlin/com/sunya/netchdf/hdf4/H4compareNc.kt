package com.sunya.netchdf.hdf4

import com.sunya.cdm.api.Section
import com.sunya.netchdf.hdf4Clib.Hdf4ClibFile
import com.sunya.netchdf.readDataCompareHC
import com.sunya.netchdf.readMyData
import com.sunya.netchdf.readDataCompareNC
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import test.util.testFilesIn
import java.util.*
import java.util.stream.Stream
import kotlin.test.assertEquals

class H4compareNc {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            val sdsNotEos = Stream.of(
                Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdf4/balloon_sonde.o3_knmi000_de.bilt_s2_20060905t112100z_002.hdf"),
                Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdf4/MAC07S0.A2008230.1250.002.2008233222357.hdf"),
                Arguments.of("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/hdf4/c402_rp_02.diag.sfc.20020122_0130z.hdf"),
                Arguments.of("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/hdf4/MI1B2T_B54_O003734_AN_05.hdf"),
            )

            val hdf4 =
                testFilesIn("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdf4")
                    .withRecursion()
                    .build()

            val hdfeos2 =
                testFilesIn("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdfeos2")
                    .withRecursion()
                    .addNameFilter { name -> !name.endsWith("MISR_AM1_GP_GMP_P040_O003734_05.eos") } // corrupted ??
                    .build()

            val moar4 =
                testFilesIn("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/hdf4")
                    .withRecursion()
                    .withPathFilter { p -> !(p.toString().contains("/eos/"))}
                    .addNameFilter { name -> !name.endsWith("MOD021KM.A2004328.1735.004.2004329164007.hdf") } // corrupted ??
                    .addNameFilter { name -> !name.endsWith("MYD021KM.A2008349.1800.005.2009329084841.hdf") } // corrupted ??
                    .addNameFilter { name -> !name.endsWith("MOD02HKM.A2007016.0245.005.2007312120020.hdf") } // corrupted ??
                    .addNameFilter { name -> !name.endsWith("MOD02OBC.A2007001.0005.005.2007307210540.hdf") } // corrupted ??
                    .addNameFilter { name -> !name.endsWith("MOD021KM.A2001149.1030.003.2001154234131.hdf") } // corrupted ??
                    .build()

            val moarEos =
                testFilesIn("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/hdf4/eos")
                    .withRecursion()
                    .addNameFilter { name -> !name.endsWith("MOD021KM.A2004328.1735.004.2004329164007.hdf") } // corrupted ??
                    .addNameFilter { name -> !name.endsWith("MYD021KM.A2008349.1800.005.2009329084841.hdf") } // corrupted ??
                    .build()

            val moar42 =
                testFilesIn("/media/twobee/netch/hdf4")
                    .withRecursion()
                    .addNameFilter { name -> !name.endsWith("sst.coralreef.fields.50km.n14.20010106.hdf") }
                    .addNameFilter { name -> !name.endsWith("VHRR-KALPANA_20081216_070002.hdf") }
                    .addNameFilter { name -> !name.endsWith("MODSCW_P2009168_C4_1805_1810_1940_1945_GM03_closest_chlora.hdf") }
                    .addNameFilter { name -> !name.endsWith(".ncml") }
                    .addNameFilter { name -> !name.endsWith(".xml") }
                    .addNameFilter { name -> !name.endsWith(".pdf") }
                    .build()

            // return Stream.of(sdsNotEos, hdf4).flatMap { i -> i}
            return Stream.of(sdsNotEos, hdf4, moar4, moar42).flatMap { i -> i}
        }
    }

    @Test
    fun hasStruct() {
        compareH4header("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdf4/17766010.hdf")
 //           "Sea_Ice_Motion_Vectors_-_17766010")
    }

    @Test
    fun swath() {
        readH4header("/media/twobee/netch/hdf4/jeffmc/swath.hdf")
    }

    @Test
    fun problem1() { // HC has atts n > 1
        compareH4header("/media/twobee/netch/hdf4/AST_L1B_00307182004110047_08122004112525.hdf")
    }

    @Test
    fun problem2() { // H4 has atts n > 1
        compareH4header("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/hdf4/ssec/2006166131201_00702_CS_2B-GEOPROF_GRANULE_P_R03_E00.hdf")
    }
    @Test
    fun problemHC() {
        readHCheader("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/hdf4/eos/misr/MISR_AM1_AGP_P040_F01_24.subset.eos")
    }
    @Test
    fun problem3C() {
        readHCheader("/media/twobee/netch/hdf4/chlora/MODSCW_P2009168_C4_1805_1810_1940_1945_GM03_closest_chlora.hdf") // malloc(): invalid size (unsorted)
    }

    @Test
    fun smallProblem() {
        compareH4header("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/hdf4/ncidc/AMSR_E_L2_Land_T06_200801012345_A.hdf")
    }

    @Test
    fun smallProblem2() {
        compareH4header("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/hdf4/MI1B2T_B55_O003734_AN_05.hdf")
    }

    @Test
    fun eos2() {
        compareH4header("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdfeos2/MISR_AM1_GP_GMP_P040_O003734_05.eos")
    }

    // compress_type = 0
    @Test
    fun compressProblem() {
        readDataCompareNC("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/hdf4/MYD021KM.A2008349.1800.005.2009329084841.hdf")
    }

    @Test
    fun linkedNotCompressed() {
        readDataCompareNC("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/hdf4/c402_rp_02.diag.sfc.20020122_0130z.hdf",
            "ALBEDO")
    }

    @Test
    fun chunkedCompressed() {
        // readH4header("/media/twobee/netch/hdf4/chlora/MODSCW_P2009168_C4_1805_1810_1940_1945_GM03_closest_chlora.hdf")
        readDataCompareHC("/media/twobee/netch/hdf4/chlora/MODSCW_P2009168_C4_1805_1810_1940_1945_GM03_closest_chlora.hdf",
            "chlor_a", Section("0:1533,0:1000"))
    }

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
    fun readHCheader(filename : String) {
        println("=================")
        println(filename)
        Hdf4ClibFile(filename).use { myfile ->
            println(" Hdf4ClibFile = \n${myfile.cdl()}")
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    fun readData(filename: String) {
        readMyData(filename)
        println()
    }

    @ParameterizedTest
    @MethodSource("params")
    fun readDataCompareWithNC(filename: String) {
        readDataCompareNC(filename)
        println()
    }


    @ParameterizedTest
    @MethodSource("params")
    fun readDataCompareWithHC(filename: String) {
        readDataCompareHC(filename)
        println()
    }

    @Test
    fun problemHeader() {
        compareH4header("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdf4/MAC07S0.A2008230.1250.002.2008233222357.hdf")
    }

    @Test
    fun groups() {
        compareH4header("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/hdf4/ncidc/AMSR_E_L3_DailyLand_B04_20080101.hdf")
    }

    // The netCDF-4 library can read HDF4 data files, if they were created with the SD (Scientific Data) API.
    // https://docs.unidata.ucar.edu/nug/current/getting_and_building_netcdf.html#build_hdf4
    @ParameterizedTest
    @MethodSource("params")
    fun compareH4header(filename : String) {
        println("=================")
        println(filename)
        Hdf4File(filename, true).use { myfile ->
            println("Hdf4File = \n${myfile.cdl()}")
            Hdf4ClibFile(filename).use { ncfile ->
                //println("actual = $root")
                //println("expect = $expect")
                assertEquals(ncfile.cdl(), myfile.cdl())
                println(myfile.cdl())
            }
        }
    }

}