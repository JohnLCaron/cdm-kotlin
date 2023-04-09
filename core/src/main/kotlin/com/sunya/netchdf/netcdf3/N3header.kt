package com.sunya.netchdf.netcdf3

import com.sunya.cdm.api.*
import com.sunya.cdm.array.*
import com.sunya.cdm.iosp.OpenFile
import com.sunya.cdm.iosp.OpenFileState
import com.sunya.netchdf.netcdf4.NetchdfFileFormat
import java.io.IOException
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*

class N3header(val raf: OpenFile, val root: Group.Builder) {
  private val filePos = OpenFileState(0L, ByteOrder.BIG_ENDIAN)
  private val valueCharset = StandardCharsets.UTF_8
  private val isPnetcdf : Boolean
  private val useLongOffset : Boolean
  private val isStreaming : Boolean

  val numrecs : Long // number of records written
  val recsize : Long // size of each record (padded)
  var unlimitedDimension: Dimension? = null

  // not really needed
  private val dataStart : Long // where the data starts
  private val recordDataStart : Long // where the record data starts
  private val nonRecordDataSize : Long // size of non-record variables

  fun formatType() : String {
    return if (isPnetcdf) "netcdf3.5" else
           if (useLongOffset) "netcdf3.2" else
           "netcdf3  "
  }

  init {
    val actualSize: Long = raf.size

    // netcdf magic number
    val format = NetchdfFileFormat.findNetcdfFormatType(raf)
    if (!format.isNetdf3format) {
      throw RuntimeException("Not a Netcdf3 File")
    }
    filePos.pos = 4
    isPnetcdf = (format == NetchdfFileFormat.NC_FORMAT_64BIT_DATA)
    useLongOffset = (format == NetchdfFileFormat.NC_FORMAT_64BIT_OFFSET) || isPnetcdf

    // number of records
    numrecs = if (isPnetcdf) raf.readLong(filePos) else raf.readInt(filePos).toLong()
    isStreaming = (numrecs == -1L)

    // dimensions
    readDimensions(raf, root)

    // global attributes
    readAttributes(root.attributes)

    // variables
    readVariables(raf, root)

    // count stuff
    val unlimitedVariables = mutableListOf<Variable.Builder>()
    var sumRecord = 0L
    var firstData = Long.MAX_VALUE
    var firstRecordData = Long.MAX_VALUE
    var nonRecordDataSum: Long = 0 // size of non-record variables
    root.variables.forEach { vb ->
      val vinfo = vb.spObject as VinfoN3
      if (vinfo.isRecordVariable) {
        sumRecord += vinfo.vsize
        firstRecordData = Math.min(firstRecordData, vinfo.begin)
        unlimitedVariables.add(vb)
      } else {
        nonRecordDataSum = Math.max(nonRecordDataSum, vinfo.begin + vinfo.vsize)
      }
      firstData = Math.min(firstData, vinfo.begin)
    }
    this.dataStart = firstData
    this.recordDataStart = firstRecordData
    this.nonRecordDataSize = nonRecordDataSum

    // Check if file affected by bug CDM-52 (netCDF-Java library used incorrect padding when
    // the file contained only one record variable and it was of type byte, char, or short).
    // Example TestDir.cdmLocalTestDataDir + "byteArrayRecordVarPaddingTest-bad.nc"
    // Example devcdm/core/src/test/data/netcdf3/WrfTimesStrUnderscore.nc
    if (unlimitedVariables.size == 1) {
      val uvar = unlimitedVariables[0]
      val dtype = uvar.datatype
      if (dtype == Datatype.CHAR || dtype == Datatype.BYTE || dtype == Datatype.SHORT) {

        var nelems = 1L
        for (dim in uvar.dimensions) {
          if (dim != unlimitedDimension) nelems *= dim.length
        }
        val vinfo = uvar.spObject as VinfoN3
        val padding = padding(nelems * dtype.size)
        val calcVsize: Long = nelems * dtype.size + padding

        if (calcVsize != vinfo.vsize) {
          if (debugHeaderSize) {
            println(" *** Special padding (${raf.location}): vsize=  ${vinfo.vsize} != calcVsize=$calcVsize padding = $padding")
          }
          sumRecord = calcVsize
          uvar.spObject = vinfo.copy(vsize = calcVsize)
        }
      }
    }
    this.recsize = sumRecord

    if (debug && debugHeaderSize) {
      println("  filePointer = ${filePos.pos} dataStart=$dataStart")
      println("  recStart = $recordDataStart dataStart+nonRecordDataSize = ${dataStart + nonRecordDataSize}")
      println("  nonRecordDataSize size= $nonRecordDataSize")
      println("  recsize= $recsize")
      println("  numrecs= $numrecs")
      println("  actualSize= $actualSize")
    }
  }

