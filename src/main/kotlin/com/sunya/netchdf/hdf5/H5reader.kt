package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.*
import com.sunya.cdm.iosp.*
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.reflect.KClass

class H5reader(val header: H5builder) {
    val raf = header.raf

    @Throws(IOException::class)
    fun readData(state: OpenFileState, layout: Layout, datatype: Datatype, shape : IntArray): ArrayTyped<*> {
        val sizeBytes = Section(shape).computeSize().toInt() * layout.elemSize
        val bb = ByteBuffer.allocate(sizeBytes)
        bb.order(state.byteOrder)
        while (layout.hasNext()) {
            val chunk: Layout.Chunk = layout.next()
            state.pos = chunk.srcPos
            raf.readIntoByteBuffer(state, bb, layout.elemSize * chunk.destElem.toInt(), layout.elemSize * chunk.nelems)
        }
        bb.position(0)

        val result = when (datatype) {
            Datatype.BYTE -> ArrayByte(bb, shape)
            Datatype.CHAR, Datatype.UBYTE, Datatype.ENUM1 -> ArrayUByte(bb, shape)
            Datatype.SHORT -> ArrayShort(bb.asShortBuffer(), shape)
            Datatype.USHORT, Datatype.ENUM2 -> ArrayUShort(bb.asShortBuffer(), shape)
            Datatype.INT, Datatype.ENUM4 -> ArrayInt(bb.asIntBuffer(), shape)
            Datatype.UINT -> ArrayUInt(bb.asIntBuffer(), shape)
            Datatype.FLOAT -> ArrayFloat(bb.asFloatBuffer(), shape)
            Datatype.DOUBLE -> ArrayDouble(bb.asDoubleBuffer(), shape)
            Datatype.LONG -> ArrayLong(bb.asLongBuffer(), shape)
            Datatype.ULONG -> ArrayULong(bb.asLongBuffer(), shape)
            Datatype.OPAQUE -> ArrayOpaque(bb, shape)
            else -> throw IllegalStateException("unimplemented type= $datatype")
        }
        // convert to array of Strings by reducing rank by 1
        if (datatype == Datatype.CHAR) {
            return (result as ArrayUByte).makeStringsFromBytes()
        }

        /*
        else if (datatype.primitiveClass == StructureData::class.java) {
            val recsize: Int = layout.getElemSize()
            while (layout.hasNext()) {
                val chunk: Layout.Chunk = layout.next()
                raf.order(byteOrder)
                state.pos = chunk.srcPos
                raf.readFully(pa, chunk.destElem * recsize, chunk.nelems * recsize)
            }
            return pa
        } else if (datatype.primitiveClass == String::class.java) {
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
    fun readStructureData(state: OpenFileState, layout: Layout, shape : IntArray, members : List<StructureMember>): ArrayStructureData {
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
    if (matt.mds.type == DataspaceType.Null) {
        return emptyList<Any>()
    }

    // Vlens
    if (h5type.hdfType == Datatype5.Vlen) {
        return readVlenData(matt, h5type)
    }

    if (h5type.hdfType == Datatype5.Compound) {
        return readCompoundData(matt, h5type)
    }

    var shape: IntArray = matt.mds.dims

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
    val layout: Layout = LayoutRegular(matt.dataPos, elemSize, shape, Section(shape))

    val h5reader = H5reader(this)
    val dataArray = h5reader.readData(state, layout, readDtype, shape)

    // convert attributes to enum strings
    if (h5type.hdfType == Datatype5.Enumerated) {
        // hopefully this is shared and not replicated
        val enumMsg = matt.mdt as DatatypeEnum
        return convertEnums(enumMsg.valuesMap, dataArray)
    }

    return dataArray.toList()
}

internal fun convertEnums(map: Map<Int, String>, values: ArrayTyped<*>): List<String> {
    val wtf : KClass<out ArrayTyped<*>> = values::class // LOOK is there something simpler ?
    return values.map {
        val num = when (wtf.simpleName) {
           "ArrayUByte" ->  (it as UByte).toInt()
           "ArrayUShort" ->  (it as UShort).toInt()
           "ArrayUInt" ->  (it as UInt).toInt()
            else -> RuntimeException("unknown ${wtf.simpleName}")
        }
        map[num] ?: "Unknown enum number=$it"
    }
}

internal fun H5builder.readCompoundData(matt: AttributeMessage, h5type: H5Type) : List<*> {
    val shape: IntArray = matt.mds.dims
    val state = OpenFileState(0, h5type.endian)
    val layout: Layout = LayoutRegular(matt.dataPos, h5type.elemSize, shape, Section(shape))

    val compoundType = matt.mdt as DatatypeCompound
    val members = compoundType.members.map { sm5  ->
        val memberType = H5Type(sm5.mdt)

        val lamda: ((Long) -> String)?  = if (memberType.hdfType == Datatype5.Reference)
            { it -> (this@readCompoundData).convertReferenceToDataObjectName(it) }
        else null

        H5StructureMember(sm5.name, memberType.datatype, sm5.offset, sm5.dims, memberType.hdfType, lamda)
    }

    val h5reader = H5reader(this)
    val h5heap = H5heap(this)
    val sdataArray = h5reader.readStructureData(state, layout, shape, members)
    members.filter { it.datatype == Datatype.STRING }.forEach { member ->
        sdataArray.forEach { sdata ->
            val sval = h5heap.readHeapString(sdataArray.bb, sdata.offset + member.offset)!!
            println("offset ${sdata.offset + member.offset} sval $sval")
            sdata.putOnHeap(member, sval)
        }
    }
    return sdataArray.toList()
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

internal fun H5builder.readVlenData(matt: AttributeMessage, h5type: H5Type) : List<*> {
    val shape: IntArray = matt.mds.dims
    val layout2 = LayoutRegular(matt.dataPos, matt.mdt().elemSize, shape, Section(shape))
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
        val vlenMdt = matt.mdt as DatatypeVlen
        val base = vlenMdt.base

        // variable length array of references, get translated into strings
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
            return refsList
        }

        // general case is to read an array of vlen objects
        // each vlen generates an Array, have to flatten them
        val readType = H5Type(base, null).datatype
        val result = mutableListOf<Any>()
        var count = 0
        while (layout2.hasNext()) {
            val chunk: Layout.Chunk = layout2.next()
            for (i in 0 until chunk.nelems) {
                val address: Long = chunk.srcPos + layout2.elemSize * i
                val vlenArray = h5heap.getHeapDataArray(address, readType, base.endian())
                vlenArray.forEach{
                    if (it != null) {
                        result.add(it)
                    }
                }
                count++
            }
        }
        return result
    } // vlen case
}