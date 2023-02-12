package com.sunya.cdm.api

class Group(val name : String,
            val dimensions : List<Dimension>,
            val attributes : List<Attribute>,
            val typedefs : List<Typedef>,
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
            return Group(name, dimensions, attributes, typedefs, variables, groups, parent)
        }
    }
}