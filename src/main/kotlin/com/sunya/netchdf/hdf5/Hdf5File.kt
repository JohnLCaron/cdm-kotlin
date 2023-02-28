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

    @Throws(IOException::class)
    override fun readArrayData(v2: Variable, section: Section?): ArrayTyped<*> {
        // LOOK do we need to adjust wantSection for vinfo.storageDims? If so, we we need to adjust back ??
        val wantSection = Section.fill(section, v2.shape)

        val vinfo = v2.spObject as DataContainerVariable
        if (vinfo.useFillValue) { // fill value only, no  data
            return ArraySingle(wantSection.shape, v2.datatype, vinfo.fillValue)
        }

        if (vinfo.mfp != null) { // filtered
            Preconditions.checkArgument(vinfo.isChunked)
            val layout = H5tiledLayoutBB(header, v2, wantSection, vinfo.mfp.filters, vinfo.h5type.endian)
            if (vinfo.h5type.isVString) {
                return readFilteredStringData(layout, wantSection)
            } else {
                val useData = if (useOld) header.readFilteredChunkedData(vinfo, layout, wantSection)
                    else header.readChunkedData(v2, wantSection)

                // val data1 =  header.readFilteredChunkedData(vinfo, layout, wantSection)
                //val same = ArrayTyped.contentEquals(data1, data2)
                //println("${v2.name} same = $same")
                return useData
            }
        }

        try {
            if (vinfo.isChunked) {
                val layout = H5tiledLayout(header, v2, wantSection, v2.datatype)
                val useData = if (useOld) header.readChunkedData(vinfo, layout, wantSection)
                else header.readChunkedData(v2, wantSection)

                //val data1 = header.readChunkedData(vinfo, layout, wantSection)
                // val data2 = header.readChunkedData(v2, wantSection)
                //val same = ArrayTyped.contentEquals(data1, data2)
                //println("${v2.name} same = $same")
                return useData
            } else {
                return header.readRegularData(vinfo, wantSection)
            }
        } catch (ex : Exception) {
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