package com.sunya.cdm.api

import com.sunya.cdm.util.makeValidCdmObjectName

class Group(orgName : String,
            val typedefs : List<Typedef>,
            val dimensions : List<Dimension>,
            val attributes : List<Attribute>,
            variableBuilders : List<Variable.Builder>,
            groupBuilders : List<Group.Builder>,
            val parent: Group?
    ) {
    val name : String
    val variables : List<Variable>
    val groups : List<Group>

    init {
        name = makeValidCdmObjectName(orgName)
        variables = variableBuilders.map { it.build(this) }
        groups = groupBuilders.map { it.build(this) }
    }

    // find dimension in this group or a parent
    fun findDimension(dimName: String) : Dimension? {
        return dimensions.find{it.name == dimName}?: parent?.findDimension(dimName)
    }

    fun allVariables() : List<Variable> {
        val allVariables = mutableListOf<Variable>()
        allVariables.addAll(variables)
        groups.forEach  { allVariables.addAll(it.allVariables()) }
        return allVariables
    }

    fun fullname() : String {
        return if (parent == null) "" else "${parent.fullname()}/$name"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Group) return false

        if (name != other.name) return false
        if (typedefs != other.typedefs) return false
        if (dimensions != other.dimensions) return false
        if (attributes != other.attributes) return false
        if (parent != other.parent) return false
        if (variables != other.variables) return false
        if (groups != other.groups) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + typedefs.hashCode()
        result = 31 * result + dimensions.hashCode()
        result = 31 * result + attributes.hashCode()
        result = 31 * result + (parent?.hashCode() ?: 0)
        result = 31 * result + variables.hashCode()
        result = 31 * result + groups.hashCode()
        return result
    }

    override fun toString(): String {
        return if (parent == null) "root" else name
    }

    class Builder(val name : String) {
        val dimensions = mutableListOf<Dimension>()
        val attributes = mutableListOf<Attribute>()
        val typedefs = mutableListOf<Typedef>()
        val variables = mutableListOf<Variable.Builder>()
        val groups = mutableListOf<Group.Builder>()
        var parent : Group.Builder? = null

        // add if dim name not already added, else RuntimeException.
        fun addDimension(dim: Dimension) : Builder {
            if ( dimensions.find {it.name == dim.name } == null) {
                dimensions.add(dim)
            } else {
                throw RuntimeException("tried to add duplicate dimension '${dim.name}'")
            }
            return this
        }

        // return true if did not exist and was added
        fun addDimensionIfNotExists(dim: Dimension): Boolean {
            if (dimensions.find {it.name == dim.name } != null) {
                return false
            }
            addDimension(dim)
            return true
        }

        fun replaceDimension(dim: Dimension): Boolean {
            val found = dimensions.removeIf{ it.name == dim.name }
            addDimension(dim)
            return found
        }

        fun addAttribute(att: Attribute) : Builder {
            attributes.add(att)
            return this
        }

        // add if vb name not already added
        fun addVariable(vb: Variable.Builder) : Builder {
            if (variables.find {it.name == vb.name } == null) {
                variables.add(vb)
            } else {
                // throw RuntimeException("tried to add duplicate variable '${vb.name}'")
                println("tried to add duplicate variable '${vb.name}'")
            }
            return this
        }

        // add if typedef name not already added
        fun addTypedef(typedef : Typedef) : Builder {
            if (typedefs.find { it.name == typedef.name} == null)
                typedefs.add(typedef)
            return this
        }

        fun addGroup(nested: Group.Builder) : Builder {
            this.groups.add(nested)
            nested.parent = this
            return this
        }

        fun removeGroupIfExists(groupName: String): Boolean {
            val egroup = groups.find {it.name == groupName }
            return if (egroup == null) false else groups.remove(egroup)
        }

        /* dunno if its worth the complexity - not currently used
        fun isParent(other: Group.Builder): Boolean {
            var found: Group.Builder = other
            while (found != this && found.parent != null) {
                found = found.parent!!
            }
            return found == this
        }

        fun commonParent(other : Group.Builder) : Group.Builder? {
            if (this == other) return this
            if (this.isParent(other)) return this
            if (other.isParent(this)) return other
            var found : Group.Builder? = other
            while (found != null && !found.isParent(this)) {
                found = found.parent
            }
            return found
        }
         */

        fun build(parent : Group?) : Group {
            val useName = makeValidCdmObjectName(name)
            return Group(useName, typedefs, dimensions, attributes, variables, groups, parent)
        }
    }
}