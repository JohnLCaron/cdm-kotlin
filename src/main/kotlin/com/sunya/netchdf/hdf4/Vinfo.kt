package com.sunya.netchdf.hdf4

import com.sunya.cdm.api.*
import com.sunya.cdm.iosp.OpenFileState

internal class Vinfo(val refno: Short) : Comparable<Vinfo?> {
    var vb: Variable.Builder? = null
    var group: Group.Builder? = null
    val tags: MutableList<Tag> =
        ArrayList<Tag>()

    // info about reading the data
    var data: TagData? = null
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
    var chunkSize = IntArray(0)

    fun setVariable(v: Variable.Builder) {
        vb = v
        v.spObject = this
    }

    override fun compareTo(other: Vinfo?): Int {
        return java.lang.Short.compare(refno, other!!.refno)
    }

    fun setData(data: TagData?, elemSize: Int) {
        this.data = data
        this.elemSize = elemSize
        hasNoData = (data == null)
    }

    fun setFillValue(att: Attribute) {
        // see IospHelper.makePrimitiveArray(int size, DataType dataType, Object fillValue)
        fillValue = if ((vb!!.datatype === Datatype.STRING)) {
            att.values[0] as String
        } else {
            att.values[0] as Number
        }
    }

    // make sure needed info is present : call this when variable needs to be read
    // this allows us to defer getting layout info until then
    fun setLayoutInfo(h4: H4builder, ncfile: Netcdf) {
        if (data == null) return
        val useData = data!!
        if (null != useData.linked) {
            isLinked = true
            useData.linked!!.getLinkedDataBlocks(h4)?.let { setDataBlocks(it, elemSize) }

        } else if (null != useData.compress) {
            isCompressed = true
            val compData: TagData = useData.compress!!.getDataTag(h4)
            tags.add(compData)
            isLinked = (compData.linked != null)
            if (isLinked) {
                compData.linked!!.getLinkedDataBlocks(h4)?.let { setDataBlocks(it, elemSize) }
            } else {
                start = compData.offset
                length = compData.length
                hasNoData = (start < 0) || (length < 0)
            }
        } else if (null != useData.chunked) {
            isChunked = true
            chunks = useData.chunked!!.getDataChunks(h4, ncfile)
            chunkSize = useData.chunked!!.chunk_length
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
            segPos[count] = tag.offset.toLong()
            segSize[count] = tag.length
            count++
        }
    }

    fun readChunks(h4 : H4builder, ncfile : Netcdf): List<SpecialDataChunk> {
        return data?.chunked?.getDataChunks(h4, ncfile) ?: emptyList()
    }

    fun read(h4 : H4builder): String {
        requireNotNull(data)
        val state = OpenFileState(data!!.offset)
        return h4.raf.readString(state, data!!.length, h4.valueCharset)
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