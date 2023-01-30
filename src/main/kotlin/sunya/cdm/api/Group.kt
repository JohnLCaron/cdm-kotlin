package sunya.cdm.api

class Group(val dimensions : List<Dimension>,
            val attributes : List<Attribute>,
            variableBuilders : List<Variable.Builder>) {
    val variables : List<Variable>

    init {
        variables = variableBuilders.map { it.build(this) }
    }

    fun cdlString() : String {
        return buildString{
            append("  dimensions:\n")
            dimensions.forEach{ append("  ${it.cdlString()}\n")}
            append("  variables:\n")
            variables.map{ append("  ${it.cdlString()}\n")}
            if (attributes.isNotEmpty()) {
                append("  // global attributes:\n")
                attributes.forEach { append("  ${it.cdlString()}\n") }
            }
        }
    }

    class Builder {
        val dimensions = mutableListOf<Dimension>()
        val attributes = mutableListOf<Attribute>()
        val variables = mutableListOf<Variable.Builder>()

        fun addDimension(dim: Dimension) {
            dimensions.add(dim)
        }

        fun addAttribute(att: Attribute) {
            attributes.add(att)
        }

        fun addVariable(variable: Variable.Builder) {
            variables.add(variable)
        }

        fun build() : Group {
            return Group(dimensions, attributes, variables)
        }
    }
}