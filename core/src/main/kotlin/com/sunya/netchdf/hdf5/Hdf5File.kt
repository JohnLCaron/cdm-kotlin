package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.ArraySection
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
class Hdf5File(val filename : String, strict : Boolean = false) : Netchdf {
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
    override val size : Long get() = raf.size

    @Throws(IOException::class)
    override fun readArrayData(v2: Variable, section: Section?): ArrayTyped<*> {
        val wantSection = Section.fill(section, v2.shape)

        // promoted attributes
        if (v2.spObject is DataContainerAttribute) {
            return header.readRegularData(v2.spObject, section)
        }

        val vinfo = v2.spObject as DataContainerVariable
        if (vinfo.onlyFillValue) { // fill value only, no data
            return ArraySingle(wantSection.shape, v2.datatype, vinfo.fillValue)
        }

        try {
             if (vinfo.isChunked) {
                return H5chunkReader(header).readChunkedData(v2, wantSection)
             } else if (vinfo.isCompact) {
                 return header.readCompactData(vinfo, wantSection)
             } else {
                return header.readRegularData(vinfo, wantSection)
            }
        } catch (ex: Exception) {
            println("failed to read ${v2.name}, $ex")
            throw ex
        }
    }

    @Throws(IOException::class)
    override fun chunkIterator(v2: Variable, section: Section?, maxElements : Int?) : Iterator<ArraySection> {
        val wantSection = Section.fill(section, v2.shape)

        val vinfo = v2.spObject as DataContainerVariable
        if (vinfo.onlyFillValue) { // fill value only, no data
            val single = ArraySection(ArraySingle(wantSection.shape, v2.datatype, vinfo.fillValue), wantSection)
            return listOf(single).iterator()
        }

        try {
            if (vinfo.isChunked) {
                return H5chunkIterator(header, v2, wantSection)
            } else {
                return H5maxIterator(header, v2, wantSection, maxElements ?: 100_000)
            }
        } catch (ex: Exception) {
            println("failed to read ${v2.name}, $ex")
            throw ex
        }
    }


}