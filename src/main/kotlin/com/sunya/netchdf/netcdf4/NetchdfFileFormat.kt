package com.sunya.netchdf.netcdf4

import com.sunya.cdm.iosp.OpenFile
import com.sunya.cdm.iosp.OpenFileState
import java.io.IOException
import java.nio.ByteOrder

/*
 * From https://www.unidata.ucar.edu/software/netcdf/docs/netcdf_8h_source.html on 3/26/2020
 * #define NC_FORMAT_CLASSIC (1)
 *
 * After adding CDF5 support, the NC_FORMAT_64BIT flag is somewhat confusing. So, it is renamed.
 * Note that the name in the contributed code NC_FORMAT_64BIT was renamed to NC_FORMAT_CDF2
 *
 * #define NC_FORMAT_64BIT_OFFSET (2)
 * #define NC_FORMAT_64BIT (NC_FORMAT_64BIT_OFFSET)
 * #define NC_FORMAT_NETCDF4 (3)
 * #define NC_FORMAT_NETCDF4_CLASSIC (4)
 * #define NC_FORMAT_64BIT_DATA (5)
 *
 * /// Alias
 * #define NC_FORMAT_CDF5 NC_FORMAT_64BIT_DATA
 *
 * #define NC_FORMATX_NC3 (1)
 * #define NC_FORMATX_NC_HDF5 (2)
 * #define NC_FORMATX_NC4 NC_FORMATX_NC_HDF5
 * #define NC_FORMATX_NC_HDF4 (3)
 * #define NC_FORMATX_PNETCDF (4)
 * #define NC_FORMATX_DAP2 (5)
 * #define NC_FORMATX_DAP4 (6)
 * #define NC_FORMATX_UDF0 (8)
 * #define NC_FORMATX_UDF1 (9)
 * #define NC_FORMATX_ZARR (10)
 * #define NC_FORMATX_UNDEFINED (0)
 *
 * To avoid breaking compatibility (such as in the python library), we need to retain the NC_FORMAT_xxx format as well.
 * This may come out eventually, as the NC_FORMATX is more clear that it's an extended format specifier.
 *
 * #define NC_FORMAT_NC3 NC_FORMATX_NC3
 * #define NC_FORMAT_NC_HDF5 NC_FORMATX_NC_HDF5
 * #define NC_FORMAT_NC4 NC_FORMATX_NC4
 * #define NC_FORMAT_NC_HDF4 NC_FORMATX_NC_HDF4
 * #define NC_FORMAT_PNETCDF NC_FORMATX_PNETCDF
 * #define NC_FORMAT_DAP2 NC_FORMATX_DAP2
 * #define NC_FORMAT_DAP4 NC_FORMATX_DAP4
 * #define NC_FORMAT_UNDEFINED NC_FORMATX_UNDEFINED
 */
/*
 * From https://www.unidata.ucar.edu/software/netcdf/docs/faq.html#How-many-netCDF-formats-are-there-and-what-are-the-
 * differences-among-them
 *
 * Q: How many netCDF formats are there, and what are the differences among them?
 *
 * A: There are four netCDF format variants:
 *
 * - the classic format
 * - the 64-bit offset format
 * - the 64-bit data format
 * - the netCDF-4 format
 * - the netCDF-4 classic model format
 *
 * (In addition, there are two textual representations for netCDF data, though these are not usually thought of as
 * formats: CDL and NcML.)
 *
 * The classic format was the only format for netCDF data created between 1989 and 2004 by the reference software from Unidata.
 * It is still the default format for new netCDF data files, and the form in which most netCDF data is stored.
 * This format is also referred as CDF-1 format.
 *
 * In 2004, the 64-bit offset format variant was added. Nearly identical to netCDF classic format, it allows users to
 * create and access
 * far larger datasets than were possible with the original format. (A 64-bit platform is not required to write or read
 * 64-bit offset
 * netCDF files.) This format is also referred as CDF-2 format.
 *
 * In 2008, the netCDF-4 format was added to support per-variable compression, multiple unlimited dimensions, more
 * complex data types,
 * and better performance, by layering an enhanced netCDF access interface on top of the HDF5 format.
 *
 * At the same time, a fourth format variant, netCDF-4 classic model format, was added for users who needed the
 * performance benefits
 * of the new format (such as compression) without the complexity of a new programming interface or enhanced data model.
 *
 * In 2016, the 64-bit data format variant was added. To support large variables with more than 4-billion array
 * elements, it replaces
 * most of the 32-bit integers used in the format specification with 64-bit integers. It also adds support for several
 * new data types
 * including unsigned byte, unsigned short, unsigned int, signed 64-bit int and unsigned 64-bit int. A 64-bit platform
 * is required to
 * write or read 64-bit data netCDF files. This format is also referred as CDF-5 format.
 *
 * With each additional format variant, the C-based reference software from Unidata has continued to support access to
 * data stored in
 * previous formats transparently, and to also support programs written using previous programming interfaces.
 *
 * Although strictly speaking, there is no single "netCDF-3 format", that phrase is sometimes used instead of the more
 * cumbersome but
 * correct "netCDF classic CDF-1, 64-bit offset CDF-2, or 64-bit data CDF-5 format" to describe files created by the
 * netCDF-3
 * (or netCDF-1 or netCDF-2) libraries. Similarly "netCDF-4 format" is sometimes used informally to mean "either the
 * general netCDF-4 format
 * or the restricted netCDF-4 classic model format". We will use these shorter phrases in FAQs below when no confusion
 * is likely.
 *
 * A more extensive description of the netCDF formats and a formal specification of the classic and 64-bit formats is
 * available as a NASA ESDS community standard
 * (https://earthdata.nasa.gov/sites/default/files/esdswg/spg/rfc/esds-rfc-011/ESDS-RFC-011v2.00.pdf)
 *
 * The 64-bit data CDF-5 format specification is available in
 * http://cucis.ece.northwestern.edu/projects/PnetCDF/CDF-5.html.
 */

