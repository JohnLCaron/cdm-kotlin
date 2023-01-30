package sunya.cdm.netcdf3

import java.io.*
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import mu.KotlinLogging
import sunya.cdm.api.*
import sunya.cdm.api.DataType.*
import sunya.cdm.iosp.ArrayLong
import sunya.cdm.iosp.Iosp
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

/** Netcdf version 3 header. Read-only version using Builders for immutablility.  */
class N3header(raf: OpenFile, root: Group.Builder, debugOut: Formatter?) {

  private val raf: OpenFile
  private var unlimitedDimension: Dimension? = null
  private val filePos = OpenFileState(0L, ByteOrder.BIG_ENDIAN)

  // N3iosp needs access to these
  private var isStreaming = false
  private var isUnlimited = false
  var numrecs = 0 // number of records written
  var recsize: Long = 0 // size of each record (padded) TODO can it really be bigger than MAX_INTEGER ?
  var recStart = Int.MAX_VALUE.toLong() // where the record data starts TODO can it really be bigger than MAX_INTEGER ?
  var useLongOffset = false
  var nonRecordDataSize: Long = 0 // size of non-record variables
  private val vars: MutableList<Vinfo> = ArrayList()
  var dataStart = Long.MAX_VALUE // where the data starts

  private val valueCharset = StandardCharsets.UTF_8

  private val fileDimensions = mutableListOf<Dimension>()
  private val unlimitedVariables = mutableListOf<Variable>() // vars that have the unlimited dimension

  init {
    this.raf = raf
    val actualSize: Long = raf.size
    nonRecordDataSize = 0 // length of non-record data
    recsize = 0 // length of single record
    recStart = Int.MAX_VALUE.toLong() // where the record data starts

    // netcdf magic number
    val b = ByteArray(4)
    raf.readBytes(b, filePos)
    if (!isMagicBytes(b)) {
      throw IOException("Not a netCDF file " + raf.location)
    }
    if (b[3].toInt() != 1 && b[3].toInt() != 2) throw IOException("Not a netCDF file " + raf.location)
    useLongOffset = b[3].toInt() == 2

    // number of records
    numrecs = raf.readInt(filePos)
    debugOut?.format("numrecs= $numrecs\n")
    if (numrecs == -1) {
      isStreaming = true
      numrecs = 0
    }

    // dimensions
    readDimensions(raf, root, debugOut)

    // global attributes
    readAttributes(root.attributes, debugOut)

    // variables
    readVariables(raf, root, debugOut)

    if (dataStart == Long.MAX_VALUE) { // if nvars == 0
      dataStart = filePos.pos
    }
    if (nonRecordDataSize > 0) { // if there are non-record variables
      nonRecordDataSize -= dataStart
    }
    if (unlimitedVariables.isEmpty()) { // if there are no record variables
      recStart = 0
    }

    /* check for streaming file - numrecs must be calculated
    // Example: TestDir.cdmUnitTestDir + "ft/station/madis2.nc"
    if (isStreaming) {
      val recordSpace = actualSize - recStart
      numrecs = if (recsize == 0L) 0 else (recordSpace / recsize).toInt()

      // set size of the unlimited dimension, reset the record variables
      if (unlimitedDimension != null) {
        unlimitedDimension = unlimitedDimension.toBuilder().setLength(numrecs).build()
        root.replaceDimension(unlimitedDimension)
        unlimitedVariables.forEach(Consumer<Variable.Builder<*>> { v: Variable.Builder<*> ->
          v.replaceDimensionByName(
            unlimitedDimension
          )
        })
      }
    }

     */

    // Check if file affected by bug CDM-52 (netCDF-Java library used incorrect padding when
    // the file contained only one record variable and it was of type byte, char, or short).
    /* Example TestDir.cdmLocalTestDataDir + "byteArrayRecordVarPaddingTest-bad.nc"
    if (unlimitedVariables.size == 1) {
      val uvar: Variable.Builder<*> = uvars[0]
      val dtype: ArrayType = uvar.dataType
      if (dtype === CHAR || dtype === BYTE || dtype === SHORT) {
        var vsize: Long = dtype.size.toLong() // works for all netcdf-3 data types
        val dims: List<Dimension> = uvar.getDimensions()
        for (curDim in dims) {
          if (!curDim.isUnlimited) vsize *= curDim.length
        }
        val vinfo = uvar.spiObject as Vinfo
        if (vsize != vinfo.vsize) {
          logger.info(
            java.lang.String.format(
              "Malformed netCDF file (%s): incorrect padding for record variable (CDM-52): fvsize= %d != calc size=%d",
              raf.location, vinfo.vsize, vsize
            )
          )
          recsize = vsize
          vinfo.vsize = vsize
        }
      }
    }

     */
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
      if (disallowFileTruncation) throw IOException("File is truncated, calculated size= $calcSize actual = $actualSize") else {
        // logger.info("File is truncated calculated size= "+calcSize+" actual = "+actualSize);
        raf.setExtendMode()
      }
    }

