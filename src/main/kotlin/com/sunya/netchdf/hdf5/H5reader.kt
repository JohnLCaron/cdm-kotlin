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

private val debugChunking = false

// Handles reading attributes and regular layout Variables (eg contiguous, maybe compact)
internal fun H5builder.readRegularData(dc: DataContainer, section : Section?): ArrayTyped<*> {
    if (dc.mds.type == DataspaceType.Null) {
        return ArrayString(intArrayOf(), listOf())
    }
    val h5type = dc.h5type
    val shape: IntArray = dc.storageDims
    val readDtype: Datatype = h5type.datatype(this)
    val endian: ByteOrder = h5type.endian
    val elemSize = h5type.elemSize

    /* if (h5type.hdfType == Datatype5.String) { // char
        if (h5type.elemSize > 1) {
            val newShape = IntArray(shape.size + 1)
            System.arraycopy(shape, 0, newShape, 0, shape.size)
            newShape[shape.size] = h5type.elemSize
            shape = newShape
            elemSize = 1
        }
    } */

    val wantSection = Section.fill(section, shape)
    val layout: Layout = LayoutRegular(dc.dataPos, elemSize, shape, wantSection)

    if (h5type.hdfType == Datatype5.Vlen) {
        return readVlenData(dc, layout, wantSection)
    }
    if (h5type.hdfType == Datatype5.Compound) {
        return readCompoundData(dc, layout, wantSection)
    }

    val state = OpenFileState(0, endian)
    val dataArray = readNonHeapData(state, layout, readDtype, wantSection.shape, h5type)

    // convert attributes to enum strings
    if (h5type.hdfType == Datatype5.Enumerated) {
        // hopefully this is shared and not replicated
        val enumMsg = dc.mdt as DatatypeEnum
        return dataArray.convertEnums(enumMsg.valuesMap)
    }

    return dataArray
}

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

    var shape: IntArray = vinfo.mds.dims
    val readDtype: Datatype = h5type.datatype(this)
    val endian: ByteOrder = h5type.endian
    var elemSize = h5type.elemSize

    // LOOK is this needed?
    if (h5type.hdfType == Datatype5.String) { // char
        if (h5type.elemSize > 1) {
            val newShape = IntArray(shape.size + 1)
            System.arraycopy(shape, 0, newShape, 0, shape.size)
            newShape[shape.size] = h5type.elemSize
            shape = newShape
            elemSize = 1
        }

    }

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

// handles datatypes that are not compound or vlen or filtered
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
        raf.readIntoByteBuffer(state, bb, layout.elemSize * chunk.destElem().toInt(), layout.elemSize * chunk.nelems())
        count++
    }
    bb.position(0)
    bb.limit(bb.capacity())

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

    var shape: IntArray = vinfo.mds.dims
    val readDtype: Datatype = h5type.datatype(this)
    val endian: ByteOrder = h5type.endian
    var elemSize = h5type.elemSize

    // LOOK isa this needed?
    if (h5type.hdfType == Datatype5.String) { // char
        if (h5type.elemSize > 1) {
            val newShape = IntArray(shape.size + 1)
            System.arraycopy(shape, 0, newShape, 0, shape.size)
            newShape[shape.size] = h5type.elemSize
            shape = newShape
            elemSize = 1 // LOOK why?
        }

    }

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

// new way, find out timing
internal fun H5builder.readChunkedDataNew(v2: Variable, wantSection : Section) : ArrayTyped<*> {
    val vinfo = v2.spObject as DataContainerVariable
    val h5type = vinfo.h5type

    if (h5type.hdfType == Datatype5.Vlen) {
        val layout = if (vinfo.mfp != null) H5tiledLayoutBB(this, v2, wantSection, vinfo.mfp.filters, vinfo.h5type.endian)
                            else H5tiledLayout(this, v2, wantSection, v2.datatype)
        return readVlenData(vinfo, layout, wantSection)
    }
    if (h5type.hdfType == Datatype5.Compound) {
        val layout = if (vinfo.mfp != null) H5tiledLayoutBB(this, v2, wantSection, vinfo.mfp.filters, vinfo.h5type.endian)
                            else H5tiledLayout(this, v2, wantSection, v2.datatype)
        return readCompoundData(vinfo, layout, wantSection)
    }

    val elemSize = vinfo.storageDims.get(vinfo.storageDims.size - 1) // last one is always the elements size

    val sizeBytes = wantSection.computeSize() * elemSize
    if (sizeBytes <= 0 || sizeBytes >= Integer.MAX_VALUE) {
        throw java.lang.RuntimeException("Illegal nbytes to read = $sizeBytes")
    }
    val bb = ByteBuffer.allocate(sizeBytes.toInt())
    bb.order(vinfo.h5type.endian)

    val btreeNew =  BTree1New(this, vinfo.dataPos, 1, v2.shape, vinfo.storageDims)
    val chunkedData = ChunkedData(btreeNew)
    val filters = H5filters(vinfo.mfp, vinfo.h5type.endian)
    if (debugChunking) println(" ${chunkedData.tiling}")

    var count = 0
    val state = OpenFileState(0L, vinfo.h5type.endian)
    for (dataChunk in chunkedData.findDataChunks(wantSection)) { // : Iterable<BTree1New.DataChunkEntry>
        if (debugChunking) println(" ${dataChunk.show(chunkedData.tiling)}")
        val dataSection = IndexSpace(dataChunk.key.offsets, vinfo.storageDims)
        val chunker = Chunker(dataSection, elemSize, wantSection)
        state.pos = dataChunk.childAddress
        val chunkData = raf.readByteBuffer(state, dataChunk.key.chunkSize)
        val filteredData = filters.apply(chunkData, dataChunk)
        chunker.transfer(filteredData, bb)
        count++
    }
    if (debugChunking) println(" New $count dataChunks; nodes: ${chunkedData}")

    bb.position(0)
    bb.limit(bb.capacity())

    val shape = wantSection.shape
    val datatype = vinfo.h5type.datatype(this)
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
        Datatype.OPAQUE -> ArrayOpaque(shape, bb, elemSize)
        else -> throw IllegalStateException("unimplemented type= $datatype")
    }
    if (datatype == Datatype.CHAR) {
        return (result as ArrayUByte).makeStringsFromBytes()
    }
    return result
}

fun Chunker.transfer(src : ByteBuffer, dst : ByteBuffer) {
    if (debugChunking) println("  $this")
    while (this.hasNext()) {
        val chunk = this.next()
        if (debugChunking) println("   $chunk")
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
    }
}


// handles datatypes that are not compound or vlen or filtered
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
        } // LOOK bulk copy ?
        count++
        if (debugChunking) println("read at ${chunk.srcElem()} ${chunk.nelems()} elements to ${chunk.destElem()} pos = ${layout.elemSize * chunk.destElem().toInt()}")
    }
    bb.position(0)
    bb.limit(bb.capacity())
    if (debugChunking) println(" Old $count dataChunks, nodes: readNodes = ${layout.readNodes()}, readChunks = ${layout.readChunks()}")

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
    }

    // Vlen (non-String)
    else {
        val base = dc.h5type.base!!

        // variable length array of references, get translated into strings LOOK always?
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

