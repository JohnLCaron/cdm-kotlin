package test.util

import org.junit.jupiter.api.Test

class TestStuff {

    @Test
    fun wtfAreSequences() {
        val seq: Sequence<Int> = generateSequence(1) {
            it + 3
        }.take(10)
        seq.forEach { println("$it,") }
        println(" sum = ${seq.sum()}") // Prints 145
    }

}