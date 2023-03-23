package com.sunya.netchdf.netcdf3

import java.io.*
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import mu.KotlinLogging
import com.sunya.cdm.api.*
import com.sunya.cdm.api.Datatype.*
import com.sunya.cdm.array.ArrayUByte
import com.sunya.cdm.iosp.OpenFile
import com.sunya.cdm.iosp.OpenFileState
import com.sunya.cdm.array.makeStringFromBytes
import com.sunya.netchdf.netcdf4.NetchdfFileFormat
import java.nio.ByteOrder

/*
 * CLASSIC
 * The maximum size of a record in the classic format in versions 3.5.1 and earlier is 2^32 - 4 bytes.
 * In versions 3.6.0 and later, there is no such restriction on total record size for the classic format
 * or 64-bit offset format.
 *
 * If you don't use the unlimited dimension, only one variable can exceed 2 GiB in size, but it can be as
 * large as the underlying file system permits. It must be the last variable in the dataset, and the offset
 * to the beginning of this variable must be less than about 2 GiB.
 *
 * The limit is really 2^31 - 4. If you were to specify a variable size of 2^31 -3, for example, it would be
 * rounded up to the nearest multiple of 4 bytes, which would be 2^31, which is larger than the largest
 * signed integer, 2^31 - 1.
 *
 * If you use the unlimited dimension, record variables may exceed 2 GiB in size, as long as the offset of the
 * start of each record variable within a record is less than 2 GiB - 4.
 */

/*
 * LARGE FILE
 * Assuming an operating system with Large File Support, the following restrictions apply to the netCDF 64-bit offset
 * format.
 *
 * No fixed-size variable can require more than 2^32 - 4 bytes of storage for its data, unless it is the last
 * fixed-size variable and there are no record variables. When there are no record variables, the last
 * fixed-size variable can be any size supported by the file system, e.g. terabytes.
 *
 * A 64-bit offset format netCDF file can have up to 2^32 - 1 fixed sized variables, each under 4GiB in size.
 * If there are no record variables in the file the last fixed variable can be any size.
 *
 * No record variable can require more than 2^32 - 4 bytes of storage for each record's worth of data,
 * unless it is the last record variable. A 64-bit offset format netCDF file can have up to 2^32 - 1 records,
 * of up to 2^32 - 1 variables, as long as the size of one record's data for each record variable except the
 * last is less than 4 GiB - 4.
 *
 * Note also that all netCDF variables and records are padded to 4 byte boundaries.
 */

private val logger = KotlinLogging.logger("N3header")

// Really a builder of the root Group.
class N3header(val raf: OpenFile, root: Group.Builder) {
  private val root: Group.Builder
  private var unlimitedDimension: Dimension? = null
  private val filePos = OpenFileState(0L, ByteOrder.BIG_ENDIAN)
  private val valueCharset = StandardCharsets.UTF_8

  private var isStreaming = false
  private var isUnlimited = false
  var numrecs = 0 // number of records written
  var recsize: Long = 0 // size of each record (padded) TODO can it really be bigger than MAX_INTEGER ?
  var recStart = Int.MAX_VALUE.toLong() // where the record data starts TODO can it really be bigger than MAX_INTEGER ?
  var useLongOffset = false
  var nonRecordDataSize: Long = 0 // size of non-record variables
  var dataStart = Long.MAX_VALUE // where the data starts

  private val fileDimensions = mutableListOf<Dimension>()

