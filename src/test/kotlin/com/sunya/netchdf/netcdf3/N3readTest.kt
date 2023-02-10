package com.sunya.netchdf.netcdf3

import java.util.*
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class N3readTest {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> = Stream.of(
            Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf3/longOffset.nc",
"""  dimensions:
    x = 6;
    y = 12;
  variables:
    int data(x=6, y=12);

"""),
            )
    }

    @ParameterizedTest
    @MethodSource("params")
    fun readN3data(filename : String, expect : String) {
        println("=================")
        println(filename)
        val ncfile = Netcdf3File(filename)
        val root = ncfile.rootGroup()
        println("actual = ${root.cdlString()}")
        //println("expect = $expect")

        assertEquals(expect, root.toString())
        assertTrue(root.toString().contains(expect))

        root.variables.forEach {
            println(" ${it.name} = ${it.spObject}")
            // println(" ${it.readData()}")
        }
    }

}