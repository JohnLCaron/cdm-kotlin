package com.sunya.cdm.api

/** A filled section of multidimensional array indices, plus the variable shape. Ignoring stride for now. */
data class SectionL(val ranges : List<LongProgression>, val varShape : LongArray) {
    val rank = ranges.size
    val shape : LongArray // or IntArray ??
    val totalElements : Long

    init {
        ranges.forEach { require(it.last - it.first + 1 < Int.MAX_VALUE) }
        shape = ranges.map { (it.last - it.first + 1) }.toLongArray()
        totalElements = shape.computeSize()
        require(totalElements >= 0) // make sure no overflow
    }

    constructor(shape: LongArray) : this( shape.map {
        LongProgression.fromClosedRange(0L, it - 1L, 1L) }, shape)

    constructor(shape: LongArray, varShape : LongArray) : this( shape.map {
        LongProgression.fromClosedRange(0L, it - 1L, 1L) }, varShape)

    companion object {
        // for testing only, assumes varshape is last() + 1
        fun fromSpec(spec : String) : SectionL {
            val sp = SectionP.fromSpec(spec)
            val varShape = sp.ranges.map { it!!.last + 1 }.toLongArray()
            return SectionP.fill(sp, varShape)
        }
    }
}

fun IntArray.computeSize(): Int {
    var product = 1
    this.forEach { product *= it }
    return product
}

fun IntArray.toLongArray(): LongArray = LongArray(this.size) { this[it].toLong() }

fun IntArray.equivalent(other: IntArray): Boolean {
    return this.toLongArray().equivalent(other.toLongArray())
}

// return outerShape, innerLength
fun IntArray.breakoutInner() : Pair<IntArray, Int> {
    val rank = this.size
    val innerLength: Int = this[rank - 1]
    val outerShape = IntArray(rank - 1)
    System.arraycopy(this, 0, outerShape, 0, rank - 1)
    return Pair(outerShape, innerLength)
}

fun LongArray.computeSize(): Long {
    var product = 1L
    this.forEach { product *= it }
    return product
}

fun LongArray.toIntArray(): IntArray = IntArray(this.size) { this[it].toInt() }

fun LongArray.equivalent(other: LongArray): Boolean {
    if (this.isScalar() and other.isScalar()) {
        return true
    }
    return this.contentEquals(other)
}

fun LongArray.isScalar(): Boolean {
    return this.size == 0 || this.size == 1 && this[0] < 2
}