  init {
    this.root = root
    val actualSize: Long = raf.size
    nonRecordDataSize = 0 // length of non-record data
    recsize = 0 // length of single record
    recStart = Int.MAX_VALUE.toLong() // where the record data starts

    // netcdf magic number
    val b = raf.readBytes(filePos, 4)
    if (!isMagicBytes(b)) {
      throw IOException("Not a netCDF file " + raf.location)
    }
    if (b[3].toInt() != 1 && b[3].toInt() != 2) throw IOException("Not a netCDF file " + raf.location)
    useLongOffset = b[3].toInt() == 2

    // number of records
    numrecs = raf.readInt(filePos)
    if (numrecs == -1) {
      isStreaming = true
      numrecs = 0
    }

    // dimensions
    readDimensions(raf, root)

    // global attributes
    readAttributes(root.attributes)

    // variables
    readVariables(raf, root)

    if (dataStart == Long.MAX_VALUE) { // if nvars == 0
      dataStart = filePos.pos
    }
    if (nonRecordDataSize > 0) { // if there are non-record variables
      nonRecordDataSize -= dataStart
    }

    val unlimitedVariables = root.variables.filter { it.isUnlimited() }
    if (unlimitedVariables.isEmpty()) { // if there are no record variables
      recStart = 0
    }

    // Check if file affected by bug CDM-52 (netCDF-Java library used incorrect padding when
    // the file contained only one record variable and it was of type byte, char, or short).
    // Example TestDir.cdmLocalTestDataDir + "byteArrayRecordVarPaddingTest-bad.nc"
    // Example devcdm/core/src/test/data/netcdf3/WrfTimesStrUnderscore.nc
    if (unlimitedVariables.size == 1) {
      val uvar = unlimitedVariables[0]
      val dtype = uvar.datatype
      if (dtype == Datatype.CHAR || dtype == Datatype.BYTE || dtype == Datatype.SHORT) {
        var vsize = dtype.size // works for all netcdf-3 data types
        val dims: List<Dimension> = uvar.dimensions
        for (curDim in dims) {
          if (!curDim.isUnlimited) vsize *= curDim.length
        }
        val vinfo = uvar.spObject as Vinfo
        if (vsize != vinfo.vsize) {
          logger.info(
            java.lang.String.format(
              "Malformed netCDF file (%s): incorrect padding for record variable (CDM-52): fvsize= %d != calc size=%d",
              raf.location, vinfo.vsize, vsize
            )
          )
          recsize = vsize.toLong()
          uvar.spObject = vinfo.copy(vsize = vsize)
        }
      }
    }

    if (debugHeaderSize) {
      println("  filePointer = ${filePos.pos} dataStart=$dataStart")
      println("  recStart = $recStart dataStart+nonRecordDataSize = ${dataStart + nonRecordDataSize}")
      println("  nonRecordDataSize size= $nonRecordDataSize")
      println("  recsize= $recsize")
      println("  numrecs= $numrecs")
      println("  actualSize= $actualSize")
    }

    /* check for truncated files
    // theres a "wart" that allows a file to be up to 3 bytes smaller than you expect.
    val calcSize = dataStart + nonRecordDataSize + recsize * numrecs
    if (calcSize > actualSize + 3) {
      // can we fix this by setting the unlimited dimension size ?
      val diff = calcSize - actualSize
      val mod = diff % recsize
      val nr = (diff / recsize).toInt()
      if ((diff % recsize) == 0L) {
        val extrarecords = (diff / recsize).toInt()
        numrecs -= extrarecords
      }
      //if (disallowFileTruncation) throw IOException("File is truncated, calculated size= $calcSize actual = $actualSize") else {
        logger.info("File is truncated calculated size= "+calcSize+" actual = "+actualSize);
        ///raf.setExtendMode()
      //}
    }

    // add a record structure if asked to do so
    if (n3iospNew.useRecordStructure && uvars.size > 0) {
      makeRecordStructure(root, uvars)
    }
     */
  }

  private fun readDimensions(raf: OpenFile, root: Group.Builder) {
    var numdims = 0
    val magic = raf.readInt(filePos)
    if (magic == 0) {
      raf.readInt(filePos) // skip 32 bits
    } else {
      if (magic != MAGIC_DIM) throw IOException("Malformed netCDF file - dim magic number wrong " + raf.location)
      numdims = raf.readInt(filePos)
    }

    // Must keep dimensions in strict order
    for (i in 0 until numdims) {
      val name = readString()!!
      val len: Int = raf.readInt(filePos)

      var dim: Dimension
      if (len == 0) {
        dim = Dimension(name, numrecs, true, true)
        unlimitedDimension = dim
      } else {
        dim = Dimension(name, len)
      }
      fileDimensions.add(dim)
      root.addDimension(dim)
    }
  }

