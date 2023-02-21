package com.sunya.cdm.api

class Group(val name : String,
            val typedefs : List<Typedef>,
            val dimensions : List<Dimension>,
            val attributes : List<Attribute>,
            variableBuilders : List<Variable.Builder>,
            groupBuilders : List<Group.Builder>,
            val parent: Group?
    ) {
    val variables : List<Variable>
    val groups : List<Group>

    init {
        variables = variableBuilders.map { it.build(this) }
        groups = groupBuilders.map { it.build(this) }
    }

    fun findDimension(dimName: String) : Dimension? {
        return dimensions.find{it.name == dimName}?: parent?.findDimension(dimName)
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

        fun addDimension(dim: Dimension) {
            dimensions.add(dim)
        }

        // return true if did not exist and was added
        fun addDimensionIfNotExists(dim: Dimension): Boolean {
            if ( dimensions.find {it.name == dim.name } != null) {
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

        fun addAttribute(att: Attribute) {
            attributes.add(att)
        }

        fun addVariable(variable: Variable.Builder) {
            variables.add(variable)
        }

        fun addGroup(group: Group.Builder) {
            groups.add(group)
        }

        fun build(parent : Group?) : Group {
            return Group(name, typedefs, dimensions, attributes, variables, groups, parent)
        }
    }
}