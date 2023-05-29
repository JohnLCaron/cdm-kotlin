package com.sunya.netchdf.hdf5Clib

import com.sunya.cdm.api.*
import com.sunya.cdm.array.*
import com.sunya.cdm.layout.MaxChunker
import com.sunya.netchdf.hdf5.Datatype5
import com.sunya.netchdf.hdf5Clib.ffm.hdf5_h
import com.sunya.netchdf.hdf5Clib.ffm.hdf5_h.C_DOUBLE
import com.sunya.netchdf.hdf5Clib.ffm.hdf5_h.C_FLOAT
import com.sunya.netchdf.hdf5Clib.ffm.hdf5_h_1.H5Fclose
import com.sunya.netchdf.hdf5Clib.ffm.hvl_t
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

    override fun <T> readArrayData(v2: Variable<T>, section: SectionPartial?): ArrayTyped<T> {
        return readArrayData(v2, SectionPartial.fill(section, v2.shape))
    }

    internal fun <T> readArrayData(v2: Variable<T>, fillSection: Section): ArrayTyped<T> {
        if (v2.spObject is Attribute<*>) {
            val att = v2.spObject as Attribute<*>
            return ArrayString(v2.shape.toIntArray(), att.values as List<String>) as ArrayTyped<T>
        }
        val vinfo = v2.spObject as Vinfo5C
        //    internal fun readRegularData(session : MemorySession, datasetId : Long, h5ctype : H5CTypeInfo, dims : IntArray) : ArrayTyped<*>
        MemorySession.openConfined().use { session ->
            return if (vinfo.h5ctype.isVlenString) {
                readVlenStrings(session, vinfo.datasetId, vinfo.h5ctype, fillSection) as ArrayTyped<T>
            } else if (vinfo.h5ctype.datatype5 == Datatype5.Vlen) {
                readVlens(session, vinfo.datasetId, vinfo.h5ctype, fillSection) as ArrayTyped<T>
            } else {
                readRegularData(session, vinfo.datasetId, vinfo.h5ctype, vinfo.h5ctype.datatype(), fillSection) as ArrayTyped<T>
            }
        }
    }

    internal fun readVlenStrings(session : MemorySession, datasetId : Long, h5ctype : H5CTypeInfo, want : Section) : ArrayString {
        val (memSpaceId, fileSpaceId) = makeSection(session, datasetId, h5ctype, want)
        val nelems = want.totalElements
        val strings_p: MemorySegment = session.allocateArray(ValueLayout.ADDRESS, nelems)
        checkErr("H5Dread VlenString",
            hdf5_h.H5Dread(datasetId, h5ctype.type_id, memSpaceId, fileSpaceId, hdf5_h.H5P_DEFAULT(), strings_p)
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
        return ArrayString(want.shape.toIntArray(), slist)
    }

    internal fun readVlens(session : MemorySession, datasetId : Long, h5ctype : H5CTypeInfo, want : Section) : ArrayVlen<*> {
        val (memSpaceId, fileSpaceId) = makeSection(session, datasetId, h5ctype, want)
        val nelems = want.totalElements
        val vlen_p: MemorySegment = hvl_t.allocateArray(nelems.toInt(), session)
        checkErr(
            "H5Dread VlenData",
            hdf5_h.H5Dread(datasetId, h5ctype.type_id, memSpaceId, fileSpaceId, hdf5_h.H5P_DEFAULT(), vlen_p)
        )
        val base = h5ctype.base!!
        val basetype = base.datatype()
        // each vlen pointer is the address of the vlen array of length arraySize
        val listOfVlen = mutableListOf<Array<*>>()
        for (elem in 0 until nelems) {
            val arraySize = hvl_t.`len$get`(vlen_p, elem).toInt()
            val address = hvl_t.`p$get`(vlen_p, elem)
            listOfVlen.add(readVlenArray(arraySize, address, basetype))
        }
        return ArrayVlen.fromArray(want.shape.toIntArray(), listOfVlen, basetype)
    }

    // TODO ENUMS seem to be wrong
    // also duplicate in H5Cbuilder ?
    private fun readVlenArray(arraySize : Int, address : MemoryAddress, datatype : Datatype<*>) : Array<*> {
        return when (datatype) {
            Datatype.FLOAT -> Array(arraySize) { idx -> address.getAtIndex(C_FLOAT, idx.toLong()) }
            Datatype.DOUBLE -> Array(arraySize) { idx -> address.getAtIndex(C_DOUBLE, idx.toLong()) }
            Datatype.BYTE, Datatype.UBYTE, Datatype.ENUM1 -> Array(arraySize) { idx -> address.get(ValueLayout.JAVA_BYTE, idx.toLong()) }
            Datatype.SHORT, Datatype.USHORT, Datatype.ENUM2 -> Array(arraySize) { idx -> address.getAtIndex(ValueLayout.JAVA_SHORT, idx.toLong()) }
            Datatype.INT,  Datatype.UINT, Datatype.ENUM4 -> Array(arraySize) { idx -> address.getAtIndex(ValueLayout.JAVA_INT, idx.toLong()) }
            Datatype.LONG, Datatype.ULONG -> Array(arraySize) { idx -> address.getAtIndex(ValueLayout.JAVA_LONG, idx.toLong()) }
            else -> throw IllegalArgumentException("unsupported datatype ${datatype}")
        }
    }

    override fun <T> chunkIterator(v2: Variable<T>, section: SectionPartial?, maxElements : Int?): Iterator<ArraySection<T>> {
        return H5CmaxIterator(v2, section, maxElements ?: 100_000)
    }

    private inner class H5CmaxIterator<T>(val v2: Variable<T>, section : SectionPartial?, maxElems: Int) : AbstractIterator<ArraySection<T>>() {
        private val debugChunking = false
        val filled = SectionPartial.fill(section, v2.shape)
        private val maxIterator  = MaxChunker(maxElems,  filled)

        override fun computeNext() {
            if (maxIterator.hasNext()) {
                val indexSection = maxIterator.next()
                if (debugChunking) println("  chunk=${indexSection}")

                val section = indexSection.section(v2.shape)
                val array = readArrayData(v2, section)
                setNext(ArraySection(array, section))
            } else {
                done()
            }
        }
    }
}

internal data class Vinfo5C(val datasetId : Long, val h5ctype : H5CTypeInfo)