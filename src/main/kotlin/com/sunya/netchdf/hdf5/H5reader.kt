package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.*
import com.sunya.cdm.api.Section.Companion.computeSize
import com.sunya.cdm.array.*
import com.sunya.cdm.iosp.*
import com.sunya.cdm.layout.Chunker
import com.sunya.cdm.layout.IndexSpace
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

private val debugChunkingDetail = false
private val debugChunking = false

// Handles non-filtered chunked layout Variables (eg contiguous, maybe compact)
internal fun H5builder.readChunkedData(vinfo: DataContainerVariable, layout : Layout, section : Section): ArrayTyped<*> {
    if (vinfo.mds.type == DataspaceType.Null) {
        return ArrayString(intArrayOf(), listOf())
    }
    val h5type = vinfo.h5type

    if (h5type.hdfType == Datatype5.Vlen) {
        return readVlenData(vinfo, layout, section)
    }
    if (h5type.hdfType == Datatype5.Compound) {
        return readCompoundData(vinfo, layout, section)
    }

    val readDtype: Datatype = h5type.datatype(this)
    val endian: ByteOrder = h5type.endian

    val state = OpenFileState(0, endian) // pos set by layout
    val dataArray = readNonHeapData(state, layout, readDtype, section.shape, h5type)

    // convert attributes to enum strings
    if (h5type.hdfType == Datatype5.Enumerated) {
        // hopefully this is shared and not replicated
        val enumMsg = vinfo.mdt as DatatypeEnum
        return dataArray.convertEnums(enumMsg.valuesMap)
    }

    return dataArray
}

// Handles non-filtered chunked layout Variables (eg contiguous, maybe compact)
// old way, use this to see what nj5 probably is doing
internal fun H5builder.readFilteredChunkedData(vinfo: DataContainerVariable, layout : H5tiledLayoutBB, section : Section): ArrayTyped<*> {
    if (vinfo.mds.type == DataspaceType.Null) {
        return ArrayString(intArrayOf(), listOf())
    }
    val h5type = vinfo.h5type

    if (h5type.hdfType == Datatype5.Vlen) {
        return readVlenData(vinfo, layout, section)
    }
    if (h5type.hdfType == Datatype5.Compound) {
        return readCompoundData(vinfo, layout, section)
    }

    val endian: ByteOrder = h5type.endian
    val readDtype: Datatype = h5type.datatype(this)

    val state = OpenFileState(0, endian) // pos set by layout
    val dataArray = readFilteredBBData(state, layout, readDtype, section.shape, h5type)

    // convert attributes to enum strings
    if (h5type.hdfType == Datatype5.Enumerated) {
        // hopefully this is shared and not replicated
        val enumMsg = vinfo.mdt as DatatypeEnum
        return dataArray.convertEnums(enumMsg.valuesMap)
    }

    return dataArray
}