  private fun readVariables(raf: OpenFile, root: Group.Builder) {
    // variables
    val magic = raf.readInt(filePos)
    val nvars = if (magic == 0) {
      raf.readInt(filePos) // skip 32 bits
      0
    } else {
      if (magic != MAGIC_VAR) throw IOException("Malformed netCDF file  - var magic number wrong ${raf.location}")
      raf.readInt(filePos)
    }

    // loop over variables
    for (i in 0 until nvars) {
      val ncvarb = Variable.Builder()
      val name = readString()!!
      ncvarb.name = name

      // get element count in non-record dimensions
      var velems: Long = 1
      var isRecord = false
      val rank: Int = raf.readInt(filePos)
      val dims = mutableListOf<Dimension>()
      for (j in 0 until rank) {
        val dimIndex: Int = raf.readInt(filePos)
        val dim: Dimension = fileDimensions[dimIndex]
        if (dim.isUnlimited) {
          isRecord = true
        } else {
          velems *= dim.length
        }
        dims.add(dim)
      }
      ncvarb.dimensions.addAll(dims)

      // variable attributes
      val varAttsPos: Long = filePos.pos
      readAttributes(ncvarb.attributes)

      // data type
      val type: Int = raf.readInt(filePos)
      val datatype = getDatatype(type)
      ncvarb.datatype = datatype

      // size and beginning data position in file
      val vsize = raf.readInt(filePos)
      val begin = if (useLongOffset) raf.readLong(filePos) else raf.readInt(filePos).toLong()
      if (debugSize) {
        println(" name= $name type=$type vsize=$vsize velems=$velems begin=$begin isRecord=$isRecord attsPos=$varAttsPos")
        val calcVsize: Long = (velems + padding(velems)) * datatype.size
        if (vsize.toLong() != calcVsize) println(" *** readVsize $vsize != calcVsize $calcVsize")
      }
      //if (vsize < 0) { // when does this happen ?? streaming i think
      //  vsize = (velems.toInt() + padding(velems)) * datatype.size
      //}
      val vinfo = Vinfo(name, vsize, begin, isRecord, datatype.size)
      ncvarb.spObject = vinfo

      // track how big each record is
      if (isRecord) {
        recsize += vsize
        recStart = Math.min(recStart, begin)
      } else {
        nonRecordDataSize = Math.max(nonRecordDataSize, begin + vsize)
      }
      dataStart = Math.min(dataStart, begin)
      root.variables.add(ncvarb)
    }
  }

  private fun isMagicBytes(bytes: ByteArray): Boolean {
    for (i in 0..2) {
      if (bytes[i] != MAGIC[i]) {
        return false
      }
    }
    return true
  }

  fun calcFileSize(): Long {
    return if (unlimitedDimension != null) recStart + recsize * numrecs else dataStart + nonRecordDataSize
  }

  @Throws(IOException::class)
  private fun readAttributes(atts: MutableList<Attribute>): Int {
    var natts = 0
    val magic: Int = raf.readInt(filePos)
    if (magic == 0) {
      raf.readInt(filePos) // skip 32 bits
    } else {
      if (magic != MAGIC_ATT) throw IOException("Malformed netCDF file  - att magic number wrong " + raf.location)
      natts = raf.readInt(filePos)
    }
    for (i in 0 until natts) {
      val name = readString()!!
      val type: Int = raf.readInt(filePos)
      var att: Attribute?
      if (type == 2) { // CHAR
        val value = readString(valueCharset)
        att = if (value == null) Attribute(name, Datatype.STRING, emptyList<String>()) // nelems = 0
              else Attribute(name, value) // may be empty string
      } else {
        val nelems: Int = raf.readInt(filePos)
        val dtype: Datatype = getDatatype(type)
        val builder = Attribute.Builder()
        builder.name = name
        builder.datatype = dtype
        if (nelems > 0) {
          val nbytes = readAttributeArray(dtype, nelems, builder)
          skipToBoundary(nbytes)
        }
        att = builder.build()
      }
      atts.add(att)
    }
    return natts
  }

  @Throws(IOException::class)
  fun readAttributeArray(type: Datatype, nelems: Int, attBuilder: Attribute.Builder): Int {
    when (type) {
      Datatype.BYTE -> {
        attBuilder.values = raf.readArrayByte(filePos, nelems).asList()
        return nelems
      }

      Datatype.CHAR -> {
        val wtf  = ArrayUByte(intArrayOf(1), raf.readByteBuffer(filePos, nelems))
        attBuilder.values = wtf.makeStringFromBytes().toList()
        return nelems
      }

      Datatype.SHORT -> {
        attBuilder.values = raf.readArrayShort(filePos, nelems).asList()
        return 2 * nelems
      }

      Datatype.INT -> {
        attBuilder.values = raf.readArrayInt(filePos, nelems).asList()
        return 4 * nelems
      }

      Datatype.FLOAT -> {
        attBuilder.values = raf.readArrayFloat(filePos, nelems).asList()
        return 4 * nelems
      }

      Datatype.DOUBLE -> {
        attBuilder.values = raf.readArrayDouble(filePos, nelems).asList()
        return 8 * nelems
      }

      Datatype.LONG -> {
        attBuilder.values = raf.readArrayLong(filePos, nelems).asList()
        return 8 * nelems
      }

      else -> return 0
    }
  }

