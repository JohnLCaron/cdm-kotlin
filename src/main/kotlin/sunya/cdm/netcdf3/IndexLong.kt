package sunya.cdm.netcdf3

/** Uses longs for indexing, otherwise similar to dev.ucdm.array.Index  */
class IndexLong {
    private val shape: IntArray
    private val stride: LongArray
    private val rank: Int
    private val offset : Int // element = offset + stride[0]*current[0] + ...
    private val current : IntArray // current element's index

    // shape = int[] {1}
    constructor() {
        shape = intArrayOf(1)
        stride = longArrayOf(1)
        rank = shape.size
        current = IntArray(rank)
        offset = 0
    }

    constructor(_shape: IntArray, _stride: LongArray) {
        shape = IntArray(_shape.size) // optimization over clone
        System.arraycopy(_shape, 0, shape, 0, _shape.size)
        stride = LongArray(_stride.size) // optimization over clone
        System.arraycopy(_stride, 0, stride, 0, _stride.size)
        rank = shape.size
        current = IntArray(rank)
        offset = 0
    }

    fun incr(): Long {
        var digit = rank - 1
        while (digit >= 0) {
            current[digit]++
            if (current[digit] < shape[digit]) break
            current[digit] = 0
            digit--
        }
        return currentElement()
    }

    fun currentElement(): Long {
        var value = offset.toLong()
        for (ii in 0 until rank) value += current[ii] * stride[ii]
        return value
    }

    companion object {
        fun computeSize(shape: IntArray): Long {
            var product: Long = 1
            for (ii in shape.indices.reversed()) product *= shape[ii].toLong()
            return product
        }
    }
}