@Throws(IOException::class)
internal fun H5builder.readFilteredBBData(state: OpenFileState, layout: H5tiledLayoutBB, datatype: Datatype, shape : IntArray, h5type : H5TypeInfo): ArrayTyped<*> {
    val sizeBytes = layout.totalNelems * layout.elemSize
    if (sizeBytes <= 0 || sizeBytes >= Integer.MAX_VALUE) {
        throw java.lang.RuntimeException("Illegal nbytes to read = $sizeBytes")
    }
    var count = 0
    val bb = ByteBuffer.allocate(sizeBytes.toInt())
    bb.order(state.byteOrder)
    // the layout handles moving around in the file, adding the filter and giving back the finished results as a byte buffer
    while (layout.hasNext()) {
        val chunk : LayoutBB.Chunk = layout.next()
        val chunkBB: ByteBuffer = chunk.byteBuffer
        val srcElem = layout.elemSize * chunk.srcElem()
        chunkBB.position(srcElem.toInt())
        var pos = layout.elemSize * chunk.destElem().toInt()
        val nelems = layout.elemSize * chunk.nelems()
        for (i in 0 until nelems) {
            bb.put(pos, chunkBB.get())
            pos++
        }
        count++
        if (debugChunkingDetail and (count < 20)) println("read at ${chunk.srcElem()} ${chunk.nelems()} elements to ${chunk.destElem()} pos = ${layout.elemSize * chunk.destElem().toInt()}")
    }
    bb.position(0)
    bb.limit(bb.capacity())
    if (debugChunkingDetail or debugChunking)
        println(" Old $count dataTransfers, nodes: readNodes = ${layout.readNodes()}, dataChunks = ${layout.readChunks()}")

    val result = when (datatype) {
        Datatype.BYTE -> ArrayByte(shape, bb)
        Datatype.CHAR, Datatype.UBYTE, Datatype.ENUM1 -> ArrayUByte(shape, bb)
        Datatype.SHORT -> ArrayShort(shape, bb.asShortBuffer())
        Datatype.USHORT, Datatype.ENUM2 -> ArrayUShort(shape, bb.asShortBuffer())
        Datatype.INT -> ArrayInt(shape, bb.asIntBuffer())
        Datatype.UINT, Datatype.ENUM4 -> ArrayUInt(shape, bb.asIntBuffer())
        Datatype.FLOAT -> ArrayFloat(shape, bb.asFloatBuffer())
        Datatype.DOUBLE -> ArrayDouble(shape, bb.asDoubleBuffer())
        Datatype.LONG -> ArrayLong(shape, bb.asLongBuffer())
        Datatype.ULONG -> ArrayULong(shape, bb.asLongBuffer())
        Datatype.OPAQUE -> ArrayOpaque(shape, bb, h5type.elemSize)
        else -> throw IllegalStateException("unimplemented type= $datatype")
    }
    // convert to array of Strings by reducing rank by 1
    if (datatype == Datatype.CHAR) {
        return (result as ArrayUByte).makeStringsFromBytes()
    }
    return result
}

//////////////////////////////////////////////////////////////////////////////////////////////////

// Handles reading attributes and regular layout Variables (eg contiguous, maybe compact)
internal fun H5builder.readRegularData(dc: DataContainer, section : Section?): ArrayTyped<*> {
    if (dc.mds.type == DataspaceType.Null) {
        return ArrayString(intArrayOf(), listOf())
    }
    val h5type = dc.h5type
    val shape: IntArray = dc.storageDims

    val endian: ByteOrder = h5type.endian
    val elemSize = h5type.elemSize

    val wantSection = Section.fill(section, shape)
    val layout: Layout = LayoutRegular(dc.dataPos, elemSize, shape, wantSection)

    if (h5type.hdfType == Datatype5.Vlen) {
        return readVlenData(dc, layout, wantSection)
    }
    if (h5type.hdfType == Datatype5.Compound) {
        return readCompoundData(dc, layout, wantSection)
    }
    val datatype: Datatype = h5type.datatype(this)

    val state = OpenFileState(0, endian)
    val dataArray = readNonHeapData(state, layout, datatype, wantSection.shape, h5type)

    // convert attributes to enum strings
    if (h5type.hdfType == Datatype5.Enumerated) {
        // hopefully this is shared and not replicated
        val enumMsg = dc.mdt as DatatypeEnum
        return dataArray.convertEnums(enumMsg.valuesMap)
    }

    return dataArray
}

