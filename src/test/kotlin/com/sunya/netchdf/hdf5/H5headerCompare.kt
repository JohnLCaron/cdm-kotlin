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

            return stream3
            // return Stream.of(stream1, stream2, stream3).flatMap { i -> i };
            //return stream2
        }
    }

    @Test
    fun tst_compounds() {
        openH5("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/tst_compounds.nc4")
    }
    /*
netcdf tst_compounds {
types:
  compound obs_t {
    byte day ;
    short elev ;
    int count ;
    float relhum ;
    double time ;
  }; // obs_t
dimensions:
	n = 3 ;
variables:
	obs_t obs(n) ;
		obs_t obs:_FillValue = {-99, -99, -99, -99, -99} ;
}
     */

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
        openH5("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/tst_solar_2.nc4")
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
}
netcdf tst_solar_2 {
types:
  int(*) unimaginatively_named_vlen_type ;

// global attributes:
		unimaginatively_named_vlen_type :equally_unimaginatively_named_attribute_YAWN = {-99}, {-99, -99} ;
}
     */

    @Test
    fun varVlen() {
        openH5("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/vlenInt.nc")
    }
    /*
netcdf vlenInt {
types:
  int(*) vlen_t ;
variables:
	vlen_t x ;
}
     */

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

    @Test
    fun attArrayStruct() {
        openH5("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/tst_solar_cmp.nc")
    }
    /*
netcdf tst_solar_cmp {
types:
  compound wind_vector {
    float u ;
    float v ;
  }; // wind_vector

// global attributes:
		wind_vector :my_favorite_wind_speeds = {13.3, 12.2}, {13.3, 12.2}, {13.3, 12.2} ;
}
     */

    @Test
    fun nameHasSpace () {
        openH5("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/dstr.h5")
    }
/*    netcdf dstr {
        variables:
        string Char\ Data ; Char space Data is the variable name ?? yikes!!
    }
 */

    @Test
    fun compoundInner () {
        openH5("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/IntTimSciSamp.nc")
    }
/*
netcdf IntTimSciSamp {
types:
  int(*) loopData ;
  compound tim_record {
    int shutterPositionA ;
    int shutterPositionD ;
    int shutterPositionB ;
    int shutterPositionC ;
    int dspGainMode ;
    int coneActiveStateA ;
    int coneActiveStateD ;
    int coneActiveStateB ;
    int coneActiveStateC ;
    loopData loopDataA(1) ;
    loopData loopDataB(1) ;
    int64 sampleVtcw ;
  }; // tim_record
dimensions:
	time = UNLIMITED ; // (29 currently)
variables:
	int64 time(time) ;
	tim_record tim_records(time) ;
}
 */

    @Test
    fun problem () {
        openH5("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/attributeStruct.nc")
}
    /*
    netcdf attributeStruct {
types:
  compound observation_type {
    float tempMin ;
    float tempMax ;
    float precip ;
  }; // observation_type
  compound observation_atts {
    string tempMin ;
    string tempMax ;
    string precip ;
  }; // observation_atts
dimensions:
	station = 2 ;
	observation = 6 ;
variables:
	int station_id(station) ;
		station_id:standard_name = "station_id" ;
	float lat(station) ;
		lat:units = "degrees_north" ;
	float lon(station) ;
		lon:units = "degrees_east" ;
	float elev(station) ;
		elev:units = "feet" ;
		elev:positive = "up" ;
	int ragged_row_size(station) ;
		ragged_row_size:standard_name = "ragged_row_size" ;
	int time(observation) ;
		time:units = "days since 1929-01-01 00 UTC" ;
	observation_type observations(observation) ;
		observation_atts observations:units = {"degF", "degF", "inches"} ;
		observation_atts observations:coordinates =
    {"time lat lon elev", "time lat lon elev", "time lat lon elev"} ;

// global attributes:
		:CF\:featureType = "stationTimeSeries" ;
		:Conventions = "CF-1.5" ;
}

     */

    @ParameterizedTest
    @MethodSource("params")
    fun openH5(filename: String) {
        println("=================")
        println(filename)
        val h5file = Hdf5File(filename, true)
        println("\nh5file = ${h5file.cdl()}")

        val ncfile : Netcdf = NetcdfClibFile(filename)
        println("ncfile = ${ncfile.cdl()}")

        assertEquals(ncfile.cdl(), h5file.cdl())
    }

}