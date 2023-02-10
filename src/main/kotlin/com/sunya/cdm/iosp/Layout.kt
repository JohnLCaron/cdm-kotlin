package com.sunya.cdm.iosp

interface Layout {
    /** Get total number of elements in the wanted subset.  */
    val totalNelems: Long

    /** Get size of each element in bytes.  */
    val elemSize: Int

    /** Is there more to do?  */
    operator fun hasNext(): Boolean

    /** Get the next chunk, not null if hasNext() is true.  */
    operator fun next(): Chunk

    /**
     * A chunk of data that is contiguous in both the source and destination.
     * Read nelems from src at filePos, store in destination at startElem.
     * (or) Write nelems to file at filePos, from array at startElem.
     */
    interface Chunk {
        /** Get the byte position in source where to read or write: eg "file position"  */
        val srcPos: Long

        /** Get number of elements to transfer contiguously (Note: elements, not bytes)  */
        val nelems: Int

        /**
         * Get starting element position as a 1D element index into the destination, eg the requested array with shape
         * "wantSection".
         *
         * @return starting element in the array (Note: elements, not bytes)
         */
        val destElem: Long // LOOK why Long?
    }
}