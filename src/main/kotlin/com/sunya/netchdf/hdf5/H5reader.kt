package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.*
import com.sunya.cdm.api.Section.Companion.computeSize
import com.sunya.cdm.iosp.*
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.reflect.KClass

// was readAttributeData
internal fun H5builder.readDataArray(dc: DataContainer): ArrayTyped<*> {
    if (dc.mds.type == DataspaceType.Null) {
        return ArrayString(intArrayOf(), listOf())
    }
    val h5type = dc.h5type

    if (h5type.hdfType == Datatype5.Vlen) {
        return readVlenData(dc)
    }
    if (h5type.hdfType == Datatype5.Compound) {
        return readCompoundData(dc)
    }

    var shape: IntArray = dc.mds.dims
    var readDtype: Datatype = h5type.datatype
    var endian: ByteOrder = h5type.endian
    var elemSize = h5type.elemSize

    if (h5type.hdfType == Datatype5.String) { // char
        if (h5type.elemSize > 1) {
            val newShape = IntArray(shape.size + 1)
            System.arraycopy(shape, 0, newShape, 0, shape.size)
            newShape[shape.size] = h5type.elemSize
            shape = newShape
            elemSize = 1
        }

    } else if (h5type.hdfType == Datatype5.Enumerated) { // enum
        readDtype = h5type.base!!.datatype
        endian = h5type.endian
    }

    val state = OpenFileState(0, endian)
    val layout: Layout = LayoutRegular(dc.dataPos, elemSize, shape, Section(shape))
    val dataArray = readNonHeapData(state, layout, readDtype, shape)

    // convert attributes to enum strings
    if (h5type.hdfType == Datatype5.Enumerated) {
        // hopefully this is shared and not replicated
        val enumMsg = dc.mdt as DatatypeEnum
        return convertEnums(enumMsg.valuesMap, dataArray)
    }

    return dataArray
}

// handles datatypes that are not compound or vlen
@Throws(IOException::class)
internal fun H5builder.readNonHeapData(state: OpenFileState, layout: Layout, datatype: Datatype, shape : IntArray): ArrayTyped<*> {
    val sizeBytes = computeSize(shape).toInt() * layout.elemSize
    val bb = ByteBuffer.allocate(sizeBytes)
    bb.order(state.byteOrder)
    while (layout.hasNext()) {
        val chunk: Layout.Chunk = layout.next()
        state.pos = chunk.srcPos
        raf.readIntoByteBuffer(state, bb, layout.elemSize * chunk.destElem.toInt(), layout.elemSize * chunk.nelems)
    }
    bb.position(0)

    val result = when (datatype) {
        Datatype.BYTE -> ArrayByte(shape, bb)
        Datatype.CHAR, Datatype.UBYTE, Datatype.ENUM1 -> ArrayUByte(shape, bb)
        Datatype.SHORT -> ArrayShort(shape, bb.asShortBuffer())
        Datatype.USHORT, Datatype.ENUM2 -> ArrayUShort(shape, bb.asShortBuffer())
        Datatype.INT, Datatype.ENUM4 -> ArrayInt(shape, bb.asIntBuffer())
        Datatype.UINT -> ArrayUInt(shape, bb.asIntBuffer())
        Datatype.FLOAT -> ArrayFloat(shape, bb.asFloatBuffer())
        Datatype.DOUBLE -> ArrayDouble(shape, bb.asDoubleBuffer())
        Datatype.LONG -> ArrayLong(shape, bb.asLongBuffer())
        Datatype.ULONG -> ArrayULong(shape, bb.asLongBuffer())
        Datatype.OPAQUE -> ArrayOpaque(shape, bb)
        else -> throw IllegalStateException("unimplemented type= $datatype")
    }
    // convert to array of Strings by reducing rank by 1
    if (datatype == Datatype.CHAR) {
        return (result as ArrayUByte).makeStringsFromBytes()
    }
    return result
}

