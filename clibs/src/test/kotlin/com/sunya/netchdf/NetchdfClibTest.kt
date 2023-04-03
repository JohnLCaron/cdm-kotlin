package com.sunya.netchdf

import com.google.common.util.concurrent.AtomicDouble
import com.sunya.cdm.api.*
import com.sunya.cdm.api.Section.Companion.computeSize
import com.sunya.cdm.api.Section.Companion.equivalent
import com.sunya.cdm.array.*
import com.sunya.cdm.util.Stats
import com.sunya.cdm.util.nearlyEquals
import com.sunya.netchdf.hdf4.Hdf4File
import com.sunya.netchdf.hdf4Clib.Hdf4ClibFile
import com.sunya.netchdf.netcdf4.openNetchdfFile
import com.sunya.netchdf.netcdfClib.NetcdfClibFile
import com.sunya.testdata.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.*
import java.util.stream.Stream
import kotlin.system.measureNanoTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Compare Netchdf against NetcdfClibFile / H4ClibFile
class NetchdfTest {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            return Stream.of( N3Files.params(),  N4Files.params(), H4Files.params(), H5Files.params()).flatMap { i -> i };
        }

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            Stats.clear() // problem with concurrent tests
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            Stats.show()
        }

        var showDataRead = true
        var showData = false
        var showFailedData = true
        var showCdl = false
    }

    /* tst_grps
        Relies on vlen-3 and vlen-4 mdt hash NOT matching v1-2 hash, so added again.

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
    }
     */
    @Test
    @Disabled
    fun tst_grps() {
        compareCdlWithClib(testData + "devcdm/netcdf4/tst_grps.nc4")
    }

    /* testNestedStructure
    Relies on field1 and fields2 mdt hash matching s1_t hash, so not added again.
    no obvious reason why mdts are not shared.
    <netcdf testNestedStructure {
      types:
        compound s1_t {
          int x ;
          int y ;
        }; // s1_t
        compound s2_t {
          s1_t field1 ;
          s1_t field2 ;
        }; // s2_t
      variables:
        s2_t x ;
    }
     */
    @Test
    fun testNestedStructure() {
        compareCdlWithClib(testData + "cdmUnitTest/formats/netcdf4/testNestedStructure.nc")
    }

    @Test
    @Disabled
    fun hdfeos() {
        compareCdlWithClib(testData + "devcdm/hdfeos5/structmetadata_eos.h5")
    }

    /*structmetadata_eos.h5
netcdf structmetadata_eos {
  group: HDFEOS INFORMATION {
    variables:
      string StructMetadata.0 ;
  }
}
nc_inq_var return -101 = NetCDF: HDF error
*/


    // possible bug in netcdf4
    @Test
    @Disabled
    fun compoundAttributeTest() {
        compareCdlWithClib(testData + "cdmUnitTest/formats/netcdf4/compound-attribute-test.nc")
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

    @Test
    fun testOneCdl() {
        compareCdlWithClib(testData + "netchdf/joleenf/IASI_20120229022657Z.atm_prof_rtv.h5")
    }

    @Test
    fun missingChunks() {
        readNetchdfData(
            testData + "cdmUnitTest/formats/netcdf4/files/xma022032.nc",
            "/xma/dialoop_back"
        )
    }

    @Test
    fun hasMissing() {
        val filename =
            testData + "cdmUnitTest/formats/netcdf4/new/OR_ABI-L2-CMIPF-M6C13_G16_s20230451800207_e20230451809526_c20230451810015.nc"
        readNetchdfData(filename, "CMI", Section(":, :"))
        readNetchdfData(filename, "DQF", Section(":, :"))
    }

    @Test
    fun readIterateCompareNC() {
        val filename = testData + "cdmUnitTest/formats/netcdf4/UpperDeschutes_t4p10_swemelt.nc"
        // showNetchdfHeader(filename, null)
        compareIterateWithClib(testData + "cdmUnitTest/formats/netcdf4/UpperDeschutes_t4p10_swemelt.nc", "UpperDeschutes_t4p10_swemelt")
        // compareIterateWithClib(testData + "cdmUnitTest/formats/netcdf4/new/OR_ABI-L2-CMIPF-M6C13_G16_s20230451800207_e20230451809526_c20230451810015.nc", "CMI")
    }

    @Test
    fun testGoes16() {
        val filename = testData + "recent/goes16/OR_ABI-L2-CMIPF-M6C13_G16_s20230451800207_e20230451809526_c20230451810015.nc"
        compareCdlWithClib(filename)
        compareDataWithClib(filename)
        compareIterateWithClib(filename)
    }

//////////////////////////////////////////////////////////////////////////////////////////////////////

    @ParameterizedTest
    @MethodSource("params")
    fun checkVersion(filename: String) {
        openNetchdfFile(filename).use { ncfile ->
            if (ncfile == null) {
                println("Not a netchdf file=$filename ")
                return
            }
            println("${ncfile.type()} $filename ")
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    fun testShowNetchdfHeader(filename: String) {
        showNetchdfHeader(filename, null)
    }


    @ParameterizedTest
    @MethodSource("params")
    fun testCdlWithClib(filename: String) {
        compareCdlWithClib(filename)
    }

    @ParameterizedTest
    @MethodSource("params")
    fun testReadNetchdfData(filename: String) {
        readNetchdfData(filename)
    }

    @ParameterizedTest
    @MethodSource("params")
    fun testCompareDataWithClib(filename: String) {
        compareDataWithClib(filename)
    }

    @ParameterizedTest
    @MethodSource("params")
    fun testIterateWithClib(filename: String) {
        compareIterateWithClib(filename)
    }

}

//////////////////////////////////////////////////////////////////////////////////////////////////////

fun showNetchdfHeader(filename: String, varname: String? = null, section: Section? = null, showCdl : Boolean = false) {
    println(filename)
    openNetchdfFile(filename).use { myfile ->
        if (myfile == null) {
            println("*** not a netchdf file = $filename")
            return
        }
        println(myfile.cdl())
    }
}

fun showNcHeader(filename: String, varname: String? = null, section: Section? = null, showCdl : Boolean = false) {
    println(filename)
    NetcdfClibFile(filename).use { ncfile ->
        println(ncfile.cdl())
    }
}

fun readNetchdfData(filename: String, varname: String? = null, section: Section? = null, showCdl : Boolean = false) {
    // println("=============================================================")
    openNetchdfFile(filename).use { myfile ->
        if (myfile == null) {
            println("*** not a netchdf file = $filename")
            return
        }
        println("--- ${myfile.type()} $filename ")
        readMyData(myfile,varname, section, showCdl)
    }
}

fun readNcData(filename: String, varname: String? = null, section: Section? = null, showCdl : Boolean = false) {
    NetcdfClibFile(filename).use { ncfile ->
        readMyData(ncfile, varname, section, showCdl)
    }
}

fun compareCdlWithClib(filename: String) {
    println("=================")
    openNetchdfFile(filename, true).use { netchdf ->
        if (netchdf == null) {
            println("*** not a netchdf file = $filename")
            return
        }
        println("${netchdf.type()} $filename ")
        println("\nnetchdf = ${netchdf.cdl()}")

        if (netchdf.type().contains("hdf4") || netchdf.type().contains("hdf-eos2")) {
            Hdf4ClibFile(filename).use { hcfile ->
                assertEquals(hcfile.cdl(), netchdf.cdl())
            }
        } else if (netchdf.type().contains("netcdf")) {
            NetcdfClibFile(filename).use { ncfile ->
                assertEquals(ncfile.cdl(), netchdf.cdl())
            }
        }
    }
}

fun compareDataWithClib(filename: String, varname: String? = null, section: Section? = null) {
    println("=============================================================")
    openNetchdfFile(filename).use { netchdf ->
        if (netchdf == null) {
            println("*** not a netchdf file = $filename")
            return
        }
        println("${netchdf.type()} $filename ${"%.2f".format(netchdf.size / 1000.0 / 1000.0)} Mbytes")
        if (NetchdfTest.showCdl) println("\n${netchdf.cdl()}")

        if (netchdf.type().contains("hdf4")) {
            Hdf4ClibFile(filename).use { ncfile ->
                compareNetcdfData(netchdf, ncfile, varname, section)
            }
        } else if (netchdf.type().contains("netcdf")) {
            NetcdfClibFile(filename).use { ncfile ->
                compareNetcdfData(netchdf, ncfile, varname, section)
            }
        }
    }
}

fun compareIterateWithClib(filename: String, varname: String? = null, section: Section? = null) {
    println("=============================================================")
    openNetchdfFile(filename).use { netchdf ->
        if (netchdf == null) {
            println("*** not a netchdf file = $filename")
            return
        }
        println("${netchdf.type()} $filename ${"%.2f".format(netchdf.size / 1000.0 / 1000.0)} Mbytes")
        if (NetchdfTest.showCdl) println("\n${netchdf.cdl()}")

        if (netchdf.type().contains("hdf4")) {
            Hdf4ClibFile(filename).use { ncfile ->
                compareIterateNetchdf(netchdf, ncfile, varname, section) // LOOK should be compareIterateWithHC
            }
        } else if (netchdf.type().contains("netcdf")) {
            NetcdfClibFile(filename).use { ncfile ->
                compareIterateNetchdf(netchdf, ncfile, varname, section)
            }
        }
    }
}

//////////////////////////////////////////////////////////////////////////////////////////////////////
// just read data from myfile

fun readMyData(myfile: Netchdf, varname: String? = null, section: Section? = null, showCdl : Boolean = false) {

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
        readOneVar(myvar, myfile, section)
    } else {
        myfile.rootGroup().allVariables().forEach { it ->
            readOneVar(it, myfile, null)
        }
    }
}

const val maxBytes = 100_000_000

fun readOneVar(myvar: Variable, myfile: Netchdf, section: Section?) {

    val section = Section.fill(section, myvar.shape)
    val nbytes = section.size() * myvar.datatype.size
    if (nbytes > maxBytes) {
        if (NetchdfTest.showDataRead) println(" * ${myvar.fullname()} read too big: ${nbytes} > $maxBytes")
    } else {
        val mydata = myfile.readArrayData(myvar, section)
        if (NetchdfTest.showDataRead) println(" ${myvar.datatype} ${myvar.fullname()}${myvar.shape.contentToString()} = " +
                    "${mydata.shape.contentToString()} ${computeSize(mydata.shape)} elems" )
        if (myvar.datatype == Datatype.CHAR) {
            testCharShape(myvar.shape, mydata.shape)
        } else {
            assertTrue(myvar.shape.equivalent(mydata.shape), "variable ${myvar.name}")
        }
        if (NetchdfTest.showData) println(mydata)
    }

    if (myvar.nelems > 8 && myvar.datatype != Datatype.CHAR) {
        readMiddleSection(myfile, myvar, myvar.shape)
    }
}

fun testCharShape(want: IntArray, got: IntArray) {
    val org = want.equivalent(got)
    val removeLast = removeLast(want)
    val removeLastOk = removeLast.equivalent(got)
    assertTrue(org or removeLastOk)
}

fun removeLast(org: IntArray): IntArray {
    if (org.size < 1) return org
    return IntArray(org.size - 1) { org[it] }
}

fun readMiddleSection(myfile: Netchdf, myvar: Variable, shape: IntArray) {
    val orgSection = Section(shape)
    val middleRanges = orgSection.ranges.map { range ->
        if (range == null) throw RuntimeException("Range is null")
        if (range.length < 9) range
        else Range(range.first + range.length / 3, range.last - range.length / 3)
    }
    val middleSection = Section(middleRanges)
    val nbytes = middleSection.size() * myvar.datatype.size
    if (nbytes > maxBytes) {
        if (NetchdfTest.showDataRead) println("  * ${myvar.fullname()}[${middleSection}] read too big: ${nbytes} > $maxBytes")
        readMiddleSection(myfile, myvar, middleSection.shape)
        return
    }

    val mydata = myfile.readArrayData(myvar, middleSection)
    if (NetchdfTest.showDataRead) println("  ${myvar.fullname()}[$middleSection] = ${mydata.shape.contentToString()} ${computeSize(mydata.shape)} elems")
    if (myvar.datatype == Datatype.CHAR) {
        testCharShape(middleSection.shape, mydata.shape)
    } else {
        assertTrue(middleSection.shape.equivalent(mydata.shape), "variable ${myvar.name}")
    }
    if (NetchdfTest.showData) println(mydata)
}

//////////////////////////////////////////////////////////////////////////////////////
// compare data from two Netchdf files

fun compareNetcdfData(myfile: Netchdf, ncfile: Netchdf, varname: String?, section: Section? = null) {
    if (varname != null) {
        val myvar = myfile.rootGroup().allVariables().find { it.fullname() == varname }
        if (myvar == null) {
            println(" *** cant find myvar $varname")
            return
        }
        val ncvar = ncfile.rootGroup().allVariables().find { it.fullname() == myvar.fullname() }
        if (ncvar == null) {
            throw RuntimeException(" *** cant find ncvar $varname")
        }
        compareOneVar(myvar, myfile, ncvar, ncfile, section)
    } else {
        myfile.rootGroup().allVariables().forEach { myvar ->
            val ncvar = ncfile.rootGroup().allVariables().find { it.fullname() == myvar.fullname() }
            if (ncvar == null) {
                println(" *** cant find ${myvar.fullname()} in ncfile")
            } else {
                compareOneVar(myvar, myfile, ncvar, ncfile, null)
            }
        }

        ncfile.rootGroup().allVariables().forEach { ncvar ->
            val myvar = myfile.rootGroup().allVariables().find { it.fullname() == ncvar.fullname() }
            if (myvar == null) {
                println(" *** cant find ${ncvar.fullname()} in myfile")
            }
        }
    }
}

fun compareOneVar(myvar: Variable, myfile: Netchdf, ncvar : Variable, ncfile: Netchdf, section: Section?) {
    val filledSection = Section.fill(section, myvar.shape)
    val nbytes = filledSection.size() * myvar.datatype.size
    if (nbytes > 100_000_000) {
        println(" * ${myvar.fullname()} read too big = ${nbytes}")
    } else {
        val mydata = myfile.readArrayData(myvar, filledSection)
        val ncdata = try {
            ncfile.readArrayData(ncvar, filledSection)
        } catch (e : Exception) {
            println(" *** FAIL ncfile.readArrayData for variable = ${ncvar.datatype} ${ncvar.fullname()} ${ncvar.dimensions.map { it.name }}")
            throw e
        }
        println(" ${myvar.datatype} ${myvar.fullname()}[${filledSection}] = ${computeSize(mydata.shape)} elems" )

        if (myvar.datatype == Datatype.CHAR) {
            compareCharData(myvar.fullname(), mydata, ncdata)
        } else {
            if (!ncdata.equals(mydata)) {
                println(" *** FAIL comparing data for variable = ${ncvar.datatype} ${ncvar.fullname()} ${ncvar.dimensions.map { it.name }}")
                if (NetchdfTest.showFailedData) {
                    println("\n mydata = $mydata")
                    println(" ncdata = $ncdata")
                } else {
                    println("\n countDifferences = ${ArrayTyped.countDiff(ncdata, mydata)}")
                }
                assertTrue(false, "variable ${myvar.name}")
                return
            } else {
                if (NetchdfTest.showData) {
                    print(" ${ncvar.cdl()}, ")
                    print("\n mydata = $mydata")
                    print(" ncdata = $ncdata")
                }
            }
        }
    }
    if (ncvar.nelems > 8 && ncvar.datatype != Datatype.CHAR) {
        compareMiddleSection(myfile, myvar, ncfile, ncvar, ncvar.shape)
    }
}

fun compareMiddleSection(myfile: Netchdf, myvar: Variable, ncfile: Netchdf, ncvar: Variable, shape : IntArray) {
    val orgSection = Section(shape)
    val middleRanges = orgSection.ranges.map { range ->
        if (range == null) throw RuntimeException("Range is null")
        if (range.length < 9) range
        else Range(range.first + range.length / 3, range.last - range.length / 3)
    }
    val middleSection = Section(middleRanges)
    val nbytes = middleSection.size() * myvar.datatype.size
    if (nbytes > 100_000_000) {
        println("  * ${myvar.fullname()} read too big = ${nbytes}")
        compareMiddleSection(myfile, myvar, ncfile, ncvar, middleSection.shape)
        return
    }

    val mydata = myfile.readArrayData(myvar, middleSection)
    val ncdata = try {
        ncfile.readArrayData(ncvar, middleSection)
    } catch (e: Exception) {
        println(" *** FAIL compareMiddleSection data for variable = ${ncvar.datatype} ${ncvar.fullname()} ${ncvar.dimensions.map { it.name }}")
        throw e
    }
    println("  ${myvar.fullname()}[$middleSection] = ${mydata.shape.contentToString()} ${Section.computeSize(mydata.shape)} elems")

    if (myvar.datatype == Datatype.CHAR) {
        compareCharData(myvar.fullname(), mydata, ncdata)
    } else {
        if (!ncdata.equals(mydata)) {
            println(" *** FAIL comparing middle section variable = ${ncvar}")
            if (NetchdfTest.showFailedData) {
                println(" mydata = $mydata")
                println(" ncdata = $ncdata")
            } else {
                println("\n countDifferences = ${ArrayTyped.countDiff(ncdata, mydata)}")
            }
            assertTrue(false, "variable ${myvar.name}")
            return
        }
    }
}
fun compareCharData(name : String, mydata: ArrayTyped<*>, ncdata: ArrayTyped<*>) {
    if (!ArrayTyped.valuesEqual(ncdata, mydata)) {
        println("   *** FAIL comparing char variable = ${name}")
        print("   ncdata = $ncdata")
        print("   mydata = $mydata")
    }
}

//////////////////////////////////////////////////////////////////////////////////////
// compare reading data chunkIterate API with two Netchdf

fun compareIterateNetchdf(myfile: Netchdf, ncfile: Netchdf, varname: String?, section: Section? = null) {
    if (varname != null) {
        val myvar = myfile.rootGroup().allVariables().find { it.fullname() == varname }
        if (myvar == null) {
            println(" *** cant find myvar $varname")
            return
        }
        val ncvar = ncfile.rootGroup().allVariables().find { it.fullname() == myvar.fullname() }
        if (ncvar == null) {
            throw RuntimeException(" *** cant find ncvar $varname")
        }
        compareOneVarIterate(myvar, myfile, ncvar, ncfile, section)
    } else {
        myfile.rootGroup().allVariables().forEach { myvar ->
            val ncvar = ncfile.rootGroup().allVariables().find { it.fullname() == myvar.fullname() }
            if (ncvar == null) {
                println(" *** cant find ${myvar.fullname()} in ncfile")
            } else {
                compareOneVarIterate(myvar, myfile, ncvar, ncfile, null)
            }
        }
    }
}

fun compareOneVarIterate(myvar: Variable, myfile: Netchdf, ncvar : Variable, ncfile: Netchdf, section: Section?) {
    val sum = AtomicDouble()
    sum.set(0.0)
    var countChunks = 0
    val time1 = measureNanoTime {
        val chunkIter = myfile.chunkIterator(myvar)
        for (pair in chunkIter) {
            // println(" ${pair.section} = ${pair.array.shape.contentToString()}")
            sumValues(pair.array)
            countChunks++
        }
    }
    Stats.of("netchdf", myfile.location(), "chunk").accum(time1, countChunks)
    val sum1 = sum.get()

    sum.set(0.0)
    countChunks = 0
    val time2 = measureNanoTime {
        val chunkIter = ncfile.chunkIterator(ncvar)
        for (pair in chunkIter) {
            // println(" ${pair.section} = ${pair.array.shape.contentToString()}")
            sumValues(pair.array)
            countChunks++
        }
    }
    Stats.of("nclib", ncfile.location(), "chunk").accum(time2, countChunks)
    val sum2 = sum.get()

    if (sum1.isFinite() && sum2.isFinite()) {
        assertTrue(nearlyEquals(sum1, sum2), "$sum1 != $sum2 sum2")
        // println("sum = $sum1")
    }
}

///////////////////////////////////////////////////////////
val sum = AtomicDouble()
fun sumValues(array : ArrayTyped<*>) {
    if (array is ArraySingle) {
        return // fillValue the same ??
    }
    // cant cast unsigned to Numbers
    val useArray = when (array.datatype) {
        Datatype.UBYTE -> ArrayByte(array.shape, (array as ArrayUByte).values)
        Datatype.USHORT -> ArrayShort(array.shape, (array as ArrayUShort).values)
        Datatype.UINT -> ArrayInt(array.shape, (array as ArrayUInt).values)
        Datatype.ULONG -> ArrayLong(array.shape, (array as ArrayULong).values)
        else -> array
    }

    if (useArray.datatype.isNumber) {
        for (value in useArray) {
            val number = (value as Number)
            val numberd: Double = number.toDouble()
            if (numberd.isFinite()) {
                sum.getAndAdd(numberd)
            }
        }
    }
}
