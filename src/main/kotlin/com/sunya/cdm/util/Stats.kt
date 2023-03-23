package com.sunya.cdm.util

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

/**
 *  Keep track of timing stats. Thread-safe
 *  Each "statName" is an independent Stats, you can have as many as you want.
 *  Stats can have any number of "what" operations, each is a Stat
 *  Each Stat accumulates "amount" nanosecs on some number of "things", and tracks number of calls
 */
class Stats(val statsName : String) {
    private val stats = mutableMapOf<String, Stat>() // LOOK need thread safe collection

    fun of(statName : String, thing: String): Stat =
        stats.getOrPut(statName) { Stat(statName, thing) }

    fun show(len: Int = 3) {
        showLines(len).forEach { println(it) }
    }

    fun count() : Int {
        return if (stats.size > 0) stats.values.first().count() else 0
    }

    fun showLines(len: Int = 3): List<String> {
        val result = mutableListOf<String>()
        if (stats.isEmpty()) {
            result.add("$statsName is empty")
            return result
        }
        result.add("\n$statsName ================================================================")
        val first = stats.values.first()
        val totalStat = Stat("total", first.thing)
        stats.values.sortedBy{ it.accum() }.forEach {
            result.add("  ${it.statName.padStart(80, ' ')}: ${it.show(len)}")
            totalStat.sum(it)
        }
        result.add("  ${totalStat.statName.padStart(80, ' ')}: ${totalStat.show(len)}")

        return result
    }

    companion object {
        private val statsMap = mutableMapOf<String, Stats>() // LOOK need thread safe collection

        fun of(statsName: String, statName : String, thing: String) : Stat {
            val stats = statsMap.getOrPut(statsName) { Stats(statsName) }
            return stats.of(statName, thing)
        }

        fun show() {
            statsMap.values.forEach { it.show() }
        }

        fun clear() {
            statsMap.clear()
        }

    }
}

class Stat(val statName: String, val thing : String) {
    var accum : AtomicLong = AtomicLong(0)
    var count : AtomicInteger = AtomicInteger(0)
    var nthings : AtomicInteger = AtomicInteger(0)

    fun accum(nanosecs : Long, nthings : Int) {
        // println("  $what $nanosecs")
        accum.addAndGet(nanosecs)
        this.nthings.addAndGet(nthings)
        count.incrementAndGet()
    }

    fun copy(accum: Long): Stat {
        val copy = Stat(this.statName, this.thing)
        copy.accum = AtomicLong(accum)
        copy.count = this.count
        copy.nthings = this.nthings
        return copy
    }

    fun accum() = this.accum.get()

    fun nthings() = this.nthings.get()

    fun count() = this.count.get()

    fun sum(other : Stat) {
        this.accum.addAndGet(other.accum())
        this.count.addAndGet(other.count())
        this.nthings.addAndGet(other.nthings())
    }
}

fun Stat.show(len: Int = 3): String {
    val total = 1e-6 * (accum().toDouble())
    val perThing = if (nthings() == 0) 0.0 else total / nthings()
    val perCount = if (count() == 0) 0.0 else total / count()
    val part1 = "took ${total.sigfig(4)} msecs = ${perThing.sigfig(4)} msecs/${thing} (${nthings()} ${thing}s)"
        val part2 = " = ${perCount.sigfig(4)} msecs/call (${count()} calls)"
    return part1 + part2
}

fun Int.pad(len: Int): String = "$this".padStart(len, ' ')
fun Long.pad(len: Int): String = "$this".padStart(len, ' ')

/**
 * Format a double value to have a minimum significant figures.
 *
 * @param minSigfigs minimum significant figures
 * @return double formatted as a string
 */
fun Double.sigfig(minSigfigs: Int = 5): String {
    val s: String = this.toString()

    // extract the sign
    val sign: String
    val unsigned: String
    if (s.startsWith("-") || s.startsWith("+")) {
        sign = s.substring(0, 1)
        unsigned = s.substring(1)
    } else {
        sign = ""
        unsigned = s
    }

    // deal with exponential notation
    val mantissa: String
    val exponent: String
    var eInd = unsigned.indexOf('E')
    if (eInd == -1) {
        eInd = unsigned.indexOf('e')
    }
    if (eInd == -1) {
        mantissa = unsigned
        exponent = ""
    } else {
        mantissa = unsigned.substring(0, eInd)
        exponent = unsigned.substring(eInd)
    }

    // deal with decimal point
    var number: StringBuilder
    val fraction: StringBuilder
    val dotInd = mantissa.indexOf('.')
    if (dotInd == -1) {
        number = StringBuilder(mantissa)
        fraction = StringBuilder()
    } else {
        number = StringBuilder(mantissa.substring(0, dotInd))
        fraction = StringBuilder(mantissa.substring(dotInd + 1))
    }

    // number of significant figures
    var numFigs = number.length
    var fracFigs = fraction.length

    // Don't count leading zeros in the fraction, if no number
    if (numFigs == 0 || number.toString() == "0" && fracFigs > 0) {
        numFigs = 0
        number = StringBuilder()
        for (element in fraction) {
            if (element != '0') {
                break
            }
            --fracFigs
        }
    }
    // Don't count trailing zeroes in the number if no fraction
    if (fracFigs == 0 && numFigs > 0) {
        for (i in number.length - 1 downTo 1) {
            if (number[i] != '0') {
                break
            }
            --numFigs
        }
    }
    // deal with min sig figures
    val sigFigs = numFigs + fracFigs
    if (sigFigs > minSigfigs) {
        // Want fewer figures in the fraction; chop (should round? )
        val chop: Int = min(sigFigs - minSigfigs, fracFigs)
        fraction.setLength(fraction.length - chop)
    }

    return if (fraction.isEmpty()) {
        "$sign$number$exponent"
    } else {
        "$sign$number.$fraction$exponent"
    }
}