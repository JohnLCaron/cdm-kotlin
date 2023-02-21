package com.sunya.cdm.util

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * copy all bytes from in to out, specify buffer size
 *
 * @param in InputStream
 * @param out OutputStream
 * @param bufferSize : internal buffer size.
 * @return number of bytes copied
 * @throws java.io.IOException on io error
 */
@Throws(IOException::class)
fun IOcopyB(input: InputStream, out: OutputStream, bufferSize: Int): Long {
    var totalBytesRead: Long = 0
    var done = 0
    var next = 1
    val buffer = ByteArray(bufferSize)
    while (true) {
        val n = input.read(buffer)
        if (n == -1) break
        out.write(buffer, 0, n)
        totalBytesRead += n.toLong()
    }
    out.flush()
    return totalBytesRead
}