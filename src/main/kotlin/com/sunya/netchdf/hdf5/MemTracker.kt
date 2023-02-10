package com.sunya.netchdf.hdf5

import java.util.*

/** Track use of space in an HDF5 file  */
class MemTracker(private val fileSize: Long) {
    private val memList = mutableListOf<Mem>()

    fun add(name: String?, start: Long, end: Long) {
        memList.add(Mem(name, start, end))
    }

    fun addByLen(name: String?, start: Long, size: Long) {
        memList.add(Mem(name, start, start + size))
    }

    fun report(f: Formatter) {
        f.format("Memory used file size= %d%n", fileSize)
        f.format("  start    end   size   name")
        Collections.sort(memList)
        var prev: Mem? = null
        for (m in memList) {
            if (prev != null && m.start > prev.end) f.format(
                " + %6d %6d %6d %6s%n",
                prev.end,
                m.start,
                m.start - prev.end,
                "HOLE"
            )
            val c = if (prev != null && prev.end != m.start) '*' else ' '
            f.format(" %s %6d %6d %6d %6s%n", c, m.start, m.end, m.end - m.start, m.name)
            prev = m
        }
        f.format("%n")
    }

    internal class Mem(val name: String?, val start: Long, val end: Long) : Comparable<Mem> {
        override fun compareTo(other: Mem): Int {
            return java.lang.Long.compare(start, other.start)
        }
    }
}