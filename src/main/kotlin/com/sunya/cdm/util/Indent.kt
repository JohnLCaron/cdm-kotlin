package com.sunya.cdm.util

/** Maintains indentation level for printing nested structures.  */
data class Indent(val nspaces : Int, var level : Int, val ntabs : Int) {
    private val spaces = " ".repeat(100)
    private val tabs = "\t".repeat(100)

    /** Create an Indent with nspaces per level, startingLevel is 1.  */
    constructor(nspaces: Int) : this(nspaces, 1, 0)

    /** Create an Indent with nspaces per level.  */
    constructor(nspaces: Int, startingLevel: Int) : this(nspaces, startingLevel, 0)

    fun incr(): Indent {
        return this.copy(level = this.level + 1)
    }

    fun decr(): Indent {
        return this.copy(level = this.level - 1)
    }

    fun incrTab(amount : Int = 1): Indent {
        return this.copy(ntabs = this.ntabs + amount)
    }

    fun decrTab(amount : Int = 1): Indent {
        return this.copy(ntabs = this.ntabs - amount)
    }

    /** Return the indentation.  */
    override fun toString(): String {
        return spaces.substring(0, nspaces * level) + tabs.substring(0, ntabs)
    }
}