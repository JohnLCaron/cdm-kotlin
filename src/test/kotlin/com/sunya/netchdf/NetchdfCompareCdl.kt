package com.sunya.netchdf

import com.sunya.cdm.api.Netcdf
import com.sunya.netchdf.netcdf4.openNetchdfFile
import com.sunya.netchdf.netcdfClib.NetcdfClibFile
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import test.util.testFilesIn
import java.util.*
import java.util.stream.Stream
import kotlin.test.assertEquals

// Compare header using cdl(!strict) with Netchdf and NetcdfClibFile
class NetchdfCompareCdl {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
             val stream3 =
                testFilesIn("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf3")
                    .build()

            val stream4 =
                testFilesIn("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4")
                    .addNameFilter { name -> !name.endsWith("tst_grps.nc4") } // nested group typedefs
                    .build()

            val moar =
                testFilesIn("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf4")
                    .withPathFilter { p -> !p.toString().contains("exclude") }
                    .addNameFilter { name -> !name.endsWith("compound-attribute-test.nc") } // bug in clib
                    .addNameFilter { name -> !name.endsWith("tst_grps.nc4") } // nested group typedefs
                    .addNameFilter { name -> !name.endsWith("tst_vars.nc4") } // too slow LOOK why?
                    .withRecursion()
                    .build()

            val eos5 =
                testFilesIn("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdfeos5")
                    .build()

