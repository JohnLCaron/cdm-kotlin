package com.sunya.netchdf.hdf5

import com.google.common.util.concurrent.AtomicDouble
import com.sunya.cdm.api.Netchdf
import com.sunya.cdm.api.Variable
import com.sunya.cdm.api.chunkConcurrent
import com.sunya.cdm.array.ArrayTyped
import com.sunya.cdm.util.Stats
import com.sunya.netchdf.readNetchIterate
import com.sunya.netchdf.readNetchdfData
import com.sunya.testdata.H5Files
import com.sunya.testdata.testData
import com.sunya.testdata.testFilesIn

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.system.measureNanoTime

// Sanity check read Hdf5File header, for non-netcdf4 files
class H5readTest {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            return H5Files.params()
        }

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            Stats.clear()
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            Stats.show()
        }
    }

    @Test
    fun superblockIsOffsetNPP() {
        testOpenH5(testData + "cdmUnitTest/formats/hdf5/superblockIsOffsetNPP.h5")
    }

    @Test
    fun hasLinkName() {
        testOpenH5(testData + "cdmUnitTest/formats/hdf5/aura/MLS-Aura_L2GP-BrO_v01-52-c01_2007d029.he5")
    }

    // a compound with a member thats a type thats not a seperate typedef.
    // the obvious thing to do is to be able to add a typedef when processing the member.
    // or look for it when building H5group
    @Test
    fun compoundEnumTypedef() {
        testOpenH5(testData + "devcdm/hdf5/enumcmpnd.h5")
    }

    @Test
    fun opaqueAttribute() {
        testOpenH5(testData + "devcdm/netcdf4/tst_opaque_data.nc4")
    }

    @Test
    fun groupHasCycle() {
        testOpenH5(testData + "cdmUnitTest/formats/hdf5/groupHasCycle.h5")
    }

    @Test
    fun timeIterateConcurrent() {
        // readH5(testData + "devcdm/hdf5/zip.h5", "/Data/Compressed_Data")
        readH5concurrent(testData + "cdmUnitTest/formats/hdf5/StringsWFilter.h5", "/observation/matrix/data")
    }

    @Test
    fun timeIterateProblem() {
        readNetchIterate(testData + "cdmUnitTest/formats/hdf5/xmdf/mesh_datasets.h5", "/2DMeshModule/mesh/Datasets/velocity_(64)/Mins")
    }

    @Test
    fun testGoes16() {
        testOpenH5(testData + "recent/goes16/OR_ABI-L2-CMIPF-M6C13_G16_s20230451800207_e20230451809526_c20230451810015.nc")
    }

    @Test
    fun testEos() {
        testOpenH5(testData + "cdmUnitTest/formats/hdf5/aura/MLS-Aura_L2GP-BrO_v01-52-c01_2007d029.he5")
    }

    // I think the npp put the structmetadata in the front of the file?
    @Test
    fun testNpp() {
        testOpenH5(testData + "cdmUnitTest/formats/hdf5/npoess/ExampleFiles/GATRO-SATMR_npp_d20020906_t0409572_e0410270_b19646_c20090720223122943227_devl_int.h5")
    }

    ///////////////////////////////////////////////////////////////////////////////////

    @ParameterizedTest
    @MethodSource("params")
    fun testOpenH5(filename: String) {
        openH5(filename, null)
    }

    @ParameterizedTest
    @MethodSource("params")
    fun testReadNetchdfData(filename: String) {
        readNetchdfData(filename)
    }

    @ParameterizedTest
    @MethodSource("params")
    fun testReadIterate(filename: String) {
        readNetchIterate(filename, null)
    }

    @ParameterizedTest
    @MethodSource("params")
    fun testReadConcurrent(filename: String) {
        readH5concurrent(filename, null)
    }

    /////////////////////////////////////////////////////////

    fun openH5(filename: String, varname : String? = null) {
        println("=================")
        println(filename)
        Hdf5File(filename).use { h5file ->
            println(h5file.cdl())
            h5file.rootGroup().allVariables().forEach { println("  ${it.fullname()}") }

            if (varname != null) {
                val h5var = h5file.rootGroup().allVariables().find { it.fullname() == varname } ?: throw RuntimeException("cant find $varname")
                val h5data = h5file.readArrayData(h5var)
                println(" $varname = $h5data")
            }
        }
    }

    fun readH5concurrent(filename: String, varname : String? = null) {
        Hdf5File(filename).use { myfile ->
            println("${myfile.type()} $filename ${"%.2f".format(myfile.size / 1000.0 / 1000.0)} Mbytes")
            var countChunks = 0
            if (varname != null) {
                val myvar = myfile.rootGroup().allVariables().find { it.fullname() == varname } ?: throw RuntimeException("cant find $varname")
                countChunks +=  testOneVarConcurrent(myfile, myvar)
            } else {
                myfile.rootGroup().allVariables().forEach { it ->
                    if (it.datatype.isNumber) {
                        countChunks += testOneVarConcurrent(myfile, it)
                    }
                }
            }
            if (countChunks > 0) {
                println("${myfile.type()} $filename ${"%.2f".format(myfile.size / 1000.0 / 1000.0)} Mbytes chunks = $countChunks")
            }
        }
    }

    fun testOneVarConcurrent(myFile: Netchdf, myvar: Variable) : Int {
        val filename = myFile.location().substringAfterLast('/')
        sum = AtomicDouble()
        var countChunks = 0
        val time1 = measureNanoTime {
            val chunkIter = myFile.chunkIterator(myvar)
            if (chunkIter == null) {
                return 0
            }
            for (pair in chunkIter) {
                // println(" ${pair.section} = ${pair.array.shape.contentToString()}")
                    sumValues(pair.array)
                countChunks++
            }
        }
        val sum1 = sum.get()
        Stats.of("serialSum", filename, "chunk").accum(time1, countChunks)

        sum.set(0.0)
        val time2 = measureNanoTime {
            myFile.chunkConcurrent(myvar, null) { sumValues(it.array) }
        }
        val sum2 = sum.get()
        Stats.of("concurrentSum", filename, "chunk").accum(time2, countChunks)

        sum.set(0.0)
        val time3 = measureNanoTime {
            val arrayData = myFile.readArrayData(myvar, null)
                sumValues(arrayData)
        }
        val sum3 = sum.get()
        Stats.of("regularSum", filename, "chunk").accum(time3, countChunks)

        /* if (sum1.isFinite() && sum2.isFinite() && sum3.isFinite()) {
            assertTrue(nearlyEquals(sum1, sum2), "$sum1 != $sum2 sum2")
            assertTrue(nearlyEquals(sum1, sum3), "$sum1 != $sum3 sum3")
        }

         */
        return countChunks
    }

    var sum = AtomicDouble()
    fun sumValues(array : ArrayTyped<*>) {
        if (!array.datatype.isNumber or true) return
        for (value in array) {
            val number = (value as Number)
            val numberd : Double = number.toDouble()
            if (numberd.isFinite()) {
                sum.getAndAdd(numberd)
            }
        }
    }

}