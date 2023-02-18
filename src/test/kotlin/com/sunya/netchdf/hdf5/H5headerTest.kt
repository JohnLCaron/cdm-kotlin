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

    @Test
    fun sharedObject() {
        openH5("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/test_enum_type.nc")
    }

    @Test
    fun fractalHeap() {
        openH5("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/test_atomic_types.nc")
        openH5("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/testCFGridWriter.nc4")
    }

    @Test
    fun hasTimeDataType() {
        openH5("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/fpcs_1dwave_2.nc")
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
        println("=================")
        println(filename)
        Hdf5File(filename).use { h5file ->
            println(h5file.cdl())
        }
    }

}