            // return moar
            return Stream.of(stream3, stream4, moar).flatMap { i -> i };
            //return stream2
        }
    }

    // @Test bug in Clib matches typedef hash, not address
    fun testProblem () {
        openNetchdf("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf4/compound-attribute-test.nc")
    }

    // @Test
    fun testProblemSlow () {
        openNetchdf("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/tst_vars.nc4")
    }

    @Test
    fun hdfeos () {
        openNetchdf("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdfeos5/grid_1_3d_xyz_aug.h5")
    }

    /* netcdf grid_1_3d_xyz_aug {
group: HDFEOS {

  group: ADDITIONAL {

    group: FILE_ATTRIBUTES {
      } // group FILE_ATTRIBUTES
    } // group ADDITIONAL

  group: GRIDS {

    group: GeoGrid {
      dimensions:
      	XDim = 8 ;
      	YDim = 4 ;
      	ZDim = 2 ;
      variables:
      	float XDim(XDim) ;
      		XDim:long_name = "longitude" ;
      		XDim:units = "degrees_east" ;
      	float YDim(YDim) ;
      		YDim:long_name = "latitude" ;
      		YDim:units = "degrees_north" ;
      	float ZDim(ZDim) ;

      group: Data\ Fields {
        variables:
        	float Latitude(YDim) ;
        		Latitude:units = "degrees_north" ;
        	float Longitude(XDim) ;
        		Longitude:units = "degrees_east" ;
        	float Pressure(ZDim) ;
        		Pressure:units = "hPa" ;
        	float Temperature(ZDim, YDim, XDim) ;
        		Temperature:units = "K" ;
        } // group Data\ Fields
      } // group GeoGrid
    } // group GRIDS
  } // group HDFEOS

group: HDFEOS\ INFORMATION {
  variables:
  	string StructMetadata.0 ;

  // group attributes:
  		:HDFEOSVersion = "HDFEOS_5.1.13" ;
  } // group HDFEOS\ INFORMATION
}
readDataObject= StructMetadata.0
 read Message SimpleDataspace(1)  Scalar []
 read Message Datatype(3)  String
 read Message FillValue(5)  has hasFillValue=false
 read Message Layout(8)  class=Contiguous
 read Message LastModified(18)  null
 read Message NIL(0)  null

     */

    // @Test
    fun compoundAttributeTest () {
        openNetchdf("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf4/compound-attribute-test.nc")
    }
    /*snake@jlc:~/dev/github/cdm-kotlin$ ncdump -h /media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf4/compound-attribute-test.nc
netcdf compound-attribute-test {
types:
  compound compound_type {
    float field0 ;
    float field1 ;
    float field2 ;
    float field3 ;
  }; // compound_type
  compound compound_att_string {
    string field0 ;
    string field1 ;
    string field2 ;
    string field3 ;
  }; // compound_att_string
  compound compound_att_char_array {
    char field0(4) ;
    char field1(4) ;
    char field2(4) ;
    char field3(4) ;
  }; // compound_att_char_array
  compound compound_att_float {
    float field0 ;
    float field1 ;
    float field2 ;
    float field3 ;
  }; // compound_att_float
dimensions:
	dim0 = 2 ;
variables:
	compound_type compound_test(dim0) ;
		compound_att_char_array compound_test:att_char_array_test = {{"a"}, {"1"}, {"abc"}, {"123"}} ;
		compound_type           compound_test:att_primitive_test = {1, 2, 3, 4} ;
		compound_att_string     compound_test:att_string_test = {"string for field 0", "field 1 has something", "hey look at me!", "writer\'s block"} ;
}
nclib
   compound_type compound_test(dim0);
      compound_att_char_array compound_test:att_char_array_test = {field0 = "a   ", field1 = "1   ", field2 = "abc ", field3 = "123 "};
      compound_type           compound_test:att_primitive_test = {field0 = 1.0, field1 = 2.0, field2 = 3.0, field3 = 4.0};
      compound_att_string     compound_test:att_string_test = {field0 = "string for field 0", field1 = "field 1 has something", field2 = "hey look at me!", field3 = "writer's block"};

h5lib LOOK I think compound_att_float is correct, compound_type has same hash, but wrong
    compound_type compound_test(dim0);
      compound_att_char_array compound_test:att_char_array_test = {field0 = "a   ", field1 = "1   ", field2 = "abc ", field3 = "123 "};
      compound_att_float      compound_test:att_primitive_test = {field0 = 1.0, field1 = 2.0, field2 = 3.0, field3 = 4.0};
      compound_att_string     compound_test:att_string_test = {field0 = "string for field 0", field1 = "field 1 has something", field2 = "hey look at me!", field3 = "writer's block"};

h5dump
  compound_att_char_array looks like it should be an array os "strings" of size 1

  H5T_ARRAY { [4] H5T_STRING {
         STRSIZE 1;
         STRPAD H5T_STR_NULLTERM;
         CSET H5T_CSET_ASCII;
         CTYPE H5T_C_S1;
      } } "field0";

      compound_att_string should be "strings" of vlen i guess
      H5T_STRING {
         STRSIZE H5T_VARIABLE;
         STRPAD H5T_STR_NULLTERM;
         CSET H5T_CSET_ASCII;
         CTYPE H5T_C_S1;
      } "field0";

         DATATYPE "compound_type" H5T_COMPOUND {
      H5T_IEEE_F32LE "field0";
      H5T_IEEE_F32LE "field1";
      H5T_IEEE_F32LE "field2";
      H5T_IEEE_F32LE "field3";
   }
   same as
      DATATYPE "compound_att_float" H5T_COMPOUND {
      H5T_IEEE_F32LE "field0";
      H5T_IEEE_F32LE "field1";
      H5T_IEEE_F32LE "field2";
      H5T_IEEE_F32LE "field3";
   }

     */

    @ParameterizedTest
    @MethodSource("params")
    fun openNetchdf(filename: String) {
        println("=================")
        println(filename)
        val netchdf : Netcdf? = openNetchdfFile(filename)
        if (netchdf == null) {
            println("*** not a netchdf file = $filename")
            return
        }
        println("\nnetchdf = ${netchdf.cdl()}")

        val nclibfile : Netcdf = NetcdfClibFile(filename)
        //println("nclibfile = ${nclibfile.cdl()}")

        assertEquals(nclibfile.cdl(), netchdf.cdl())

        netchdf.close()
        nclibfile.close()
    }

}