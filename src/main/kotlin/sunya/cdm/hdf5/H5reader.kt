package sunya.cdm.hdf5

import sunya.cdm.api.*
import sunya.cdm.iosp.*
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.ArrayList

class H5reader(val header: H5builder) {
    val raf = header.raf

    @Throws(IOException::class)
    fun readData(state: OpenFileState, layout: Layout, dataType: DataType, shape : IntArray): ArrayTyped<*> {
        val sizeBytes = Section(shape).computeSize().toInt() * layout.elemSize
        val bb = ByteBuffer.allocate(sizeBytes)
        while (layout.hasNext()) {
            val chunk: Layout.Chunk = layout.next()
            state.pos = chunk.srcPos
            raf.readIntoByteBuffer(state, bb, layout.elemSize * chunk.destElem.toInt(), layout.elemSize * chunk.nelems)
        }

        val result = when (dataType.primitiveClass) {
            Byte::class.java -> ArrayByte(bb, shape)
            Short::class.java -> ArrayShort(bb.asShortBuffer(), shape)
            Int::class.java -> ArrayInt(bb.asIntBuffer(), shape)
            Float::class.java -> ArrayFloat(bb.asFloatBuffer(), shape)
            Double::class.java -> ArrayDouble(bb.asDoubleBuffer(), shape)
            Long::class.java -> ArrayLong(bb.asLongBuffer(), shape)
            else -> throw IllegalStateException("unimplemented type= $dataType")
        }
        // convert to array of Strings by reducing rank by 1
        if (dataType == DataType.CHAR) {
            return (result as ArrayByte).makeStringsFromBytes()
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
    fun readStructureData(state: OpenFileState, layout: Layout, dataType: DataType, shape : IntArray, members : StructureMembers): ArrayTyped<*> {
        val sizeBytes = Section(shape).computeSize().toInt() * layout.elemSize
        val bb = ByteBuffer.allocate(sizeBytes)
        while (layout.hasNext()) {
            val chunk: Layout.Chunk = layout.next()
            state.pos = chunk.srcPos
            raf.readIntoByteBuffer(state, bb, layout.elemSize * chunk.destElem.toInt(), layout.elemSize * chunk.nelems)
        }

        return ArrayStructureData(bb, layout.elemSize, shape, members)
    }
}

internal fun H5builder.readAttributeData(
    matt: AttributeMessage,
    h5type: H5Type,
    dataType: DataType
): List<*> {
    // Vlens
    if (h5type.hdfType == Datatype5.Vlen) {
        return readVlenData(matt, h5type, dataType)
    }

    if (h5type.hdfType == Datatype5.Compound) {
        return readCompoundData(matt, h5type, dataType)
    }

    var shape: IntArray = matt.mds.dims

    var readDtype: DataType = dataType
    var endian: ByteOrder = h5type.endian

    if (h5type.hdfType === Datatype5.Time) { // time
        readDtype = h5type.dataType
    } else if (h5type.hdfType == Datatype5.String) { // char
        if (h5type.elemSize > 1) {
            val newShape = IntArray(shape.size + 1)
            System.arraycopy(shape, 0, newShape, 0, shape.size)
            newShape[shape.size] = h5type.elemSize
            shape = newShape
        }

    } else if (h5type.hdfType == Datatype5.Enumerated) { // enum
        val baseInfo = h5type.base!!
        readDtype = baseInfo.dataType
        endian = baseInfo.endian
    }

    val state = OpenFileState(0, endian)
    val layout: Layout = LayoutRegular(matt.dataPos, h5type.elemSize, shape, Section(shape))

    //     fun readData(state: OpenFileState, layout: Layout, dataType: DataType, shape : IntArray): ArrayTyped<*> {
    val h5reader = H5reader(this)
    val dataArray = h5reader.readData(state, layout, h5type.dataType, shape)

    /*
    if (dataType === DataType.OPAQUE) {
        dataArray = pdata as Array<*>
    } else if (dataType === DataType.CHAR) {
        if (h5type.elemSize > 1) { // chop back into pieces
            val bdata = pdata as ByteArray
            val strlen: Int = h5type.elemSize
            val n = bdata.size / strlen
            val sarray = mutableListOf<String>()
            for (i in 0 until n) {
                val sval: String = this.convertString(bdata, i * strlen, strlen)
                sarray.add(sval)
            }
            return sarray
        } else {
            val sval: String = this.convertString(pdata as ByteArray)
            return listOf(sval)
        }
    } else {
        dataArray = if (pdata is Array) pdata else Arrays.factory(readDtype, shape, pdata)
    }

    // convert attributes to enum strings
    if (h5type.hdfType === Datatype5.Enumerated && matt.mdt.map != null) {
        dataArray = convertEnums(matt.mdt.map, dataType, dataArray as Array<Number?>?)
    }

     */
    return dataArray.toList()
}

internal fun H5builder.readCompoundData(matt: AttributeMessage, h5type: H5Type, dataType: DataType) : List<*> {
    val shape: IntArray = matt.mds.dims
    val state = OpenFileState(0, h5type.endian)
    val layout: Layout = LayoutRegular(matt.dataPos, h5type.elemSize, shape, Section(shape))

    val compoundType = matt.mdt as DatatypeCompound
    val ms = compoundType.members.map { sm5  ->
        val memberType = H5Type(sm5.mdt)
        StructureMember(sm5.name, memberType.dataType, sm5.offset, 1)
    }
    val members = StructureMembers(ms)

    val h5reader = H5reader(this)
    val dataArray = h5reader.readStructureData(state, layout, h5type.dataType, shape, members)
    return dataArray.toList()
}

internal fun H5builder.readVlenData(matt: AttributeMessage, h5type: H5Type, dataType: DataType) : List<*> {
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
                sarray.add(sval?: "failed")
            }
        }
        return sarray
    } // vlen Strings case

    // Vlen (non-String)
    else {
        var endian: ByteOrder? = h5type.endian
        var readType: DataType = dataType
        if (h5type.base.hdfType == Datatype5.Reference) { // reference
            readType = DataType.LONG
            endian = ByteOrder.LITTLE_ENDIAN // apparently always LE
        }

        // variable length array of references, get translated into strings
        if (h5type.base.hdfType == Datatype5.Reference) {
            val refsList = mutableListOf<String>()
            while (layout2.hasNext()) {
                val chunk: Layout.Chunk = layout2.next() ?: continue
                for (i in 0 until chunk.nelems) {
                    val address: Long = chunk.srcPos + layout2.elemSize * i
                    val vlenArray = h5heap.getHeapDataArray(address, readType, endian)
                    val refsArray = h5heap.convertReferenceArray(vlenArray as Array<Long>)
                    for (s in refsArray) {
                        refsList.add(s)
                    }
                }
            }
            return refsList
        }

        throw RuntimeException("vlen not implemented")

        /* general case is to read an array of vlen objects
        // each vlen generates an Array - so return ArrayObject of Array
        val size = layout2.totalNelems as Int
        val vlenStorage: StorageMutable<Array<String>> = ArrayVlen.createStorage(readType, size, null)
        var count = 0
        while (layout2.hasNext()) {
            val chunk: Layout.Chunk = layout2.next() ?: continue
            for (i in 0 until chunk.getNelems()) {
                val address: Long = chunk.getSrcPos() + layout2.getElemSize() * i
                val vlenArray: Array<*> = getHeapDataArray(address, readType, endian)
                if (vinfo.typeInfo.base.hdfType === 7) {
                    vlenStorage.setPrimitiveArray(count, h5iosp.convertReferenceArray(vlenArray as Array<Long?>))
                } else {
                    vlenStorage.setPrimitiveArray(count, vlenArray)
                }
                count++
            }
        }
        return ArrayVlen.createFromStorage(readType, shape, vlenStorage)
        */
    } // vlen case
}