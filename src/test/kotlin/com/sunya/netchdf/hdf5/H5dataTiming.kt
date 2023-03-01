package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.*
import com.sunya.cdm.api.Section.Companion.computeSize
import com.sunya.netchdf.netcdf4.openNetchdfFile
import com.sunya.netchdf.netcdfClib.NetcdfClibFile
import org.junit.jupiter.api.Test
import kotlin.system.measureNanoTime

// Time data reading
class H5dataTiming {
    val reversed = "/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf4/Ike.egl3.SWI.tidal.nc"
    val chunked = "/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf4/e562p1_fp.inst3_3d_asm_Nv.20100907_00z+20100909_1200z.nc4"
    val chunked2 = "/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf4/UpperDeschutes_t4p10_swemelt.nc"

    @Test
    fun chunkedReverse() {
        println("===============================================")
        println("chunkedReverse [191, 242, 1, 4]/[191, 242, 589]")
        //  variable eta[191, 242, 589], storageDims = [191, 242, 1, 4]
        readData(reversed, "eta", Section("0:9, 0:9, 0:9"))
        readData(reversed, "fxx", Section("0:99, 0:9, 0:9"))
        readData(reversed, "fyy", Section("0:99, 0:99, 0:9"))
        readData(reversed, "pxx", Section("0:99, 0:99, 0:99"))
        readData(reversed, "pyy", Section("0:99, 0:99, :"))
    }

    @Test
    fun chunked() {
        println("===============================================")
        println("chunked [1, 1, 91, 144, 4]/[1, 72, 721, 1152]")
        //  variable EPV[1, 72, 721, 1152], Chunked dims=[1, 1, 91, 144, 4]
        readData(chunked, "EPV", Section("0, 0:9, 0:9, 0:9"))
        readData(chunked, "O3", Section("0, 0:9, 0:99, 0:9"))
        readData(chunked, "H", Section("0, 0:9, 0:99, 0:99"))
        readData(chunked, "RH", Section("0, 0:9, 0:99, 0:999"))
    }

    @Test
    fun chunkedFiltered() {
        println("===============================================")
        println("chunkedFiltered [1, 30, 30, 8]/[8395, 781, 385]")
        //  variable UpperDeschutes_t4p10_swemelt[8395, 781, 385], Chunked dims=[1, 30, 30, 8]
        readData(chunked2, "UpperDeschutes_t4p10_swemelt", Section("0:9, :, :"))
        readData(chunked2, "UpperDeschutes_t4p10_swemelt", Section("0:100, 0:30, 0:40"))
        readData(chunked2, "UpperDeschutes_t4p10_swemelt", Section("0:1000, 0:30, 0:40"))
        readData(chunked2, "UpperDeschutes_t4p10_swemelt", Section("0:1000, 0:30, :"))
    }

    @Test
    fun netcdf3() {
        println("===============================================")
        println("netcdf3 [5, 40, 56, 75]")
        val filename = "/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf3/awips.nc"
        readData(filename, "uw", Section(intArrayOf(5, 40, 56, 75)))
        readData(filename, "uw", Section("0:4,13:26,18:37,25:49"))
        readData(filename, "vw", Section(intArrayOf(5, 40, 56, 75)))
        readData(filename, "vw", Section("0:4,13:26,18:37,25:49"))
    }

    /*
    pyy in /media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf4/Ike.egl3.SWI.tidal.nc
     readNcdata[0:99,0:99,:] took 0.1724 secs; read values/sec = 34170861 size=5890000
     new readH5data[0:99,0:99,:] took 69.7259 secs; read values/sec = 84474 size=5890000
     old readH5data[0:99,0:99,:] took 0.3582 secs; read values/sec = 16442110 size=5890000 // LOOK cache??
     read values/sec nclib/h5new = 404.5150, nclib/h5old = 2.0783, h5new/h5old = 0.0051

    pyy in /media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf4/Ike.egl3.SWI.tidal.nc
     readNcdata[0:99,0:99,:] took 0.2159 secs; read values/sec = 27279576 size=5890000
     new readNetchdf[0:99,0:99,:] took 68.7298 secs; read values/sec = 85698 size=5890000
     old readNetchdf[0:99,0:99,:] took 146.2897 secs; read values/sec = 40263 size=5890000
     read values/sec nclib/new = 318.3227, nclib/old = 677.5417, new/old = 2.1285
     */
    @Test
    fun problem() {
        //  variable UpperDeschutes_t4p10_swemelt[8395, 781, 385], Chunked dims=[1, 30, 30, 8]
        readData(chunked2, "UpperDeschutes_t4p10_swemelt", Section("0:9, :, :"))
    }

    @Test
    fun problem2() {
        //  variable UpperDeschutes_t4p10_swemelt[8395, 781, 385], Chunked dims=[1, 30, 30, 8]
        readData(chunked2, "UpperDeschutes_t4p10_swemelt", Section("0:100, 0:30, 0:40"))
    }

    fun readData(filename: String, varname: String, readSection : Section) {
        println("$varname in $filename ")
        //val h5old = readNetchData(filename, varname, readSection, true)
        val h5new = readNetchData(filename, varname, readSection, false)
        val nclib = readNcdata(filename, varname, readSection)
        println("read values/sec nclib/new = ${"%.4f".format(nclib/h5new)}")
        //print(", nclib/old = ${"%.4f".format(nclib/h5old)}")
        //println(", new/old = ${"%.4f".format(h5new/h5old)}")
        println()
    }

    fun readNetchData(filename: String, varname: String, readSection : Section, useOld : Boolean): Double {
        openNetchdfFile(filename)!!.use { h5file ->
            // println(h5file.cdl())
            val myvar = h5file.rootGroup().variables.find { it.name == varname }

            var size : Long = 0L
            val elapsed = measureNanoTime {
                Hdf5File.useOld = useOld
                val mydata = h5file.readArrayData(myvar!!, readSection)
                size = computeSize(mydata.shape)
                Hdf5File.useOld = false
            }.toDouble() * 1.0e-9

            val tookPer = size / elapsed
            val what = if (useOld) "old" else "new"
            println(" $what readNetchdf[$readSection] took ${"%.4f".format(elapsed)} secs; read values/sec = ${"%.0f".format(tookPer)} size=$size")
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
            println(" readNcdata[$readSection] took ${"%.4f".format(elapsed)} secs; read values/sec = ${"%.0f".format(tookPer)} size=$size")
            return tookPer
        }
    }
}
