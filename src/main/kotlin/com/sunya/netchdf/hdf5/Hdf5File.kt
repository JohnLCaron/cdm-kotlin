package com.sunya.netchdf.hdf5

import com.google.common.base.Preconditions
import com.sunya.cdm.api.Netcdf
import com.sunya.cdm.api.Section
import com.sunya.cdm.api.Variable
import com.sunya.cdm.array.ArraySingle
import com.sunya.cdm.array.ArrayString
import com.sunya.cdm.array.ArrayTyped
import com.sunya.cdm.iosp.*
import java.io.IOException
import java.nio.ByteBuffer

/**
 * @param strict true = make it agree with nclib if possible
 */
class Hdf5File(val filename : String, strict : Boolean = true) : Iosp, Netcdf {
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
    override fun cdl(strict : Boolean) = com.sunya.cdm.api.cdl(this, strict)
    override fun type() = header.formatType()

    @Throws(IOException::class)
    override fun readArrayData(v2: Variable, section: Section?): ArrayTyped<*> {
        val wantSection = Section.fill(section, v2.shape)

        val vinfo = v2.spObject as DataContainerVariable
        if (vinfo.onlyFillValue) { // fill value only, no data
            return ArraySingle(wantSection.shape, v2.datatype, vinfo.fillValue)
        }

        try {
            if (vinfo.mfp != null) { // filtered
                Preconditions.checkArgument(vinfo.isChunked)
                if (vinfo.h5type.isVString) {
                    val layout = H5tiledLayoutBB(header, v2, wantSection, vinfo.mfp.filters, vinfo.h5type.endian)
                    return readFilteredStringData(layout, wantSection)
                } else {
                    val result = if (useOld) {
                        val layout = H5tiledLayoutBB(header, v2, wantSection, vinfo.mfp.filters, vinfo.h5type.endian)
                        header.readFilteredChunkedData(vinfo, layout, wantSection)
                    } else header.readChunkedDataNew(v2, wantSection)
                    return result
                }
            }

            if (vinfo.isChunked) {
                val result = if (useOld) {
                    val layout = H5tiledLayout(header, v2, wantSection, v2.datatype) // eager read
                    header.readChunkedData(vinfo, layout, wantSection)
                } else header.readChunkedDataNew(v2, wantSection)
                return result
            } else {
                return header.readRegularData(vinfo, wantSection)
            }
        } catch (ex: Exception) {
            println("failed to read ${v2.name}, $ex")
            throw ex
        }
    }

    @Throws(IOException::class)
    private fun readFilteredStringData(layout: H5tiledLayoutBB, wantSection : Section): ArrayString {
        val sa = mutableListOf<String>()
        val h5heap = H5heap(header)
        while (layout.hasNext()) {
            val chunk = layout.next()
            val bb: ByteBuffer = chunk.byteBuffer!!
            var destPos = chunk.destElem().toInt()
            for (i in 0 until chunk.nelems()) { // 16 byte "heap ids"
                // TODO does this handle section correctly ??
                val s = h5heap.readHeapString(bb, (chunk.srcElem().toInt() + i) * 16)
                sa.add(s!!)
            }
        }
        return ArrayString(wantSection.shape, sa)
    }

    companion object {
        var useOld = false
        var checkBoth = false
    }


}