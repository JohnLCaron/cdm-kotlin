package com.sunya.cdm.api

import com.sunya.cdm.util.makeValidCdmObjectName

data class Attribute(val orgName : String, val datatype : Datatype, val values : List<*>) {
    val name = makeValidCdmObjectName(orgName)
    val isString = (datatype == Datatype.STRING)

    constructor(name : String, svalue : String) : this(name, Datatype.STRING, List<String>(1) { svalue })

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

        fun setValues(values : List<*>) : Builder {
            this.values = values
            return this
        }

        fun build() : Attribute {
            if (datatype == Datatype.CHAR && values == null) {
                values = listOf("") // special case to match c library
            }
            var useType = datatype
            // TODO is this worth it ? make calling routine do this ??
            // note this only happens on build()
            if (datatype == Datatype.CHAR && values != null && values!!.isNotEmpty() && values!![0] is String) {
                useType = Datatype.STRING
            }
            return Attribute(name!!, useType!!, values ?: emptyList<Any>())
        }
    }
}
