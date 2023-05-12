package com.sunya.netchdf.hdf4

import com.sunya.cdm.util.Stats
import com.sunya.netchdf.*
import com.sunya.netchdf.hdf4Clib.Hdf4ClibFile
import com.sunya.testdata.H4Files
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import com.sunya.testdata.testData
import java.util.*
import java.util.stream.Stream
import kotlin.test.assertEquals

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
    @Disabled
    fun coreDump() { // fakeDims, HC coredump "double free or corruption (out)"
        try {
            // tried to add duplicate variable 'RIATTR0.0N'
            compareH4header(testData + "hdf4/eisalt/VHRR-KALPANA_20081216_070002.hdf")
        } catch (t : Throwable) {
            print(t.stackTrace)
        }
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

    @Test
    fun problem() {
        val filename = testData + "hdf4/jeffmc/swath.hdf"
        //readH4header(filename)
        compareH4header(filename)
        //compareData(filename)
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