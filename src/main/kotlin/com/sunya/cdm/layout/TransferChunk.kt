package com.sunya.cdm.layout

/**
 * A chunk of data that is contiguous in both the source and destination.
 * Everything here is in elements, not bytes.
 * Read nelems from src at srcElem, store in destination at destElem.
 */
data class TransferChunk(
    val srcElem : Long, // start reading here in the source
    val nelems: Int,    // read these many contiguous elements
    val destElem: Long  // start transferring to here in destination
)