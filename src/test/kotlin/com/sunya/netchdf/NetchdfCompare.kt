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

class NetchdfCompare {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
             val stream3 =
                testFilesIn("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf3")
                    .build()

            val stream4 =
                testFilesIn("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4")
                    .addNameFilter{ it != "dstr.h5"} // currently failing
                    .build()

            val eos5 =
                testFilesIn("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdfeos5")
                    .addNameFilter{ it != "dstr.h5"} // currently failing
                    .build()

            // return stream3
            return Stream.of(stream3, stream4).flatMap { i -> i };
            //return stream2
        }
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

    @Test
    fun problem () {
        openNetchdf("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/attributeStruct.nc")
    }

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
        println("nclibfile = ${nclibfile.cdl()}")

        assertEquals(nclibfile.cdl(), netchdf.cdl())

        netchdf.close()
        nclibfile.close()
    }

}