  private fun readDimensions(raf: OpenFile, root: Group.Builder) {
    val magic = raf.readInt(filePos)
    val numdims = if (magic == 0) {
      if (isPnetcdf) filePos.pos += 8 else filePos.pos += 4
      0
    } else {
      if (magic != MAGIC_DIM) throw IOException("Malformed netCDF file - dim magic number wrong " + raf.location)
      if (isPnetcdf) raf.readLong(filePos).toInt() else raf.readInt(filePos)
    }

    // Must keep dimensions in strict order
    for (i in 0 until numdims) {
      val name = readString()!!
      val len = if (isPnetcdf) raf.readLong(filePos) else raf.readInt(filePos).toLong()
      if (len > Int.MAX_VALUE) {
        throw RuntimeException("Dimension $name length $len too big")
      }

      var dim: Dimension
      if (len == 0L) {
        dim = Dimension(name, numrecs.toInt())
        unlimitedDimension = dim
      } else {
        dim = Dimension(name, len.toInt())
      }
      root.addDimension(dim)
      if (debug) println("  dim $dim pos=${filePos.pos} isRecord=${dim == unlimitedDimension}")
    }
  }

  private fun readVariables(raf: OpenFile, root: Group.Builder) {
    // variables
    val magic = raf.readInt(filePos)
    val nvars = if (magic == 0) {
      if (isPnetcdf) filePos.pos += 8 else filePos.pos += 4
      0
    } else {
      if (magic != MAGIC_VAR) throw IOException("Malformed netCDF file - var magic number wrong ${raf.location}")
      if (isPnetcdf) raf.readLong(filePos).toInt() else raf.readInt(filePos)
    }

    // loop over variables
    for (i in 0 until nvars) {
      val name = readString()!!
      val ncvarb = Variable.Builder(name)
      if (debug) println("  reading variable ${name} pos=${filePos.pos}")

      // get element count in non-record dimensions
      var nelems: Long = 1
      var isRecord = false
      val rank: Int = if (isPnetcdf) raf.readLong(filePos).toInt() else raf.readInt(filePos)
      val dims = mutableListOf<Dimension>()
      val dimIdx = mutableListOf<Int>()
      val dimLengths = mutableListOf<Int>()
      for (j in 0 until rank) {
        val dimIndex: Int = if (isPnetcdf) raf.readLong(filePos).toInt() else raf.readInt(filePos)
        val dim: Dimension = root.dimensions[dimIndex]
        if (dim == unlimitedDimension) {
          isRecord = true
        } else {
          nelems *= dim.length
        }
        dims.add(dim)
        dimIdx.add(dimIndex)
        dimIdx.add(dim.length)
      }
      ncvarb.dimensions.addAll(dims)
      if (debug) println("  reading variable ${name} pos=${filePos.pos} dimIdx = ${dimIdx}")

      // variable attributes
      readAttributes(ncvarb.attributes)

      // data type
      val type: Int = raf.readInt(filePos)
      val datatype = getDatatype(type)
      ncvarb.datatype = datatype

      // Use vsize, even if it disagrees with the "calculated" value. Clib recalculates, at least sometimes.
      val vsize = if (isPnetcdf) raf.readLong(filePos) else raf.readInt(filePos).toLong()
      val begin = if (useLongOffset) raf.readLong(filePos) else raf.readInt(filePos).toLong()
      if (debugVariableSize) {
        println("  reading variable $name type=$type nelems=$nelems elemSize=${datatype.size} vsize=$vsize begin=$begin isRecord=$isRecord")
        val padding = padding(nelems * datatype.size)
        val calcVsize: Long = nelems * datatype.size + padding
        if (vsize != calcVsize) println("    *** readVsize $vsize != calcVsize $calcVsize")
      }

      ncvarb.spObject = VinfoN3(name, vsize, begin, isRecord, datatype.size)
      root.variables.add(ncvarb)
      if (debug) println("  done variable ${ncvarb.datatype} ${ncvarb.name}${dimLengths} pos=${filePos.pos}")
    }
  }

