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
            return H4Files.params()
        }

        val filenames = mutableMapOf<String, MutableList<String>>()
        val showAllFiles = false

        @JvmStatic
        @AfterAll
        fun afterAll() {
            println("*** nfiles = ${filenames.size}")
            filenames.keys.sorted().forEach {
                val paths = filenames[it]!!
                if (paths.size > 1) {
                    println("$it")
                    paths.forEach { println("  $it") }
                }
            }
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