/** Enumeration of the kinds of NetCDF file formats. NC_FORMAT_64BIT_DATA is not currently supported in this library.  */
enum class NetchdfFileFormat(private val version: Int, private val formatName: String) {
    INVALID(0, "Invalid"),  //
    NC_FORMAT_CLASSIC(1, "NetCDF-3"),  //
    NC_FORMAT_64BIT_OFFSET(2, "netcdf-3 64bit-offset"),  //
    NC_FORMAT_NETCDF4(3, "NetCDF-4"),  // This is really just HDF-5, dont know yet if its written by netcdf4.
    NC_FORMAT_NETCDF4_CLASSIC(4, "netcdf-4 classic"),  // psuedo format I think
    NC_FORMAT_64BIT_DATA(5, "netcdf-5"),
    HDF5(5, "hdf5"); // not a netcdf4 file

    fun version(): Int {
        return version
    }

    fun formatName(): String {
        return formatName
    }

    val isNetdf3format: Boolean
        get() = this == NC_FORMAT_CLASSIC || this == NC_FORMAT_64BIT_OFFSET || this == NC_FORMAT_64BIT_DATA
    val isNetdf4format: Boolean
        get() = this == NC_FORMAT_NETCDF4 || this == NC_FORMAT_NETCDF4_CLASSIC
    val isExtendedModel: Boolean
        get() = this == NC_FORMAT_NETCDF4 // || this == NCSTREAM;
    val isLargeFile: Boolean
        get() = this == NC_FORMAT_64BIT_OFFSET
    val isClassicModel: Boolean
        get() = this == NC_FORMAT_CLASSIC || this == NC_FORMAT_64BIT_OFFSET || this == NC_FORMAT_NETCDF4_CLASSIC || this == NC_FORMAT_64BIT_DATA