// handles datatypes that are not compound or vlen or filtered
// H5tiledLayout seems to be ~ 6/5 faster than readChunkedDataNew for reversed chunk size
@Throws(IOException::class)
internal fun H5builder.readNonHeapData(state: OpenFileState, layout: Layout, datatype: Datatype, shape : IntArray, h5type : H5TypeInfo): ArrayTyped<*> {
    val sizeBytes = layout.totalNelems * layout.elemSize
    if (sizeBytes <= 0 || sizeBytes >= Integer.MAX_VALUE) {
        throw java.lang.RuntimeException("Illegal nbytes to read = $sizeBytes")
    }
    val bb = ByteBuffer.allocate(sizeBytes.toInt())
    bb.order(state.byteOrder)
    var count = 0
    while (layout.hasNext()) {
        val chunk = layout.next()
        state.pos = chunk.srcPos()
        raf.readIntoByteBufferDirect(state, bb, layout.elemSize * chunk.destElem().toInt(), layout.elemSize * chunk.nelems())
        count++
        if (debugChunkingDetail and (count < 20)) println("oldchunk = $chunk")
    }
    if (debugChunkingDetail or debugChunking) {
        if (layout is H5tiledLayout)
            println(" readNonHeapData $count dataTransfers, nodes: readNodes = ${layout.readNodes()}, dataChunks = ${layout.readChunks()}")
    }
    bb.position(0)
    bb.limit(bb.capacity())

    val result = when (datatype) {
        Datatype.BYTE -> ArrayByte(shape, bb)
        Datatype.STRING, Datatype.CHAR, Datatype.UBYTE, Datatype.ENUM1 -> ArrayUByte(shape, bb)
        Datatype.SHORT -> ArrayShort(shape, bb.asShortBuffer())
        Datatype.USHORT, Datatype.ENUM2 -> ArrayUShort(shape, bb.asShortBuffer())
        Datatype.INT -> ArrayInt(shape, bb.asIntBuffer())
        Datatype.UINT, Datatype.ENUM4 -> ArrayUInt(shape, bb.asIntBuffer())
        Datatype.FLOAT -> ArrayFloat(shape, bb.asFloatBuffer())
        Datatype.DOUBLE -> ArrayDouble(shape, bb.asDoubleBuffer())
        Datatype.LONG -> ArrayLong(shape, bb.asLongBuffer())
        Datatype.ULONG -> ArrayULong(shape, bb.asLongBuffer())
        Datatype.OPAQUE -> ArrayOpaque(shape, bb, h5type.elemSize)
        else -> throw IllegalStateException("unimplemented type= $datatype")
    }
    // convert to array of Strings by reducing rank by 1
    if (h5type.hdfType == Datatype5.String) {
        return (result as ArrayUByte).makeStringsFromBytes()
    }
    if ((h5type.hdfType == Datatype5.Reference) and h5type.isRefObject) {
        return ArrayString(shape, this.convertReferencesToDataObjectName(result as ArrayLong))
    }
    return result
}

