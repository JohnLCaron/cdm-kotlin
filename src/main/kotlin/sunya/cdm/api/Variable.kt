package sunya.cdm.api

data class Variable(
    val group : Group,
    val name: String,
    val dataType: DataType,
    val dimensions: List<Dimension>,
    val attributes: List<Attribute>,
    val spObject: Any?,
) {
    val isUnlimited = dimensions.isNotEmpty() &&
            dimensions.map { it.isUnlimited }.reduce { a,b -> a or b}
    val rank : Int = dimensions.size
    val shape : IntArray = dimensions.map { it.length }.toIntArray()
    val nelems : Long = computeSize(this.shape)
    val elementSize = dataType.size

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

    fun cdlString() : String {
        return buildString {
            append("  ${dataType.cdlName} $name")
            if (dimensions.isNotEmpty()) {
                append("(")
                dimensions.forEachIndexed { idx, it ->
                    if (idx > 0) append(", ")
                    append(it.name + "=" + it.length)
                }
                append(")")
            }
            append(";")
            if (attributes.isNotEmpty()) {
                append("\n")
                attributes.forEach { append("      ${it.cdlString()}\n") }
            } else {
                append("\n")
            }
        }
    }

    class Builder {
        var name : String? = null
        var dataType : DataType? = null
        val dimensions = mutableListOf<Dimension>()
        val attributes = mutableListOf<Attribute>()
        var spObject: Any? = null

        fun build(group : Group) : Variable {
            return Variable(group, name!!, dataType!!, dimensions, attributes, spObject)
        }
    }
}