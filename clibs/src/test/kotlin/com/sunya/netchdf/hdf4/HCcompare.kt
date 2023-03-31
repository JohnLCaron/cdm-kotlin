package com.sunya.netchdf.hdf4

import com.sunya.netchdf.*
import com.sunya.netchdf.hdf4Clib.Hdf4ClibFile
import com.sunya.netchdf.netcdf4.openNetchdfFile
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import test.util.testData
import test.util.testFilesIn
import java.util.*
import java.util.stream.Stream
import kotlin.test.assertEquals

class HCcompare {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            val hdfeos2 =
                testFilesIn(testData + "devcdm/hdfeos2")
                    .withRecursion()
                    .build()

            val devcdm = testFilesIn(testData + "devcdm/hdf4")
                .withRecursion()
                .build()

            // remove files that core dump
            val hdf4NoCore =
                testFilesIn(testData + "hdf4")
                    .withRecursion()
                //    .withPathFilter { p -> !(p.toString().contains("/eos/"))}
                    .addNameFilter { name -> !name.endsWith("VHRR-KALPANA_20081216_070002.hdf") }
                    .addNameFilter { name -> !name.endsWith("MOD01.A2007303.0325.005.2007306182401.hdf") }
                    .addNameFilter { name -> !name.endsWith("MOD02OBC.A2007001.0005.005.2007307210540.hdf") }
                    .addNameFilter { name -> !name.endsWith("MYD01.A2007001.0440.005.2007311085701.hdf") }
                    .build()