    // add a record structure if asked to do so
    if (n3iospNew.useRecordStructure && uvars.size > 0) {
      makeRecordStructure(root, uvars)
    }

     */
  }

  private fun readDimensions(raf: OpenFile, root: Group.Builder, debugOut: Formatter?) {
    var numdims = 0
    val magic = raf.readInt(filePos)
    if (magic == 0) {
      raf.readInt(filePos) // skip 32 bits
    } else {
      if (magic != MAGIC_DIM) throw IOException("Malformed netCDF file - dim magic number wrong " + raf.location)
      numdims = raf.readInt(filePos)
      debugOut?.format("numdims= $numdims\n")
    }

    // Must keep dimensions in strict order
    for (i in 0 until numdims) {
      debugOut?.format("  dim $i pos= ${filePos.pos}\n")
      val name = readString()
      val len: Int = raf.readInt(filePos)

      var dim: Dimension
      if (len == 0) {
        dim = Dimension(name, numrecs, true)
        unlimitedDimension = dim
      } else {
        dim = Dimension(name, len)
      }
      fileDimensions.add(dim)
      root.addDimension(dim)
      debugOut?.format(" added dimension $dim\n")
    }
  }

  private fun readVariables(raf: OpenFile, root: Group.Builder, debugOut: Formatter?) {
    // variables
    val magic = raf.readInt(filePos)
    val nvars = if (magic == 0) {
      raf.readInt(filePos) // skip 32 bits
      0
    } else {
      if (magic != MAGIC_VAR) throw IOException("Malformed netCDF file  - var magic number wrong ${raf.location}")
      raf.readInt(filePos)
    }
    debugOut?.format("num variables= $nvars\n")

    // loop over variables
    for (i in 0 until nvars) {
      val ncvarb = Variable.Builder()
      val name = readString()
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

      if (debugOut != null) {
        debugOut.format("---name=<${ncvarb.name}> dims = [")
        for (dim in dims) debugOut.format("${dim.name} ")
        debugOut.format("]\n")
      }

      // variable attributes
      val varAttsPos: Long = filePos.pos
      readAttributes(ncvarb.attributes, debugOut)

      // data type
      val type: Int = raf.readInt(filePos)
      val dataType = getDataType(type)
      ncvarb.dataType = dataType

      // size and beginning data position in file
      val vsize = raf.readInt(filePos)
      val begin = if (useLongOffset) raf.readLong(filePos) else raf.readInt(filePos).toLong()
      if (debugOut != null) {
        debugOut.format(
          " name= $name type=$type vsize=$vsize velems=$velems begin=$begin isRecord=$isRecord attsPos=$varAttsPos\n"
        )
        val calcVsize: Long = (velems + padding(velems)) * dataType.size
        if (vsize.toLong() != calcVsize) debugOut.format(" *** readVsize $vsize != calcVsize $calcVsize\n")
      }
      //if (vsize < 0) { // when does this happen ?? streaming i think
      //  vsize = (velems + padding(velems)) * dataType.size
      //}
      val vinfo = Vinfo(name, vsize.toUInt(), begin, isRecord, varAttsPos.toULong())
      vars.add(vinfo)
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
      //if (ncvarb.isUnlimited) {
      //  unlimitedVariables.add(ncvarb) // track record variables
      //}
    }
  }

  /**
   * Check if the given bytes correspond to
   * [magic bytes][.MAGIC] of the header.
   *
   * @param bytes given bytes.
   * @return `true` if the given bytes correspond to
   * [magic bytes][.MAGIC] of the header. Otherwise `false`.
   */
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
  private fun readAttributes(atts: MutableList<Attribute>, debugOut: Formatter?): Int {
    var natts = 0
    val magic: Int = raf.readInt(filePos)
    if (magic == 0) {
      raf.readInt(filePos) // skip 32 bits
    } else {
      if (magic != MAGIC_ATT) throw IOException("Malformed netCDF file  - att magic number wrong " + raf.location)
      natts = raf.readInt(filePos)
    }
    debugOut?.format(" num atts= %d%n", natts)
    for (i in 0 until natts) {
      debugOut?.format("***att $i pos= ${filePos.pos}\n")
      val name = readString()
      val type: Int = raf.readInt(filePos)
      var att: Attribute?
      if (type == 2) { // CHAR
        debugOut?.format(" begin read String val pos= ${filePos.pos}\n")
        val value = readString(valueCharset)
        debugOut?.format(" end read String val pos= ${filePos.pos}\n")
        att = Attribute(name, value)
      } else {
        debugOut?.format(" begin read val ${filePos.pos}\n")
        val nelems: Int = raf.readInt(filePos)
        val dtype: DataType = getDataType(type)
        val builder = Attribute.Builder()
        builder.name = name
        builder.dataType = dtype
        if (nelems > 0) {
          val nbytes = readAttributeArray(dtype, nelems, builder)
          skipToBoundary(nbytes)
        }
        att = builder.build()
        debugOut?.format(" end read val pos= ${filePos.pos}\n")
      }
      atts.add(att)
      debugOut?.format("  $att\n")
    }
    return natts
  }

  @Throws(IOException::class)
  fun readAttributeArray(type: DataType, nelems: Int, attBuilder: Attribute.Builder): Int {
    when (type) {
      CHAR, BYTE -> {
        attBuilder.values = raf.readArrayByte(filePos, nelems).asList()
        return nelems
      }

      SHORT -> {
        attBuilder.values = raf.readArrayShort(filePos, nelems).asList()
        return 2 * nelems
      }

      INT -> {
        attBuilder.values = raf.readArrayInt(filePos, nelems).asList()
        return 4 * nelems
      }

      FLOAT -> {
        attBuilder.values = raf.readArrayFloat(filePos, nelems).asList()
        return 4 * nelems
      }

      DOUBLE -> {
        attBuilder.values = raf.readArrayDouble(filePos, nelems).asList()
        return 8 * nelems
      }

      LONG -> {
        attBuilder.values = raf.readArrayLong(filePos, nelems).asList()
        return 8 * nelems
      }

      else -> return 0
    }
  }

  // read a string = (nelems, byte array), then skip to 4 byte boundary
  @Throws(IOException::class)
  fun readString(): String {
    return readString(StandardCharsets.UTF_8)
  }

  @Throws(IOException::class)
  private fun readString(charset: Charset): String {
    val nelems: Int = raf.readInt(filePos)
    val b = ByteArray(nelems)
    raf.readBytes(b, filePos)
    skipToBoundary(nelems) // pad to 4 byte boundary
    if (nelems == 0) {
      return ""
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

  /*
  private fun makeRecordStructure(root: Group.Builder, uvars: MutableList<Variable.Builder<*>>): Boolean {
    val recordStructure: Structure.Builder<*> = Structure.builder().setName("record")
    recordStructure.setParentGroupBuilder(root).setDimensionsByName(udim.getShortName())
    for (v in uvars) {
      val memberV: Variable.Builder<*> = v.makeSliceBuilder(0, 0) // set unlimited dimension to 0
      recordStructure.addMemberVariable(memberV)
    }
    root.addVariable(recordStructure)
    uvars.add(recordStructure)
    return true
  }

   */


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
    for (vinfo in vars) {
      out.format(
        String.format(
          String.format(
            "  %20s %8d %8d  %s %n",
            vinfo.name,
            vinfo.begin.toLong(),
            vinfo.vsize.toLong(),
            vinfo.isRecordVariable
          )
        )
      )
    }
  }

  companion object {
    val MAGIC = byteArrayOf(0x43, 0x44, 0x46, 0x01)
    val MAGIC_LONG = byteArrayOf(0x43, 0x44, 0x46, 0x02)   // 64-bit offset format

    const val MAGIC_DIM = 10
    const val MAGIC_VAR = 11
    const val MAGIC_ATT = 12
    var disallowFileTruncation = false
    var debugHeaderSize = false

    @Throws(IOException::class)
    fun isValidFile(raf: OpenFile): Boolean {
      return when (NetcdfFileFormat.findNetcdfFormatType(raf)) {
        NetcdfFileFormat.NETCDF3, NetcdfFileFormat.NETCDF3_64BIT_OFFSET -> true
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

    fun getDataType(type: Int): DataType {
      return when (type) {
        1 -> BYTE
        2 -> CHAR
        3 -> SHORT
        4 -> INT
        5 -> FLOAT
        6 -> DOUBLE
        else -> throw IllegalArgumentException("unknown type == $type")
      }
    }

    fun getType(dataType: DataType): Int {
      return when (dataType) {
        BYTE -> 1
        CHAR -> 2
        SHORT -> 3
        INT -> 4
        FLOAT -> 5
        DOUBLE -> 6
        else -> throw IllegalArgumentException("unknown DataType == $dataType")
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
    val vsize: UInt,
    val begin: Long,
    val isRecordVariable: Boolean,
    val attsPos: ULong
  )


  fun getIosp() : Iosp = N3iosp(raf)

}
