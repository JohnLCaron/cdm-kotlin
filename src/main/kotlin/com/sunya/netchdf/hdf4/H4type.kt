package com.sunya.netchdf.hdf4

import com.sunya.cdm.api.Datatype

/** Convert HDF4 data type values  */
object H4type {
    // p 110 table 9a : probably the "class" of the number type
    fun getNumberType(type: Int): String {
        return when (type) {
            0 -> "NONE"
            1 -> "IEEE"
            2 -> "VAX"
            3 -> "CRAY"
            4 -> "PC"
            5 -> "CONVEX"
            else -> throw IllegalStateException("unknown type= $type")
        }
    }

    /*
   * type info codes from hntdefs.h
   * #define DFNT_UCHAR8 3
   * #define DFNT_CHAR8 4
   * #define DFNT_FLOAT32 5
   * #define DFNT_FLOAT64 6
   * 
   * #define DFNT_INT8 20
   * #define DFNT_UINT8 21
   * #define DFNT_INT16 22
   * #define DFNT_UINT16 23
   * #define DFNT_INT32 24
   * #define DFNT_UINT32 25
   * #define DFNT_INT64 26
   * #define DFNT_UINT64 27
   */
    fun getDataType(type: Int): Datatype {
        return when (type) {
            3, 21 -> Datatype.UBYTE
            4 -> Datatype.CHAR
            5 -> Datatype.FLOAT
            6 -> Datatype.DOUBLE
            20 -> Datatype.BYTE
            22 -> Datatype.SHORT
            23 -> Datatype.USHORT
            24 -> Datatype.INT
            25 -> Datatype.UINT
            26 -> Datatype.LONG
            27 -> Datatype.ULONG
            else -> throw IllegalStateException("unknown type= $type")
        }
    }
}