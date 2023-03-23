package com.sunya.cdm.layout

interface Layout : Iterator<Layout.Chunk> {
    /** The total number of elements in the wanted subset.  */
    val totalNelems: Long

    /** The size of each element in bytes.  */
    val elemSize: Int

    /** Is there more to do?  Must be called before calling next()*/
    override operator fun hasNext(): Boolean

    /** Get the next chunk, guarenteed not null if hasNext() is true.  */
    override operator fun next(): Chunk

    /**
     * A chunk of data that is contiguous in both the source and destination.
     * Read nelems from src at srcPos/srcElem, copy to the destination at destElem.
     */
    interface Chunk {
        /** The byte position in source where to read from: may be a "file position" or offset into a ByteBuffer  */
        fun srcPos() : Long

        /** The 1D element position in the source to read from. (Note: elements, not bytes)   */
        fun srcElem() : Long

        /** Number of elements to transfer contiguously (Note: elements, not bytes)  */
        fun nelems() : Int

        /** The 1D element position in the destination to copy into. (Note: elements, not bytes) */
        fun destElem() : Long // LOOK why Long?
    }
}