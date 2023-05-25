package com.sunya.cdm.array

import com.sunya.cdm.api.*
import com.sunya.cdm.layout.IndexND
import com.sunya.cdm.layout.IndexSpace
import java.lang.foreign.ValueLayout
import java.nio.ByteBuffer

// fake ByteBuffer
class ArrayVlen<T>(shape : IntArray, val values : List<Array<T>>, val baseType : Datatype)
    : ArrayTyped<Array<T>>(ByteBuffer.allocate(1), Datatype.VLEN, shape) {

    override fun iterator(): Iterator<Array<T>> = ArrayIterator()
    private inner class ArrayIterator : AbstractIterator<Array<T>>() {
        private var idx = 0
        override fun computeNext() = if (idx >= values.size) done() else setNext(values[idx++])
    }

    override fun showValues(): String {
        return buildString {
            val iter = this@ArrayVlen.iterator()
            var count = 0
            for (value : Array<T> in iter) {
                append("${value.contentToString()},")
                count++
            }
        }
    }

    override fun section(section: Section): ArrayVlen<T> {
        val odo = IndexND(IndexSpace(section), this.shape.toLongArray())
        val sectionList = mutableListOf<Array<T>>()
        for (index in odo) {
            sectionList.add(values[odo.element().toInt()])
        }
        return ArrayVlen(section.shape.toIntArray(), sectionList, baseType)
    }

    // TODO other Array types need to override equals
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArrayVlen<*>) return false
        if (!super.equals(other)) return false

        if (values != other.values) return false
        return baseType == other.baseType
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + values.hashCode()
        result = 31 * result + baseType.hashCode()
        return result
    }

    companion object {
        fun fromArray(shape : IntArray, arrays: List<Array<*>>, baseType : Datatype) : ArrayVlen<*> {
            return when (baseType) {
                Datatype.BYTE -> ArrayVlen(shape, arrays.map { it as Array<Byte> }, baseType)
                Datatype.UBYTE, Datatype.ENUM1 -> ArrayVlen(shape, arrays.map { it as Array<UByte> }, baseType)
                Datatype.SHORT -> ArrayVlen(shape, arrays.map { it as Array<Short> }, baseType)
                Datatype.USHORT, Datatype.ENUM2 -> ArrayVlen(shape, arrays.map { it as Array<UShort> }, baseType)
                Datatype.INT -> ArrayVlen(shape, arrays.map { it as Array<Int> }, baseType)
                Datatype.UINT, Datatype.ENUM4 -> ArrayVlen(shape, arrays.map { it as Array<UInt> }, baseType)
                Datatype.LONG -> ArrayVlen(shape, arrays.map { it as Array<Long> }, baseType)
                Datatype.ULONG -> ArrayVlen(shape, arrays.map { it as Array<ULong> }, baseType)
                Datatype.FLOAT -> ArrayVlen(shape, arrays.map { it as Array<Float> }, baseType)
                Datatype.DOUBLE -> ArrayVlen(shape, arrays.map { it as Array<Double> }, baseType)
                Datatype.STRING -> ArrayVlen(shape, arrays.map { it as Array<String> }, baseType)
                Datatype.OPAQUE -> ArrayVlen(shape, arrays.map { it as Array<ByteBuffer> }, baseType)
                Datatype.COMPOUND -> ArrayVlen(shape, arrays.map { it as Array<ArrayStructureData.StructureData> }, baseType)
                else -> throw IllegalArgumentException("unsupported datatype ${baseType}")
            }
        }
    }
}