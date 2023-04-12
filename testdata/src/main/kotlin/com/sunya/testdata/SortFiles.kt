package com.sunya.testdata

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class SortFiles {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> {
            return Stream.of( H4Files.params(), H5Files.params(), N3Files.params(), N4Files.params(), NetchdfExtraFiles.params(false)).flatMap { i -> i };
        }

        val filenames = mutableMapOf<String, MutableList<String>>()
        val showAllFiles = true

        @JvmStatic
        @AfterAll
        fun afterAll() {
            println("*** nfiles = ${filenames.size}")
            var dups = 0
            filenames.keys.sorted().forEach {
                val paths = filenames[it]!!
                if (paths.size > 1) {
                    println("$it")
                    paths.forEach { println("  $it") }
                }
                dups += paths.size - 1
            }
            println("*** nduplicates = ${dups}")

            if (showAllFiles) {
                println("*** nfiles = ${filenames.size}")
                filenames.keys.sorted().forEach {
                    val paths = filenames[it]!!
                    paths.forEach {path-> println("$path/$it") }
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    fun sortFilenames(filename: String) {
        val path = filename.substringBeforeLast("/")
        val name = filename.substringAfterLast("/")
        val paths = filenames.getOrPut(name) { mutableListOf() }
        paths.add(path)
    }
}