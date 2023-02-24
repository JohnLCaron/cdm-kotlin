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

// Compare header using cdl(!strict) with Hdf5File and NetcdfClibFile
class H5headerCompare {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            // bootstrap selected files
            val stream1 = Stream.of(
                // sb1
                Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/string_attrs.nc4"),
                Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/attstr.h5"),
                // sb2
                Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/tst_dims.nc"),
                Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/dimScales.h5"),
                Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/tst_solar_1.nc4"),
                Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/tst_groups.nc"),
            )

            val stream4 =
                testFilesIn("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4")
                    .addNameFilter { name -> !name.endsWith("tst_grps.nc4") } // nested group typedefs
                    .withRecursion()
                    .build()

            // about half of these fail because they are not netcdf4 files, so nc4lib sees them as empty
            // LOOK should compare against HDF5 library directly (!)
            val hdfStream =
                testFilesIn("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdf5")
                    .withRecursion()
                    .build()

            return Stream.of(stream4).flatMap { i -> i };
            //return stream2
        }
    }

    // @Test failing because differs from ncfile
    fun problem() {
        openH5("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/tst_grps.nc4")
    }
    /*
    ncfile adds extra typedefs:

    netcdf tst_grps {
  types:
    opaque(10) opaque-1 ;
    int(*) vlen-1 ;

  group: the_in_crowd {
    types:
      opaque(7) opaque-2 ;
      byte(*) vlen-2 ;
  }

  group: the_out_crowd {
    types:
      opaque(4) opaque-3 ;
      byte(*) vlen-3 ;

    group: the_confused_crowd {
      types:
        opaque(13) opaque-4 ;
        byte(*) vlen-4 ;
    }
  }

  h5file eliminates them because they are identical:
  netcdf tst_grps {
  types:
    opaque(10) opaque-1 ;
    int(*) vlen-1 ;

  group: the_in_crowd {
    types:
      opaque(7) opaque-2 ;
      byte(*) vlen-2 ;
  }

  group: the_out_crowd {
    types:
      opaque(4) opaque-3 ;

    group: the_confused_crowd {
      types:
        opaque(13) opaque-4 ;
    }
  }
}
}
     */

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
        openH5("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/tst_enums.nc")
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
        // openH5("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/tst_solar_2.nc4")
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

    @Test // currently fails
    fun charVar () {
        openH5("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/dstr.h5")
    }
/*    netcdf dstr {
        variables:
        string Char\ Data ; Char space Data is the variable name ?? yikes!!
    }
 */

    @Test
    fun compoundInnerVlen () {
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
data:

 time = 1031111536548600, 1031336029112720, 1031336030112720,
    1032744682300880, 1033386252364830, 1033436507387280, 1033625909928750,
    1034720141744040, 1034777862124190, 1034812140933520, 1035166037088710,
    1038857775888520, 1039392441890480, 1040992959891280, 1041027759893420,
    1041237566888640, 1041906851888650, 1041931946851890, 1043767397826310,
    1044072303892950, 1045100010827670, 1045195564890470, 1045889419890110,
    1046613199831130, 1048382474827570, 1049148580891530, 1050509662880560,
    1054030033913430, 1072247943885370 ;

 tim_records =
    {0, 0, 0, 0, 1, 1, 0, 1, 0, {{30007, 50334}}, {{33761, 54686}}, 1031111536548600},
    {0, 0, 1, 0, 0, 1, 0, 1, 0, {{50343}}, {{9182}}, 1031336029112720},
    {0, 0, 1, 0, 0, 1, 0, 1, 0, {{50347}}, {{9185}}, 1031336030112720},
    {0, 0, 1, 0, 0, 1, 0, 1, 0, {{50346}}, {{9221}}, 1032744682300880},
    {0, 0, 1, 0, 0, 1, 0, 1, 0, {{50338}}, {{8347}}, 1033386252364830},
    {0, 0, 0, 0, 0, 1, 0, 1, 0, {{50378}}, {{54660}}, 1033436507387280},
    {0, 0, 1, 0, 0, 1, 0, 1, 0, {{50295}}, {{8529}}, 1033625909928750},
    {0, 0, 0, 0, 0, 1, 0, 1, 0, {{50336, 50343}}, {{54660, 8171}}, 1034720141744040},
    {0, 0, 1, 0, 0, 1, 0, 1, 0, {{50342}}, {{8103}}, 1034777862124190},
    {0, 0, 1, 0, 0, 1, 0, 1, 0, {{50348}}, {{8139}}, 1034812140933520},
    {0, 0, 0, 0, 0, 1, 0, 1, 0, {{50375}}, {{54653}}, 1035166037088710},
    {0, 0, 0, 0, 0, 1, 0, 1, 0, {{50391}}, {{54644}}, 1038857775888520},
    {0, 0, 0, 0, 0, 1, 0, 1, 0, {{50332}}, {{54934}}, 1039392441890480},
    {0, 0, 0, 0, 0, 1, 0, 1, 0, {{50331}}, {{52569}}, 1040992959891280},
    {0, 0, 0, 0, 0, 1, 0, 1, 0, {{8798}}, {{62999}}, 1041027759893420},
    {0, 0, 0, 0, 0, 1, 0, 1, 0, {{50396}}, {{55057}}, 1041237566888640},
    {0, 0, 0, 0, 0, 1, 0, 1, 0, {{50401}}, {{54806}}, 1041906851888650},
    {0, 0, 0, 0, 0, 1, 0, 1, 0, {{50475}}, {{52271}}, 1041931946851890},
    {0, 0, 0, 0, 0, 1, 0, 1, 0, {{50371}}, {{46638}}, 1043767397826310},
    {0, 0, 0, 0, 0, 1, 0, 1, 0, {{54551}}, {{37057}}, 1044072303892950},
    {0, 0, 1, 0, 0, 1, 0, 1, 0, {{50412}}, {{7519}}, 1045100010827670},
    {0, 0, 0, 0, 0, 1, 0, 1, 0, {{50368}}, {{54935}}, 1045195564890470},
    {0, 0, 0, 0, 0, 1, 0, 1, 0, {{50421}}, {{54694}}, 1045889419890110},
    {0, 0, 0, 0, 0, 1, 0, 1, 0, {{50438}}, {{52173}}, 1046613199831130},
    {1, 0, 1, 0, 0, 1, 0, 1, 0, {{4282}}, {{10016}}, 1048382474827570},
    {0, 0, 0, 0, 0, 1, 0, 1, 0, {{50453}}, {{52320}}, 1049148580891530},
    {0, 0, 1, 0, 0, 1, 0, 1, 0, {{50425}}, {{9049}}, 1050509662880560},
    {0, 0, 0, 0, 0, 1, 0, 1, 0, {{50369, 62999, 50325, 50285}}, {{54963, 62999, 54918, 54656}}, 1054030033913430},
    {0, 0, 1, 0, 0, 1, 0, 1, 0, {{7164}}, {{50348}}, 1072247943885370} ;
}
 */

    @Test
    fun attCompoundInnerString () {
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

        val nclibfile : Netcdf = NetcdfClibFile(filename)
        println("ncfile = ${nclibfile.cdl()}")

        assertEquals(nclibfile.cdl(), h5file.cdl())

        h5file.close()
        nclibfile.close()
    }

}