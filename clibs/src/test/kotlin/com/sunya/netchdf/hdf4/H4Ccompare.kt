package com.sunya.netchdf.hdf4

import com.sunya.cdm.api.Datatype
import com.sunya.cdm.array.ArrayUByte
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
import org.junit.jupiter.api.Assertions
import java.util.*
import java.util.stream.Stream
import kotlin.test.*

class H4Ccompare {

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

    @Test
    fun testRasterData() {
        val filename = testData + "hdf4/nsidc/GESC/AIRS/AIRS.2006.08.28.A.L1B.Browse_AMSU.v4.0.9.0.G06241184547.hdf"
        compareH4header(filename)
        // readH4CheckUnused(filename)
        compareData(filename)
    }

    // Using Raster Images in a VGroup. VHRR
    @Test
    fun testRasterImageGroup() {
        val filename = testData + "hdf4/eisalt/VHRR-KALPANA_20081216_070002.hdf"
        compareH4header(filename)
        readH4CheckUnused(filename)
        compareData(filename, "Curves_at_2721.35_1298.84_lookup") // fails
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
        readH4CheckUnused(testData + "hdf4/AST_L1B_00307182004110047_08122004112525.hdf")
    }

    //// * tag=DFTAG_VS (1963) Vdata Storage  refno=  5 vclass=           offset=3985033 length=79
    // --- hdf-eos2  /home/all/testdata/hdf4/nsidc/AMSR_E_L2A_BrightnessTemperatures_V08_200801012345_A.hdf
    @Test
    fun testUnusedVdata() {
        compareH4header(testData + "hdf4/nsidc/AMSR_E_L2A_BrightnessTemperatures_V08_200801012345_A.hdf")
        readH4CheckUnused(testData + "hdf4/nsidc/AMSR_E_L2A_BrightnessTemperatures_V08_200801012345_A.hdf")
    }

    // not working
    @Test
    fun testUnusedVhrr() {
        val filename = testData + "hdf4/eisalt/VHRR-KALPANA_20081216_070002.hdf"
        // readH4header(filename)
        compareH4header(filename)
        //compareH4header(filename)
        //readH4CheckUnused(filename)
    }

    //// * tag=DFTAG_VG (1965) Vgroup         refno=  2 vclass=DATA_GRANULE offset=158466140 length=83 name= 'DATA_GRANULE' var='null' class='DATA_GRANULE' extag=0 exref=0 version=3 name='DATA_GRANULE' nelems=3 elems=1962 4,1962 5,1965 3,
    // --- hdf4      /home/all/testdata/hdf4/nsidc/GESC/Other_TRMM/1B21.071022.56609.6.HDF
    @Test
    fun testUnusedGroup() {
        compareH4header(testData + "hdf4/nsidc/GESC/Other_TRMM/1B21.071022.56609.6.HDF")
        readH4CheckUnused(testData + "hdf4/nsidc/GESC/Other_TRMM/1B21.071022.56609.6.HDF")
    }

    //// * tag=DFTAG_COMPRESSED (40) Compressed special element refno=  1 vclass=           offset=2725 length=94
    @Test
    fun testUnusedCompressed() {
        readH4CheckUnused(testData + "hdf4/keegstra/MODSCW_P2009173_C4_1820_1825_2000_2005_GM03_closest_chlora.hdf")
    }

    @Test
    fun problem() {
        compareH4header(testData + "hdf4/eos/modis/MOD13Q1.A2012321.h00v08.005.2012339011757.hdf")
        readH4CheckUnused(testData + "hdf4/eos/modis/MOD13Q1.A2012321.h00v08.005.2012339011757.hdf")
    }

