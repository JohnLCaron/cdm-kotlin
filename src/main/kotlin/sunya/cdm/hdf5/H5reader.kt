package sunya.cdm.hdf5

import sunya.cdm.api.DataType
import sunya.cdm.api.Section
import sunya.cdm.api.StructureData
import sunya.cdm.iosp.*
import java.io.IOException
import java.nio.ByteBuffer

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

        return when (dataType.primitiveClass) {
            Byte::class.java -> ArrayByte(bb, shape)
            Short::class.java -> ArrayShort(bb.asShortBuffer(), shape)
            Int::class.java -> ArrayInt(bb.asIntBuffer(), shape)
            Float::class.java -> ArrayFloat(bb.asFloatBuffer(), shape)
            Double::class.java -> ArrayDouble(bb.asDoubleBuffer(), shape)
            Long::class.java -> ArrayLong(bb.asLongBuffer(), shape)
            else -> throw IllegalStateException("unimplemented type= $dataType")
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
    }
}