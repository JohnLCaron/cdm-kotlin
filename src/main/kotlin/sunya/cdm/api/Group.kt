package sunya.cdm.api

class Group(val name : String,
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

    fun cdlString(indent : Indent = Indent(2)) : String {
        return buildString{
            if (dimensions.isNotEmpty()) {
                append("${indent}dimensions:\n")
                dimensions.forEach { append("${it.cdlString(indent.incrNew())}\n") }
            }
            if (variables.isNotEmpty()) {
                append("${indent}variables:\n")
                variables.map { append("${it.cdlString(indent.incrNew())}") }
            }
            if (groups.isNotEmpty()) {
                groups.forEach {
                    append("${indent}group: ${it.name} {\n")
                    append("${it.cdlString(indent.incrNew())}")
                    append("${indent}}\n")
                }
            }
            if (attributes.isNotEmpty()) {
                append("${indent}// group attributes:\n")
                attributes.forEach { append("${it.cdlString(indent)}\n") }
            }
        }
    }

    class Builder(val name : String) {
        val dimensions = mutableListOf<Dimension>()
        val attributes = mutableListOf<Attribute>()
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
            return Group(name, dimensions, attributes, variables, groups, parent)
        }
    }
}