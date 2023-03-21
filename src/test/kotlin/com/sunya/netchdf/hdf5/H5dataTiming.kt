package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.*
import com.sunya.cdm.api.Section.Companion.computeSize
import com.sunya.netchdf.netcdf4.openNetchdfFile
import com.sunya.netchdf.netcdfClib.NetcdfClibFile
import org.junit.jupiter.api.Test
import test.util.testData
import kotlin.system.measureNanoTime

private const val showDetail = true

/* Time data reading
3/18/23
    nclib/h5new, h5new/h5old (should now equal 1)
    0.2679, 4.1996, eta[0:9,0:9,0:9]
    1.6565, 1.0501, fxx[0:99,0:9,0:9]
    1.4914, 1.6512, fyy[0:99,0:99,0:9]
    0.7003, 0.8649, pxx[0:99,0:99,0:99]
    1.8484, 1.0879, pyy[0:99,0:99,:]
    0.5656, 0.8638, eta[:,:,11:20]
    0.1171, 1.3866, CMI[:,:]
    1.0043, 2.0313, DQF[:,:]
    0.4928, 1.1373, UpperDeschutes_t4p10_swemelt[0:100,0:30,0:40]
    0.3837, 0.9939, fyy[0:99,0:99,0:9]
    1.1360, 3.8823, EPV[0:0,0:9,0:9,0:9]
    1.0826, 2.0128, O3[0:0,0:9,0:99,0:9]
    1.5274, 1.0071, H[0:0,0:9,0:99,0:99]
    1.5564, 1.1484, RH[0:0,0:9,0:99,0:999]
    1.5534, 0.9903, EPV[:,:,:,11:20]
    1.2087, 1.1326, UpperDeschutes_t4p10_swemelt[0:9,:,:]
    2.1526, 1.0946, UpperDeschutes_t4p10_swemelt[0:100,0:30,0:40]
    2.2494, 0.7369, UpperDeschutes_t4p10_swemelt[0:1000,0:30,0:40]
    0.8358, 1.0735, UpperDeschutes_t4p10_swemelt[0:1000,0:30,:]
    1.1458, 0.9503, UpperDeschutes_t4p10_swemelt[:,17:17,44:55]
    0.0950, 2.5175, uw[0:4,0:39,0:55,0:74]
    0.9646, 4.1978, uw[0:4,13:26,18:37,25:49]
    0.0741, 1.0043, vw[0:4,0:39,0:55,0:74]
    1.2154, 0.7166, vw[0:4,13:26,18:37,25:49]
    3.1355, 1.0944, uw[:,:,:,25:25]
 */
class H5dataTiming {
    val reversed = testData + "cdmUnitTest/formats/netcdf4/Ike.egl3.SWI.tidal.nc"
    val chunked = testData + "cdmUnitTest/formats/netcdf4/e562p1_fp.inst3_3d_asm_Nv.20100907_00z+20100909_1200z.nc4"
    val chunked2 = testData + "cdmUnitTest/formats/netcdf4/UpperDeschutes_t4p10_swemelt.nc"

    @Test
    fun chunkedReverse() {
        if (showDetail) println("===============================================")
        if (showDetail) println("chunkedReverse [191, 242, 1, 4]/[191, 242, 589]")
        //  variable eta[191, 242, 589], storageDims = [191, 242, 1, 4]
        readData(reversed, "eta", Section("0:9, 0:9, 0:9"))
        readData(reversed, "fxx", Section("0:99, 0:9, 0:9"))
        readData(reversed, "fyy", Section("0:99, 0:99, 0:9"))
        readData(reversed, "pxx", Section("0:99, 0:99, 0:99"))
        readData(reversed, "pyy", Section("0:99, 0:99, :"))
        readData(reversed, "eta", Section(":, :, 11:20"))
    }

    @Test
    fun chunked() {
        if (showDetail) println("===============================================")
        if (showDetail) println("chunked [1, 1, 91, 144, 4]/[1, 72, 721, 1152]")
        //  variable EPV[1, 72, 721, 1152], Chunked dims=[1, 1, 91, 144, 4]
        readData(chunked, "EPV", Section("0, 0:9, 0:9, 0:9"))
        readData(chunked, "O3", Section("0, 0:9, 0:99, 0:9"))
        readData(chunked, "H", Section("0, 0:9, 0:99, 0:99"))
        readData(chunked, "RH", Section("0, 0:9, 0:99, 0:999"))
        readData(chunked, "EPV", Section(":, :, :, 11:20"))
    }

