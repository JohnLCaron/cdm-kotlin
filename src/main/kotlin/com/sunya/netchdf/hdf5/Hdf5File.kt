package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.Netchdf
import com.sunya.cdm.api.Section
import com.sunya.cdm.api.Variable
import com.sunya.cdm.array.ArraySingle
import com.sunya.cdm.array.ArrayTyped
import com.sunya.cdm.iosp.*
import java.io.IOException

/**
 * @param strict true = make it agree with nclib if possible
 */
class Hdf5File(val filename : String, strict : Boolean = false) : Iosp, Netchdf {
    private val raf : OpenFile = OpenFile(filename)
    private val header : H5builder

    init {
        header = H5builder(raf, strict)
    }

    override fun close() {
        raf.close()
    }

    override fun rootGroup() = header.cdmRoot
    override fun location() = filename
    override fun cdl() = com.sunya.cdm.api.cdl(this)
    override fun type() = header.formatType()

    @Throws(IOException::class)
    override fun readArrayData(v2: Variable, section: Section?): ArrayTyped<*> {
        val wantSection = Section.fill(section, v2.shape)

        val vinfo = v2.spObject as DataContainerVariable
        if (vinfo.onlyFillValue) { // fill value only, no data
            return ArraySingle(wantSection.shape, v2.datatype, vinfo.fillValue)
        }

        try {
             if (vinfo.isChunked) {
                return H5chunkReader(header).readChunkedDataNew(v2, wantSection)
            } else {
                return header.readRegularData(vinfo, wantSection)
            }
        } catch (ex: Exception) {
            println("failed to read ${v2.name}, $ex")
            throw ex
        }
    }

    companion object {
        var useOld = false
        var checkBoth = false
    }

}