package sunya.cdm.iosp

import java.nio.FloatBuffer

class ArrayFloat(val values : FloatBuffer, val shape : IntArray) : ArrayTyped<Float>() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ArrayFloat

        if (values != other.values) return false
        if (!shape.contentEquals(other.shape)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = values.hashCode()
        result = 31 * result + shape.contentHashCode()
        return result
    }


    override fun toString(): String {
        return buildString {
            append("shape=${shape.contentToString()})\n")
            for (i in 0 until values.limit()) { append("${values[i]},")}
            append("\n")
        }
    }
}