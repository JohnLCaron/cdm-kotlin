package com.sunya.netchdf.hdf4

import com.sunya.cdm.util.Stats
import com.sunya.netchdf.*
import com.sunya.netchdf.netcdf4.openNetchdfFile
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

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            Stats.clear() // problem with concurrent tests
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            if (count > 0) println("$count files")
            Stats.show()
        }

        private var count = 0
    }

    @Test
    fun unsolved1() { // H4 has atts n > 1
        readH4header(testData + "cdmUnitTest/formats/hdf4/ssec/2006166131201_00702_CS_2B-GEOPROF_GRANULE_P_R03_E00.hdf")
    }

    @Test
    fun chunkIterator() { // H4 has atts n > 1
        testReadIterate(testData + "cdmUnitTest/formats/hdf4/ssec/AIRS.2005.08.28.103.L1B.AIRS_Rad.v4.0.9.0.G05241172839.hdf")
    }

    @Test
    fun problem1() { // HC coredump
        readH4header(testData + "hdf4/nsidc/LAADS/MOD/MOD01.A2007303.0325.005.2007306182401.hdf")
        // compareH4header(testData + "hdf4/nsidc/LAADS/MOD/MOD03.A2007001.0000.005.2007041030714.hdf")
    }

    @Test
    fun problemData() {
        readH4header(testData + "hdf4/nsidc/LAADS/MYD/MYD01.A2007001.0440.005.2007311085701.hdf")
        readNetchdfData(testData + "hdf4/nsidc/LAADS/MYD/MYD01.A2007001.0440.005.2007311085701.hdf", "Discarded_Packets") // , "/mod08/Data_Fields/Angstrom_Exponent_2_Ocean_Std_Deviation_Mean")
    }

    @Test
    fun problemDuplicateVariable() {
        readH4header(testData + "hdf4/AST_L1B_00307182004110047_08122004112525.hdf")
    }

    // /home/all/testdata/hdf4/NOAA.CRW.OAPS.25km.GCR.200402.hdf
    //home/all/testdata/hdf4/MYD021KM.A2008349.1800.005.2009329084841.hdf
    //home/all/testdata/cdmUnitTest/formats/hdf4/ssec/AIRS.2005.08.28.103.L1B.AIRS_Rad.v4.0.9.0.G05241172839.hdf, radiances
    //home/all/testdata/cdmUnitTest/formats/hdf4/MYD29.A2009152.0000.005.2009153124331.hdf, Sea_Ice_by_Reflectance

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
            count++
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