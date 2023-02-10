package com.sunya.cdm.api

/** Maintains indentation level for printing nested structures.  */
class Indent {
    private val blanks = " ".repeat(100)
    private val nspaces: Int
    private var level: Int

    /** Create an Indent with nspaces per level.  */
    constructor(nspaces: Int) {
        this.nspaces = nspaces
        level = 1
    }

    /** Create an Indent with nspaces per level.  */
    constructor(nspaces: Int, level: Int) {
        this.nspaces = nspaces
        this.level = level
    }

    fun incr(): Indent {
        level++
        return this
    }

    /** Decrement the indent level  */
    fun decr(): Indent {
        level--
        return this
    }

    /** Increment the indent level, return new object  */
    fun incrNew(): Indent {
        return Indent(nspaces, level + 1)
    }

    /** Decrement the indent level, , return new object  */
    fun decrNew(): Indent {
        return Indent(nspaces, level - 1)
    }

    /** Return a String of nspaces * level blanks which is the indentation.  */
    override fun toString(): String {
        return blanks.substring(0, nspaces * level)
    }
}