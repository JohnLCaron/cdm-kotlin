package com.sunya.netchdf.hdf4

import com.sunya.cdm.util.Indent
import java.util.*

/*
 * http://newsroom.gsfc.nasa.gov/sdptoolkit/hdfeosfaq.html
 * 
 * 3.2 What types of metadata are embedded in an HDF-EOS file and what are the added storage requirements?
 * An HDF-EOS file must contain ECS "core" metadata which is essential for ECS search services. Core metadata are
 * populated using the SDP Toolkit, rather than through HDF-EOS calls. "Archive" metadata (supplementary information
 * included by the data provider) may also be present. If grid, point, or swath data types have been used,
 * there also will be structural metadata describing how these data types have been translated into standard HDF
 * data types. Metadata resides in human-readable form in the Object Descriptor Language (ODL). Structural metadata uses
 * 32K of storage, regardless of the amount actually required. The sizes of the core and archive metadata vary
 * depending on what has been entered by the user.
 * 
 * 3.3 What are the options for adding ECS metadata to standard HDF files?
 * For data products that will be accessed by ECS but which remain in native HDF, there is a choice of
 *  1) adding no ECS metadata in the HDF file,
 *  2) inserting ECS metadata into the HDF file, or
 *  3) "appending" ECS metadata to the HDF file. "Append" means updating the HDF location table so that the appended
 * metadata becomes known to the HDF libraries/tools.
 * 
 * 3.4 Some DAACs currently provide descriptor files that give background information about the data. Will this
 * information be included in an HDF-EOS file? Yes. The descriptor file will be retained. It can be viewed by
 * EOSView if it stored either as a global attribute or a file annotation.
 */

class ODLtransform() {
    val renameGroups = listOf("SwathName", "GridName", "PointName")
    val wantGroup = listOf("GeoField", "DataField", "Dimension")
    val wantGroupNewName = listOf("Geolocation_Fields", "Data_Fields", "Dimension")

    fun transform(org: ODLgroup): ODLgroup {
        val root = ODLgroup("root", null)
        transform(org, root)
        return root
    }

    private fun transform(org: ODLgroup, trans: ODLgroup) {
        // println("massage(${parent.name}, ${org.name})")
        val namePair = org.attributes.find { renameGroups.contains(it.component1()) }
        if (namePair != null) {
            trans.name = namePair.component2()
        }

        if (org.name  == "Dimension") {
            trans.variables.add(transformDims(org))
            return
        }

        var wantName: String? = null
        wantGroup.forEachIndexed { idx, it ->
            if (it == org.name) {
                wantName = wantGroupNewName[idx]
            }
        }

        if (wantName != null) {
            val nestedTrans = ODLgroup(wantName!!, trans)
            trans.nested.add(nestedTrans)
            nestedTrans.variables.add(transformVariables(org))
        } else {
            org.nested.forEach { transform(it, trans) }
        }
    }

    fun transformDims(dimGroup : ODLgroup): ODLobject {
        val result = ODLobject("Dimensions")
        dimGroup.variables.filter { it.name.startsWith("Dimension") }.map {
            var name : String = "N/A"
            var size : String = "N/A"
            it.attributes.forEach { att ->
                if (att.component1() == "DimensionName") {
                    name = att.component2()
                }
                if (att.component1() == "Size") {
                    size = att.component2()
                }
            }
            result.attributes.add(Pair(name, size))
        }
        return result
    }

    fun transformVariables(fldGroup : ODLgroup): ODLobject {
        val result = ODLobject("Variables")
        fldGroup.variables.filter { it.name.contains("Field") }.map {
            var name : String = "N/A"
            var dims : String = "N/A"
            it.attributes.forEach { att ->
                if (att.component1().contains("FieldName")) {
                    name = att.component2()
                }
                if (att.component1().equals("DimList")) {
                    dims = att.component2()
                }
            }
            result.attributes.add(Pair(name, dims))
        }
        return result
    }

    fun massageV(variables: List<ODLobject>): List<ODLobject> {
        val result = mutableListOf<ODLobject>()
        variables.forEach {
            if (it.name.startsWith("Dimension")) {
                val dimVar = ODLobject(it.name)
                it.attributes.forEach { att ->
                    if (att.component1() == "DimensionName") {
                        dimVar.name = att.component2()
                    }
                    if (att.component1() == "Size") {
                        dimVar.attributes.add(att)
                    }
                }
                result.add(dimVar)
            }
        }
        return result
    }
}

data class ODLobject(var name : String) {
    val attributes = mutableListOf<Pair<String, String>>()
    fun toString(indent : Indent): String {
        return buildString {
            append("${indent}var $name\n")
            val nindent = indent.incr()
            attributes.forEach { append("$nindent${it.component1()}=${it.component2()}\n") }
        }
    }
}

