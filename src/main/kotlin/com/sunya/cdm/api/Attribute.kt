package com.sunya.cdm.api

// could use Array<*>
data class Attribute(val name : String, val dataType : DataType, val values : List<*>) {

    constructor(name : String, svalue : String) : this(name, DataType.STRING, List<String>(1) { svalue})

    val isString = (dataType == DataType.STRING)

    class Builder {
        var name : String? = null
        var dataType : DataType? = null
        var values :List<*>? = null

        fun setName(name : String) : Attribute.Builder {
            this.name = name
            return this
        }

        fun setDataType(type : DataType) : Attribute.Builder {
            this.dataType = type
            return this
        }

        fun build() : Attribute {
            if (dataType == DataType.CHAR && values == null) {
                values = listOf("") // special case to match c library
            }
            val useType = if (dataType == DataType.CHAR) DataType.STRING else dataType
            return Attribute(name!!, useType!!, values ?: emptyList<Any>())
        }
    }
}