    companion object {
        // from PnetCDF project
        // NCSTREAM(42, "ncstream"); // No assigned version, not part of C library.
        private const val MAGIC_NUMBER_LEN = 8
        private const val MAXHEADERPOS: Long = 500000 // header's gotta be within this range
        private val H5HEAD = byteArrayOf(
            0x89.toByte(),
            'H'.code.toByte(),
            'D'.code.toByte(),
            'F'.code.toByte(),
            '\r'.code.toByte(),
            '\n'.code.toByte(),
            0x1a.toByte(),
            '\n'.code.toByte()
        )
        private val CDF1HEAD = byteArrayOf('C'.code.toByte(), 'D'.code.toByte(), 'F'.code.toByte(), 0x01.toByte())
        private val CDF2HEAD = byteArrayOf('C'.code.toByte(), 'D'.code.toByte(), 'F'.code.toByte(), 0x02.toByte())
        private val CDF5HEAD = byteArrayOf('C'.code.toByte(), 'D'.code.toByte(), 'F'.code.toByte(), 0x05.toByte())

        /**
         * Figure out what kind of netcdf-related file we have.
         * Constraint: leave raf read pointer to point just after the magic number.
         *
         * @param raf to test type
         * @return NetcdfFileFormat that matches constants in netcdf-c/include/netcdf.h, or INVALID if not a netcdf file.
         */
        @Throws(IOException::class)
        fun findNetcdfFormatType(raf: OpenFile): NetchdfFileFormat {
            val magic = ByteArray(MAGIC_NUMBER_LEN)
            if (raf.readBytes(magic, OpenFileState(0, ByteOrder.nativeOrder())) != magic.size) {
                return INVALID
            }

            // If this is not an HDF5 file, then the magic number is at position 0;
            // If it is an HDF5 file, then we need to search forward for it.
            return if (memequal(CDF1HEAD, magic, CDF1HEAD.size)) NC_FORMAT_CLASSIC
                else if (memequal(CDF2HEAD, magic, CDF2HEAD.size)) NC_FORMAT_64BIT_OFFSET
                else if (memequal(CDF5HEAD, magic, CDF5HEAD.size)) NC_FORMAT_64BIT_DATA
                else searchForwardHdf5(raf, magic)
        }

        fun netcdfFormat(format : Int): NetchdfFileFormat {
            return when (format) {
                0 -> INVALID
                1 -> NC_FORMAT_CLASSIC
                2 -> NC_FORMAT_64BIT_OFFSET
                3 -> NC_FORMAT_NETCDF4
                4 -> NC_FORMAT_NETCDF4_CLASSIC
                5 -> NC_FORMAT_64BIT_DATA
                else -> throw RuntimeException("Unknown netcdfFormat $format")
            }
        }

        fun netcdfFormatExtended(formatx : Int): String {
            return when (formatx) {
                0 -> "NC_FORMATX_UNDEFINED"
                1 -> "NC_FORMATX_NC3"
                2 -> "NC_FORMATX_NC_HDF5"
                3 -> "NC_FORMATX_NC_HDF4"
                4 -> "NC_FORMATX_PNETCDF"
                5 -> "NC_FORMATX_DAP2"
                6 -> "NC_FORMATX_DAP4"
                8 -> "NC_FORMATX_UDF0"
                9 -> "NC_FORMATX_UDF1"
                10 -> "NC_FORMATX_NCZARR"
                else -> throw RuntimeException("Unknown netcdfFormatExtended $formatx")
                }
        }

        fun netcdfMode(mode : Int): String {
            return buildString {
                if (mode and 1 == 1) {
                    append("NC_WRITE ")
                }
                if ((mode and 8) == 8) {
                    append("NC_DISKLESS ")
                }
                if ((mode and 16) == 16) {
                    append("NC_MMAP ")
                }
                if ((mode and 32) == 32) {
                    append("NC_64BIT_DATA ")
                }
                if ((mode and 64) == 64) {
                    append("NC_UDF0 ")
                }
                if ((mode and 128) == 128) {
                    append("NC_UDF1 ")
                }
                if ((mode and 0x100) == 0x100) {
                    append("NC_CLASSIC_MODEL ")
                }
                if ((mode and 0x200) == 0x200) {
                    append("NC_64BIT_OFFSET ")
                }
                if ((mode and 0x1000) == 0x1000) {
                    append("NC_NETCDF4 ")
                }
                if ((mode and 0x20000) == 0x20000) {
                    append("NC_NOATTCREORD ")
                }
                if ((mode and 0x40000) == 0x40000) {
                    append("NC_NODIMSCALE_ATTACH ")
                }
            }
        }

        private fun searchForwardHdf5(raf: OpenFile, magic: ByteArray): NetchdfFileFormat {
            // For HDF5, we need to search forward on 512 block sizes
            val filePos = OpenFileState(0L, ByteOrder.BIG_ENDIAN)
            var format : NetchdfFileFormat? = null
            while (filePos.pos < raf.size - 8 && filePos.pos < MAXHEADERPOS && format == null) {
                if (raf.readBytes(magic, filePos) < MAGIC_NUMBER_LEN) {
                    format = INVALID
                } else if (memequal(H5HEAD, magic, H5HEAD.size)) {
                    format = NC_FORMAT_NETCDF4 // actually dont know here if its netcdf4 or just hdf5.
                } else {
                    filePos.pos = if (filePos.pos == 0L) 512 else 2 * filePos.pos
                }
            }
            return format ?: INVALID
        }
    }
}

private fun memequal(b1: ByteArray?, b2: ByteArray?, len: Int): Boolean {
    if (b1.contentEquals(b2)) return true
    if (b1 == null || b2 == null) return false
    if (b1.size < len || b2.size < len) return false
    for (i in 0 until len) {
        if (b1[i] != b2[i]) return false
    }
    return true
}