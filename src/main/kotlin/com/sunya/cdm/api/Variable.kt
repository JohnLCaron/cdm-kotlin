package com.sunya.cdm.api

data class Variable(
    val group : Group,
    val name: String,
    val datatype: Datatype,
    val dimensions: List<Dimension>,
    val attributes: List<Attribute>,
    val spObject: Any?,
) {
    val rank : Int = dimensions.size
    val shape : IntArray = dimensions.map { it.length }.toIntArray()
    val nelems : Long = computeSize(this.shape)
    val elementSize = datatype.size

    fun isUnlimited() = dimensions.isNotEmpty() &&
            dimensions.map { it.isUnlimited }.reduce { a,b -> a or b}

    /////////////////////////////////////////////////////////////////////////////////////////
    fun computeSize(shape: IntArray?): Long {
        if (shape == null) {
            return 1
        }
        var product: Long = 1
        for (aShape in shape) {
            if (aShape < 0) {
                break // stop at vlen
            }
            product *= aShape.toLong()
        }
        return product
    }

    class Builder {
        var name : String? = null
        var datatype : Datatype? = null
        val dimensions = mutableListOf<Dimension>()
        val attributes = mutableListOf<Attribute>()
        var spObject: Any? = null

        fun isUnlimited() = dimensions.isNotEmpty() &&
                dimensions.map { it.isUnlimited }.reduce { a,b -> a or b}

        fun build(group : Group) : Variable {
            return Variable(group, name!!, datatype!!, dimensions, attributes, spObject)
        }
    }
}