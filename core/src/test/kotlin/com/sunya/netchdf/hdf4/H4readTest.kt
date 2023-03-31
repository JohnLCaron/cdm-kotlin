package com.sunya.netchdf.hdf4

import com.sunya.cdm.util.Stats
import com.sunya.netchdf.*
import com.sunya.netchdf.netcdf4.openNetchdfFile
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
            if (versions.size > 0) {
                versions.keys.forEach{ println("$it = ${versions[it]!!.size } files") }
            }
            Stats.show()
        }

        private val versions = mutableMapOf<String, MutableList<String>>()
    }

    @Test
    @Disabled
    fun HCcoredump() { // HC coredump
        readH4header("/home/all/testdata/hdf4/nsidc/LAADS/MOD/MOD01.A2007303.0325.005.2007306182401.hdf")
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
    fun testReadIterate(filename: String) {
        readNetchIterate(filename, null)
    }

}