package com.sunya.cdm.util

import com.sunya.cdm.api.*

/**
 * Helper class for tree traversal and full names
 *
 * Find an object, with the specified (escaped full) name. It may be nested in multiple groups and/or structures.
 * An embedded "." is interpreted as structure.member.
 * An embedded "/" is interpreted as group/group or group/variable.
 * An embedded "@" is interpreted as variable@attribute.
 * If the name actually has a ".", you must escape it (call Escaping.backslashEscape(name)).
 * Any other chars may also be escaped, as the backslash is removed before matching.
 */
class CdmFullNames(val root: Group) {

    /** Find a Group, with the specified (escaped full) name. */
    fun findGroup(fullName: String): Group? {
        val names : List<String> = fullName.split("/").filter { !it.isEmpty() }
        return if (names.size == 0) root else root.findNestedGroupByRelativeName(names)
    }

    /** Find a Variable, with the specified (escaped full) name. */
    fun findVariable(fullName: String): Variable? {
        val names : List<String> = fullName.split("/").filter { !it.isEmpty() }
        if (names.isEmpty()) {
            return null
        }
        val varName = names[names.size - 1]
        val group = if (names.size == 1) root else root.findNestedGroupByRelativeName(names.subList(0, names.size - 1))
        if (group == null) {
            return null
        }
        return group.variables.find { it.name == varName }
    }

    /** Find a Dimension, with the specified (escaped full) name. */
    fun findDimension(fullName: String): Dimension? {
        val names : List<String> = fullName.split("/").filter { !it.isEmpty() }
        if (names.isEmpty()) {
            return null
        }
        val dimName = names[names.size - 1]
        val group = if (names.size == 1) root else root.findNestedGroupByRelativeName(names.subList(0, names.size - 1))
        if (group == null) {
            return null
        }
        return group.dimensions.find { it.name == dimName }
    }

    /** Find an Attribute, with the specified (escaped full) name.
     * An embedded "/" is interpreted as group/group or group/variable.
     * An embedded "@" is interpreted as variable@attribute or group@attribute.
     * '@attribute' is an attribute in the root group
     */
    fun findAttribute(fullName: String): Attribute? {
        val names : List<String> = fullName.split("/","@").filter { !it.isEmpty() }
        if (names.isEmpty()) {
            return null
        }
        val attName = names[names.size - 1]
        if (names.size == 1) {
            return root.attributes.find { it.name == attName }
        }
        val group = if (names.size < 3) root else root.findNestedGroupByRelativeName(names.subList(0, names.size - 2))
        if (group == null) {
            return null
        }
        val objName = names[names.size - 2]

        // nested group or variable ??
        val ngroup = group.groups.find { it.name == objName }
        val variable = group.variables.find { it.name == objName }
        val groupatt = ngroup?.attributes?.find { it.name == attName }
        val varatt = variable?.attributes?.find { it.name == attName }

        if (groupatt != null && varatt != null) {
            throw RuntimeException("attribute '$fullName' is ambiguous - contained in both group and variable")
        }
        return varatt?: groupatt
    }
}

fun Group.findNestedGroupByRelativeName(relativeNames : List<String>) : Group? {
    if (relativeNames.isEmpty()) {
        return null
    }
    val found = groups.find { it.name == relativeNames[0] }
    if (found == null) {
        return null
    }
    if (relativeNames.size > 1) {
        return found.findNestedGroupByRelativeName(relativeNames.subList(1, relativeNames.size))
    }
    return found
}
