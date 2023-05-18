package com.sunya.netchdf.hdf4

import com.sunya.cdm.util.Stats
import com.sunya.netchdf.*
import com.sunya.netchdf.hdf4Clib.Hdf4ClibFile
import com.sunya.testdata.H4Files
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import com.sunya.testdata.testData
import java.util.*
import java.util.stream.Stream
import kotlin.test.*

class HCcompare {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            return H4Files.params()
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            if (versions.size > 0) {
                versions.keys.forEach{ println("$it = ${versions[it]!!.size } files") }
            }
            Stats.show()
        }

        private val versions = mutableMapOf<String, MutableList<String>>()
    }

    @Test
    fun problemDuplicateTypedefs() { // duplicate typedefs, look for ones that match before adding
        val filename = testData + "/hdf4/nasa/eos-grid-1d/MISR_AM1_CGAS_FIRSTLOOK_AUG_30_2007_F11_0027.hdf"
        compareH4header(filename)
    }

    // ODL
    @Test
    fun problemWithODL() {
        val filename = testData + "hdf4/nsidc/LAADS/MOD/MODARNSS.Abracos_Hill.A2007001.1515.005.2007003050459.hdf"
        readH4header(filename)
    }
    // /home/all/testdata/hdf4/mak/MOD13Q1.2000.049.aust.005.b01.250m_ndvi.hdf
    ///home/all/testdata/hdf4/mak/MOD13Q1.2000.049.aust.005.b01.250m_ndvi.hdf
    //home/all/testdata/hdf4/mak/MOD13Q1.2008.353.aust.005.b01.250m_ndvi.hdf
    ///home/all/testdata/hdf4/nsidc/LAADS/MOD/MODCSR_8.A2007001.005.2007012175136.hdf
    ///home/all/testdata/hdf4/nsidc/LAADS/MOD/MODCSR_D.A2007001.005.2007004142531.hdf
    ///home/all/testdata/hdf4/nsidc/LAADS/MOD/MODCSR_G.A2007001.0000.005.2007003022635.hdf

    @Test
    fun odlHasZeroDimension() {
        val filename = testData + "hdf4/nsidc/GESC/AIRS/AIRS.2007.10.17.L1B.Cal_Subset.v5.0.16.0.G07292194950.hdf"
        // readH4header(filename)
        compareData(filename, "/L1B_AIRS_Cal_Subset/Data_Fields/radiances")
    }

    // https://github.com/HDFGroup/hdf4/issues/340
    @Test
    fun issue340() {
        val filename = testData + "hdf4/ssec/2006166131201_00702_CS_2B-GEOPROF_GRANULE_P_R03_E00.hdf"
        readHCheader(filename)
       // readH4header(filename)
       // compareH4header(filename)
    }

    // hdf4      /home/all/testdata/devcdm/hdf4/TOVS_BROWSE_MONTHLY_AM_B861001.E861031_NF.HDF 0.21 Mbytes
    // *** FAIL cfile.readArrayData for variable = char Raster_Image_#0 [, ]
    @Test
    fun problemRasterData() {
        val filename = testData + "devcdm/hdf4/TOVS_BROWSE_MONTHLY_AM_B861001.E861031_NF.HDF"
        compareH4header(filename)
        compareData(filename)
    }

    // Using Raster Images in a VGroup. VHRR
    @Test
    fun testRasterImageGroup() {
        val filename = testData + "hdf4/eisalt/VHRR-KALPANA_20081216_070002.hdf"
        // readH4header(filename)
        // readHCheader(filename)
        compareH4header(filename)
        // compareData(filename)
    }

    //// * tag=DFTAG_RI8 (202) Raster-8 image refno=  2 vclass=           offset=294 length=75
    // --- hdf4      /home/all/testdata/devcdm/hdf4/TOVS_BROWSE_MONTHLY_AM_B861001.E861031_NF.HDF

    //// * tag=DFTAG_LUT (301) Image Palette  refno=  1 vclass=           offset=18664902 length=80
    // --- hdf4      /home/all/testdata/hdf4/S2007329.L3m_DAY_CHLO_9
    // --- hdf4      /home/all/testdata/hdf4/NOAA.CRW.OAPS.25km.GCR.200402.hdf

    //// * tag=DFTAG_VG (1965) Vgroup         refno=247 vclass=Ancillary  offset=124396221 length=81 name= 'Ancillary_Data' var='null' class='Ancillary' extag=0 exref=0 version=3 name='Ancillary_Data' nelems=30 elems=1962 248,1962 249,1962 250,1962 251,1962 252,1962 253,1962 254,1962 255,1962 256,1962 257,1962 258,1962 259,1962 260,1962 261,1962 262,1962 263,1962 264,1962 265,1962 266,1962 267,1962 268,1962 269,1962 270,1962 271,1962 272,1962 273,1962 274,1962 275,1962 276,1962 277,
    // --- hdf-eos2  /home/all/testdata/hdf4/AST_L1B_00307182004110047_08122004112525.hdf
    @Test
    fun testUnusedGroup2() {
        // readH4header(testData + "hdf4/AST_L1B_00307182004110047_08122004112525.hdf")
        // readHCheader(testData + "hdf4/AST_L1B_00307182004110047_08122004112525.hdf")
        compareH4header(testData + "hdf4/AST_L1B_00307182004110047_08122004112525.hdf")
        // readH4DataCheckUnused(testData + "hdf4/AST_L1B_00307182004110047_08122004112525.hdf")
    }

    //// * tag=DFTAG_VS (1963) Vdata Storage  refno=  5 vclass=           offset=3985033 length=79
    // --- hdf-eos2  /home/all/testdata/hdf4/nsidc/AMSR_E_L2A_BrightnessTemperatures_V08_200801012345_A.hdf
    @Test
    fun testUnusedVdata() {
        compareH4header(testData + "hdf4/nsidc/AMSR_E_L2A_BrightnessTemperatures_V08_200801012345_A.hdf")
        readH4DataCheckUnused(testData + "hdf4/nsidc/AMSR_E_L2A_BrightnessTemperatures_V08_200801012345_A.hdf")
    }

    // not working
    @Test
    fun testUnusedVhrr() {
        compareH4header(testData + "hdf4/eisalt/VHRR-KALPANA_20081216_070002.hdf")
        readH4DataCheckUnused(testData + "hdf4/eisalt/VHRR-KALPANA_20081216_070002.hdf")
    }

    //// * tag=DFTAG_VG (1965) Vgroup         refno=  2 vclass=DATA_GRANULE offset=158466140 length=83 name= 'DATA_GRANULE' var='null' class='DATA_GRANULE' extag=0 exref=0 version=3 name='DATA_GRANULE' nelems=3 elems=1962 4,1962 5,1965 3,
    // --- hdf4      /home/all/testdata/hdf4/nsidc/GESC/Other_TRMM/1B21.071022.56609.6.HDF
    @Test
    fun testUnusedGroup() {
        compareH4header(testData + "hdf4/nsidc/GESC/Other_TRMM/1B21.071022.56609.6.HDF")
        readH4DataCheckUnused(testData + "hdf4/nsidc/GESC/Other_TRMM/1B21.071022.56609.6.HDF")
    }

    //// * tag=DFTAG_COMPRESSED (40) Compressed special element refno=  1 vclass=           offset=2725 length=94
    @Test
    fun testUnusedCompressed() {
        readH4DataCheckUnused(testData + "hdf4/keegstra/MODSCW_P2009173_C4_1820_1825_2000_2005_GM03_closest_chlora.hdf")
    }

    @Test
    fun problem() {
        compareH4header(testData + "devcdm/hdf4/17766010.hdf")
    }

    @Test
    fun problem2() {
        readHCheader(testData + "devcdm/hdf4/balloon_sonde.o3_knmi000_de.bilt_s2_20060905t112100z_002.hdf")
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
            val paths = versions.getOrPut(ncfile.type()) { mutableListOf() }
            paths.add(filename)
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

    @ParameterizedTest
    @MethodSource("params")
    fun compareH4header(filename : String) {
        println("=================")
        println(filename)
        Hdf4File(filename).use { myfile ->
            println("Hdf4File = \n${myfile.cdl()}")
            Hdf4ClibFile(filename).use { hcfile ->
                //println("actual = $root")
                //println("expect = $expect")
                assertEquals(hcfile.cdl(), myfile.cdl())
                // println(myfile.cdl())
            }
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    fun readH4DataCheckUnused(filename: String) {
        Hdf4File(filename).use { h4file ->
            println("--- ${h4file.type()} $filename ")
            // readDataIterate(h4file, null, null)
            assertTrue( 0 == h4file.header.showTags(false, true, true))
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    fun readH4data(filename: String) {
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