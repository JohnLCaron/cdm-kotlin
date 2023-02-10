package com.sunya.netchdf.hdf5

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import test.util.testFilesIn
import java.util.*
import java.util.stream.Stream

class H5headerTest {
    val debug = false

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            val stream1 = Stream.of(
                //sb1
                Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/string_attrs.nc4"),
                Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/attstr.h5"),
                // sb2
                Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/tst_dims.nc"),
                Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/dimScales.h5"),
                Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/tst_solar_1.nc"),
                Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/tst_groups.nc"),
            )
            val stream2 =
                testFilesIn("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdf5")
                    .withRecursion()
                    .build()
            val stream3 =
                testFilesIn("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4")
                    .withRecursion()
                    .build()

            // return stream1
            return Stream.of(stream1, stream2, stream3).flatMap { i -> i };
            //return stream2
        }
    }

    @Test
    fun baseAddressNotZero() {
        openH5("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdf5/GATRO-SATMR_npp_d20020906_t0409572_e0410270_b19646_c20090720223122943227_devl_int.h5")
    }

    @Test
    fun sharedObject() {
        openH5("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/test_enum_type.nc")
    }

    @Test
    fun fractalHeap() {
        openH5("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/test_atomic_types.nc")
    }

    @Test
    fun hasTimeDataType() {
        openH5("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/fpcs_1dwave_2.nc")
    }

    @Test
    fun problem() {
        openH5("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/testCFGridWriter.nc4")
    }

    @ParameterizedTest
    @MethodSource("params")
    fun openH5(filename: String) {
        println("=================")
        println(filename)
        val h4file = Hdf5File(filename)
        /* val rootClib = ncheader.rootGroup.build(null)
        println(rootClib.cdlString())

        val nciosp = ncheader.getIosp()

         */

        /*
        val ncvars = rootClib.variables
        ncvars.forEach { n4var ->
            val ncdata = nciosp.readArrayData(n4var)
            println("===============\n${n4var.name}")
            println("ncdata = $ncdata")
        }

         */
    }

}