    // * IP8/1             usedBy=false pos=18664902/32 rgb=147,0,108,144,0,111,141,0,114,138,0,117,135,0,120,132,0,123,129,0,126,126,0,129,123,0,132,120,0,135,117,0,138,114,0,141,111,0,144,108,0,147,105,0,150,102,0,153,99,0,156,96,0,159,93,0,162,90,0,165,87,0,168,84,0,171,81,0,174,78,0,177,75,0,180,72,0,183,69,0,186,66,0,189,63,0,192,60,0,195,57,0,198,54,0,201,51,0,204,48,0,207,45,0,210,42,0,213,39,0,216,36,0,219,33,0,222,30,0,225,27,0,228,24,0,231,21,0,234,18,0,237,15,0,240,12,0,243,9,0,246,6,0,249,0,0,252,0,0,255,0,5,255,0,10,255,0,16,255,0,21,255,0,26,255,0,32,255,0,37,255,0,42,255,0,48,255,0,53,255,0,58,255,0,64,255,0,69,255,0,74,255,0,80,255,0,85,255,0,90,255,0,96,255,0,101,255,0,106,255,0,112,255,0,117,255,0,122,255,0,128,255,0,133,255,0,138,255,0,144,255,0,149,255,0,154,255,0,160,255,0,165,255,0,170,255,0,176,255,0,181,255,0,186,255,0,192,255,0,197,255,0,202,255,0,208,255,0,213,255,0,218,255,0,224,255,0,229,255,0,234,255,0,240,255,0,245,255,0,250,255,0,255,255,0,255,247,0,255,239,0,255,231,0,255,223,0,255,215,0,255,207,0,255,199,0,255,191,0,255,183,0,255,175,0,255,167,0,255,159,0,255,151,0,255,143,0,255,135,0,255,127,0,255,119,0,255,111,0,255,103,0,255,95,0,255,87,0,255,79,0,255,71,0,255,63,0,255,55,0,255,47,0,255,39,0,255,31,0,255,23,0,255,15,0,255,0,8,255,0,16,255,0,24,255,0,32,255,0,40,255,0,48,255,0,56,255,0,64,255,0,72,255,0,80,255,0,88,255,0,96,255,0,104,255,0,112,255,0,120,255,0,128,255,0,136,255,0,144,255,0,152,255,0,160,255,0,168,255,0,176,255,0,184,255,0,192,255,0,200,255,0,208,255,0,216,255,0,224,255,0,232,255,0,240,255,0,248,255,0,255,255,0,255,251,0,255,247,0,255,243,0,255,239,0,255,235,0,255,231,0,255,227,0,255,223,0,255,219,0,255,215,0,255,211,0,255,207,0,255,203,0,255,199,0,255,195,0,255,191,0,255,187,0,255,183,0,255,179,0,255,175,0,255,171,0,255,167,0,255,163,0,255,159,0,255,155,0,255,151,0,255,147,0,255,143,0,255,139,0,255,135,0,255,131,0,255,127,0,255,123,0,255,119,0,255,115,0,255,111,0,255,107,0,255,103,0,255,99,0,255,95,0,255,91,0,255,87,0,255,83,0,255,79,0,255,75,0,255,71,0,255,67,0,255,63,0,255,59,0,255,55,0,255,51,0,255,47,0,255,43,0,255,39,0,255,35,0,255,31,0,255,27,0,255,23,0,255,19,0,255,15,0,255,11,0,255,7,0,255,3,0,255,0,0,250,0,0,245,0,0,240,0,0,235,0,0,230,0,0,225,0,0,220,0,0,215,0,0,210,0,0,205,0,0,200,0,0,195,0,0,190,0,0,185,0,0,180,0,0,175,0,0,170,0,0,165,0,0,160,0,0,155,0,0,150,0,0,145,0,0,140,0,0,135,0,0,130,0,0,125,0,0,120,0,0,115,0,0,110,0,0,105,0,0,0,0,0,
    // * LUT/1             usedBy=false pos=18664902/32 nelems=null
    //  DIL/1             usedBy=true pos=18665670/31 for=1/201 text=palette
    @Test
    fun problemDIL() {
        compareH4header(testData + "hdf4/S2007329.L3m_DAY_CHLO_9")
        readH4CheckUnused(testData + "hdf4/S2007329.L3m_DAY_CHLO_9")
    }

    @Test
    fun problem2() {
        compareH4header(testData + "devcdm/hdf4/TOVS_BROWSE_MONTHLY_AM_B861001.E861031_NF.HDF")
        compareData(testData + "devcdm/hdf4/TOVS_BROWSE_MONTHLY_AM_B861001.E861031_NF.HDF", "Raster_Image_#0")
    }

    @Test
    fun problemReadData() {
        val filename = testData + "hdf4/nsidc/GESC/AIRS/AIRS.2006.08.28.A.L1B.Browse_AMSU.v4.0.9.0.G06241184547.hdf"
        readH4header(filename)
        testIterateWithClib(filename)
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
                assertEquals(hcfile.cdl(), myfile.cdl())
            }
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    fun readH4CheckUnused(filename: String) {
        Hdf4File(filename).use { h4file ->
            println("--- ${h4file.type()} $filename ")
            assertTrue( 0 == h4file.header.showTags(true, true, false))
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
    fun readCharDataCompare(filename : String) {
        compareSelectedDataWithClib(filename) { it.datatype == Datatype.CHAR } //  || it.datatype == Datatype.STRING }
    }

    @ParameterizedTest
    @MethodSource("params")
    fun testIterateWithClib(filename: String) {
        compareIterateWithClib(filename)
    }
}