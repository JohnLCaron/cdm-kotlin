package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.*
import com.sunya.cdm.api.Section.Companion.computeSize
import com.sunya.netchdf.netcdfClib.NetcdfClibFile
import org.junit.jupiter.api.Test

// Test chunked data reading
class H5dataTiming {
    val reversed = "/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf4/Ike.egl3.SWI.tidal.nc"
    val chunked = "/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf4/e562p1_fp.inst3_3d_asm_Nv.20100907_00z+20100909_1200z.nc4"
    val chunked2 = "/media/snake/0B681ADF0B681ADF1/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf4/UpperDeschutes_t4p10_swemelt.nc"

    @Test
    fun chunkedReverse() {
        //  variable eta[191, 242, 589], storageDims = [191, 242, 1, 4]
        readData(reversed, "eta", Section("0:9, 0:9, 0:9"))
        readData(reversed, "fxx", Section("0:99, 0:9, 0:9"))
        readData(reversed, "fyy", Section("0:99, 0:99, 0:9"))
        readData(reversed, "pxx", Section("0:99, 0:99, 0:99"))
        // readData(filename, "pyy", Section("0:99, 0:99, :"))
    }

    @Test
    fun chunked() {
        //  variable EPV[1, 72, 721, 1152], Chunked dims=[1, 1, 91, 144, 4]
        readData(chunked, "EPV", Section("0, 0:9, 0:9, 0:9"))
        readData(chunked, "O3", Section("0, 0:9, 0:99, 0:9"))
        readData(chunked, "H", Section("0, 0:9, 0:99, 0:99"))
        readData(chunked, "RH", Section("0, 0:9, 0:99, 0:999"))
    }

    @Test
    fun chunked2() {
        //  variable UpperDeschutes_t4p10_swemelt[8395, 781, 385], Chunked dims=[1, 30, 30, 8]
        // readData(chunked2, "UpperDeschutes_t4p10_swemelt", Section("0:9, :, :"))
        readData(chunked2, "UpperDeschutes_t4p10_swemelt", Section("0:100, 0:30, 0:40"))
        // readData(chunked2, "UpperDeschutes_t4p10_swemelt", Section("0:1000, 0:30, 0:40"))
    }

    fun readData(filename: String, varname: String, readSection : Section) {
        readNcdata(filename, varname, readSection)
        readH5data(filename, varname, readSection)
    }

    fun readH5data(filename: String, varname: String, readSection : Section) {
        Hdf5File(filename).use { h5file ->
            // println(h5file.cdl())
            val myvar = h5file.rootGroup().variables.find { it.name == varname }

            val starting = System.currentTimeMillis()
            val mydata = h5file.readArrayData(myvar!!, readSection)
            val took = (System.currentTimeMillis() - starting) / 1000.0

            val size = computeSize(mydata.shape)
            val tookPer = size / took
            println(" readH5data[$readSection] took $took secs; read values/sec = ${"%.0f".format(tookPer)} size=$size")
        }
    }

    fun readNcdata(filename: String, varname: String, readSection : Section) {
        NetcdfClibFile(filename).use { h5file ->
            // println(h5file.cdl())
            val myvar = h5file.rootGroup().variables.find { it.name == varname }

            val starting = System.currentTimeMillis()
            val mydata = h5file.readArrayData(myvar!!, readSection)
            val took = (System.currentTimeMillis() - starting) / 1000.0

            val size = computeSize(mydata.shape)
            val tookPer = size / took
            println(" readNcdata[$readSection] took $took secs; read values/sec = ${"%.0f".format(tookPer)} size=$size")
        }
    }
}