    @Test
    fun chunkedFiltered() {
        if (showDetail) println("===============================================")
        if (showDetail) println("chunkedFiltered [1, 30, 30, 8]/[8395, 781, 385]")
        //  variable UpperDeschutes_t4p10_swemelt[8395, 781, 385], Chunked dims=[1, 30, 30, 8]
        readData(chunked2, "UpperDeschutes_t4p10_swemelt", Section("0:9, :, :"))
        readData(chunked2, "UpperDeschutes_t4p10_swemelt", Section("0:100, 0:30, 0:40"))
        readData(chunked2, "UpperDeschutes_t4p10_swemelt", Section("0:1000, 0:30, 0:40"))
        readData(chunked2, "UpperDeschutes_t4p10_swemelt", Section("0:1000, 0:30, :"))
        readData(chunked2, "UpperDeschutes_t4p10_swemelt", Section(":, 17, 44:55"))
    }

    @Test
    fun netcdf3() {
        if (showDetail) println("===============================================")
        if (showDetail) println("netcdf3 [5, 40, 56, 75]")
        val filename = testData + "cdmUnitTest/formats/netcdf3/awips.nc"
        readData(filename, "uw", Section(intArrayOf(5, 40, 56, 75)))
        readData(filename, "uw", Section("0:4,13:26,18:37,25:49"))
        readData(filename, "vw", Section(intArrayOf(5, 40, 56, 75)))
        readData(filename, "vw", Section("0:4,13:26,18:37,25:49"))
        readData(filename, "uw", Section(":,:,:,25"))
    }

    @Test
    fun problem() {
        readData(reversed, "fyy", Section("0:99, 0:99, 0:9"))
    }

    @Test
    fun problem2() {
        //  variable UpperDeschutes_t4p10_swemelt[8395, 781, 385], Chunked dims=[1, 30, 30, 8]
        readData(chunked2, "UpperDeschutes_t4p10_swemelt", Section("0:100, 0:30, 0:40"))
    }

    @Test
    fun hasMissing() {
        val filename = testData + "cdmUnitTest/formats/netcdf4/new/OR_ABI-L2-CMIPF-M6C13_G16_s20230451800207_e20230451809526_c20230451810015.nc"
        readData(filename, "CMI", Section(":, :"))
        readData(filename, "DQF", Section(":, :"))
    }

    fun readData(filename: String, varname: String, readSection : Section) {
        if (showDetail) println("$varname in $filename ")
        val h5old = readNetchData(filename, varname, readSection, true)
        val nclib = readNcdata(filename, varname, readSection)
        val h5new = readNetchData(filename, varname, readSection, false)
        print("${"%.4f".format(nclib/h5new)}")
        //print(", ${"%.4f".format(nclib/h5old)}")
        print(", ${"%.4f".format(h5new/h5old)}")
        println(", $varname[$readSection]")
        if (showDetail) println()
    }

    fun readNetchData(filename: String, varname: String, readSection : Section, useOld : Boolean): Double {
        openNetchdfFile(filename)!!.use { h5file ->
            // println(h5file.cdl())
            val myvar = h5file.rootGroup().variables.find { it.name == varname }

            var size : Long = 0L
            val elapsed = measureNanoTime {
                val mydata = h5file.readArrayData(myvar!!, readSection)
                size = computeSize(mydata.shape)
            }.toDouble() * 1.0e-9

            val tookPer = size / elapsed
            val what = if (useOld) "old" else "new"
            if (showDetail) println(" $what readNetchdf[$readSection] took ${"%.4f".format(elapsed)} secs; read values/sec = ${"%.0f".format(tookPer)} size=$size")
            return tookPer
        }
    }

    fun readNcdata(filename: String, varname: String, readSection : Section) : Double {
        NetcdfClibFile(filename).use { h5file ->
            // println(h5file.cdl())
            val myvar = h5file.rootGroup().variables.find { it.name == varname }

            var size : Long = 0L
            val elapsed = measureNanoTime {
                val mydata = h5file.readArrayData(myvar!!, readSection)
                size = computeSize(mydata.shape)
            }.toDouble() * 1.0e-9

            val tookPer = size / elapsed
            if (showDetail) println(" readNcdata[$readSection] took ${"%.4f".format(elapsed)} secs; read values/sec = ${"%.0f".format(tookPer)} size=$size")
            return tookPer
        }
    }
}