internal fun H5builder.readChunkedDataNew(v2: Variable, wantSection : Section) : ArrayTyped<*> {
    val vinfo = v2.spObject as DataContainerVariable
    val h5type = vinfo.h5type

    if ((h5type.hdfType == Datatype5.Vlen) or (h5type.hdfType == Datatype5.Compound)) {
        val layout = if (vinfo.mfp != null) H5tiledLayoutBB(this, v2, wantSection, vinfo.mfp.filters, vinfo.h5type.endian)
                            else H5tiledLayout(this, v2, wantSection, v2.datatype)
        when (h5type.hdfType) {
            Datatype5.Vlen -> return readVlenData(vinfo, layout, wantSection)
            Datatype5.Compound -> return readCompoundData(vinfo, layout, wantSection)
            // Datatype5.Reference -> return readReferenceData(h5type, layout, wantSection)
            else -> { } // fall through
        }
    }

    val elemSize = vinfo.storageDims.get(vinfo.storageDims.size - 1) // last one is always the elements size
    val datatype = vinfo.h5type.datatype(this)

    val wantSpace = IndexSpace(wantSection)
    val sizeBytes = wantSpace.totalElements * elemSize
    if (sizeBytes <= 0 || sizeBytes >= Integer.MAX_VALUE) {
        throw java.lang.RuntimeException("Illegal nbytes to read = $sizeBytes")
    }
    val bb = ByteBuffer.allocate(sizeBytes.toInt())
    bb.order(vinfo.h5type.endian)

    // prefill with fill value
    /* val sbb = bb.asShortBuffer()
    sbb.position(0)
    val fill = vinfo.fillValue as Short
    repeat(wantSpace.totalElements.toInt()) { bb.putShort(fill) } // performance ?? */

    val btreeNew =  BTree1New(this, vinfo.dataPos, 1, v2.shape, vinfo.storageDims)
    val chunkedData = TiledData(btreeNew)
    val filters = H5filters(v2.name, vinfo.mfp, vinfo.h5type.endian)
    if (debugChunking) println(" ${chunkedData.tiling}")

    chunkers = 0
    transfers = 0
    missingChunks = 0
    var count = 0
    var transferChunks = 0
    val state = OpenFileState(0L, vinfo.h5type.endian)
    for (dataChunk in chunkedData.findDataChunks(wantSpace)) { // : Iterable<BTree1New.DataChunkEntry>
        val dataSection = IndexSpace(dataChunk.key.offsets, vinfo.storageDims)
        val chunker = Chunker(dataSection, elemSize, wantSpace)
        if (dataChunk.isMissing()) {
            if (debugMissing) println(" ${dataChunk.show(chunkedData.tiling)}")
            chunker.transferMissing(vinfo, datatype, bb)
        } else {
            if (debugChunkingDetail and (count < 1)) println(" ${dataChunk.show(chunkedData.tiling)}")
            state.pos = dataChunk.childAddress
            val chunkData = raf.readByteBufferDirect(state, dataChunk.key.chunkSize)
            val filteredData = filters.apply(chunkData, dataChunk)
            chunker.transfer(filteredData, bb)
            transferChunks += chunker.transferChunks
        }
        count++
    }
    if (debugChunkingDetail or debugChunking) println(" New $count dataChunks; nodes: ${chunkedData} " +
            "transferChunks = $transferChunks missing = $missingChunks, missingElems = $missingElems")

    bb.position(0)
    bb.limit(bb.capacity())

    val shape = wantSpace.nelems
    val result = when (datatype) {
        Datatype.BYTE -> ArrayByte(shape, bb)
        Datatype.STRING, Datatype.CHAR, Datatype.UBYTE, Datatype.ENUM1 -> ArrayUByte(shape, bb)
        Datatype.SHORT -> ArrayShort(shape, bb.asShortBuffer())
        Datatype.USHORT, Datatype.ENUM2 -> ArrayUShort(shape, bb.asShortBuffer())
        Datatype.INT -> ArrayInt(shape, bb.asIntBuffer())
        Datatype.UINT, Datatype.ENUM4 -> ArrayUInt(shape, bb.asIntBuffer())
        Datatype.FLOAT -> ArrayFloat(shape, bb.asFloatBuffer())
        Datatype.DOUBLE -> ArrayDouble(shape, bb.asDoubleBuffer())
        Datatype.LONG -> ArrayLong(shape, bb.asLongBuffer())
        Datatype.ULONG -> ArrayULong(shape, bb.asLongBuffer())
        Datatype.OPAQUE -> ArrayOpaque(shape, bb, elemSize)
        else -> throw IllegalStateException("unimplemented type= $datatype")
    }
    if (h5type.hdfType == Datatype5.String) {
        return (result as ArrayUByte).makeStringsFromBytes()
    }
    if ((h5type.hdfType == Datatype5.Reference) and h5type.isRefObject) {
        return ArrayString(shape, this.convertReferencesToDataObjectName(result as ArrayLong))
    }
    return result
}

