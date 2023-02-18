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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Variable) return false

        if (name != other.name) return false
        if (datatype != other.datatype) return false
        if (dimensions != other.dimensions) return false
        if (attributes != other.attributes) return false
        if (!shape.contentEquals(other.shape)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + datatype.hashCode()
        result = 31 * result + dimensions.hashCode()
        result = 31 * result + attributes.hashCode()
        result = 31 * result + shape.contentHashCode()
        return result
    }

    class Builder {
        var name : String? = null
        var datatype : Datatype? = null
        val dimensions = mutableListOf<Dimension>()
        val attributes = mutableListOf<Attribute>()
        var spObject: Any? = null
        var dimList: List<String>? = null

        fun isUnlimited() = dimensions.isNotEmpty() &&
                dimensions.map { it.isUnlimited }.reduce { a,b -> a or b}

        fun build(group : Group) : Variable {
            val useDimensions = if (dimList != null) dimList!!.map {
                group.findDimension(it) ?: throw RuntimeException("Cant find dimension $it")
            } else dimensions

            return Variable(group, name!!, datatype!!, useDimensions, attributes, spObject)
        }
    }
}