package com.sunya.cdm.api

import com.sunya.cdm.util.makeValidCdmObjectName

data class Attribute<T>(val orgName : String, val datatype : Datatype<T>, val values : List<T>) {
    val name = makeValidCdmObjectName(orgName)
    val isString = (datatype == Datatype.STRING)

    companion object {
        fun from(name : String, value : String) = Attribute(name, Datatype.STRING, listOf(value))
    }

    class Builder<T>(val name : String, var datatype : Datatype<T>) {
        var values : List<T> = emptyList()

        fun setValues(values : List<*>) : Builder<T> {
            this.values = values as List<T> // TODO immutable ??
            return this
        }

        fun setValuesCheckChar(values : List<*>) : Builder<T> {
            this.values = values as List<T> // TODO immutable ??
            return this
        }

        fun setValue(value : Any) : Builder<T> {
            this.values = listOf(value as T)
            return this
        }

        fun build() : Attribute<T> {
            /*
            if (datatype == Datatype.CHAR && values == null) {
                values = listOf("") // special case to match c library
            }
            // TODO is this worth it ? make calling routine do this ?? note this only happens on build()
            var useType = if (datatype == Datatype.CHAR && values != null && values!!.isNotEmpty() && values!![0] is String) {
               Datatype.STRING
            } else {
                datatype
            }
            // return Attribute(name, useType, values ?: emptyList<T>())
             */

            return Attribute(name, datatype, values)
        }
    }
}