var transfers = 0
var transferMissing = 0
var chunkers = 0
var missingChunks = 0
fun Chunker.transfer(src : ByteBuffer, dst : ByteBuffer) {
    if (debugChunkingDetail and (chunkers < 5)) println("  $this")
    while (this.hasNext()) {
        val chunk = this.next()
        if (debugChunkingDetail and (transfers < 20)) println("   $chunk")
        src.position(this.elemSize * chunk.srcElem.toInt())
        dst.position(this.elemSize * chunk.destElem.toInt())
        // Object src,  int  srcPos, Object dest, int destPos, int length
        System.arraycopy(
            src.array(),
            this.elemSize * chunk.srcElem.toInt(),
            dst.array(),
            this.elemSize * chunk.destElem.toInt(),
            this.elemSize * chunk.nelems,
        )
        transfers++
    }
    chunkers++
}

val debugMissing = false
var missingElems = 0L
private fun Chunker.transferMissing(vinfo : DataContainerVariable, datatype : Datatype, dst : ByteBuffer) {
    missingElems += this.totalNelems
    if (vinfo.fillValue == null || debugMissing) {
        // could use some default, but 0 is pretty good
        return
    }
    while (this.hasNext()) {
        val chunk = this.next()
        dst.position(this.elemSize * chunk.destElem.toInt())
        // println("  missing transfer $chunk")
        when (datatype) {
            Datatype.STRING, Datatype.CHAR, Datatype.BYTE, Datatype.UBYTE, Datatype.ENUM1 -> {
                val fill = vinfo.fillValue as Byte
                repeat(chunk.nelems) { dst.put(fill) }
            }
            Datatype.SHORT, Datatype.USHORT, Datatype.ENUM2 -> repeat(chunk.nelems) { dst.putShort(vinfo.fillValue as Short) }
            Datatype.INT, Datatype.UINT, Datatype.ENUM4 -> repeat(chunk.nelems) { dst.putInt(vinfo.fillValue as Int) }
            Datatype.FLOAT -> repeat(chunk.nelems) { dst.putFloat(vinfo.fillValue as Float) }
            Datatype.DOUBLE -> repeat(chunk.nelems) { dst.putDouble(vinfo.fillValue as Double) }
            Datatype.LONG, Datatype.ULONG -> repeat(chunk.nelems) { dst.putLong(vinfo.fillValue as Long) }
            Datatype.OPAQUE -> {
                val fill = vinfo.fillValue as ByteBuffer
                repeat(chunk.nelems) {
                    fill.position(0)
                    dst.put(fill) }
            }
            else -> throw IllegalStateException("unimplemented type= $datatype")
        }
        transferMissing++
    }
    missingChunks++
}

// The structure data is not on the heap, but the variable length members (vlen, string) are
internal fun H5builder.readCompoundData(dc: DataContainer, layout : Layout, section : Section) : ArrayStructureData {
    val datatype = dc.h5type.datatype(this)
    require(datatype == Datatype.COMPOUND)
    requireNotNull(datatype.typedef)
    require(datatype.typedef is CompoundTypedef)

    val state = OpenFileState(0, dc.h5type.endian)

    val sdataArray = readArrayStructureData(state, layout, section.shape, datatype.typedef.members)
    val h5heap = H5heap(this)
    sdataArray.putStringsOnHeap {  offset -> h5heap.readHeapString(sdataArray.bb, offset)!! }

    sdataArray.putVlensOnHeap { member, offset ->
        val listOfArrays = mutableListOf<Array<*>>()
        for (i in 0 until member.nelems) {
            val heapId = h5heap.readHeapIdentifier(sdataArray.bb, offset)
            val vlenArray = h5heap.getHeapDataArray(heapId, member.datatype, dc.h5type.endian)
            // println("  ${vlenArray.contentToString()}")
            listOfArrays.add(vlenArray)
        }
        ArrayVlen(member.dims, listOfArrays, member.datatype)
    }

    return sdataArray
}

