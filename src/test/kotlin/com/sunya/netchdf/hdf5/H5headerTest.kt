package com.sunya.netchdf.hdf5

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import test.util.testFilesIn
import java.util.*
import java.util.stream.Stream

// Sanity check read Hdf5File header, for non-netcdf4 files
class H5headerTest {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            val stream2 =
                testFilesIn("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdf5")
                    .withRecursion()
                    .build()
            val stream3 =
                testFilesIn("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4")
                    .withRecursion()
                    .build()

            // return stream1
            return Stream.of(stream2, stream3).flatMap { i -> i };
            //return stream2
        }
    }

    // these are not netcdf4. we have a compound type without a typedef. should just add it as a typedef??
    // could also just be a local variable (eg like netcdf-java). MAy not be a seperate name.
    @Test
    fun compoundTypedef() {
        openH5("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdf5/cstr.h5")
    }

    // same with opaque
    @Test
    fun opaqueTypedef() {
        openH5("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdf5/opaque.h5", "Opaque")
    }

    // same with enum
    @Test
    fun enumTypedef() {
        openH5("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdf5/enum.h5")
    }

    @Test
    fun vlenAttribute() {
        openH5("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/tst_vlen_data.nc4")
    }

    @Test
    fun opaqueAttribute() {
        openH5("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/tst_opaque_data.nc4")
    }

    @ParameterizedTest
    @MethodSource("params")
    fun openH5(filename: String) {
        openH5(filename, null)
    }

    fun openH5(filename: String, varname : String? = null) {
        println("=================")
        println(filename)
        Hdf5File(filename).use { h5file ->
            println(h5file.cdl())
            if (varname != null) {
                val h5var = h5file.rootGroup().variables.find { it.name == varname } ?: throw RuntimeException("cant find $varname")
                val h5data = h5file.readArrayData(h5var)
                println(" $varname = $h5data")
            }
        }
    }

}