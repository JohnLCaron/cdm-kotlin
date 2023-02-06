package sunya.cdm.hdf5

import sunya.cdm.api.Attribute
import sunya.cdm.api.DataType
import sunya.cdm.api.Group
import sunya.cdm.api.Section
import sunya.cdm.iosp.Layout
import sunya.cdm.iosp.LayoutRegular
import sunya.cdm.iosp.OpenFileState
import java.nio.ByteOrder

internal fun H5builder.buildCdm(h5root : H5Group) : Group {
    return buildGroup(h5root, null)
}

internal fun H5builder.buildGroup(group5 : H5Group, parent: Group?) : Group {
    val builder = Group.Builder(group5.name)
    for (att5 in group5.attributes()) {
        println("${att5.show()}")
        builder.addAttribute(buildAttribute(att5))
    }
    return builder.build(parent)
}

internal fun H5builder.buildAttribute(att5 : AttributeMessage) : Attribute {
    val h5type = H5Type(att5.mdt)
    val values = this.readAttributeData(att5, h5type, h5type.dataType)
    return Attribute(att5.name,  h5type.dataType, values)
}

// read non-Structure attribute values without creating a Variable
private fun H5builder.readAttributeData(
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
        readDtype = h5type.dataType!!
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
        readDtype = baseInfo.dataType!!
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