@Throws(IOException::class)
internal fun H5builder.readArrayStructureData(state: OpenFileState, layout: Layout, shape : IntArray, members : List<StructureMember>): ArrayStructureData {
    val sizeBytes = computeSize(shape).toInt() * layout.elemSize
    val bb = ByteBuffer.allocate(sizeBytes)
    bb.order(state.byteOrder)
    while (layout.hasNext()) {
        val chunk: Layout.Chunk = layout.next()
        state.pos = chunk.srcPos()
        raf.readIntoByteBuffer(state, bb, layout.elemSize * chunk.destElem().toInt(), layout.elemSize * chunk.nelems())
    }
    bb.position(0)
    bb.limit(bb.capacity())
    return ArrayStructureData(shape, bb, layout.elemSize, members)
}

internal fun H5builder.readVlenData(dc: DataContainer, layout : Layout, wantedSection : Section) : ArrayTyped<*> {
    val h5heap = H5heap(this)

    // Strings
    if (dc.h5type.isVString) {
        val sarray = mutableListOf<String>()
        while (layout.hasNext()) {
            val chunk: Layout.Chunk = layout.next()
            for (i in 0 until chunk.nelems()) {
                val address: Long = chunk.srcPos() + layout.elemSize * i
                val sval = h5heap.readHeapString(address)
                sarray.add(sval ?: "")
            }
        }
        return ArrayString(wantedSection.shape, sarray)

    } else {
        val base = dc.h5type.base!!

        /* variable length array of references, get translated into strings LOOK always? NPP has reference regions
        if (base.hdfType == Datatype5.Reference) {
            val refsList = mutableListOf<String>()
            while (layout.hasNext()) {
                val chunk: Layout.Chunk = layout.next()
                for (i in 0 until chunk.nelems()) {
                    val address: Long = chunk.srcPos() + layout.elemSize * i
                    val refObjName = this.convertReferenceToDataObjectName(address)
                    refsList.add(refObjName)
                }
            }
            return ArrayString(wantedSection.shape, refsList)
        } */

        if (base.hdfType == Datatype5.Reference) {
            val refsList = mutableListOf<String>()
            while (layout.hasNext()) {
                val chunk: Layout.Chunk = layout.next()
                for (i in 0 until chunk.nelems()) {
                    val address: Long = chunk.srcPos() + layout.elemSize * i
                    val vlenArray = h5heap.getHeapDataArray(address, Datatype.LONG, base.endian)
                    // LOOK require vlenArray is Array<Long>
                    val refsArray = this.convertReferencesToDataObjectName(vlenArray as Array<Long>)
                    for (s in refsArray) {
                        refsList.add(s)
                    }
                }
            }
            return ArrayString(wantedSection.shape, refsList)
        }

        // general case is to read an array of vlen objects
        // each vlen generates an Array of type baseType
        val listOfArrays = mutableListOf<Array<*>>()
        val readDatatype = base.datatype(this)
        var count = 0
        while (layout.hasNext()) {
            val chunk: Layout.Chunk = layout.next()
            for (i in 0 until chunk.nelems()) {
                val address: Long = chunk.srcPos() + layout.elemSize * i
                val vlenArray = h5heap.getHeapDataArray(address, readDatatype, base.endian)
                // LOOK require vlenArray is Array<T>
                listOfArrays.add(vlenArray)
                count++
            }
        }
        return ArrayVlen(wantedSection.shape, listOfArrays.toList(), readDatatype)
    }
}

// LOOK is this needed?
internal class H5StructureMember(name: String, datatype : Datatype, offset: Int, dims : IntArray,
                                 val hdfType: Datatype5, val lamda: ((Long) -> String))
    : StructureMember(name, datatype, offset, dims) {

    override fun value(sdata : ArrayStructureData.StructureData) : Any {
        if (hdfType == Datatype5.Reference) {
            val offset = sdata.offset + this.offset
            val reference = sdata.bb.getLong(offset)
            return lamda(reference)
        }
        return super.value(sdata)
    }
}

