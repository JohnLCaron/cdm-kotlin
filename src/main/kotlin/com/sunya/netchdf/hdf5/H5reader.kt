package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.*
import com.sunya.cdm.iosp.*
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class H5reader(val header: H5builder) {
    val raf = header.raf

    @Throws(IOException::class)
    fun readData(state: OpenFileState, layout: Layout, dataType: DataType, shape : IntArray): ArrayTyped<*> {
        val sizeBytes = Section(shape).computeSize().toInt() * layout.elemSize
        val bb = ByteBuffer.allocate(sizeBytes)
        bb.order(state.byteOrder)
        while (layout.hasNext()) {
            val chunk: Layout.Chunk = layout.next()
            state.pos = chunk.srcPos
            raf.readIntoByteBuffer(state, bb, layout.elemSize * chunk.destElem.toInt(), layout.elemSize * chunk.nelems)
        }
        bb.position(0)

        val result = when (dataType) {
            DataType.BYTE -> ArrayByte(bb, shape)
            DataType.CHAR, DataType.UBYTE, DataType.ENUM1 -> ArrayUByte(bb, shape)
            DataType.SHORT -> ArrayShort(bb.asShortBuffer(), shape)
            DataType.USHORT, DataType.ENUM2 -> ArrayUShort(bb.asShortBuffer(), shape)
            DataType.INT, DataType.ENUM4 -> ArrayInt(bb.asIntBuffer(), shape)
            DataType.UINT -> ArrayUInt(bb.asIntBuffer(), shape)
            DataType.FLOAT -> ArrayFloat(bb.asFloatBuffer(), shape)
            DataType.DOUBLE -> ArrayDouble(bb.asDoubleBuffer(), shape)
            DataType.LONG -> ArrayLong(bb.asLongBuffer(), shape)
            DataType.ULONG -> ArrayULong(bb.asLongBuffer(), shape)
            DataType.OPAQUE -> ArrayByte(bb, shape)
            else -> throw IllegalStateException("unimplemented type= $dataType")
        }
        // convert to array of Strings by reducing rank by 1
        if (dataType == DataType.CHAR) {
            return (result as ArrayUByte).makeStringsFromBytes()
        }

        /*
        else if (dataType.primitiveClass == StructureData::class.java) {
            val recsize: Int = layout.getElemSize()
            while (layout.hasNext()) {
                val chunk: Layout.Chunk = layout.next()
                raf.order(byteOrder)
                state.pos = chunk.srcPos
                raf.readFully(pa, chunk.destElem * recsize, chunk.nelems * recsize)
            }
            return pa
        } else if (dataType.primitiveClass == String::class.java) {
            val size = layout.getTotalNelems() as Int
            val elemSize: Int = layout.getElemSize()
            val sb = StringBuilder(size)
            while (layout.hasNext()) {
                val chunk: Layout.Chunk = layout.next() ?: continue
                for (i in 0 until chunk.nelems) {
                    sb.append(raf.readString(elemSize))
                }
            }
            return sb.toString()
            */
        return result
    }

    @Throws(IOException::class)
    fun readStructureData(state: OpenFileState, layout: Layout, shape : IntArray, members : StructureMembers): ArrayStructureData {
        val sizeBytes = Section(shape).computeSize().toInt() * layout.elemSize
        val bb = ByteBuffer.allocate(sizeBytes)
        bb.order(state.byteOrder)
        while (layout.hasNext()) {
            val chunk: Layout.Chunk = layout.next()
            state.pos = chunk.srcPos
            raf.readIntoByteBuffer(state, bb, layout.elemSize * chunk.destElem.toInt(), layout.elemSize * chunk.nelems)
        }
        bb.position(0)
        return ArrayStructureData(bb, layout.elemSize, shape, members)
    }
}

internal fun H5builder.readAttributeData(
    matt: AttributeMessage,
    h5type: H5Type,
): List<*> {
    // Vlens
    if (h5type.hdfType == Datatype5.Vlen) {
        return readVlenData(matt, h5type)
    }

    if (h5type.hdfType == Datatype5.Compound) {
        return readCompoundData(matt, h5type)
    }

    var shape: IntArray = matt.mds.dims

    var readDtype: DataType = h5type.dataType
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
        val baseInfo = h5type.base!!
        readDtype = baseInfo.dataType
        endian = baseInfo.endian
    }

    val state = OpenFileState(0, endian)
    val layout: Layout = LayoutRegular(matt.dataPos, elemSize, shape, Section(shape))

    val h5reader = H5reader(this)
    val dataArray = h5reader.readData(state, layout, readDtype, shape)

    return dataArray.toList()
}