  // read a string = (nelems, byte array), then skip to 4 byte boundary
  @Throws(IOException::class)
  fun readString(): String? {
    return readString(StandardCharsets.UTF_8)
  }

  @Throws(IOException::class)
  private fun readString(charset: Charset): String? {
    val nelems: Int = raf.readInt(filePos)
    val b = raf.readBytes(filePos, nelems)
    skipToBoundary(nelems) // pad to 4 byte boundary
    if (nelems == 0) {
      return null
    }

    // null terminates
    var count = 0
    while (count < nelems) {
      if (b[count].toInt() == 0) {
        break
      }
      count++
    }
    return String(b, 0, count, charset)
  }

  // skip to a 4 byte boundary in the file
  @Throws(IOException::class)
  fun skipToBoundary(nbytes: Int) {
    filePos.pos += padding(nbytes)
  }

  @Throws(IOException::class)
  fun showDetail(out: Formatter) {
    val actual: Long = raf.size
    out.format("  raf length= $actual\n")
    out.format("  useLongOffset= $useLongOffset\n")
    out.format("  dataStart= $dataStart\n")
    out.format("  nonRecordData size= $nonRecordDataSize\n")
    out.format("  unlimited dimension = $unlimitedDimension\n")
    if (unlimitedDimension != null) {
      out.format("  record Data starts = $recStart\n")
      out.format("  recsize = $recsize\n")
      out.format("  numrecs = $numrecs\n")
    }
    val calcSize = calcFileSize()
    out.format("  computedSize = $calcSize\n")
    if (actual < calcSize)
      out.format("  TRUNCATED!! actual size = $actual (${calcSize - actual} bytes) \n")
    else if (actual != calcSize)
      out.format(" actual size larger = $actual (${calcSize - actual} bytes extra) \n")
    out.format(String.format(String.format("%n  %20s____start_____size__unlim%n", "name")))
    for (vb in root.variables) {
      val vinfo = vb.spObject as Vinfo
      out.format("  %20s %8d %8d  %s %n",
            vinfo.name,
            vinfo.begin,
            vinfo.vsize.toLong(),
            vinfo.isRecordVariable
          )
    }
  }

  companion object {
    val MAGIC = byteArrayOf(0x43, 0x44, 0x46, 0x01)
    val MAGIC_LONG = byteArrayOf(0x43, 0x44, 0x46, 0x02)   // 64-bit offset format
    val debugSize = false

    const val MAGIC_DIM = 10
    const val MAGIC_VAR = 11
    const val MAGIC_ATT = 12
    var debugHeaderSize = false

    @Throws(IOException::class)
    fun isValidFile(raf: OpenFile): Boolean {
      return when (NetchdfFileFormat.findNetcdfFormatType(raf)) {
        NetchdfFileFormat.NC_FORMAT_CLASSIC, NetchdfFileFormat.NC_FORMAT_64BIT_OFFSET -> true
        else -> false
      }
    }

    // find number of bytes needed to pad to a 4 byte boundary
    fun padding(nbytes: Int): Int {
      var pad = nbytes % 4
      if (pad != 0) pad = 4 - pad
      return pad
    }

    // find number of bytes needed to pad to a 4 byte boundary
    fun padding(nbytes: Long): Int {
      var pad = (nbytes % 4).toInt()
      if (pad != 0) pad = 4 - pad
      return pad
    }

    fun getDatatype(type: Int): Datatype {
      return when (type) {
        1 -> Datatype.BYTE
        2 -> Datatype.CHAR
        3 -> Datatype.SHORT
        4 -> Datatype.INT
        5 -> Datatype.FLOAT
        6 -> Datatype.DOUBLE
        else -> throw IllegalArgumentException("unknown type == $type")
      }
    }
  }

// variable info for reading/writing
  /**
   * @param vsize size of array in bytes; if isRecordVariable, size per record.
   * @param begin offset of start of data from start of file
   * @param isRecordVariable  is it a record variable?
   * @param attsPos attributes start here - used for update
   */
  data class Vinfo(
    val name: String,
    val vsize: Int, // if record, per record
    val begin: Long,
    val isRecordVariable: Boolean,
    val elemSize: Int
  )

}
