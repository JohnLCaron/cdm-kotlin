package com.sunya.netchdf

import com.sunya.cdm.api.*
import com.sunya.cdm.api.Section.Companion.computeSize
import com.sunya.cdm.iosp.Iosp
import com.sunya.netchdf.netcdf4.openNetchdfFile
import com.sunya.netchdf.netcdfClib.NetcdfClibFile
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import test.util.testFilesIn
import java.util.*
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Compare header using cdl(!strict) with Netchdf and NetcdfClibFile
class NetchdfTest {

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

            val moar3 =
                testFilesIn("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf3")
                    .withPathFilter { p -> !p.toString().contains("exclude") }
                    .withRecursion()
                    .build()

            val moar4 =
                testFilesIn("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf4")
                    .withPathFilter { p -> !p.toString().contains("exclude") }
                    .addNameFilter { name -> !name.endsWith("compound-attribute-test.nc") } // bug in clib
                    .addNameFilter { name -> !name.endsWith("tst_grps.nc4") } // nested group typedefs
                    .withRecursion()
                    .build()

            val eos5 =
                testFilesIn("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdfeos5")
                    .build()

            // return moar3
            return Stream.of(stream3, stream4, moar3, moar4).flatMap { i -> i };
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            println("\ncountVariables = $countVariables")
        }

        var countVariables = 0
        var showData = false
    }

    @Test
    @Disabled
    fun hdfeos() {
        compareCdlWithClib("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/hdfeos5/structmetadata_eos.h5")
    }

    /*
netcdf structmetadata_eos {
  group: HDFEOS INFORMATION {
    variables:
      string StructMetadata.0 ;
  }
}
nc_inq_var return -101 = NetCDF: HDF error
*/

    @Test
    @Disabled
    fun compoundAttributeTest() {
        compareCdlWithClib("/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf4/compound-attribute-test.nc")
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
    fun compareCdlWithClib(filename: String) {
        println("=================")
        println(filename)
        val netchdf: Netcdf? = openNetchdfFile(filename)
        if (netchdf == null) {
            println("*** not a netchdf file = $filename")
            return
        }
        println("\nnetchdf = ${netchdf.cdl()}")

        val nclibfile: Netcdf = NetcdfClibFile(filename)
        //println("nclibfile = ${nclibfile.cdl()}")

        assertEquals(nclibfile.cdl(), netchdf.cdl())

        netchdf.close()
        nclibfile.close()
    }

    @Test
    fun testOneCdl() {
        compareCdlWithClib("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/tst_vlen_data.nc4")
    }

    @Test
    fun missingChunks() {
        readData(
            "/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf4/files/xma022032.nc",
            "/xma/dialoop_back"
        )
    }

    @Test
    fun hasMissing() {
        val filename =
            "/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf4/new/OR_ABI-L2-CMIPF-M6C13_G16_s20230451800207_e20230451809526_c20230451810015.nc"
        readData(filename, "CMI", Section(":, :"))
        readData(filename, "DQF", Section(":, :"))
    }

    @ParameterizedTest
    @MethodSource("params")
    fun readDataForProfiling(filename: String) {
        println(filename)
        readData(filename)
        println()
    }
}

fun readData(filename: String, varname: String? = null, section: Section? = null, showCdl : Boolean = false) {
    openNetchdfFile(filename).use { myfile ->
        if (myfile == null) {
            println("*** not a netchdf file = $filename")
            return
        }
        readData(myfile,varname, section, showCdl)
    }
}

fun readDataNc(filename: String, varname: String? = null, section: Section? = null, showCdl : Boolean = false) {
    NetcdfClibFile(filename).use { ncfile ->
        readData(ncfile, varname, section, showCdl)
    }
}

fun readData(myfile: Netcdf, varname: String? = null, section: Section? = null, showCdl : Boolean = false) {

    if (showCdl) {
        println(myfile.cdl())
    }
    // println(myfile.rootGroup().allVariables().map { it.fullname() })
    if (varname != null) {
        val myvar = myfile.rootGroup().allVariables().find { it.fullname() == varname }
        if (myvar == null) {
            println("cant find $varname")
            return
        }
        oneVar(myvar, myfile, section)
    } else {
        myfile.rootGroup().allVariables().forEachIndexed { idx, it ->
            oneVar(it, myfile, null)
        }
    }
}

const val maxBytes = 100_000_000

fun oneVar(myvar: Variable, h5file: Iosp, section: Section?) {

    val section = Section.fill(section, myvar.shape)
    val nbytes = section.size() * myvar.datatype.size
    if (nbytes > maxBytes) {
        println(" * ${myvar.fullname()} read too big: ${nbytes} > $maxBytes")
    } else {
        val mydata = h5file.readArrayData(myvar, section)
        println(" ${myvar.datatype} ${myvar.fullname()}${myvar.shape.contentToString()} = " +
                    "${mydata.shape.contentToString()} ${computeSize(mydata.shape)} elems" )
        if (myvar.datatype == Datatype.CHAR) {
            testCharShape(myvar.shape, mydata.shape)
        } else {
            assertTrue(myvar.shape.contentEquals(mydata.shape))
        }
        if (NetchdfTest.showData) println(mydata)
    }

    if (myvar.nelems > 8 && myvar.datatype != Datatype.CHAR) {
        testMiddleSection(h5file, myvar, myvar.shape)
    }
}

fun testCharShape(want: IntArray, got: IntArray) {
    val org = want.contentEquals(got)
    val removeLast = removeLast(want)
    val removeLastOk = removeLast.contentEquals(got)
    if (!org and !removeLastOk) {
        println("HEY")
    }
    assertTrue(org or removeLastOk)
}

fun removeLast(org: IntArray): IntArray {
    if (org.size < 1) return org
    return IntArray(org.size - 1) { org[it] }
}

fun testMiddleSection(myfile: Iosp, myvar: Variable, shape: IntArray) {
    val orgSection = Section(shape)
    val middleRanges = orgSection.ranges.map { range ->
        if (range == null) throw RuntimeException("Range is null")
        if (range.length < 9) range
        else Range(range.first + range.length / 3, range.last - range.length / 3)
    }
    val middleSection = Section(middleRanges)
    val nbytes = middleSection.size() * myvar.datatype.size
    if (nbytes > maxBytes) {
        println("  * ${myvar.fullname()}[${middleSection}] read too big: ${nbytes} > $maxBytes")
        testMiddleSection(myfile, myvar, middleSection.shape)
        return
    }

    val mydata = myfile.readArrayData(myvar, middleSection)
    println("  ${myvar.fullname()}[$middleSection] = ${mydata.shape.contentToString()} ${computeSize(mydata.shape)} elems")
    if (myvar.datatype == Datatype.CHAR) {
        testCharShape(middleSection.shape, mydata.shape)
    } else {
        assertTrue(middleSection.shape.contentEquals(mydata.shape))
    }
}
