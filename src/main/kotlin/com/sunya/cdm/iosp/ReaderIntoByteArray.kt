package com.sunya.cdm.iosp

interface ReaderIntoByteArray {
    fun readIntoByteArray(state : OpenFileState, dest : ByteArray, destPos : Int, nbytes : Int) : Int
}