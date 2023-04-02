package com.sunya.netchdf

import com.google.common.util.concurrent.AtomicDouble
import com.sunya.cdm.api.*
import com.sunya.cdm.api.Section.Companion.computeSize
import com.sunya.cdm.api.Section.Companion.equivalent
import com.sunya.cdm.array.*
import com.sunya.cdm.util.Stats
import com.sunya.cdm.util.nearlyEquals
import com.sunya.netchdf.netcdf4.openNetchdfFile
import com.sunya.testdata.N3Files
import com.sunya.testdata.N4Files
import com.sunya.testdata.testData
import com.sunya.testdata.testFilesIn
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


// Test files opened and read through openNetchdfFile().
class NetchdfTest {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            return Stream.of( N3Files.params(),  N4Files.params()).flatMap { i -> i };
        }

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            Stats.clear() // problem with concurrent tests
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            if (versions.size > 0) {
                versions.keys.forEach{ println("$it = ${versions[it]!!.size } files") }
            }
            Stats.show()
        }

        private val versions = mutableMapOf<String, MutableList<String>>()

        var showDataRead = true
        var showData = false
        var showFailedData = false
        var showCdl = false
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
    fun problemNetchIterate() {
        readNetchIterate(testData + "cdmUnitTest/formats/netcdf4/UpperDeschutes_t4p10_swemelt.nc", "UpperDeschutes_t4p10_swemelt")
        // readNetchIterate(testData + "cdmUnitTest/formats/netcdf4/files/xma022032.nc", "/xma/dialoop_back")
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
            val paths = versions.getOrPut(ncfile.type()) { mutableListOf() }
            paths.add(filename)
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    fun testShowNetchdfHeader(filename: String) {
        showNetchdfHeader(filename, null)
    }

    @ParameterizedTest
    @MethodSource("params")
    fun testReadNetchdfData(filename: String) {
        readNetchdfData(filename)
    }

    @ParameterizedTest
    @MethodSource("params")
    fun testReadNetchIterate(filename: String) {
        readNetchIterate(filename)
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
    if (nbytes > maxBytes) {
        println(" * ${myvar.fullname()} read too big = ${nbytes}")
    } else {
        val mydata = myfile.readArrayData(myvar, filledSection)
        val ncdata = ncfile.readArrayData(ncvar, filledSection)
        println(" ${myvar.datatype} ${myvar.fullname()}[${filledSection}] = ${Section.computeSize(mydata.shape)} elems" )

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
    if (nbytes > maxBytes) {
        println("  * ${myvar.fullname()} read too big = ${nbytes}")
        compareMiddleSection(myfile, myvar, ncfile, ncvar, middleSection.shape)
        return
    }

    val mydata = myfile.readArrayData(myvar, middleSection)
    val ncdata = ncfile.readArrayData(ncvar, middleSection)
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
// compare reading data regular and through the chunkIterate API

fun readNetchIterate(filename: String, varname : String? = null, compare : Boolean = true) {
    openNetchdfFile(filename).use { myfile ->
        if (myfile == null) {
            println("*** not a netchdf file = $filename")
            return
        }
        println("${myfile.type()} $filename ${"%.2f".format(myfile.size / 1000.0 / 1000.0)} Mbytes")
        var countChunks = 0
        if (varname != null) {
            val myvar = myfile.rootGroup().allVariables().find { it.fullname() == varname } ?: throw RuntimeException("cant find $varname")
            countChunks +=  compareOneVarIterate(myfile, myvar, compare)
        } else {
            myfile.rootGroup().allVariables().forEach { it ->
                countChunks += compareOneVarIterate(myfile, it, compare)
            }
        }
        if (countChunks > 0) {
            println("${myfile.type()} $filename ${"%.2f".format(myfile.size / 1000.0 / 1000.0)} Mbytes chunks = $countChunks")
        }
    }
}

fun compareOneVarIterate(myFile: Netchdf, myvar: Variable, compare : Boolean = true) : Int {
    val filename = myFile.location().substringAfterLast('/')
    val varBytes = myvar.nelems
    if (varBytes >= maxBytes) {
        println(" *** ${myvar.nameAndShape()} cant readArrayData too many bytes= $varBytes")
        return 0
    }

    val sum1 = AtomicDouble()
    val sumArrayData = if (compare) {
        val time3 = measureNanoTime {
            val arrayData = myFile.readArrayData(myvar, null)
            sumValues(arrayData, sum1)
        }
        Stats.of("readArrayData", filename, "chunk").accum(time3, 1)
        sum1.get()
    } else 0.0

    val sum2 = AtomicDouble()
    var countChunks = 0
    val time1 = measureNanoTime {
        val chunkIter = myFile.chunkIterator(myvar)
        for (pair in chunkIter) {
            // println(" ${pair.section} = ${pair.array.shape.contentToString()}")
            sumValues(pair.array, sum2)
            countChunks++
        }
    }
    val sumChunkIterator = sum2.get()
    if (compare) Stats.of("chunkIterator", filename, "chunk").accum(time1, countChunks)

    if (compare && sumChunkIterator.isFinite() && sumArrayData.isFinite()) {
        // println("  sumChunkIterator = $sumChunkIterator for ${myvar.nameAndShape()}")
        assertTrue(nearlyEquals(sumArrayData, sumChunkIterator), "chunkIterator $sumChunkIterator != $sumArrayData sumArrayData")
    }
    return countChunks
}

//////////////////////////////////////////////////////////////////////////////////////
// compare reading data chunkIterate API with Netch and NC

fun compareIterateWithNC(myfile: Netchdf, ncfile: Netchdf, varname: String?, section: Section? = null) {
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
    var countChunks = 0
    val time1 = measureNanoTime {
        val chunkIter = myfile.chunkIterator(myvar)
        for (pair in chunkIter) {
            // println(" ${pair.section} = ${pair.array.shape.contentToString()}")
            sumValues(pair.array, sum)
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
            sumValues(pair.array, sum)
            countChunks++
        }
    }
    Stats.of("nclib", ncfile.location(), "chunk").accum(time2, countChunks)
    val sum2 = sum.get()

    if (sum1.isFinite() && sum2.isFinite()) {
        assertTrue(nearlyEquals(sum1, sum2), "$sum1 != $sum2 sum2")
        println("sum = $sum1")
    }
}

///////////////////////////////////////////////////////////
fun sumValues(array : ArrayTyped<*>, sum : AtomicDouble) {
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
