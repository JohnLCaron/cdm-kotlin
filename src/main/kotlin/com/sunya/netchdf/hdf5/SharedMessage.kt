package com.sunya.netchdf.hdf5

import com.sunya.cdm.iosp.OpenFileState

// Level 2A2 - Data Object Header Messages
// “shared message” encoding
fun H5builder.getSharedDataObject(state : OpenFileState, mtype: MessageType): DataObject {
    val sharedVersion = raf.readByte(state).toInt()
    val sharedType = raf.readByte(state).toInt()
    if (sharedVersion == 1) {
        state.pos += 6
    }
    if (sharedVersion == 3 && sharedType == 1) {
        val heapId = raf.readLong(state)
        // LOOK Message stored in file’s shared object header message heap (a shared message).
        //   the 8-byte fractal heap ID for the message in the file’s shared object header message heap.
        //   umm, where is the shared object header message heap?
        throw UnsupportedOperationException("****SHARED MESSAGE type = $mtype heapId = $heapId")
    } else {
        // The address of the object header containing the message to be shared
        val address: Long = this.readOffset(state)
        val dobj: DataObject = this.getDataObject(address, null)
        if (mtype === MessageType.Datatype) {
            return dobj
        }
        throw UnsupportedOperationException("****SHARED MESSAGE type = $mtype")
    }
}