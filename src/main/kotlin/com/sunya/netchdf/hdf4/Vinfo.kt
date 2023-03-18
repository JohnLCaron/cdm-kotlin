package com.sunya.netchdf.hdf4

import com.sunya.cdm.api.*
import com.sunya.cdm.iosp.OpenFileState
import com.sunya.netchdf.netcdf4.Netcdf4.Companion.NC_FILL_BYTE
import com.sunya.netchdf.netcdf4.Netcdf4.Companion.NC_FILL_DOUBLE
import com.sunya.netchdf.netcdf4.Netcdf4.Companion.NC_FILL_FLOAT
import com.sunya.netchdf.netcdf4.Netcdf4.Companion.NC_FILL_INT
import com.sunya.netchdf.netcdf4.Netcdf4.Companion.NC_FILL_SHORT
import com.sunya.netchdf.netcdf4.Netcdf4.Companion.NC_FILL_UBYTE
import com.sunya.netchdf.netcdf4.Netcdf4.Companion.NC_FILL_UINT
import com.sunya.netchdf.netcdf4.Netcdf4.Companion.NC_FILL_USHORT
import com.sunya.netchdf.netcdf4.Netcdf4.Companion.NC_FILL_INT64
import com.sunya.netchdf.netcdf4.Netcdf4.Companion.NC_FILL_UINT64
import java.nio.ByteOrder

internal class Vinfo(val refno: Int) : Comparable<Vinfo?> {
    var vb: Variable.Builder? = null
    val tags = mutableListOf<Tag>()

    // info about reading the data
    var tagDataRI: TagRasterImage? = null
    var tagData: TagData? = null
    var elemSize = 0 // for Structures, this is recsize
    var fillValue: Any? = null

    // below is not set until setLayoutInfo() is called
    var isLinked = false
    var isCompressed = false
    var isChunked = false
    var hasNoData = false

    // regular
    var start = -1L
    var length = 0

    // linked
    var segPos = LongArray(0)
    var segSize = IntArray(0)

    // chunked
    var chunks: List<SpecialDataChunk>? = null
    var chunkLengths = IntArray(0)

    // internal string valued
    var svalue : String? = null

    // LOOK
    var endian = ByteOrder.BIG_ENDIAN // LOOK TABLE 2H Little-Endian Format Data Type Definitions


    fun setVariable(v: Variable.Builder) {
        vb = v
        v.spObject = this
    }

    override fun compareTo(other: Vinfo?): Int {
        return Integer.compare(refno, other!!.refno)
    }

    fun setData(data: TagData?, elemSize: Int) {
        this.tagData = data
        this.elemSize = elemSize
        hasNoData = (data == null)
    }

    fun setFillValue(att: Attribute) {
        fillValue = att.values[0]
    }

    fun setSValue(svalue : String) : Vinfo {
        this.svalue = svalue
        return this
    }

    fun getFillValueOrDefault() : Any {
        return if (fillValue != null) fillValue!! else {
            when (vb!!.datatype) {
                Datatype.BYTE -> NC_FILL_BYTE
                Datatype.CHAR, Datatype.UBYTE -> NC_FILL_UBYTE
                Datatype.SHORT -> NC_FILL_SHORT
                Datatype.USHORT -> NC_FILL_USHORT
                Datatype.INT -> NC_FILL_INT
                Datatype.UINT -> NC_FILL_UINT
                Datatype.FLOAT -> NC_FILL_FLOAT
                Datatype.DOUBLE -> NC_FILL_DOUBLE
                Datatype.LONG -> NC_FILL_INT64
                Datatype.ULONG -> NC_FILL_UINT64
                else -> 0
            }
        }
    }

    // make sure needed info is present : call this when variable needs to be read
    // this allows us to defer getting layout info until then
    fun setLayoutInfo(h4file: Hdf4File) {
        if (tagData == null) return
        val useData = tagData!!
        if (null != useData.linked) {
            isLinked = true
            setDataBlocks(useData.linked!!.getLinkedDataBlocks(h4file.header), elemSize)

        } else if (null != useData.compress) {
            isCompressed = true
            val compData: TagData = useData.compress!!.getDataTag(h4file.header)
            tags.add(compData)
            isLinked = (compData.linked != null)
            if (isLinked) {
                setDataBlocks(compData.linked!!.getLinkedDataBlocks(h4file.header), elemSize)
            } else {
                start = compData.offset
                length = compData.length
                hasNoData = (start < 0) || (length < 0)
            }
        } else if (null != useData.chunked) {
            isChunked = true
            chunks = useData.chunked!!.getDataChunks(h4file)
            chunkLengths = useData.chunked!!.chunkLength
            isCompressed = useData.chunked!!.isCompressed
        } else {
            start = useData.offset
            hasNoData = (start < 0)
        }
    }

    private fun setDataBlocks(
        linkedBlocks: List<TagLinkedBlock>,
        elemSize: Int
    ) {
        val nsegs = linkedBlocks.size
        segPos = LongArray(nsegs)
        segSize = IntArray(nsegs)
        var count = 0
        for (tag: TagLinkedBlock in linkedBlocks) {
            segPos[count] = tag.offset
            segSize[count] = tag.length
            count++
        }
    }

    override fun toString(): String {
        return buildString {
            vb?.let { append("refno=%d name=%s fillValue=%s %n", refno, it.name, fillValue) }
            append(" isChunked=%s isCompressed=%s isLinked=%s hasNoData=%s %n", isChunked, isCompressed, isLinked, hasNoData)
            append(" elemSize=%d data start=%d length=%s %n%n", elemSize, start, length)
            for (t: Tag in tags) append(" %s%n", t.detail())
        }
    }
}