  @Throws(IOException::class)
  private fun readAttributes(atts: MutableList<Attribute>): Int {
    val magic: Int = raf.readInt(filePos)
    val natts = if (magic == 0) {
      if (isPnetcdf) filePos.pos += 8 else filePos.pos += 4
      0
    } else {
      if (magic != MAGIC_ATT) throw IOException("Malformed netCDF file  - att magic number wrong " + raf.location)
      if (isPnetcdf) raf.readLong(filePos).toInt() else raf.readInt(filePos)
    }

    for (i in 0 until natts) {
      val name = readString()!!
      val type: Int = raf.readInt(filePos)
      val att = if (type == 2) { // CHAR
        val value = readString(valueCharset)
        if (value == null) Attribute(name, Datatype.STRING, emptyList<String>()) // nelems = 0
              else Attribute(name, value) // may be empty string
      } else {
        val nelems: Int = if (isPnetcdf) raf.readLong(filePos).toInt() else raf.readInt(filePos)
        val dtype: Datatype = getDatatype(type)
        val builder = Attribute.Builder()
        builder.name = name
        builder.datatype = dtype
        if (nelems > 0) {
          val nbytes = readAttributeArray(dtype, nelems, builder)
          skipToBoundary(nbytes)
        }
        builder.build()
      }
      atts.add(att)
      if (debug) println("    $att pos=${filePos.pos}")
    }
    return natts
  }

  @Throws(IOException::class)
  fun readAttributeArray(type: Datatype, nelems: Int, attBuilder: Attribute.Builder): Int {
    return when (type) {
      Datatype.BYTE -> {
        attBuilder.values = raf.readArrayByte(filePos, nelems).asList()
        nelems
      }
      Datatype.UBYTE -> {
        attBuilder.values = raf.readArrayByte(filePos, nelems).map { it.toUByte() }
        nelems
      }
      Datatype.CHAR -> {
        val wtf  = ArrayUByte(intArrayOf(1), raf.readByteBuffer(filePos, nelems))
        attBuilder.values = wtf.makeStringFromBytes().toList()
        nelems
      }
      Datatype.SHORT -> {
        attBuilder.values = raf.readArrayShort(filePos, nelems).asList()
        2 * nelems
      }
      Datatype.USHORT -> {
        attBuilder.values = raf.readArrayShort(filePos, nelems).map { it.toUShort() }
        2 * nelems
      }
      Datatype.INT -> {
        attBuilder.values = raf.readArrayInt(filePos, nelems).asList()
        4 * nelems
      }
      Datatype.UINT -> {
        attBuilder.values = raf.readArrayShort(filePos, nelems).map { it.toUInt() }
        4 * nelems
      }
      Datatype.FLOAT -> {
        attBuilder.values = raf.readArrayFloat(filePos, nelems).asList()
        4 * nelems
      }
      Datatype.DOUBLE -> {
        attBuilder.values = raf.readArrayDouble(filePos, nelems).asList()
        8 * nelems
      }
      Datatype.LONG -> {
        attBuilder.values = raf.readArrayLong(filePos, nelems).asList()
        8 * nelems
      }
      Datatype.ULONG -> {
        attBuilder.values = raf.readArrayLong(filePos, nelems).map { it.toULong() }
        8 * nelems
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
    val nelems: Int = if (isPnetcdf) raf.readLong(filePos).toInt() else raf.readInt(filePos)
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

  companion object {
    val debug = false
    val debugVariableSize = false
    var debugHeaderSize = false

    const val MAGIC_DIM = 10
    const val MAGIC_VAR = 11
    const val MAGIC_ATT = 12

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
        7 -> Datatype.UBYTE
        8 -> Datatype.USHORT
        9 -> Datatype.UINT
        10 -> Datatype.LONG
        11 -> Datatype.ULONG
        else -> throw IllegalArgumentException("unknown type == $type")
      }
    }
  }
}

/**
 * @param vsize size of data in bytes; if isRecordVariable, size per record.
 * @param begin offset of start of data from start of file
 * @param isRecordVariable  is it a record variable?
 * @param elemSize  size in bytes of one element
 */
data class VinfoN3(
  val name: String,
  val vsize: Long, // number of bytes. if record, per record else total.
  val begin: Long,
  val isRecordVariable: Boolean,
  val elemSize: Int
)