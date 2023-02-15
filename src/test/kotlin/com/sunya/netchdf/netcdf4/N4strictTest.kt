package com.sunya.netchdf.netcdf4

import com.sunya.cdm.api.Netcdf
import com.sunya.netchdf.netcdfClib.NetcdfClibFile
import org.junit.jupiter.api.Test
import java.util.*
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class N4strictTest {

    @Test
    fun tst_dims() {
        readN4header("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/tst_dims.nc",
            """netcdf tst_dims {
dimensions:
	latitude = 6 ;
	longitude = 12 ;
variables:
	float longitude(longitude) ;
}"""
        )
    }



    @Test
    fun string_attrs() {
        readN4header("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/string_attrs.nc4",
"""netcdf string_attrs {
variables:
	byte var ;
		string var:NULL_STR_ATTR = NIL ;
		string var:EMPTY_STR_ATTR = "" ;

// global attributes:
		string :NULL_STR_GATTR = NIL ;
		string :EMPTY_STR_GATTR = "" ;
}"""
        )
    }

    @Test
    fun attstr() {
        readN4header(
            "/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/attstr.h5",
"""netcdf attstr {

group: MyGroup {

    // group attributes:
    :data_contents = "important_data" ;
} // group MyGroup
}"""
        )
    }

    @Test
    fun tst_groups() {
        readN4header(
            "/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/tst_groups.nc",
            """netcdf tst_groups {
dimensions:
	dim = 4 ;
variables:
	float var(dim) ;
		var:units = "m/s" ;

// global attributes:
		:title = "for testing groups" ;

group: g1 {
  dimensions:
  	dim = 1 ;
  variables:
  	float var(dim) ;
  		var:units = "km/hour" ;

  // group attributes:
  		:title = "in first group" ;
  } // group g1

group: g2 {
  dimensions:
  	dim = 2 ;
  variables:
  	float var(dim) ;
  		var:units = "cm/sec" ;

  // group attributes:
  		:title = "in second group" ;

  group: g3 {
    dimensions:
    	dim = 3 ;
    variables:
    	float var(dim) ;
    		var:units = "mm/msec" ;

    // group attributes:
    		:title = "in third group" ;
    } // group g3
  } // group g2
}"""
        )
    }

    @Test
    fun tst_solar_1() {
        readN4header(
            "/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/tst_solar_1.nc",
            """netcdf tst_solar_1 {
dimensions:
	length_of_name = 2 ;

// global attributes:
		:Number_of_vogons = 2UB, 23UB, 230UB ;
		:Number_of_vogon_poems = 23232244ULL, 1214124123423ULL, 2353424234ULL ;

group: solar_system {

  group: Earth {

    // group attributes:
    		:alien_concept_number_which_cannot_be_understood_by_humans = -23232244LL, 1214124123423LL, -2353424234LL ;

    group: Luna {
      variables:
      	int64 var_name(length_of_name) ;

      // group attributes:
      		:Vogon_Poem = "See, see the netCDF-filled sky\nMarvel at its big barf-green depths.\nTell me, Ed do you\nWonder why the yellow-bellied Snert ignores you?\nWhy its foobly stare\nmakes you feel ubiquitous obliquity.\nI can tell you, it is\nWorried by your HDF5-eating facial growth\nThat looks like\nA moldy pile of ASCII data.\nWhat\'s more, it knows\nYour redimensioning potting shed\nSmells of booger.\nEverything under the big netCDF-filled sky\nAsks why, why do you even bother?\nYou only charm software defects." ;
      } // group Luna
    } // group Earth
  } // group solar_system
}
"""
        )
    }

    @Test
    fun test_enum_type() {
        readN4header(
            "/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/test_enum_type.nc",
"""netcdf test_enum_type {
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
}"""
        )
    }

    @Test
    fun IntTimSciSamp() {
        readN4header(
            "/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/IntTimSciSamp.nc",
            """netcdf IntTimSciSamp {
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
"""
        )
    }



            fun readN4header(filename : String, expect : String) {
        println("=================")
        println(filename)
        val ncfile : Netcdf = NetcdfClibFile(filename)
        //println("actual = ${ncfile.cdlStrict().normalize()}")
        //println("expect = ${expect.normalize()}")

        assertEquals(normalize(expect), normalize(ncfile.cdlStrict()))
    }

    fun normalize(org : String) : String {
        return buildString {
            for (line in org.lines()) {
                append(line.trim())
                append("\n")
            }
        }
    }

}