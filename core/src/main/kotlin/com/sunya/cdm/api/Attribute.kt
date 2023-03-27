package com.sunya.cdm.api

import com.sunya.cdm.util.makeValidCdmObjectName

data class Attribute(val orgName : String, val datatype : Datatype, val values : List<*>) {
    val name = makeValidCdmObjectName(orgName)

    constructor(name : String, svalue : String) : this(name, Datatype.STRING, List<String>(1) { svalue })

    val isString = (datatype == Datatype.STRING)

    class Builder {
        var name : String? = null
        var datatype : Datatype? = null
        var values : List<*>? = null

        fun setName(name : String) : Builder {
            this.name = name
            return this
        }

        fun setDatatype(type : Datatype) : Builder {
            this.datatype = type
            return this
        }

        fun build() : Attribute {
            if (datatype == Datatype.CHAR && values == null) {
                values = listOf("") // special case to match c library
            }
            val useType = if (datatype == Datatype.CHAR) Datatype.STRING else datatype
            return Attribute(name!!, useType!!, values ?: emptyList<Any>())
        }
    }
}
