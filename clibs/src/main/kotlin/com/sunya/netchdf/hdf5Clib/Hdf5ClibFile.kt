package com.sunya.netchdf.hdf5Clib

import com.sunya.cdm.api.*
import com.sunya.cdm.array.*
import com.sunya.netchdf.hdf5Clib.ffm.hdf5_h
import com.sunya.netchdf.hdf5Clib.ffm.hdf5_h_1.H5Fclose
import java.lang.foreign.MemoryAddress
import java.lang.foreign.MemorySegment
import java.lang.foreign.MemorySession
import java.lang.foreign.ValueLayout

class Hdf5ClibFile(val filename: String) : Netchdf {
    private val header = H5Cbuilder(filename)
    private val rootGroup: Group = header.rootBuilder.build(null)

    override fun rootGroup() = rootGroup
    override fun location() = filename
    override fun cdl() = cdl(this)

    override fun type() = header.formatType()

    override fun close() {
        val status = H5Fclose(header.file_id)
    }

    override fun readArrayData(v2: Variable, section: Section?): ArrayTyped<*> {
        if (v2.spObject is Attribute) {
            val att = v2.spObject as Attribute
            return ArrayString(v2.shape, att.values as List<String>)
        }
        val vinfo = v2.spObject as Vinfo5C
        //    internal fun readRegularData(session : MemorySession, datasetId : Long, h5ctype : H5CTypeInfo, dims : IntArray) : ArrayTyped<*>
        MemorySession.openConfined().use { session ->
            return if (vinfo.h5ctype.isVlenString) {
                readVlenStrings(session, vinfo.datasetId, vinfo.h5ctype, v2.shape)
            } else {
                readRegularData(session, vinfo.datasetId, vinfo.h5ctype, v2.shape)
            }
        }
    }

    internal fun readVlenStrings(session : MemorySession, datasetId : Long, h5ctype : H5CTypeInfo, shape : IntArray) : ArrayString {
        val nelems = shape.computeSize().toLong()
        val strings_p: MemorySegment = session.allocateArray(ValueLayout.ADDRESS, nelems)
        checkErr("H5Dread VlenString",
            hdf5_h.H5Dread(datasetId, h5ctype.type_id, hdf5_h.H5S_ALL(), hdf5_h.H5S_ALL(), hdf5_h.H5P_DEFAULT(), strings_p)
        )

        val slist = mutableListOf<String>()
        for (i in 0 until nelems) {
            val s2: MemoryAddress = strings_p.getAtIndex(ValueLayout.ADDRESS, i)
            if (s2 != MemoryAddress.NULL) {
                val value = s2.getUtf8String(0)
                // val tvalue = transcodeString(value)
                slist.add(value)
            } else {
                slist.add("")
            }
        }
        // not sure about this
        // checkErr("H5Dvlen_reclaim", H5Dvlen_reclaim(attr_id, h5ctype.type_id, H5S_ALL(), strings_p)) // ??
        return ArrayString(shape, slist)
    }

    override fun chunkIterator(v2: Variable, section: Section?, maxElements: Int?): Iterator<ArraySection> {
        TODO("Not yet implemented")
    }
}

internal data class Vinfo5C(val datasetId : Long, val h5ctype : H5CTypeInfo)