internal fun H5builder.readCompoundData(matt: AttributeMessage, h5type: H5Type) : List<*> {
    val shape: IntArray = matt.mds.dims
    val state = OpenFileState(0, h5type.endian)
    val layout: Layout = LayoutRegular(matt.dataPos, h5type.elemSize, shape, Section(shape))

    val compoundType = matt.mdt as DatatypeCompound
    val ms = compoundType.members.map { sm5  ->
        val memberType = H5Type(sm5.mdt)
        // LOOK how many elements? I guess 1 unless sm5.mdt is an array ??
        val nelems = if (sm5.mdt is DatatypeArray) {
            val dims = sm5.mdt.dims
            Section(dims).computeSize().toInt()
        } else 1

        val lamda: ((Long) -> String)?  = if (memberType.hdfType == Datatype5.Reference)
            { it -> (this@readCompoundData).convertReferenceToDataObjectName(it) }
        else null

        H5StructureMember(sm5.name, memberType.dataType, sm5.offset, nelems, memberType.hdfType, lamda)
    }
    val members = StructureMembers(ms)

    val h5reader = H5reader(this)
    val sdataArray = h5reader.readStructureData(state, layout, shape, members)
    return sdataArray.toList()
}

internal class H5StructureMember(name: String, dataType : DataType, offset: Int, nelems : Int,
                                 val hdfType: Datatype5, val lamda: ((Long) -> String)?)
    : StructureMember(name, dataType, offset, nelems) {

    override fun value(sdata : StructureData) : Any {
        if (hdfType == Datatype5.Reference && lamda != null) {
            val bb = sdata.bb
            val offset = sdata.offset + this.offset
            val reference = bb.getLong(offset)
            return lamda!!(reference)
        }
        return super.value(sdata)
    }
}

internal fun H5builder.readVlenData(matt: AttributeMessage, h5type: H5Type) : List<*> {
    val shape: IntArray = matt.mds.dims
    val layout2 = LayoutRegular(matt.dataPos, matt.mdt.elemSize, shape, Section(shape))
    val h5heap = H5heap(this)

    // Strings
    if (h5type.isVString) {
        val size = layout2.totalNelems.toInt()
        val sarray = mutableListOf<String>()
        while (layout2.hasNext()) {
            val chunk: Layout.Chunk = layout2.next() ?: continue
            for (i in 0 until chunk.nelems) {
                val address: Long = chunk.srcPos + layout2.elemSize * i
                val sval = h5heap.readHeapString(address)
                if (sval != null) sarray.add(sval)
            }
        }
        return sarray
    }

    // Vlen (non-String)
    else {
        var endian: ByteOrder? = h5type.endian
        var readType: DataType = h5type.dataType
        if (h5type.base!!.hdfType == Datatype5.Reference) { // reference
            readType = DataType.LONG
            endian = ByteOrder.LITTLE_ENDIAN // apparently always LE
        }

        // variable length array of references, get translated into strings
        if (h5type.base!!.hdfType == Datatype5.Reference) {
            val refsList = mutableListOf<String>()
            while (layout2.hasNext()) {
                val chunk: Layout.Chunk = layout2.next() ?: continue
                for (i in 0 until chunk.nelems) {
                    val address: Long = chunk.srcPos + layout2.elemSize * i
                    val vlenArray = h5heap.getHeapDataArray(address, readType, endian)
                    val refsArray = this.convertReferencesToDataObjectName(vlenArray as Array<Long>)
                    for (s in refsArray) {
                        refsList.add(s)
                    }
                }
            }
            return refsList
        }

        // throw RuntimeException("vlen not implemented")

        // general case is to read an array of vlen objects
        // each vlen generates an Array, have to combine them
        val result = mutableListOf<Array<*>>()
        var count = 0
        while (layout2.hasNext()) {
            val chunk: Layout.Chunk = layout2.next()
            for (i in 0 until chunk.nelems) {
                val address: Long = chunk.srcPos + layout2.elemSize * i
                val vlenArray = h5heap.getHeapDataArray(address, readType, endian)
                result.add(vlenArray)
                count++
            }
        }
        return result
    } // vlen case
}