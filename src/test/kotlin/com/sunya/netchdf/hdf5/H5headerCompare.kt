package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.Netcdf
import com.sunya.netchdf.netcdfClib.NetcdfClibFile
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import test.util.testFilesIn
import java.util.*
import java.util.stream.Stream
import kotlin.test.assertEquals

class H5headerCompare {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            val stream1 = Stream.of(
                // sb1
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
        // openH5("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdf5/GATRO-SATMR_npp_d20020906_t0409572_e0410270_b19646_c20090720223122943227_devl_int.h5")
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
/*
netcdf tst_opaque_data {
types:
  opaque(11) raw_obs_t ;
dimensions:
	time = 5 ;
variables:
	raw_obs_t raw_obs(time) ;
		raw_obs_t raw_obs:_FillValue = 0XCAFEBABECAFEBABECAFEBA ;
}
 */

    @Test
    fun attEnum() {
        openH5("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/test_enum_type.nc")
    }
    /*
    netcdf test_enum_type {
types:
  ubyte enum cloud_class_t {Clear = 0, Cumulonimbus = 1, Stratus = 2,
      Stratocumulus = 3, Cumulus = 4, Altostratus = 5, Nimbostratus = 6,
      Altocumulus = 7, Cirrostratus = 8, Cirrocumulus = 9, Cirrus = 10,
      Missing = 255} ;
dimensions:
	station = 5 ;
variables:
	cloud_class_t primary_cloud(station) ;
		cloud_class_t primary_cloud:_FillValue = Missing ;
} */

    @Test
    fun attVlen() {
        openH5("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/tst_vlen_data.nc4")
    }
    /*
    snake@jlc:~$ ncdump -h /home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/tst_vlen_data.nc4
netcdf tst_vlen_data {
types:
  float(*) row_of_floats ;
dimensions:
	m = 5 ;
variables:
	row_of_floats ragged_array(m) ;
		row_of_floats ragged_array:_FillValue = {-999} ;
} */

    @Test
    fun outofOrder() {
        openH5("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/fpcs_1dwave_2.nc")
    }
    /*
    snake@jlc:~$ ncdump -h /home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/fpcs_1dwave_2.nc
netcdf fpcs_1dwave_2 {
types:
  ushort(*) vlen_t ;
dimensions:
	acqtime = UNLIMITED ; // (10 currently)
variables:
	uint64 acqtime(acqtime) ;
		acqtime:long_name = "Acquisition time" ;
	uint speriod(acqtime) ;
		speriod:long_name = "Sample period for this data block" ;
		speriod:units = "ns" ;
	uint srate(acqtime) ;
		srate:long_name = "Sample rate for this data block" ;
		srate:units = "samples/s" ;
	double scale_factor(acqtime) ;
		scale_factor:long_name = "Scale factor for this data block" ;
	double offset(acqtime) ;
		offset:long_name = "Offset value to be added after applying the scale_factor" ;
	int nelems(acqtime) ;
		nelems:long_name = "Number of elements of this data block" ;
	vlen_t levels(acqtime) ;
		levels:long_name = "Acquired values array" ;

// global attributes:
		:channelID = "Channel_1" ;
		:title = "Acquisition channel data" ;
		:version = 1. ;
		:time_stamp_start_secs = 10000000000LL ;
		:time_stamp_start_nanosecs = 10000000000LL ;
		:time_coverage_duration = 0 ;
		:license = "Freely available" ;
} */

    @ParameterizedTest
    @MethodSource("params")
    fun openH5(filename: String) {
        println("=================")
        println(filename)
        val h5file = Hdf5File(filename, true)
        println("h5file = ${h5file.cdl()}")

        val ncfile : Netcdf = NetcdfClibFile(filename)
        println("ncfile = ${ncfile.cdl()}")

        assertEquals(ncfile.cdl(), h5file.cdl())
    }

}