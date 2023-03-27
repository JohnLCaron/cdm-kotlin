package com.sunya.cdm.api

import com.sunya.cdm.util.makeValidCdmObjectName

data class Variable(
    val group : Group,
    val orgName: String,
    val datatype: Datatype,
    val dimensions: List<Dimension>,
    val attributes: List<Attribute>,
    val spObject: Any?,
) {
    val name = makeValidCdmObjectName(orgName)
    val rank : Int = dimensions.size
    val shape : IntArray = dimensions.map { it.length }.toIntArray()
    val nelems : Long = Section.computeSize(this.shape)

    fun isUnlimited() = dimensions.isNotEmpty() &&
            dimensions.map { it.isUnlimited }.reduce { a,b -> a or b}

    fun fullname() : String {
        return if (group.fullname() == "") name else "${group.fullname()}/$name"
    }

    /////////////////////////////////////////////////////////////////////////////////////////

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

    fun nameAndShape(): String {
        return "${datatype} ${fullname()}${shape.contentToString()}"
    }

    class Builder(val name : String) {
        var datatype : Datatype? = null
        val dimensions = mutableListOf<Dimension>()
        val attributes = mutableListOf<Attribute>()
        var spObject: Any? = null
        var dimList: List<String>? = null

        fun addAttribute(attr : Attribute) : Builder {
            attributes.add(attr)
            return this
        }

        fun addDimension(dim : Dimension) : Builder {
            dimensions.add(dim)
            return this
        }

        fun setDatatype(datatype : Datatype) : Builder {
            this.datatype = datatype
            return this
        }

        fun setDimensionsAnonymous(shape : IntArray) {
            dimensions.clear()
            dimList = null
            for (len in shape) {
                dimensions.add(Dimension("", len, false, false))
            }
        }

        fun isUnlimited() = dimensions.isNotEmpty() &&
                dimensions.map { it.isUnlimited }.reduce { a,b -> a or b}

        fun build(group : Group) : Variable {
            var useDimensions = dimensions.toList()
            if (dimList != null) {
                useDimensions = dimList!!.map { getDimension(it, group) }
            }

            val useName = makeValidCdmObjectName(name)
            return Variable(group, useName, datatype!!, useDimensions, attributes, spObject)
        }

        private fun getDimension(dimName : String, group : Group) : Dimension {
            val name = makeValidCdmObjectName(dimName)
            var d = group.findDimension(name)
            if (d == null) {
                try {
                    val length = dimName.toInt()
                    d = Dimension("", length, false, false)
                } catch(e : Exception) {
                    group.findDimension(name)
                    d = Dimension("", 1, false, false)
                }
            }
            return d!!
        }

        override fun toString(): String {
            return "'$name' $datatype, dimensions=$dimensions, dimList=$dimList)"
        }
    }
}