class ODLgroup(var name : String, val parent : ODLgroup?) {
    val nested = mutableListOf<ODLgroup>()
    val variables = mutableListOf<ODLobject>()
    val attributes = mutableListOf<Pair<String, String>>()

    override fun toString(): String {
        return toString(Indent(2))
    }
    fun toString(indent : Indent): String {
        return buildString {
            append("${indent}group $name\n")
            val nindent = indent.incr()
            attributes.forEach { append("$nindent${it.component1()}=${it.component2()}\n") }
            variables.forEach{ append(it.toString(nindent)) }
            nested.forEach{ append( it.toString(nindent)) }
        }
    }
}

class ODLparser() {

    // parses the original ODL
    fun parseFromString(text: String): ODLgroup {
        val root = ODLgroup("root", null)
        var currentStruct = root
        var currentObject : ODLobject? = null
        val lineFinder = StringTokenizer(text, "\t\n\r\u000c")
        while (lineFinder.hasMoreTokens()) {
            var line = lineFinder.nextToken() ?: continue
            line = line.trim()
            if (line.startsWith("GROUP")) {
                currentStruct = startGroup(currentStruct, line)
            } else if (line.startsWith("OBJECT")) {
                currentObject = startObject(currentStruct, line)
            } else if (line.startsWith("END_OBJECT")) {
                endObject(currentObject!!, line)
                currentObject = null
            } else if (line.startsWith("END_GROUP")) {
                endGroup(currentStruct, line)
                currentStruct = currentStruct.parent!!
            } else if (line.startsWith("END")) {
                // noop
            } else if (currentObject != null) {
                addFieldToObject(currentObject!!, line)
            } else  {
                addFieldToGroup(currentStruct!!, line)
            }
        }
        return root
    }

    fun startGroup(parent: ODLgroup, line: String): ODLgroup {
        val stoke = StringTokenizer(line, "=")
        val toke = stoke.nextToken()
        require(toke == "GROUP")
        val name = stoke.nextToken()
        val group = ODLgroup(name, parent)
        parent.nested.add(group)
        return group
    }

    fun endGroup(current: ODLgroup, line: String) {
        val stoke = StringTokenizer(line, "=")
        val toke = stoke.nextToken()
        require(toke == "END_GROUP")
        val name = stoke.nextToken()
        require(name == current.name)
    }

    fun startObject(current: ODLgroup, line: String): ODLobject {
        val stoke = StringTokenizer(line, "=")
        val toke = stoke.nextToken()
        require(toke == "OBJECT")
        val name = stoke.nextToken()
        val obj = ODLobject(name)
        current.variables.add(obj)
        return obj
    }

    fun endObject(current: ODLobject, line: String) {
        val stoke = StringTokenizer(line, "=")
        val toke = stoke.nextToken()
        require(toke == "END_OBJECT")
        val name = stoke.nextToken()
        require(name == current.name)
    }

    fun addFieldToGroup(current: ODLgroup, line: String) {
        val stoke = StringTokenizer(line, "=")
        val name = stoke.nextToken()
        if (stoke.hasMoreTokens()) {
            var value = stoke.nextToken()
            if (value.startsWith("(")) {
                current.attributes.add( Pair(name, parseValueCollection(value)))
                return
            }
            value = stripQuotes(value)
            current.attributes.add(Pair(name, value))
        }
    }

    fun addFieldToObject(current: ODLobject, line: String) {
        val stoke = StringTokenizer(line, "=")
        val name = stoke.nextToken()
        if (stoke.hasMoreTokens()) {
            var value = stoke.nextToken()
            if (value.startsWith("(")) {
                current.attributes.add( Pair(name, parseValueCollection(value)))
                return
            }
            value = stripQuotes(value)
            current.attributes.add(Pair(name, value))
        }
    }

    fun parseValueCollection(valueInput: String) : String {
        val value = stripParens(valueInput)
        val stoke = StringTokenizer(value, ",")
        val list = mutableListOf<String>()
        while (stoke.hasMoreTokens()) {
            val t = stoke.nextToken()
            list.add(stripQuotes(t))
        }
        return list.joinToString(",")
    }

    fun stripParens(name: String): String {
        var name = name
        if (name.startsWith("(")) name = name.substring(1)
        if (name.endsWith(")")) name = name.substring(0, name.length - 1)
        return name
    }

    fun stripQuotes(name: String): String {
        var name = name
        if (name.startsWith("\"")) name = name.substring(1)
        if (name.endsWith("\"")) name = name.substring(0, name.length - 1)
        return name
    }
}