// The structure data is not on the heap, but the variable length members (vlen, string) are
internal fun H5builder.readCompoundData(dc: DataContainer) : ArrayStructureData {
    val shape: IntArray = dc.mds.dims
    val state = OpenFileState(0, dc.h5type.endian)
    val layout: Layout = LayoutRegular(dc.dataPos, dc.h5type.elemSize, shape, Section(shape))

    val compoundType = dc.mdt as DatatypeCompound
    val members = compoundType.members.map { sm5  ->
        val memberType = H5Type(sm5.mdt)

        val lamda: ((Long) -> String)?  = if (memberType.hdfType == Datatype5.Reference)
            { it -> (this@readCompoundData).convertReferenceToDataObjectName(it) }
        else null

        H5StructureMember(sm5.name, memberType.datatype, sm5.offset, sm5.dims, memberType.hdfType, lamda)
    }

    val h5heap = H5heap(this)
    val sdataArray = readArrayStructureData(state, layout, shape, members)
    members.filter { it.datatype == Datatype.STRING }.forEach { member ->
        sdataArray.forEach { sdata ->
            val sval = h5heap.readHeapString(sdataArray.bb, sdata.offset + member.offset)!!
            println("offset ${sdata.offset + member.offset} sval $sval")
            sdata.putOnHeap(member, sval)
        }
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
        state.pos = chunk.srcPos
        raf.readIntoByteBuffer(state, bb, layout.elemSize * chunk.destElem.toInt(), layout.elemSize * chunk.nelems)
    }
    bb.position(0)
    return ArrayStructureData(shape, bb, layout.elemSize, members)
}

internal fun H5builder.readVlenData(dc: DataContainer) : ArrayTyped<*> {
    val shape: IntArray = dc.mds.dims
    val layout2 = LayoutRegular(dc.dataPos, dc.mdt.elemSize, shape, Section(shape))
    val h5heap = H5heap(this)

    // Strings
    if (dc.h5type.isVString) {
        val sarray = mutableListOf<String>()
        while (layout2.hasNext()) {
            val chunk: Layout.Chunk = layout2.next()
            for (i in 0 until chunk.nelems) {
                val address: Long = chunk.srcPos + layout2.elemSize * i
                val sval = h5heap.readHeapString(address)
                if (sval != null) sarray.add(sval)
            }
        }
        return ArrayString(shape, sarray)
    }

    // Vlen (non-String)
    else {
        val vlenMdt = dc.mdt as DatatypeVlen
        val base = vlenMdt.base

        // variable length array of references, get translated into strings LOOK always?
        if (base.type == Datatype5.Reference) {
            val refsList = mutableListOf<String>()
            while (layout2.hasNext()) {
                val chunk: Layout.Chunk = layout2.next() ?: continue
                for (i in 0 until chunk.nelems) {
                    val address: Long = chunk.srcPos + layout2.elemSize * i
                    val vlenArray = h5heap.getHeapDataArray(address, Datatype.LONG, base.endian())
                    val refsArray = this.convertReferencesToDataObjectName(vlenArray as Array<Long>)
                    for (s in refsArray) {
                        refsList.add(s)
                    }
                }
            }
            return ArrayString(shape, refsList)
        }

        // general case is to read an array of vlen objects
        // each vlen generates an Array of type baseType
        val baseType = H5Type(base, null).datatype
        val listOfArrays = mutableListOf<Array<*>>()
        var count = 0
        while (layout2.hasNext()) {
            val chunk: Layout.Chunk = layout2.next()
            for (i in 0 until chunk.nelems) {
                val address: Long = chunk.srcPos + layout2.elemSize * i
                val vlenArray = h5heap.getHeapDataArray(address, baseType, base.endian())
                listOfArrays.add(vlenArray)
                count++
            }
        }
        return ArrayVlen(intArrayOf(count), listOfArrays, baseType)
    }
}

internal class H5StructureMember(name: String, datatype : Datatype, offset: Int, dims : IntArray,
                                 val hdfType: Datatype5, val lamda: ((Long) -> String)?)
    : StructureMember(name, datatype, offset, dims) {

    override fun value(sdata : ArrayStructureData.StructureData) : Any {
        if (hdfType == Datatype5.Reference && lamda != null) {
            val offset = sdata.offset + this.offset
            val reference = sdata.bb.getLong(offset)
            return lamda!!(reference)
        }
        return super.value(sdata)
    }
}
internal fun convertEnums(map: Map<Int, String>, enumValues: ArrayTyped<*>): ArrayString {
    val wtf : KClass<out ArrayTyped<*>> = enumValues::class // LOOK is there something simpler ?
    val size = computeSize(enumValues.shape).toInt()
    val enumIter = enumValues.iterator()
    val stringValues = Array(size) {
        val enumVal = enumIter.next()
        val num = when (wtf.simpleName) {
            "ArrayUByte" ->  (enumVal as UByte).toInt()
            "ArrayUShort" ->  (enumVal as UShort).toInt()
            "ArrayUInt" ->  (enumVal as UInt).toInt()
            else -> RuntimeException("unknown ${wtf.simpleName}")
        }
        map[num] ?: "Unknown enum number=$enumVal"
    }
    return ArrayString(enumValues.shape, stringValues)
}
