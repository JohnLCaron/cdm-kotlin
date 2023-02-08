package sunya.cdm.hdf5

import sunya.cdm.api.DataType
import sunya.cdm.api.Section
import sunya.cdm.iosp.*
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.ArrayList

class H5reader(val header: H5builder) {
    val raf = header.raf

    @Throws(IOException::class)
    fun readData(state: OpenFileState, layout: Layout, dataType: DataType, shape : IntArray): ArrayTyped<*> {
        val sizeBytes = Section(shape).computeSize().toInt()
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
}

@Throws(IOException::class)
internal fun H5builder.readAttribute(matt: MessageAttribute): Attribute? {
    val h5type = H5Type(att5.mdt)

    // check for empty attribute case
    if (matt.mds.type === 2) {
        return if (dtype === ArrayType.CHAR) {
            // empty char considered to be a null string attr
            Attribute.builder(matt.name).setArrayType(ArrayType.STRING).build()
        } else {
            Attribute.builder(matt.name).setArrayType(dtype).build()
        }
    }

    val attData: Array<*>
    try {
        attData = readAttributeData(matt, vinfo, dtype)
    } catch (e: InvalidRangeException) {
        println("failed to read Attribute " + matt.name)
        return null
    }

    val result: Attribute
    result = if (attData.isVlen()) {
        val dataList: MutableList<Any> = ArrayList()
        for (value in attData) {
            val nestedArray = value as Array<*>
            for (nested in nestedArray) {
                dataList.add(nested)
            }
        }
        // TODO probably wrong? flattening them out ??
        Attribute.builder(matt.name).setValues(dataList, matt.mdt.unsigned).build()
    } else {
        Attribute.fromArray(matt.name, attData)
    }
    raf.order(RandomAccessFile.LITTLE_ENDIAN)
    return result
}


// read non-Structure attribute values without creating a Variable
internal fun H5builder.readAttributeData(
    matt: AttributeMessage,
    h5type: H5Type,
    dataType: DataType
): List<*> {
    var shape: IntArray = matt.mds.dims
    // why doesnt this depend on matt.mds.DataspaceType ?
    val layout2 = LayoutRegular(matt.dataPos, matt.mdt.elemSize, shape, Section(shape))
    val h5heap = H5heap(this)
    val h5reader = H5reader(this)

    // Strings
    if (h5type.hdfType == Datatype5.Vlen && h5type.isVString) {
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
    if (h5type.hdfType == Datatype5.Vlen) {
        var endian: ByteOrder? = h5type.endian
        var readType: DataType = dataType
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
                    val refsArray = h5heap.convertReferenceArray(vlenArray as Array<Long>)
                    for (s in refsArray) {
                        refsList.add(s)
                    }
                }
            }
            return refsList
        }

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

    // NON-STRUCTURE CASE
    var readDtype: DataType = dataType
    var elemSize: Int = dataType.size
    var endian: ByteOrder? = h5type.endian
    if (h5type.hdfType === Datatype5.Time) { // time
        readDtype = h5type.dataType
        elemSize = readDtype.size
    } else if (h5type.hdfType == Datatype5.String) { // char
        if (h5type.elemSize > 1) {
            val newShape = IntArray(shape.size + 1)
            System.arraycopy(shape, 0, newShape, 0, shape.size)
            newShape[shape.size] = h5type.elemSize
            shape = newShape
        }
    } else if (h5type.hdfType == Datatype5.Opaque) { // opaque
        elemSize = h5type.elemSize
    } else if (h5type.hdfType == Datatype5.Enumerated) { // enum
        val baseInfo = h5type.base!!
        readDtype = baseInfo.dataType
        elemSize = readDtype.size
        endian = baseInfo.endian
    }

    val state = OpenFileState(0, h5type.endian)
    val layout: Layout = LayoutRegular(matt.dataPos, elemSize, shape, Section(shape))

    //     fun readData(state: OpenFileState, layout: Layout, dataType: DataType, shape : IntArray): ArrayTyped<*> {
    val dataArray = h5reader.readData(state, layout, dataType, shape)

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