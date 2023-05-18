package com.sunya.netchdf.hdf4

import com.sunya.cdm.api.SectionPartial
import com.sunya.cdm.util.Stats
import com.sunya.netchdf.*
import com.sunya.netchdf.openNetchdfFile
import com.sunya.testdata.H4Files
import com.sunya.testdata.testData
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
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
            return H4Files.params()
        }

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            Stats.clear() // problem with concurrent tests
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
    fun problem() {
        readH4header(testData + "devcdm/hdfeos2/MISR_AM1_GP_GMP_P040_O003734_05.eos")
    }

    @Test
    fun testUsed() {
        readH4header(testData + "devcdm/hdf4/balloon_sonde.o3_knmi000_de.bilt_s2_20060905t112100z_002.hdf")
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
    fun readData(filename: String) {
        readNetchdfData(filename, null, null, true)
        println()
    }

    @ParameterizedTest
    @MethodSource("params")
    fun readH4DataCheckUnused(filename: String) {
        // println("=============================================================")
        Hdf4File(filename).use { h4file ->
            println("--- ${h4file.type()} $filename ")
            readMyData(h4file, null, null)
            h4file.header.showTags(true, true)
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    fun testReadIterate(filename: String) {
        readNetchIterate(filename)
    }

}