            return Stream.of(devcdm, hdfeos2, hdf4NoCore).flatMap { i -> i}
        }
    }

    @Test
    fun hasStruct() {
        readHCdata(testData + "devcdm/hdf4/17766010.hdf")
        readH4Data(testData + "devcdm/hdf4/17766010.hdf")
        NetchdfTest.showFailedData = true
        compareDataWithClib(testData + "devcdm/hdf4/17766010.hdf")
        NetchdfTest.showFailedData = false
 //           "Sea_Ice_Motion_Vectors_-_17766010")
    }


    @Test
    fun unsolved1() { // H4 has atts n > 1
        compareH4header(testData + "cdmUnitTest/formats/hdf4/ssec/2006166131201_00702_CS_2B-GEOPROF_GRANULE_P_R03_E00.hdf")
    }

    @Test
    fun unsolved2() { // variable Aerosol_Cldmask_Byproducts_Land; data fails compare
        compareDataWithClib(testData + "cdmUnitTest/formats/hdf4/ssec/MYD04_L2.A2006188.1830.005.2006194121515.hdf")
    }

    @Test
    fun problem1() { // fakeDims, HC coredump "double free or corruption (out)"
        compareH4header(testData + "hdf4/nsidc/LAADS/MOD/MOD01.A2007303.0325.005.2007306182401.hdf")
        // compareH4header(testData + "hdf4/nsidc/LAADS/MOD/MOD03.A2007001.0000.005.2007041030714.hdf")
    }

    @Test
    fun problem2() { // missing lots of tags, HC coredump
        readH4header(testData + "hdf4/eisalt/VHRR-KALPANA_20081216_070002.hdf")
    }

    @Test
    fun smallProblem2() {
        compareData(testData + "hdf4/nsidc/LP_DAAC/MOD/MOD09GA.A2007268.h10v08.005.2007272184810.hdf")
    }

    @Test
    fun dimProblem() {
        //NetchdfTest.showData = true
        //readHCdata(testData + "devcdm/hdf4/MAC07S0.A2008230.1250.002.2008233222357.hdf", "Band_number")
        //NetchdfTest.showData = false
        compareH4header(testData + "netchdf/hdf4/mak/MOD13Q1.2008.353.aust.005.b01.250m_ndvi.hdf")
    }

    @Test
    fun problemHeader() {
        val filename = testData + "hdf4/nasa/eos-grid-1d/MISR_AM1_CGAS_FIRSTLOOK_AUG_30_2007_F11_0027.hdf"
        // compareData(filename, "AlgorithmType_Enumeration")
        compareH4header(filename)
    }

    @Test
    fun problemDuplicateTypedefs() { // duplicate typedefs, look for ones that match before adding
        val filename = testData + "/hdf4/nasa/eos-grid-1d/MISR_AM1_CGAS_FIRSTLOOK_AUG_30_2007_F11_0027.hdf"
        compareH4header(filename)
    }

    //// eos
    @Test
    fun problemEos() {
        compareH4header(testData + "hdf4/eos/misr/MISR_AM1_AGP_P040_F01_24.subset")
        compareData(testData + "hdf4/eos/misr/MISR_AM1_AGP_P040_F01_24.subset") // , "/Standard/Data_Fields/StdDevSceneElevRelSlp")
    }

    // /home/all/testdata/hdf4/nsidc/LAADS/MOD/MOD08_D3.A2007001.005.2007004130642.hdf short /mod08/Data_Fields/Number_Pixels_Used_Land_Minimum []
    //home/all/testdata/hdf4/nsidc/GESC/AIRS/AIRS.2007.10.21.069.L2.RetSup.v5.0.14.0.G07295084430.hdf float /L2_Support_atmospheric&surface_product/Data_Fields/cldFreq []
    // /home/all/testdata/hdf4/nsidc/GESC/AIRS/AIRS.2003.01.24.116.L2.RetSup_H.v5.0.14.0.G07295101113.hdf float /L2_Support_atmospheric&surface_product/Data_Fields/cldFreq []
    // /home/all/testdata/hdf4/nsidc/GESC/GV/1B51.070101.1.HSTN.2.HDF // FAIL comparing data for variable = char Comment1 []
    // /home/all/testdata/hdf4/nsidc/GESC/GV/1C51.070101.1.HSTN.4.HDF // FAIL comparing data for variable = char Comment1 []

    // 117  /home/all/testdata/hdf4/nsidc/LAADS/MOD/MOD08_D3.A2007001.005.2007004130642.hdf short /mod08/Data_Fields/Number_Pixels_Used_Land_Minimum []
    @Test
    fun problemReadData() {
        val filename = testData + "hdf4/nsidc/LAADS/MOD/MOD08_D3.A2007001.005.2007004130642.hdf"
        readH4header(filename)
        compareData(filename, "/mod08/Data_Fields/Number_Pixels_Used_Land_Minimum")
    }

    // dunno; claims compressed but not linked or chunked. and only 2 bytes. HC gives error "SDreaddata return -1"
    @Test
    fun problemHCerror() {
        val filename = testData + "hdf4/nsidc/GESC/AIRS/AIRS.2003.01.24.116.L2.RetSup_H.v5.0.14.0.G07295101113.hdf"
        compareH4header(filename)
        compareData(filename)
    }

    @Test // HC core dump
    fun coreDumps() {
        readHCheader(testData + "hdf4/nsidc/LAADS/MOD/MOD01.A2007303.0325.005.2007306182401.hdf")
        //readHCheader(testData + "cdmUnitTest/formats/hdf4/eos/misr/MISR_AM1_AGP_P040_F01_24.subset.eos")
        //readHCheader(testData + "netchdf/hdf4/chlora/MODSCW_P2009168_C4_1805_1810_1940_1945_GM03_closest_chlora.hdf") // malloc(): invalid size (unsorted)
       // readH4header(testData + "cdmUnitTest/formats/hdf4/ncidc/AMSR_E_L3_DailyLand_B04_20080101.hdf")
    }

    //////////////////////////////////////////////////////////////////////

    @ParameterizedTest
    @MethodSource("params")
    fun checkVersion(filename: String) {
        openNetchdfFile(filename).use { ncfile ->
            if (ncfile == null) {
                println("Not a netchdf file=$filename ")
                return
            }
            println("${ncfile.type()} $filename ")
        }
    }

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

    // The netCDF-4 library can read HDF4 data files, if they were created with the SD (Scientific Data) API.
    // https://docs.unidata.ucar.edu/nug/current/getting_and_building_netcdf.html#build_hdf4
    @ParameterizedTest
    @MethodSource("params")
    fun compareH4header(filename : String) {
        println("=================")
        println(filename)
        Hdf4File(filename).use { myfile ->
            println("Hdf4File = \n${myfile.cdl()}")
            Hdf4ClibFile(filename).use { ncfile ->
                //println("actual = $root")
                //println("expect = $expect")
                assertEquals(ncfile.cdl(), myfile.cdl())
                println(myfile.cdl())
            }
        }
    }

    fun readHCdata(filename : String, varname : String? = null) {
        println("=================")
        println(filename)
        Hdf4ClibFile(filename).use { hcfile ->
            readMyData(hcfile, varname)
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    fun readH4Data(filename: String) {
        readNetchdfData(filename, null, null, true)
        println()
    }

    @ParameterizedTest
    @MethodSource("params")
    fun compareData(filename: String) {
        compareData(filename, null)
    }

    fun compareData(filename: String, varname : String?) {
        println("=================")
        println(filename)
        Hdf4File(filename).use { myfile ->
            // println("Hdf4File = \n${myfile.cdl()}")
            Hdf4ClibFile(filename).use { ncfile ->
                compareNetcdfData(myfile, ncfile, varname)
            }
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    fun testIterateWithClib(filename: String) {
        compareIterateWithClib(filename)
    }

}