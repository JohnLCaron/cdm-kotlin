package com.sunya.netchdf

import org.junit.jupiter.params.provider.Arguments
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream

const val testData = "/home/all/testdata/"

fun testFilesIn(dirPath: String): TestFiles.StreamBuilder {
    return TestFiles.StreamBuilder(dirPath)
}

// list of suffixes to include
class FileFilterIncludeSuffixes(suffixes: String) : (String) -> Boolean {
    var suffixes: Array<String>

    init {
        this.suffixes = suffixes.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    }

    override fun invoke(filename: String): Boolean {
        suffixes.forEach { suffix ->
            if (filename.endsWith(suffix)) {
                return true
            }
        }
        return false
    }
}

// list of suffixes to exclude
class FileFilterSkipSuffixes(suffixes: String) : (String) -> Boolean {
    var suffixes: Array<String>

    init {
        this.suffixes = suffixes.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    }

    override fun invoke(filename: String): Boolean {
        suffixes.forEach { suffix ->
            if (filename.endsWith(suffix)) {
                return false
            }
        }
        return true
    }
}

class NameFilterAnd(val filters : List<(String) -> Boolean>) : (String) -> Boolean {
    override fun invoke(filename: String): Boolean {
        filters.forEach {
            if (!it.invoke(filename)) {
                return false
            }
        }
        return true
    }
}

class NameFilterOr(val filters : List<(String) -> Boolean>) : (String) -> Boolean {
    override fun invoke(filename: String): Boolean {
        filters.forEach {
            if (it.invoke(filename)) {
                return true
            }
        }
        return false
    }
}

class TestFiles {

    class StreamBuilder(var dirPath: String) {
        var nameFilters = mutableListOf<(String) -> Boolean>()
        var pathFilter : (Path) -> Boolean = {  true }
        var recursion = false

        // filename only, not path
        fun addNameFilter(filter : (String) -> Boolean): StreamBuilder {
            this.nameFilters.add(filter)
            return this
        }

        // full path
        fun withPathFilter(filter : (Path) -> Boolean): StreamBuilder {
            this.pathFilter = filter
            return this
        }

        fun withRecursion(): StreamBuilder {
            this.recursion = true
            return this
        }

        fun build() : Stream<Arguments> {
            return if (recursion) all(dirPath) else one(dirPath)
        }

        @Throws(IOException::class)
        fun one(dirName : String): Stream<Arguments> {
            return Files.list(Paths.get(dirName))
                .filter { file: Path -> !Files.isDirectory(file) }
                .filter { this.pathFilter(it) }
                .filter { NameFilterAnd(nameFilters).invoke(it.fileName.toString()) }
                .map { obj: Path -> obj.toString() }
                .map { arguments: String? -> Arguments.of(arguments) }
        }

        @Throws(IOException::class)
        fun all(dirName : String): Stream<Arguments> {
            return Stream.concat(one(dirName), subdirs(dirName))
        }

        @Throws(IOException::class)
        fun subdirs(dirName : String): Stream<Arguments> {
            return Files.list(Paths.get(dirName))
                .filter { file: Path -> Files.isDirectory(file) }
                .flatMap { obj: Path -> all(obj.toString()) }
        }
    }
}