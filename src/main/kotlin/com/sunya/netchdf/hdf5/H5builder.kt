package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.*
import com.sunya.cdm.dsl.structdsl
import com.sunya.cdm.iosp.*
import mu.KotlinLogging
import java.io.IOException
import java.nio.*
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*

private val debugStart = false
private val debugTracker = true
private val debugSuperblock = false

/**
 * Build the rootGroup for an HD5 file.
 * @param valueCharset used when reading HDF5 header.
 */
class H5builder(val raf: OpenFile,
                val strict : Boolean,
                val valueCharset: Charset = StandardCharsets.UTF_8,
) {
    
    private var baseAddress: Long = 0 // may be offset for arbitrary metadata
    var sizeOffsets: Int = 0
    var sizeLengths: Int = 0
    var sizeHeapId = 0
    var isOffsetLong = false
    var isLengthLong = false
    val memTracker = MemTracker(raf.size)

    var isNetcdf4 = false
    private val h5rootGroup : H5Group
    internal val hashGroups = mutableMapOf<Long, H5GroupBuilder>()

    internal val symlinkMap = mutableMapOf<String, DataObjectFacade>()
    private val addressMap = mutableMapOf<Long, DataObject>()

    val cdmRoot : Group

    init {
        // search for the superblock - no limits on how far into the file
        val state = OpenFileState(0L, ByteOrder.LITTLE_ENDIAN)
        var filePos = 0L
        while (filePos < raf.size - 8L) {
            state.pos = filePos
            val testForMagic = raf.readByteBuffer(state, 8).array()
            if (testForMagic.contentEquals(magicHeader)) {
                break
            }
            filePos = if (filePos == 0L) 512 else 2 * filePos
        }
        val superblockStart = filePos
        if (debugStart) {
            println("H5builder opened file ${raf.location} at pos $superblockStart")
        }
        if (debugTracker) memTracker.add("header", 0, superblockStart)
        this.baseAddress = superblockStart

        val rootGroupBuilder : H5GroupBuilder
        val superBlockVersion = raf.readByte(state).toInt()
        if (superBlockVersion < 2) {
            rootGroupBuilder = readSuperBlock01dsl(superblockStart, state, superBlockVersion)
            // state.pos = superblockStart + 9
            //}
            //if (version < 2) { // 0 and 1
            //    readSuperBlock01(superblockStart, version, state)
        } else if (superBlockVersion < 4) { // 2 and 3
            rootGroupBuilder = readSuperBlock23(superblockStart, state, superBlockVersion)
        } else {
            throw IOException("Unknown superblock version= $superBlockVersion")
        }

        // now look for symbolic links TODO this doesnt work??
        replaceSymbolicLinks(rootGroupBuilder)

        // build tree of H5groups
        h5rootGroup = rootGroupBuilder.build()
        // convert into CDM
        this.cdmRoot = this.buildCdm(h5rootGroup)
        // println(" cdmRoot = {\n${cdmRoot.cdlString()}}")

        /* recursively run through all the dataObjects and add them to the ncfile
        val allSharedDimensions = makeNetcdfGroup(root, h5rootGroup)
        if (allSharedDimensions) isNetcdf4 = true
        if (debugTracker) {
            val f = Formatter()
            memTracker.report(f)
            println(f.toString())
        } */
    }
    private fun readSuperBlock01dsl(superblockStart : Long, state : OpenFileState, version : Int) : H5GroupBuilder {
        // have to cheat a bit
        state.pos = superblockStart + 13
        this.sizeOffsets = raf.readByte(state).toInt()
        this.sizeLengths = raf.readByte(state).toInt()
        this.isOffsetLong = (sizeOffsets == 8)
        this.isLengthLong = (sizeLengths == 8)
        this.sizeHeapId = 8 + sizeOffsets

        state.pos = superblockStart

      val superblock01 =
          structdsl("superblock01", raf, state) {
            fld("format", 8)
            fld("version", 1)
            fld("versionFSS", 1)
            fld("versionGST", 1)
            skip(1)
            fld("versionSHMF", 1)
            fld("sizeOffsets", 1)
            fld("sizeLengths", 1)
            skip(1)
            fld("groupLeafNodeSize", 2)
            fld("groupInternalNodeSize", 2)
            fld("flags", 4)
            if (version == 1) {
                fld("storageInternalNodeSize", 2)
                skip(2)
            }
            fld("baseAddress") { sizeOffsets }
            fld("heapAddress") { sizeOffsets }
            fld("eofAddress") { sizeOffsets }
            fld("driverAddress") { sizeOffsets }
        }
        if (debugSuperblock) superblock01.show()

        // look for file truncation
        var eofAddress : Long = superblock01.getLong("eofAddress")
        if (superblock01.getLong("baseAddress") != superblockStart) {
            eofAddress += superblockStart
            if (debugStart) {
                println(" baseAddress set to superblockStart")
            }
        }
        if (raf.size < eofAddress) throw IOException(
            "File is truncated should be= $eofAddress actual ${raf.size} location= ${state.pos}")

        // extract the root group object, recursively read all objects
        val rootSymbolTableEntry = this.readSymbolTable(state)
        val rootObject = this.getDataObject(rootSymbolTableEntry.objectHeaderAddress, "root")

        return this.readH5Group(DataObjectFacade(null, "root").setDataObject(rootObject))!!
    }

    @Throws(IOException::class)
    private fun readSuperBlock23(superblockStart: Long,  state : OpenFileState, version: Int) : H5GroupBuilder {
        if (debugStart) {
            println("readSuperBlock version = $version")
        }
        sizeOffsets = raf.readByte(state).toInt()
        isOffsetLong = (sizeOffsets == 8)
        sizeLengths = raf.readByte(state).toInt()
        isLengthLong = (sizeLengths == 8)
        if (debugStart) {
            println(" sizeOffsets= $sizeOffsets sizeLengths= $sizeLengths")
            println(" isLengthLong= $isLengthLong isOffsetLong= $isOffsetLong")
        }
        val fileFlags: Byte = raf.readByte(state)
        if (debugStart) {
            println(" fileFlags= 0x${Integer.toHexString(fileFlags.toInt())}")
        }
        baseAddress = readOffset(state)
        val extensionAddress = readOffset(state)
        var eofAddress = readOffset(state)
        val rootObjectAddress = readOffset(state)
        val checksum: Int = raf.readInt(state)
        if (debugStart) {
            println(" baseAddress= 0x${java.lang.Long.toHexString(baseAddress)}")
            println(" extensionAddress= 0x${java.lang.Long.toHexString(extensionAddress)}")
            println(" eof Address=$eofAddress")
            println(" raf length= ${raf.size}")
            println(" rootObjectAddress= 0x${java.lang.Long.toHexString(rootObjectAddress)}")
            println("")
        }
        if (debugTracker) memTracker.add("superblock", superblockStart, state.pos)
        if (baseAddress != superblockStart) {
            baseAddress = superblockStart
            eofAddress += superblockStart
            if (debugStart) {
                println(" baseAddress set to superblockStart")
            }
        }

        // look for file truncation
        val fileSize: Long = raf.size
        if (fileSize < eofAddress) {
            throw IOException("File is truncated should be= $eofAddress actual = $fileSize")
        }

        val rootObject = this.getDataObject(rootObjectAddress, "root")
        val facade = DataObjectFacade(null, "root").setDataObject( rootObject)
        return this.readH5Group(facade)!!
    }

    //////////////////////////////////////////////////////////////
    // Internal organization of Data Objects


    @Throws(IOException::class)
    fun convertReferenceToDataObjectName(reference: Long): String {
        val name = getDataObjectName(reference)
        return name ?: reference.toString() // LOOK
    }

    @Throws(IOException::class)
    fun convertReferencesToDataObjectName(refArray: Array<Long>): List<String> {
        return refArray.map { convertReferenceToDataObjectName(it) }
    }

    /**
     * Get a data object's name, using the objectId you get from a reference (aka hard link).
     *
     * @param objId address of the data object
     * @return String the data object's name, or null if not found
     * @throws IOException on read error
     */
    @Throws(IOException::class)
    fun getDataObjectName(objId: Long): String? {
        val dobj = getDataObject(objId, null)
        return if (dobj == null) {
            logger.error("getDataObjectName cant find dataObject id= $objId")
            null
        } else {
            dobj.name
        }
    }

    /**
     * All access to data objects come through here, so we can cache.
     * Look in cache first; read if not in cache.
     *
     * @param address object address (aka id)
     * @param name optional name
     * @return DataObject
     * @throws IOException on read error
     */
    @Throws(IOException::class)
    fun getDataObject(address: Long, name: String?): DataObject {
        // find it
        var dobj = addressMap[address]
        if (dobj != null) {
            if (dobj.name == null && name != null) dobj.name = name
            return dobj
        }
        // if (name == null) return null; // ??

        // read it
        dobj = this.readDataObject(address, name?: "")
        addressMap[address] = dobj
        return dobj
    }

    /*
    private fun replaceSymbolicLinks(group: H5Group?) {
        if (group == null) return
        val objList: MutableList<DataObjectFacade> = group.nestedObjects
        var count = 0
        while (count < objList.size) {
            val dof: DataObjectFacade = objList[count]
            if (dof.group != null) { // group - recurse
                replaceSymbolicLinks(dof.group)
            } else if (dof.linkName != null) { // symbolic links
                val link: DataObjectFacade? = symlinkMap[dof.linkName]
                if (link == null) {
                    H5builder.Companion.log.warn(" WARNING Didnt find symbolic link={} from {}", dof.linkName, dof.name)
                    objList.removeAt(count)
                    continue
                }

                // dont allow loops
                if (link.group != null) {
                    if (group.isChildOf(link.group)) {
                        H5builder.Companion.log.warn(" ERROR Symbolic Link loop found ={}", dof.linkName)
                        objList.removeAt(count)
                        continue
                    }
                }

                // dont allow in the same group. better would be to replicate the group with the new name
                if (dof.parent === link.parent) {
                    objList.remove(dof)
                    count-- // negate the incr
                } else  // replace
                    objList[count] = link
                if (H5builder.Companion.debugSoftLink) {
                    println("  Found symbolic link={}", dof.linkName)
                }
            }
            count++
        }
    }

    fun addSymlinkMap(name: String, facade: DataObjectFacade) {
        symlinkMap[name] = facade
    }

    ///////////////////////////////////////////////////////////////
    // construct netcdf objects
    @Throws(IOException::class)
    private fun makeNetcdfGroup(parentGroup: Group.Builder, h5group: H5Group?): Boolean {

        /*
     * 6/21/2013 new algorithm for dimensions.
     * 1. find all objects with all CLASS = "DIMENSION_SCALE", make into a dimension. use shape(0) as length. keep in
     * order
     * 2. if also a variable (NAME != "This is a ...") then first dim = itself, second matches length, if multiple
     * match, use :_Netcdf4Coordinates = 0, 3 and order of dimensions.
     * 3. use DIMENSION_LIST to assign dimensions to data variables.
     */

        // 1. find all objects with all CLASS = "DIMENSION_SCALE", make into a dimension. use shape(0) as length. keep in
        // order
        for (facade: DataObjectFacade in h5group.nestedObjects) {
            if (facade.isVariable) findDimensionScales(parentGroup, h5group, facade)
        }

        // 2. if also a variable (NAME != "This is a ...") then first dim = itself, second matches length, if multiple
        // match, use :_Netcdf4Coordinates = 0, 3 and order of dimensions.
        for (facade: DataObjectFacade in h5group.nestedObjects) {
            if (facade.is2DCoordinate) findDimensionScales2D(h5group, facade)
        }
        var allHaveSharedDimensions = true

        // 3. use DIMENSION_LIST to assign dimensions to other variables.
        for (facade: DataObjectFacade in h5group.nestedObjects) {
            if (facade.isVariable) allHaveSharedDimensions =
                allHaveSharedDimensions and findSharedDimensions(parentGroup, h5group, facade)
        }
        createDimensions(parentGroup, h5group)

        // process types first
        for (facadeNested: DataObjectFacade in h5group.nestedObjects) {
            if (facadeNested.isTypedef) {
                if (H5builder.Companion.debugReference && facadeNested.dobj.mdt.type === 7) {
                    println("{}", facadeNested)
                }
                if (facadeNested.dobj.mdt.map != null) {
                    var enumTypedef: EnumTypedef? = parentGroup.findEnumeration(facadeNested.name).orElse(null)
                    if (enumTypedef == null) {
                        var basetype: ArrayType
                        when (facadeNested.dobj.mdt.byteSize) {
                            1 -> basetype = ArrayType.ENUM1
                            2 -> basetype = ArrayType.ENUM2
                            else -> basetype = ArrayType.ENUM4
                        }
                        enumTypedef = EnumTypedef(facadeNested.name, facadeNested.dobj.mdt.map, basetype)
                        parentGroup.addEnumTypedef(enumTypedef)
                    }
                }
                if (H5builder.Companion.debugV) {
                    println("  made enumeration {}", facadeNested.name)
                }
            }
        } // loop over typedefs

        // nested objects - groups and variables
        for (facadeNested: DataObjectFacade in h5group.nestedObjects) {
            if (facadeNested.isGroup) {
                val h5groupNested: H5Group = h5objects.readH5Group(facadeNested)
                if (facadeNested.group == null) // hard link with cycle
                    continue  // just skip it
                val nestedGroup: Group.Builder = Group.builder().setName(facadeNested.name)
                parentGroup.addGroup(nestedGroup)
                allHaveSharedDimensions = allHaveSharedDimensions and makeNetcdfGroup(nestedGroup, h5groupNested)
                if (debugStart) {
                    println(("--made Group " + nestedGroup.shortName).toString() + " add to " + parentGroup.shortName)
                }
            } else if (facadeNested.isVariable) {
                if (H5builder.Companion.debugReference && facadeNested.dobj.mdt.type === 7) {
                    println("{}", facadeNested)
                }
                val v: Variable.Builder<*>? = makeVariable(parentGroup, facadeNested)
                if ((v != null) && (v.dataType != null)) {
                    parentGroup.addVariable(v)
                    if (v.dataType.isEnum()) {
                        val enumTypeName: String = v.getEnumTypeName()
                        if (enumTypeName == null) {
                            H5builder.Companion.log.warn("EnumTypedef is missing for variable: {}", v.shortName)
                            throw IllegalStateException("EnumTypedef is missing for variable: " + v.shortName)
                        }
                        // This code apparently addresses the possibility of an anonymous enum
                        if (enumTypeName.isEmpty()) {
                            var enumTypedef: EnumTypedef? = parentGroup.findEnumeration(facadeNested.name).orElse(null)
                            if (enumTypedef == null) {
                                enumTypedef = EnumTypedef(facadeNested.name, facadeNested.dobj.mdt.map)
                                parentGroup.addEnumTypedef(enumTypedef)
                                v.setEnumTypeName(enumTypedef.getShortName())
                            }
                        }
                    }
                    val vinfo: H5builder.Vinfo = v.spiObject as H5builder.Vinfo
                    if (H5builder.Companion.debugV) {
                        println(("  made Variable " + v.shortName).toString() + "  vinfo= " + vinfo + "\n" + v)
                    }
                }
            }
        } // loop over nested objects

        // create group attributes last. need enums to be found first
        val fatts: List<MessageAttribute> = filterAttributes(h5group.facade.dobj.attributes)
        for (matt: MessageAttribute in fatts) {
            try {
                makeAttributes(null, matt, parentGroup.getAttributeContainer())
            } catch (e: InvalidRangeException) {
                throw IOException(e.getMessage())
            }
        }

        // add system attributes
        processSystemAttributes(h5group.facade.dobj.messages, parentGroup.getAttributeContainer())
        return allHaveSharedDimensions
    }

    /////////////////////////
    /*
   * from https://www.unidata.ucar.edu/software/netcdf/docs/netcdf.html#NetCDF_002d4-Format
   * C.3.7 Attributes
   *
   * Attributes in HDF5 and netCDF-4 correspond very closely. Each attribute in an HDF5 file is represented as an
   * attribute in the netCDF-4 file, with the exception of the attributes below, which are ignored by the netCDF-4 API.
   *
   * _Netcdf4Coordinates An integer array containing the dimension IDs of a variable which is a multi-dimensional
   * coordinate variable.
   * _nc3_strict When this (scalar, H5T_NATIVE_INT) attribute exists in the root group of the HDF5 file, the netCDF API
   * will enforce the netCDF classic model on the data file.
   * REFERENCE_LIST This attribute is created and maintained by the HDF5 dimension scale API.
   * CLASS This attribute is created and maintained by the HDF5 dimension scale API.
   * DIMENSION_LIST This attribute is created and maintained by the HDF5 dimension scale API.
   * NAME This attribute is created and maintained by the HDF5 dimension scale API.
   *
   * ----------
   * from dim_scales_wk9 - Nunes.ppt
   *
   * Attribute named "CLASS" with the value "DIMENSION_SCALE"
   * Optional attribute named "NAME"
   * Attribute references to any associated Dataset
   *
   * -------------
   * from https://www.unidata.ucar.edu/mailing_lists/archives/netcdfgroup/2008/msg00093.html
   *
   * Then comes the part you will have to do for your datasets. You open the data
   * dataset, get an ID, DID variable here, open the latitude dataset, get its ID,
   * DSID variable here, and "link" the 2 with this call
   *
   * if (H5DSattach_scale(did,dsid,DIM0) < 0)
   *
   * what this function does is to associated the dataset DSID (latitude) with the
   * dimension* specified by the parameter DIM0 (0, in this case, the first
   * dimension of the 2D array) of the dataset DID
   *
   * If you open HDF Explorer and expand the attributes of the "data" dataset you
   * will see an attribute called DIMENSION_LIST.
   * This is done by this function. It is an array that contains 2 HDF5 references,
   * one for the latitude dataset, other for the longitude)
   *
   * If you expand the "lat" dataset , you will see that it contains an attribute
   * called REFERENCE_LIST. It is a compound type that contains
   * 1) a reference to my "data" dataset
   * 2) the index of the data dataset this scale is to be associated with (0 for the lat, 1 for the lon)
   */
    // find the Dimension Scale objects, turn them into shared dimensions
    // always has attribute CLASS = "DIMENSION_SCALE"
    // note that we dont bother looking at their REFERENCE_LIST
    @Throws(IOException::class)
    private fun findDimensionScales(g: Group.Builder, h5group: H5Group?, facade: DataObjectFacade) {
        val iter: MutableIterator<MessageAttribute> = facade.dobj.attributes.iterator()
        while (iter.hasNext()) {
            val matt: MessageAttribute = iter.next()
            if (matt.name.equals(H5builder.Companion.HDF5_CLASS)) {
                val att: Attribute? = makeAttribute(matt)
                if (att == null || !att.isString()) {
                    throw IllegalStateException()
                }
                val `val`: String = att.getStringValue()
                if ((`val` == H5builder.Companion.HDF5_DIMENSION_SCALE) && facade.dobj.mds.ndims > 0) {
                    // create a dimension - always use the first dataspace length
                    facade.dimList = addDimension(
                        g,
                        h5group,
                        facade.name,
                        facade.dobj.mds.dimLength.get(0),
                        facade.dobj.mds.maxLength.get(0) === -1
                    )
                    facade.hasNetcdfDimensions = true
                    if (!h5iosp.includeOriginalAttributes) {
                        iter.remove()
                    }
                    if (facade.dobj.mds.ndims > 1) {
                        facade.is2DCoordinate = true
                    }
                }
            }
        }
    }

    private fun findDimensionScales2D(h5group: H5Group?, facade: DataObjectFacade) {
        val lens: IntArray = facade.dobj.mds.dimLength
        if (lens.size > 2) {
            H5builder.Companion.log.warn("DIMENSION_LIST: dimension scale > 2 = {}", facade.getName())
            return
        }

        // first dimension is itself
        val name: String = facade.getName()
        val pos = name.lastIndexOf('/')
        val dimName = if ((pos >= 0)) name.substring(pos + 1) else name
        val sbuff = StringBuilder()
        sbuff.append(dimName)
        sbuff.append(" ")

        // second dimension is really an anonymous dimension, ironically now we go through amazing hoops to keep it shared
        // 1. use dimids if they exist
        // 2. if length matches and unique, use it
        // 3. if no length matches or multiple matches, then use anonymous
        val want_len = lens[1] // second dimension
        var match: Dimension? = null
        var unique = true
        for (d: Dimension in h5group.dimList) {
            if (d.getLength() === want_len) {
                if (match == null) {
                    match = d
                } else {
                    unique = false
                }
            }
        }
        if (match != null && unique) {
            sbuff.append(match.getShortName()) // 2. if length matches and unique, use it
        } else {
            if (match == null) { // 3. if no length matches or multiple matches, then use anonymous
                H5builder.Companion.log.warn(
                    "DIMENSION_LIST: dimension scale {} has second dimension {} but no match",
                    facade.getName(),
                    want_len
                )
                sbuff.append(want_len)
            } else {
                H5builder.Companion.log.warn(
                    "DIMENSION_LIST: dimension scale {} has second dimension {} but multiple matches", facade.getName(),
                    want_len
                )
                sbuff.append(want_len)
            }
        }
        facade.dimList = sbuff.toString()
    }

    /*
   * private void findNetcdf4DimidAttribute(DataObjectFacade facade) throws IOException {
   * for (MessageAttribute matt : facade.dobj.attributes) {
   * if (matt.name.equals(Nc4.NETCDF4_DIMID)) {
   * if (dimIds == null) dimIds = new HashMap<Integer, DataObjectFacade>();
   * Attribute att_dimid = makeAttribute(matt);
   * Integer dimid = (Integer) att_dimid.getNumericValue();
   * dimIds.put(dimid, facade);
   * return;
   * }
   * }
   * if (dimIds != null) // supposed to all have them
   * log.warn("Missing "+Nc4.NETCDF4_DIMID+" attribute on "+facade.getName());
   * }
   */
    /*
   * the case of multidimensional dimension scale. We need to identify which index to use as the dimension length.
   * the pattern is, eg:
   * _Netcdf4Coordinates = 6, 4
   * _Netcdf4Dimid = 6
   *
   * private int findCoordinateDimensionIndex(DataObjectFacade facade, H5Group h5group) throws IOException {
   * Attribute att_coord = null;
   * Attribute att_dimid = null;
   * for (MessageAttribute matt : facade.dobj.attributes) {
   * if (matt.name.equals(Nc4.NETCDF4_COORDINATES))
   * att_coord = makeAttribute(matt);
   * if (matt.name.equals(Nc4.NETCDF4_DIMID))
   * att_dimid = makeAttribute(matt);
   * }
   * if (att_coord != null && att_dimid != null) {
   * facade.netcdf4CoordinatesAtt = att_coord;
   * Integer want = (Integer) att_dimid.getNumericValue();
   * for (int i=0; i<att_coord.getLength(); i++) {
   * Integer got = (Integer) att_dimid.getNumericValue(i);
   * if (want.equals(got))
   * return i;
   * }
   * log.warn("Multidimension dimension scale attributes "+Nc4.NETCDF4_COORDINATES+" and "+Nc4.
   * NETCDF4_DIMID+" dont match. Assume Dimension is index 0 (!)");
   * return 0;
   * }
   * if (att_coord != null) {
   * facade.netcdf4CoordinatesAtt = att_coord;
   * int n = h5group.dimList.size(); // how many dimensions are already defined
   * facade.dimList = "%REDO%"; // token to create list when all dimensions found
   * for (int i=0 ;i<att_coord.getLength(); i++) {
   * if (att_coord.getNumericValue(i).intValue() == n) return i;
   * }
   * log.warn("Multidimension dimension scale attribute "+Nc4.
   * NETCDF4_DIMID+" missing. Dimension ordering is not found. Assume index 0 (!)");
   * return 0;
   * }
   *
   * log.warn("Multidimension dimension scale doesnt have "+Nc4.
   * NETCDF4_COORDINATES+" attribute. Assume Dimension is index 0 (!)");
   * return 0;
   * }
   */
    // look for references to dimension scales, ie the variables that use them
    // return true if this variable is compatible with netcdf4 data model
    @Throws(IOException::class)
    private fun findSharedDimensions(g: Group.Builder, h5group: H5Group?, facade: DataObjectFacade): Boolean {
        val iter: MutableIterator<MessageAttribute> = facade.dobj.attributes.iterator()
        while (iter.hasNext()) {
            val matt: MessageAttribute = iter.next()
            when (matt.name) {
                H5builder.Companion.HDF5_DIMENSION_LIST -> {
                    // references : may extend the dimension length
                    val att: Attribute? = makeAttribute(matt) // this reads in the data
                    if (att == null) {
                        H5builder.Companion.log.warn("DIMENSION_LIST: failed to read on variable {}", facade.getName())
                    } else if (att.getLength() !== facade.dobj.mds.dimLength.length) { // some attempts to writing hdf5 directly
                        // fail here
                        H5builder.Companion.log.warn(
                            "DIMENSION_LIST: must have same number of dimension scales as dimensions att={} on variable {}",
                            att, facade.getName()
                        )
                    } else {
                        val sbuff = StringBuilder()
                        var i = 0
                        while (i < att.getLength()) {
                            val name: String = att.getStringValue(i)
                            val dimName = extendDimension(g, h5group, name, facade.dobj.mds.dimLength.get(i))
                            sbuff.append(dimName).append(" ")
                            i++
                        }
                        facade.dimList = sbuff.toString()
                        facade.hasNetcdfDimensions = true
                        if (H5builder.Companion.debugDimensionScales) {
                            println(
                                "Found dimList '{}' for group '{}' matt={}",
                                facade.dimList,
                                g.shortName,
                                matt
                            )
                        }
                        if (!h5iosp.includeOriginalAttributes) iter.remove()
                    }
                }

                H5builder.Companion.HDF5_DIMENSION_NAME -> {
                    val att: Attribute? = makeAttribute(matt) ?: throw IllegalStateException()
                    val `val`: String = att.getStringValue()
                    if (`val`.startsWith("This is a netCDF dimension but not a netCDF variable")) {
                        facade.isVariable = false
                        isNetcdf4 = true
                    }
                    if (!h5iosp.includeOriginalAttributes) iter.remove()
                    if (H5builder.Companion.debugDimensionScales) {
                        println("Found {}", `val`)
                    }
                }

                H5builder.Companion.HDF5_REFERENCE_LIST -> if (!h5iosp.includeOriginalAttributes) iter.remove()
            }
        }
        return facade.hasNetcdfDimensions || facade.dobj.mds.dimLength.length === 0
    }

    // add a dimension, return its name
    private fun addDimension(
        parent: Group.Builder,
        h5group: H5Group,
        name: String,
        length: Int,
        isUnlimited: Boolean
    ): String {
        val pos = name.lastIndexOf('/')
        val dimName = if ((pos >= 0)) name.substring(pos + 1) else name
        var d: Dimension = h5group.dimMap.get(dimName) // first look in current group
        if (d == null) { // create if not found
            d = Dimension.builder().setName(name).setIsUnlimited(isUnlimited).setLength(length).build()
            h5group.dimMap.put(dimName, d)
            h5group.dimList.add(d)
            parent.addDimension(d)
            if (H5builder.Companion.debugDimensionScales) {
                println("addDimension name=" + name + " dim= " + d + " to group " + parent.shortName)
            }
        } else { // check has correct length
            if (d.getLength() !== length) {
                throw IllegalStateException(
                    "addDimension: DimScale has different length than dimension it references dimScale=$dimName"
                )
            }
        }
        return d.getShortName()
    }

    // look for unlimited dimensions without dimension scale - must get length from the variable
    private fun extendDimension(parent: Group.Builder, h5group: H5Group, name: String, length: Int): String {
        val pos = name.lastIndexOf('/')
        val dimName = if ((pos >= 0)) name.substring(pos + 1) else name
        var d: Dimension = h5group.dimMap.get(dimName) // first look in current group
        if (d == null) {
            d = parent.findDimension(dimName).orElse(null) // then look in parent groups
        }
        if (d != null) {
            if (d.isUnlimited() && (length > d.getLength())) {
                parent.replaceDimension(d.toBuilder().setLength(length).build())
            }
            if (!d.isUnlimited() && (length != d.getLength())) {
                throw IllegalStateException(
                    "extendDimension: DimScale has different length than dimension it references dimScale=$dimName"
                )
            }
            return d.getShortName()
        }
        return dimName
    }

    private fun createDimensions(g: Group.Builder, h5group: H5Group?) {
        for (d: Dimension in h5group.dimList) {
            g.addDimensionIfNotExists(d)
        }
    }

    private fun filterAttributes(attList: List<MessageAttribute>): List<MessageAttribute> {
        val result: MutableList<MessageAttribute> = ArrayList<MessageAttribute>(attList.size)
        for (matt: MessageAttribute in attList) {
            if ((matt.name.equals(NetcdfFormatUtils.NETCDF4_COORDINATES) || matt.name.equals(NetcdfFormatUtils.NETCDF4_DIMID)
                        || matt.name.equals(NetcdfFormatUtils.NETCDF4_STRICT))
            ) {
                isNetcdf4 = true
            } else {
                result.add(matt)
            }
        }
        return result
    }

    /**
     * Create Attribute objects from the MessageAttribute and add to list
     *
     * @param sb if attribute for a Structure, then deconstruct and add to member variables
     * @param matt attribute message
     * @param attContainer add Attribute to this
     * @throws IOException on io error
     * @throws InvalidRangeException on shape error
     */
    @Throws(IOException::class, InvalidRangeException::class)
    private fun makeAttributes(
        sb: Structure.Builder<*>?,
        matt: MessageAttribute,
        attContainer: AttributeContainerMutable
    ) {
        val mdt: MessageDatatype = matt.mdt
        if (mdt.type === 6) { // structure
            val vinfo: H5builder.Vinfo = H5builder.Vinfo(matt.mdt, matt.mds, matt.dataPos)
            val attData: StructureDataArray = readAttributeStructureData(matt, vinfo)
            if (null == sb) {
                // flatten and add to list
                for (sm: StructureMembers.Member in attData.getStructureMembers().getMembers()) {
                    val memberData: Array<*> = attData.extractMemberArray(sm)
                    attContainer.addAttribute(Attribute.fromArray(matt.name + "." + sm.getName(), memberData))
                }
            } else if (matt.name.equals(CDM.FIELD_ATTS)) {
                // flatten and add to list
                for (sm: StructureMembers.Member in attData.getStructureMembers().getMembers()) {
                    val memberName: String = sm.getName()
                    val pos = memberName.indexOf(":")
                    if (pos < 0) {
                        continue
                    }
                    val fldName = memberName.substring(0, pos)
                    val attName = memberName.substring(pos + 1)
                    val memberData: Array<*> = attData.extractMemberArray(sm)
                    sb.findMemberVariable(fldName)
                        .ifPresent { vb ->
                            vb.getAttributeContainer().addAttribute(Attribute.fromArray(attName, memberData))
                        }
                }
            } else { // assign separate attribute for each member
                val attMembers: StructureMembers = attData.getStructureMembers()
                for (v: Variable.Builder<*> in sb.vbuilders) {
                    // does the compound attribute have a member with same name as nested variable ?
                    val sm: StructureMembers.Member = attMembers.findMember(v.shortName)
                    if (null != sm) {
                        // if so, add the att to the member variable, using the name of the compound attribute
                        val memberData: Array<*> = attData.extractMemberArray(sm)
                        v.addAttribute(Attribute.fromArray(matt.name, memberData)) // TODO check for missing values
                    }
                }

                // look for unassigned members, add to the list
                for (sm: StructureMembers.Member in attData.getStructureMembers().getMembers()) {
                    val vb: Variable.Builder<*> = sb.findMemberVariable(sm.getName()).orElse(null)
                    if (vb == null) {
                        val memberData: Array<*> = attData.extractMemberArray(sm)
                        attContainer.addAttribute(Attribute.fromArray(matt.name + "." + sm.getName(), memberData))
                    }
                }
            }
        } else {
            // make a single attribute
            val att: Attribute? = makeAttribute(matt)
            if (att != null) attContainer.addAttribute(att)
        }

        // reading attribute values might change byte order during a read
        // put back to little endian for further header processing
        raf.order(OpenFile.LITTLE_ENDIAN)
    }

    @Throws(IOException::class)
    private fun makeAttribute(matt: MessageAttribute): Attribute? {
        val vinfo: H5builder.Vinfo = H5builder.Vinfo(matt.mdt, matt.mds, matt.dataPos)
        val dtype: ArrayType = vinfo.getNCArrayType()

        // check for empty attribute case
        if (matt.mds.type === 2) {
            return if (dtype === ArrayType.CHAR) {
                // empty char considered to be a null string attr
                Attribute.builder(matt.name).setArrayType(ArrayType.STRING).build()
            } else {
                Attribute.builder(matt.name).setArrayType(dtype).build()
            }
        }
        val attData: Array<*>
        try {
            attData = readAttributeData(matt, vinfo, dtype)
        } catch (e: InvalidRangeException) {
            H5builder.Companion.log.warn(("failed to read Attribute " + matt.name).toString() + " HDF5 file=" + raf.getLocation())
            return null
        }
        val result: Attribute
        if (attData.isVlen()) {
            val dataList: MutableList<Any> = ArrayList()
            for (`val`: Any in attData) {
                val nestedArray = `val` as Array<*>
                for (nested: Any in nestedArray) {
                    dataList.add(nested)
                }
            }
            // TODO probably wrong? flattening them out ??
            result = Attribute.builder(matt.name).setValues(dataList, matt.mdt.unsigned).build()
        } else {
            result = Attribute.fromArray(matt.name, attData)
        }
        raf.order(OpenFile.LITTLE_ENDIAN)
        return result
    }

    // read non-Structure attribute values without creating a Variable
    @Throws(IOException::class, InvalidRangeException::class)
    private fun readAttributeData(matt: MessageAttribute, vinfo: H5builder.Vinfo, dataType: ArrayType): Array<*> {
        var shape: IntArray = matt.mds.dimLength
        val layout2: Layout = LayoutRegular(matt.dataPos, matt.mdt.byteSize, shape, Section(shape))

        // Strings
        if ((vinfo.typeInfo.hdfType === 9) && (vinfo.typeInfo.isVString)) {
            val size = layout2.getTotalNelems() as Int
            val sarray = arrayOfNulls<String>(size)
            var count = 0
            while (layout2.hasNext()) {
                val chunk: Layout.Chunk = layout2.next() ?: continue
                for (i in 0 until chunk.getNelems()) {
                    val address: Long = chunk.getSrcPos() + layout2.getElemSize() * i
                    val sval = readHeapString(address)
                    sarray[count++] = sval
                }
            }
            return Arrays.factory(ArrayType.STRING, intArrayOf(size), sarray)
        } // vlen Strings case

        // Vlen (non-String)
        if (vinfo.typeInfo.hdfType === 9) {
            var endian: ByteOrder? = vinfo.typeInfo.endian
            var readType: ArrayType = dataType
            if (vinfo.typeInfo.base.hdfType === 7) { // reference
                readType = ArrayType.LONG
                endian = ByteOrder.LITTLE_ENDIAN // apparently always LE
            }

            // variable length array of references, get translated into strings
            if (vinfo.typeInfo.base.hdfType === 7) {
                val refsList: MutableList<String> = ArrayList()
                while (layout2.hasNext()) {
                    val chunk: Layout.Chunk = layout2.next() ?: continue
                    for (i in 0 until chunk.getNelems()) {
                        val address: Long = chunk.getSrcPos() + layout2.getElemSize() * i
                        val vlenArray: Array<*> = getHeapDataArray(address, readType, endian)
                        val refsArray: ucar.array.Array<String> =
                            h5iosp.convertReferenceArray(vlenArray as Array<Long?>)
                        for (s: String in refsArray) {
                            refsList.add(s)
                        }
                    }
                }
                return Arrays.factory(ArrayType.STRING, intArrayOf(refsList.size), refsList.toTypedArray())
            }

            // general case is to read an array of vlen objects
            // each vlen generates an Array - so return ArrayObject of Array
            val size = layout2.getTotalNelems() as Int
            val vlenStorage: StorageMutable<Array<String>> = ArrayVlen.createStorage(readType, size, null)
            var count = 0
            while (layout2.hasNext()) {
                val chunk: Layout.Chunk = layout2.next() ?: continue
                for (i in 0 until chunk.getNelems()) {
                    val address: Long = chunk.getSrcPos() + layout2.getElemSize() * i
                    val vlenArray: Array<*> = getHeapDataArray(address, readType, endian)
                    if (vinfo.typeInfo.base.hdfType === 7) {
                        vlenStorage.setPrimitiveArray(count, h5iosp.convertReferenceArray(vlenArray as Array<Long?>))
                    } else {
                        vlenStorage.setPrimitiveArray(count, vlenArray)
                    }
                    count++
                }
            }
            return ArrayVlen.createFromStorage(readType, shape, vlenStorage)
        } // vlen case

        // NON-STRUCTURE CASE
        var readDtype: ArrayType = dataType
        var elemSize: Int = dataType.getSize()
        var endian: ByteOrder = vinfo.typeInfo.endian
        if (vinfo.typeInfo.hdfType === 2) { // time
            readDtype = vinfo.mdt.timeType
            elemSize = readDtype.getSize()
        } else if (vinfo.typeInfo.hdfType === 3) { // char
            if (vinfo.mdt.byteSize > 1) {
                val newShape = IntArray(shape.size + 1)
                System.arraycopy(shape, 0, newShape, 0, shape.size)
                newShape[shape.size] = vinfo.mdt.byteSize
                shape = newShape
            }
        } else if (vinfo.typeInfo.hdfType === 5) { // opaque
            elemSize = vinfo.mdt.byteSize
        } else if (vinfo.typeInfo.hdfType === 8) { // enum
            val baseInfo: Hdf5Type = vinfo.typeInfo.base
            readDtype = baseInfo.dataType
            elemSize = readDtype.getSize()
            endian = baseInfo.endian
        }
        val layout: Layout = LayoutRegular(matt.dataPos, elemSize, shape, Section(shape))

        // So an attribute cant be a Structure ??
        val pdata: Any = h5iosp.readArrayOrPrimitive(vinfo, null, layout, dataType, shape, null, endian)
        var dataArray: Array<*>
        if (dataType === ArrayType.OPAQUE) {
            dataArray = pdata as Array<*>
        } else if ((dataType === ArrayType.CHAR)) {
            if (vinfo.mdt.byteSize > 1) { // chop back into pieces
                val bdata = pdata as ByteArray
                val strlen: Int = vinfo.mdt.byteSize
                val n = bdata.size / strlen
                val sarray = arrayOfNulls<String>(n)
                for (i in 0 until n) {
                    val sval = convertString(bdata, i * strlen, strlen)
                    sarray[i] = sval
                }
                dataArray = Arrays.factory(ArrayType.STRING, intArrayOf(n), sarray)
            } else {
                val sval = convertString(pdata as ByteArray)
                dataArray = Arrays.factory(ArrayType.STRING, intArrayOf(1), arrayOf(sval))
            }
        } else {
            dataArray = if ((pdata is Array)) pdata else Arrays.factory(readDtype, shape, pdata)
        }

        // convert attributes to enum strings
        if ((vinfo.typeInfo.hdfType === 8) && (matt.mdt.map != null)) {
            dataArray = convertEnums(matt.mdt.map, dataType, dataArray as Array<Number>)
        }
        return dataArray
    }

    // read attribute values without creating a Variable
    @Throws(IOException::class, InvalidRangeException::class)
    private fun readAttributeStructureData(matt: MessageAttribute, vinfo: H5builder.Vinfo): StructureDataArray {
        val shape: IntArray = matt.mds.dimLength
        var hasStrings = false
        val builder: StructureMembers.Builder = StructureMembers.builder().setName(matt.name)
        for (h5sm: StructureMember in matt.mdt.members) {
            var dt: ArrayType
            var dim: IntArray
            when (h5sm.mdt.type) {
                9 -> {
                    dt = ArrayType.STRING
                    dim = intArrayOf(1)
                }

                10 -> {
                    dt = Hdf5Type(h5sm.mdt).dataType
                    dim = h5sm.mdt.dim
                }

                else -> {
                    dt = Hdf5Type(h5sm.mdt).dataType
                    dim = intArrayOf(1)
                }
            }
            val mb: StructureMembers.MemberBuilder = builder.addMember(h5sm.name, null, null, dt, dim)
            if (h5sm.mdt.endian != null) { // apparently each member may have separate byte order (!!!??)
                mb.setByteOrder(h5sm.mdt.endian)
            }
            mb.setOffset(h5sm.offset) // offset since start of Structure
            if (dt === ArrayType.STRING) {
                hasStrings = true
            }
        }
        val recsize: Int = matt.mdt.byteSize
        val layout: Layout = LayoutRegular(matt.dataPos, recsize, shape, Section(shape))
        builder.setStructureSize(recsize)
        val members: StructureMembers = builder.build()
        Preconditions.checkArgument(members.getStorageSizeBytes() === recsize)

        // copy data into an byte[] for efficiency
        val nrows = Arrays.computeSize(shape) as Int
        val result = ByteArray((nrows * members.getStorageSizeBytes()) as Int)
        while (layout.hasNext()) {
            val chunk: Layout.Chunk = layout.next() ?: continue
            if (H5builder.Companion.debugStructure) {
                println((" readStructure " + matt.name).toString() + " chunk= " + chunk + " index.getElemSize= " + layout.getElemSize())
            }
            raf.seek(chunk.getSrcPos())
            raf.readFully(result, chunk.getDestElem() as Int * recsize, chunk.getNelems() * recsize)
        }
        val bb = ByteBuffer.wrap(result)
        val storage = StructureDataStorageBB(members, bb, nrows)

        // strings are stored on the heap, and must be read separately
        if (hasStrings) {
            var destPos = 0
            for (i in 0 until layout.getTotalNelems()) { // loop over each structure
                h5iosp.convertHeapArray(bb, storage, destPos, members)
                destPos += layout.getElemSize()
            }
        }
        return StructureDataArray(members, shape, storage)
    } // readAttributeStructureData

    private fun convertString(b: ByteArray): String {
        // null terminates
        var count = 0
        while (count < b.size) {
            if (b[count].toInt() == 0) break
            count++
        }
        return kotlin.String(b, 0, count, valueCharset) // all strings are considered to be UTF-8 unicode
    }

    private fun convertString(b: ByteArray, start: Int, len: Int): String {
        // null terminates
        var count = start
        while (count < start + len) {
            if (b[count].toInt() == 0) break
            count++
        }
        return kotlin.String(b, start, count - start, valueCharset) // all strings are considered to be UTF-8
        // unicode
    }

    protected fun convertEnums(map: Map<Int?, String?>, dataType: ArrayType?, values: Array<Number>): Array<String> {
        val size = Arrays.computeSize(values.getShape()) as Int
        val sarray = arrayOfNulls<String>(size)
        var count = 0
        for (`val`: Number in values) {
            val ival = `val`.toInt()
            var sval = map[ival]
            if (sval == null) {
                sval = "Unknown enum value=$ival"
            }
            sarray[count++] = sval
        }
        return Arrays.factory(ArrayType.STRING, values.getShape(), sarray)
    }

    @Throws(IOException::class)
    private fun makeVariable(parentGroup: Group.Builder, facade: DataObjectFacade): Variable.Builder<*>? {
        val vinfo: H5builder.Vinfo = H5builder.Vinfo(facade)
        if (vinfo.getNCArrayType() == null) {
            println(("SKIPPING ArrayType= " + vinfo.typeInfo.hdfType).toString() + " for variable " + facade.name)
            return null
        }

        // deal with filters, cant do SZIP
        if (facade.dobj.mfp != null) {
            for (f: Filter in facade.dobj.mfp.filters) {
                if (f.id === 4) {
                    println(("SKIPPING variable with SZIP Filter= " + facade.dobj.mfp).toString() + " for variable " + facade.name)
                    return null
                }
            }
        }
        var fillAttribute: Attribute? = null
        for (mess: HeaderMessage in facade.dobj.messages) {
            if (mess.mtype === MessageType.FillValue) {
                val fvm: MessageFillValue = mess.messData as MessageFillValue
                if (fvm.hasFillValue) {
                    vinfo.fillValue = fvm.value
                }
            } else if (mess.mtype === MessageType.FillValueOld) {
                val fvm: MessageFillValueOld = mess.messData as MessageFillValueOld
                if (fvm.size > 0) {
                    vinfo.fillValue = fvm.value
                }
            }
            val fillValue: Any = vinfo.getFillValueNonDefault()
            if (fillValue != null) {
                val defFillValue: Any = NetcdfFormatUtils.getFillValueDefault(vinfo.typeInfo.dataType)
                if (fillValue != defFillValue) fillAttribute =
                    Attribute.builder(CDM.FILL_VALUE).setNumericValue(fillValue as Number?, vinfo.typeInfo.unsigned)
                        .build()
            }
        }
        val dataAddress: Long = facade.dobj.msl.dataAddress

        // deal with unallocated data
        if (dataAddress == -1L) {
            vinfo.useFillValue = true

            // if didnt find, use zeroes !!
            if (vinfo.fillValue == null) {
                vinfo.fillValue = ByteArray(vinfo.typeInfo.dataType.getSize())
            }
        }
        val vb: Variable.Builder<*>?
        var sb: Structure.Builder<*>? = null
        if (facade.dobj.mdt.type === 6) { // Compound
            val vname: String = facade.name
            sb = Structure.builder().setName(vname)
            vb = sb
            vb.setParentGroupBuilder(parentGroup)
            if (!makeVariableShapeAndType(parentGroup, sb, facade.dobj.mdt, facade.dobj.mds, vinfo, facade.dimList)) {
                return null
            }
            addMembersToStructure(parentGroup, sb, facade.dobj.mdt)
            vinfo.setElementSize(facade.dobj.mdt.byteSize)
        } else {
            var vname: String = facade.name
            if (vname.startsWith(NetcdfFormatUtils.NETCDF4_NON_COORD)) {
                vname = vname.substring(NetcdfFormatUtils.NETCDF4_NON_COORD.length()) // skip prefix
            }
            vb = Variable.builder().setName(vname)
            vb.setParentGroupBuilder(parentGroup)
            if (!makeVariableShapeAndType(parentGroup, vb, facade.dobj.mdt, facade.dobj.mds, vinfo, facade.dimList)) {
                return null
            }

            // special case of variable length strings
            if (vb.dataType === ArrayType.STRING) {
                vinfo.setElementSize(sizeHeapId) // because the array has elements that are HeapIdentifier
            } else if (vb.dataType === ArrayType.OPAQUE) { // special case of opaque
                vinfo.setElementSize(facade.dobj.mdt.getBaseSize())
            }
        }
        vb.setSPobject(vinfo)

        // look for attributes
        val fatts: List<MessageAttribute> = filterAttributes(facade.dobj.attributes)
        for (matt: MessageAttribute in fatts) {
            try {
                makeAttributes(sb, matt, vb.getAttributeContainer())
            } catch (e: InvalidRangeException) {
                throw IOException(e.getMessage())
            }
        }
        val atts: AttributeContainerMutable = vb.getAttributeContainer()
        processSystemAttributes(facade.dobj.messages, atts)
        if (fillAttribute != null && atts.findAttribute(CDM.FILL_VALUE) == null) vb.addAttribute(fillAttribute)
        // if (vinfo.typeInfo.unsigned)
        // v.addAttribute(new Attribute(CDM.UNSIGNED, "true"));
        if (facade.dobj.mdt.type === 5) {
            val desc: String = facade.dobj.mdt.opaque_desc
            if ((desc != null) && (!desc.isEmpty())) vb.addAttribute(Attribute("_opaqueDesc", desc))
        }
        val shape = makeVariableShape(facade.dobj.mdt, facade.dobj.mds, facade.dimList)
        if (vinfo.isChunked) { // make the data btree, but entries are not read in
            vinfo.btree = DataBTree(this, dataAddress, shape, vinfo.storageSize, memTracker)
            if (vinfo.isChunked) { // add an attribute describing the chunk size
                val chunksize: MutableList<Int> = ArrayList()
                for (i in 0 until (vinfo.storageSize.size - 1))  // skip last one - its the element size
                    chunksize.add(vinfo.storageSize.get(i))
                vb.addAttribute(Attribute.builder(CDM.CHUNK_SIZES).setValues(chunksize, true).build())
            }
        }
        if (H5builder.Companion.transformReference && (facade.dobj.mdt.type === 7) && (facade.dobj.mdt.referenceType === 0)) { // object reference
            // System.out.printf("new transform object Reference: facade= %s variable name=%s%n", facade.name, vb.shortName);
            vb.setArrayType(ArrayType.STRING)
            val rawData = vinfo.readArray() as Array<Long>
            val refData = findReferenceObjectNames(rawData)
            vb.setSourceData(refData) // so H5iosp.read() is never called
            vb.addAttribute(Attribute("_HDF5ReferenceType", "values are names of referenced Variables"))
        }
        if (H5builder.Companion.transformReference && (facade.dobj.mdt.type === 7) && (facade.dobj.mdt.referenceType === 1)) { // region reference
            if (H5builder.Companion.warnings) H5builder.Companion.log.warn(("transform region Reference: facade=" + facade.name).toString() + " variable name=" + vb.shortName)

            /*
       * TODO doesnt work yet
       * int nelems = (int) vb.getSize();
       * int heapIdSize = 12;
       * for (int i = 0; i < nelems; i++) {
       * H5builder.RegionReference heapId = new RegionReference(vinfo.dataPos + heapIdSize * i);
       * }
       */

            // fake data for now
            vb.setArrayType(ArrayType.LONG)
            val newData: Array<*> = Arrays.factory(ArrayType.LONG, shape, LongArray(Arrays.computeSize(shape) as Int))
            vb.setSourceData(newData) // so H5iosp.read() is never called
            vb.addAttribute(Attribute("_HDF5ReferenceType", "values are regions of referenced Variables"))
        }

        // debugging
        vinfo.setOwner(vb)
        if ((vinfo.mfp != null) && H5builder.Companion.warnings) {
            for (f: Filter in vinfo.mfp.getFilters()) {
                if (f.filterType === H5objects.FilterType.unknown) {
                    H5builder.Companion.log.warn(("  Variable " + facade.name).toString() + " has unknown Filter(s) = " + vinfo.mfp)
                    break
                }
            }
        }
        if (debugStart) {
            println(("makeVariable " + vb.shortName).toString() + "; vinfo= " + vinfo)
        }
        return vb
    }

    // convert an array of longs which are data object references to an array of strings,
    // the names of the data objects (dobj.who)
    @Throws(IOException::class)
    private fun findReferenceObjectNames(data: Array<Long>): Array<String> {
        val size = Arrays.computeSize(data.getShape()) as Int
        val sarray = arrayOfNulls<String>(size)
        var count = 0
        for (`val`: Long in data) {
            val dobj: DataObject? = getDataObject(`val`, null)
            if (dobj == null) {
                H5builder.Companion.log.warn("readReferenceObjectNames cant find obj= {}", `val`)
            } else {
                if (H5builder.Companion.debugReference) {
                    println(" Referenced object= {}", dobj.who)
                }
                sarray[count] = dobj.who
            }
            count++
        }
        return Arrays.factory(ArrayType.STRING, data.getShape(), sarray)
    }

    @Throws(IOException::class)
    private fun addMembersToStructure(parent: Group.Builder, s: Structure.Builder<*>?, mdt: MessageDatatype) {
        for (m: StructureMember in mdt.members) {
            val v: Variable.Builder<*>? = makeVariableMember(parent, m.name, m.offset, m.mdt)
            if (v != null) {
                s.addMemberVariable(v)
                if (debugStart) {
                    println(("  made Member Variable " + v.shortName).toString() + "\n" + v)
                }
            }
        }
    }

    // Used for Structure Members
    @Throws(IOException::class)
    private fun makeVariableMember(
        parentGroup: Group.Builder, name: String, dataPos: Long,
        mdt: MessageDatatype
    ): Variable.Builder<*>? {
        val vinfo: H5builder.Vinfo = H5builder.Vinfo(mdt, null, dataPos) // TODO need mds
        if (vinfo.getNCArrayType() == null) {
            println(("SKIPPING ArrayType= " + vinfo.typeInfo.hdfType).toString() + " for variable " + name)
            return null
        }
        if (mdt.type === 6) {
            val sb: Structure.Builder<*> = Structure.builder().setName(name).setParentGroupBuilder(parentGroup)
            makeVariableShapeAndType(parentGroup, sb, mdt, null, vinfo, null)
            addMembersToStructure(parentGroup, sb, mdt)
            vinfo.setElementSize(mdt.byteSize)
            sb.setSPobject(vinfo)
            vinfo.setOwner(sb)
            return sb
        } else {
            val vb: Variable.Builder<*> = Variable.builder().setName(name).setParentGroupBuilder(parentGroup)
            makeVariableShapeAndType(parentGroup, vb, mdt, null, vinfo, null)

            // special case of variable length strings
            if (vb.dataType === ArrayType.STRING) {
                vinfo.setElementSize(sizeHeapId) // because the array has elements that are HeapIdentifier
            } else if (vb.dataType === ArrayType.OPAQUE) { // special case of opaque
                vinfo.setElementSize(mdt.getBaseSize())
            }
            vb.setSPobject(vinfo)
            vinfo.setOwner(vb)
            return vb
        }
    }

    private fun processSystemAttributes(messages: List<HeaderMessage>, attContainer: AttributeContainerMutable) {
        for (mess: HeaderMessage in messages) {
            if (mess.mtype === MessageType.Comment) {
                val m: MessageComment = mess.messData as MessageComment
                attContainer.addAttribute(Attribute("_comment", m.comment))
            }
        }
    }

    private val hdfDateFormatter: SimpleDateFormat
        private get() {
            if (hdfDateParser == null) {
                hdfDateParser = SimpleDateFormat("yyyyMMddHHmmss")
                hdfDateParser!!.timeZone = TimeZone.getTimeZone("GMT") // same as UTC
            }
            return hdfDateParser!!
        }

    // get the shape of the Variable
    private fun makeVariableShape(mdt: MessageDatatype, msd: MessageDataspace?, dimNames: String?): IntArray? {
        var shape: IntArray? = if ((msd != null)) msd.dimLength else IntArray(0)
        if (shape == null) {
            shape = IntArray(0) // scaler
        }

        // merge the shape for array type (10)
        if (mdt.type === 10) {
            var len: Int = shape.size + mdt.dim.length
            if (mdt.isVlen()) {
                len++
            }
            val combinedDim = IntArray(len)
            System.arraycopy(shape, 0, combinedDim, 0, shape.size)
            System.arraycopy(mdt.dim, 0, combinedDim, shape.size, mdt.dim.length) // // type 10 is the inner dimensions
            if (mdt.isVlen()) {
                combinedDim[len - 1] = -1
            }
            shape = combinedDim
        }

        // dimension names were not passed in
        if (dimNames == null) {
            if (mdt.type === 3) { // fixed length string - ArrayType.CHAR, add string length
                if (mdt.byteSize !== 1) { // scalar string member variable
                    val rshape = IntArray(shape.size + 1)
                    System.arraycopy(shape, 0, rshape, 0, shape.size)
                    rshape[shape.size] = mdt.byteSize
                    return rshape
                }
            } else if (mdt.isVlen()) { // variable length (not a string)
                if ((shape.size == 1) && (shape.get(0) == 1)) { // replace scalar with vlen
                    return intArrayOf(-1)
                } else if (mdt.type !== 10) { // add vlen dimension already done above for array
                    val rshape = IntArray(shape.size + 1)
                    System.arraycopy(shape, 0, rshape, 0, shape.size)
                    rshape[shape.size] = -1
                    return rshape
                }
            }
        }
        return shape
    }

    // set the type and shape of the Variable
    private fun makeVariableShapeAndType(
        parent: Group.Builder, v: Variable.Builder<*>?, mdt: MessageDatatype,
        msd: MessageDataspace?, vinfo: H5builder.Vinfo, dimNames: String?
    ): Boolean {
        val shape = makeVariableShape(mdt, msd, dimNames)

        // set dimensions on the variable
        if (dimNames != null) { // dimensions were passed in
            if ((mdt.type === 9) && !mdt.isVString) {
                v.setDimensionsByName("$dimNames *")
            } else {
                v.setDimensionsByName(dimNames)
            }
        } else {
            v.setDimensionsAnonymous(shape)
        }

        // set the type
        val dt: ArrayType = vinfo.getNCArrayType() ?: return false
        v.setArrayType(dt)

        // set the enumTypedef
        if (dt.isEnum()) {
            // TODO Not sure why, but there may be both a user type and a "local" mdt enum. May need to do a value match?
            var enumTypedef: EnumTypedef = parent.findEnumeration(mdt.enumTypeName).orElse(null)
            if (enumTypedef == null) { // if shared object, wont have a name, shared version gets added later
                val local = EnumTypedef(mdt.enumTypeName, mdt.map)
                enumTypedef =
                    parent.enumTypedefs.stream().filter { e -> e.equalsMapOnly(local) }.findFirst().orElse(local)
                parent.addEnumTypedef(enumTypedef)
            }
            v.setEnumTypeName(enumTypedef.getShortName())
        }
        return true
    }

    val rootGroup: Builder
        get() = root

    fun makeVinfoForDimensionMapVariable(parent: Builder?, v: Variable.Builder<*>) {
        // this is a self contained variable, doesnt need any extra info, just make a dummy.
        val vinfo: H5builder.Vinfo = H5builder.Vinfo() // TODO noop
        vinfo.owner = v
    }

    @Throws(IOException::class)
    fun readStructMetadata(structMetadataVar: Variable.Builder<*>): String {
        val vinfo: H5builder.Vinfo = structMetadataVar.spiObject as H5builder.Vinfo
        return vinfo.readString()
    }

    // Holder of all H5 specific information for a Variable, needed to do IO.
    inner class Vinfo {
        var owner: Variable.Builder<*>? = null // debugging
        var facade: DataObjectFacade? = null // debugging
        var dataPos: Long = 0 // for regular variables, needs to be absolute, with baseAddress added if needed

        // for member variables, is the offset from start of structure
        var typeInfo: Hdf5Type? = null
        var elementSize = 0 // total length in bytes on disk of one element
        var chunking // for type 1 (continuous) : mds.dimLength;
                : IntArray

        // for type 2 (chunked) : msl.chunkSize (last number is element size)
        // null for attributes
        var isvlen = false // VLEN, but not vlenstring

        // chunked stuff
        var isChunked = false
        var btree: DataBTree? = null // only if isChunked
        var mdt: MessageDatatype? = null
        var mds: MessageDataspace? = null
        var mfp: MessageFilter? = null
        var useFillValue = false
        var fillValue: ByteArray?
        val compression: String?
            get() {
                if (mfp == null) return null
                val f = Formatter()
                for (filt: Filter in mfp.filters) {
                    f.format("%s ", filt.name)
                }
                return f.toString()
            }

        fun useFillValue(): Boolean {
            return useFillValue
        }

        @Throws(IOException::class)
        fun countStorageSize(f: Formatter?): LongArray {
            val result = LongArray(2)
            if (btree == null) {
                f?.format("btree is null%n")
                return result
            }
            if (useFillValue) {
                f?.format("useFillValue - no data is stored%n")
                return result
            }
            var count = 0
            var total: Long = 0
            val iter: DataBTree.DataChunkIterator = btree.getDataChunkIteratorFilter(null)
            while (iter.hasNext()) {
                val dc: DataBTree.DataChunk = iter.next()
                f?.format(" %s%n", dc)
                total += dc.size
                count++
            }
            result[0] = total
            result[1] = count.toLong()
            return result
        }

        internal constructor()

        /**
         * Constructor
         *
         * @param facade DataObjectFacade: always has an mdt and an msl
         */
        internal constructor(facade: DataObjectFacade) {
            this.facade = facade
            // TODO if compact, do not use fileOffset
            dataPos =
                if ((facade.dobj.msl.type === 0)) facade.dobj.msl.dataAddress else getFileOffset(facade.dobj.msl.dataAddress)
            mdt = facade.dobj.mdt
            mds = facade.dobj.mds
            mfp = facade.dobj.mfp
            isvlen = mdt.isVlen()
            if (!facade.dobj.mdt.isOK && H5builder.Companion.warnings) {
                println(("WARNING HDF5 file " + raf.getLocation()).toString() + " not handling " + facade.dobj.mdt)
                return  // not a supported datatype
            }
            isChunked = (facade.dobj.msl.type === 2)
            if (isChunked) {
                chunking = facade.dobj.msl.chunkSize
            } else {
                chunking = facade.dobj.mds.dimLength
            }

            // figure out the data type
            typeInfo = Hdf5Type(facade.dobj.mdt)
            val nelems = Arrays.computeSize(facade.dobj.mdt.dim) as Int
            setElementSize(facade.dobj.mdt.byteSize / nelems)
        }

        /**
         * Constructor, used for reading attributes
         *
         * @param mdt datatype
         * @param mds dataspace
         * @param dataPos start of data in file
         */
        internal constructor(mdt: MessageDatatype, mds: MessageDataspace?, dataPos: Long) {
            this.mdt = mdt
            this.mds = mds
            this.dataPos = dataPos
            if (!mdt.isOK && H5builder.Companion.warnings) {
                println(("WARNING HDF5 file " + raf.getLocation()).toString() + " not handling " + mdt)
                return  // not a supported datatype
            }
            isvlen = this.mdt.isVlen()

            // figure out the data type
            typeInfo = Hdf5Type(mdt)
            val nelems = Arrays.computeSize(mdt.dim) as Int
            setElementSize(mdt.byteSize / nelems)
        }

        fun setOwner(owner: Variable.Builder<*>) {
            this.owner = owner
            if (btree != null) {
                btree.setOwner(owner)
            }
        }

        fun setElementSize(elementSize: Int) {
            this.elementSize = elementSize
        }

        override fun toString(): String {
            val buff = Formatter()
            buff.format("dataPos= %d datatype= %s elementSize = %d", dataPos, typeInfo, elementSize)
            if (isChunked) {
                buff.format(" isChunked (%s)", Arrays.toString(chunking))
            }
            if (mfp != null) {
                buff.format(" hasFilter")
            }
            buff.format("; // %s", extraInfo())
            if (null != facade) {
                buff.format("%n %s", facade)
            }
            return buff.toString()
        }

        fun extraInfo(): String {
            val buff = Formatter()
            if ((typeInfo.dataType !== ArrayType.CHAR) && (typeInfo.dataType !== ArrayType.STRING)) buff.format(if (typeInfo.unsigned) " unsigned" else " signed")
            buff.format("ByteOrder= %s", typeInfo.endian)
            if (useFillValue) {
                buff.format(" useFillValue")
            }
            return buff.toString()
        }

        val nCArrayType: ArrayType
            get() = typeInfo.dataType

        /**
         * Get the Fill Value, return default if one was not set.
         *
         * @return wrapped primitive (Byte, Short, Integer, Double, Float, Long), or null if none
         */
        fun getFillValue(): Any {
            return (if ((fillValue == null)) NetcdfFormatUtils.getFillValueDefault(typeInfo.dataType) else fillValueNonDefault)!!
        }

        val fillValueNonDefault: Any?
            get() {
                if (fillValue == null) return null
                if ((typeInfo.dataType.getPrimitiveClass() === Byte::class.java) || (typeInfo.dataType === ArrayType.CHAR)) return fillValue!![0]
                val bbuff = ByteBuffer.wrap(fillValue)
                if (typeInfo.endian != null) bbuff.order(typeInfo.endian)
                if (typeInfo.dataType.getPrimitiveClass() === Short::class.java) {
                    val tbuff = bbuff.asShortBuffer()
                    return tbuff.get()
                } else if (typeInfo.dataType.getPrimitiveClass() === Int::class.java) {
                    val tbuff = bbuff.asIntBuffer()
                    return tbuff.get()
                } else if (typeInfo.dataType.getPrimitiveClass() === Long::class.java) {
                    val tbuff = bbuff.asLongBuffer()
                    return tbuff.get()
                } else if (typeInfo.dataType === ArrayType.FLOAT) {
                    val tbuff = bbuff.asFloatBuffer()
                    return tbuff.get()
                } else if (typeInfo.dataType === ArrayType.DOUBLE) {
                    val tbuff = bbuff.asDoubleBuffer()
                    return tbuff.get()
                }
                return null
            }

        // limited reader; Variable is not built yet.
        @Throws(IOException::class)
        fun readArray(): Array<*> {
            val shape: IntArray = mds.dimLength
            val dataType: ArrayType = typeInfo.dataType
            val layout: Layout
            try {
                if (isChunked) {
                    layout = H5tiledLayout(this, dataType, Section(shape))
                } else {
                    layout = LayoutRegular(dataPos, dataType.getSize(), shape, null)
                }
            } catch (e2: InvalidRangeException) {
                // cant happen because we use null for wantSection
                throw IllegalStateException()
            }
            val data: Any = IospArrayHelper.readDataFill(raf, layout, dataType, getFillValue(), typeInfo.endian)
            return Arrays.factory(dataType, shape, data)
        }

        // limited reader; Variable is not built yet.
        @Throws(IOException::class)
        fun readString(): String? {
            val shape = intArrayOf(mdt.byteSize)
            val dataType: ArrayType = typeInfo.dataType
            val layout: Layout
            try {
                if (isChunked) {
                    layout = H5tiledLayout(this, dataType, Section(shape))
                } else {
                    layout = LayoutRegular(dataPos, dataType.getSize(), shape, null)
                }
            } catch (e: InvalidRangeException) {
                // cant happen because we use null for wantSection
                throw IllegalStateException()
            }
            val data: Any = IospArrayHelper.readDataFill(raf, layout, dataType, getFillValue(), typeInfo.endian)

            // read and parse the ODL
            var result: String? = ""
            if (data is String) {
                // Sometimes StructMetadata.0 is stored as a string,
                // and IospHelper returns it directly as a string, so pass it along
                result = data
            } else {
                val dataArray: Array<*> = Arrays.factory(dataType, shape, data)
                // read and parse the ODL
                if (dataArray.getArrayType() === ArrayType.CHAR) {
                    result = Arrays.makeStringFromChar(dataArray as Array<Byte?>)
                } else {
                    H5builder.Companion.log.error(
                        "Unsupported array type {} for StructMetadata",
                        dataArray.getArrayType()
                    )
                }
            }
            return result
        }
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Fetch a Vlen data array.
     *
     * @param globalHeapIdAddress address of the heapId, used to get the String out of the heap
     * @param dataType type of data
     * @param endian byteOrder of the data (0 = BE, 1 = LE)
     * @return the Array read from the heap
     * @throws IOException on read error
     */
    @Throws(IOException::class)
    fun getHeapDataArray(globalHeapIdAddress: Long, dataType: ArrayType?, endian: ByteOrder?): Array<*> {
        val heapId: HeapIdentifier = h5objects.readHeapIdentifier(globalHeapIdAddress)
        if (H5builder.Companion.debugHeap) {
            println(" heapId= {}", heapId)
        }
        return getHeapDataArray(heapId, dataType, endian)
    }

    @Throws(IOException::class)
    fun getHeapDataArray(heapId: HeapIdentifier, dataType: ArrayType, endian: ByteOrder?): Array<*> {
        val ho: GlobalHeap.HeapObject = heapId.getHeapObject()
            ?: throw IllegalStateException("Illegal Heap address, HeapObject = $heapId")
        if (H5builder.Companion.debugHeap) {
            println(" HeapObject= {}", ho)
        }
        if (endian != null) {
            raf.order(endian)
        }
        if (ArrayType.FLOAT === dataType) {
            val pa = FloatArray(heapId.nelems)
            raf.seek(ho.dataPos)
            raf.readFloat(pa, 0, pa.size)
            return Arrays.factory(dataType, intArrayOf(pa.size), pa)
        } else if (ArrayType.DOUBLE === dataType) {
            val pa = DoubleArray(heapId.nelems)
            raf.seek(ho.dataPos)
            raf.readDouble(pa, 0, pa.size)
            return Arrays.factory(dataType, intArrayOf(pa.size), pa)
        } else if (dataType.getPrimitiveClass() === Byte::class.java) {
            val pa = ByteArray(heapId.nelems)
            raf.seek(ho.dataPos)
            raf.readFully(pa, 0, pa.size)
            return Arrays.factory(dataType, intArrayOf(pa.size), pa)
        } else if (dataType.getPrimitiveClass() === Short::class.java) {
            val pa = ShortArray(heapId.nelems)
            raf.seek(ho.dataPos)
            raf.readShort(pa, 0, pa.size)
            return Arrays.factory(dataType, intArrayOf(pa.size), pa)
        } else if (dataType.getPrimitiveClass() === Int::class.java) {
            val pa = IntArray(heapId.nelems)
            raf.seek(ho.dataPos)
            raf.readInt(pa, 0, pa.size)
            return Arrays.factory(dataType, intArrayOf(pa.size), pa)
        } else if (dataType.getPrimitiveClass() === Long::class.java) {
            val pa = LongArray(heapId.nelems)
            raf.seek(ho.dataPos)
            raf.readLong(pa, 0, pa.size)
            return Arrays.factory(dataType, intArrayOf(pa.size), pa)
        }
        throw UnsupportedOperationException("getHeapDataAsArray dataType=$dataType")
    }

    /**
     * Fetch a String from the heap.
     *
     * @param heapIdAddress address of the heapId, used to get the String out of the heap
     * @return String the String read from the heap
     * @throws IOException on read error
     */
    @Throws(IOException::class)
    fun readHeapString(heapIdAddress: Long): String {
        val heapId: HeapIdentifier = h5objects.readHeapIdentifier(heapIdAddress)
        if (heapId.isEmpty()) {
            return H5builder.Companion.NULL_STRING_VALUE
        }
        val ho: GlobalHeap.HeapObject = heapId.getHeapObject()
            ?: throw IllegalStateException("Cant find Heap Object,heapId=$heapId")
        if (ho.dataSize > 1000 * 1000) return java.lang.String.format("Bad HeapObject.dataSize=%s", ho)
        raf.seek(ho.dataPos)
        return raf.readString(ho.dataSize as Int, valueCharset)
    }

    /**
     * Fetch a String from the heap, when the heap identifier has already been put into a ByteBuffer at given pos
     *
     * @param bb heap id is here
     * @param pos at this position
     * @return String the String read from the heap
     * @throws IOException on read error
     */
    @Throws(IOException::class)
    fun readHeapString(bb: ByteBuffer?, pos: Int): String {
        val heapId: HeapIdentifier = h5objects.readHeapIdentifier(bb, pos)
        if (heapId.isEmpty()) {
            return H5builder.Companion.NULL_STRING_VALUE
        }
        val ho: GlobalHeap.HeapObject = heapId.getHeapObject()
            ?: throw IllegalStateException("Cant find Heap Object,heapId=$heapId")
        raf.seek(ho.dataPos)
        return raf.readString(ho.dataSize as Int, valueCharset)
    }

    @Throws(IOException::class)
    fun readHeapVlen(bb: ByteBuffer?, pos: Int, dataType: ArrayType?, endian: ByteOrder?): Array<*> {
        val heapId: HeapIdentifier = h5objects.readHeapIdentifier(bb, pos)
        return getHeapDataArray(heapId, dataType, endian)
    }

    /**
     * Get a data object's name, using the objectId you get from a reference (aka hard link).
     *
     * @param objId address of the data object
     * @return String the data object's name, or null if not found
     * @throws IOException on read error
     */
    @Throws(IOException::class)
    fun getDataObjectName(objId: Long): String? {
        val dobj: DataObject? = getDataObject(objId, null)
        if (dobj == null) {
            H5builder.Companion.log.error("H5iosp.readVlenData cant find dataObject id= {}", objId)
            return null
        } else {
            if (H5builder.Companion.debugVlen) {
                println(" Referenced object= {}", dobj.who)
            }
            return dobj.who
        }
    }
    //////////////////////////////////////////////////////////////
    // Internal organization of Data Objects
    /**
     * All access to data objects come through here, so we can cache.
     * Look in cache first; read if not in cache.
     *
     * @param address object address (aka id)
     * @param name optional name
     * @return DataObject
     * @throws IOException on read error
     */
    @Throws(IOException::class)
    fun getDataObject(address: Long, name: String?): DataObject? {
        // find it
        var dobj: DataObject? = addressMap[address]
        if (dobj != null) {
            if ((dobj.who == null) && name != null) dobj.who = name
            return dobj
        }
        // if (name == null) return null; // ??

        // read it
        dobj = h5objects.readDataObject(address, name)
        addressMap[address] = dobj // look up by address (id)
        return dobj
    }

     */

    //////////////////////////////////////////////////////////////
    // utilities

    fun makeIntFromBytes(bb: ByteArray, start: Int, n: Int): Int {
        var result = 0
        for (i in start + n - 1 downTo start) {
            result = result shl 8
            val b = bb[i].toInt()
            result += if ((b < 0)) b + 256 else b
        }
        return result
    }


    fun getFileOffset(address: Long): Long {
        return baseAddress + address
    }

    @Throws(IOException::class)
    fun readLength(state : OpenFileState): Long {
        return if (isLengthLong) raf.readLong(state) else raf.readInt(state).toLong()
    }

    @Throws(IOException::class)
    fun readOffset(state : OpenFileState): Long {
        return if (isOffsetLong) raf.readLong(state) else raf.readInt(state).toLong()
    }

    // size of data depends on "maximum possible number"
    @Throws(IOException::class)
    fun readVariableSizeMax(state : OpenFileState, maxNumber: Long): Long {
        val size: Int = this.getNumBytesFromMax(maxNumber)
        return this.readVariableSizeUnsigned(state, size)
    }

    // always skip 8 bytes
    @Throws(IOException::class)
    fun readVariableSizeFactor(state : OpenFileState, sizeFactor: Int): Long {
        val size = variableSizeFactor (sizeFactor)
        return readVariableSizeUnsigned(state, size)
    }

    fun variableSizeFactor(sizeFactor: Int): Int {
        return when (sizeFactor) {
            0 -> 1
            1 -> 2
            2 -> 4
            3 -> 8
            else -> throw RuntimeException("Illegal SizFactor $sizeFactor")
        }
    }

    @Throws(IOException::class)
    fun readVariableSizeUnsigned(state : OpenFileState, size: Int): Long {
        val vv: Long
        if (size == 1) {
            vv = DataType.unsignedByteToShort(raf.readByte(state)).toLong()
        } else if (size == 2) {
            val s = raf.readShort(state)
            vv = DataType.unsignedShortToInt(s).toLong()
        } else if (size == 4) {
            vv = DataType.unsignedIntToLong(raf.readInt(state))
        } else if (size == 8) {
            vv = raf.readLong(state)
        } else {
            vv = readVariableSizeN(state, size)
        }
        return vv
    }

    @Throws(IOException::class)
    private fun readVariableSizeN(state : OpenFileState, nbytes : Int): Long {
        val ch = IntArray(nbytes)
        for (i in 0 until nbytes) ch[i] = raf.readByte(state).toInt()
        var result = ch[nbytes - 1].toLong()
        for (i in nbytes - 2 downTo 0) {
            result = result shl 8
            result += ch[i].toLong()
        }
        return result
    }

    @Throws(IOException::class)
    fun readAddress(state : OpenFileState): Long {
        return getFileOffset(readOffset(state))
    }

    // size of data depends on "maximum possible number"
    fun getNumBytesFromMax(maxNumber: Long): Int {
        var maxn = maxNumber
        var size = 0
        while (maxn != 0L) {
            size++
            maxn = maxn ushr 8 // right shift with zero extension
        }
        return size
    }

    internal fun convertString(b: ByteArray): String {
        // null terminates
        var count = 0
        while (count < b.size) {
            if (b[count].toInt() == 0) break
            count++
        }
        return String(b, 0, count, valueCharset) // all strings are considered to be UTF-8 unicode
    }

    internal fun convertString(b: ByteArray, start: Int, len: Int): String {
        // null terminates
        var count = start
        while (count < start + len) {
            if (b[count].toInt() == 0) break
            count++
        }
        return String(b, start, count - start, valueCharset) // all strings are considered to be UTF-8
        // unicode
    }

    internal fun convertEnums(map: Map<Int, String>, values: Array<Number>): List<String> {
        val sarray = mutableListOf<String>()
        for (noom in values) {
            val ival = noom.toInt()
            var sval = map[ival]
            if (sval == null) {
                sval = "Unknown enum value=$ival"
            }
            sarray.add(sval)
        }
        return sarray
    }

    /*
    val OpenFile: OpenFile
        get() = raf

    val isClassic: Boolean
        get() = false // TODO

    fun close() {
        if (debugTracker) {
            val f = Formatter()
            memTracker.report(f)
            println("{}", f)
        }
    }

    @Throws(IOException::class)
    fun getEosInfo(f: Formatter?) {
        HdfEos.getEosInfo(raf.getLocation(), this, root, f)
    }

    val dataObjects: List<Any>
        // debug - hdf5Table
        get() {
            val result: ArrayList<DataObject> = ArrayList<Any?>(addressMap.values)
            result.sort(Comparator.comparingLong<DataObject>(ToLongFunction<DataObject> { o: DataObject -> o.address }))
            return result
        }

     */

    companion object {
        private val logger = KotlinLogging.logger("H5builder")

        // special attribute names in HDF5
        val HDF5_CLASS = "CLASS"
        val HDF5_DIMENSION_LIST = "DIMENSION_LIST"
        val HDF5_DIMENSION_SCALE = "DIMENSION_SCALE"
        val HDF5_DIMENSION_LABELS = "DIMENSION_LABELS"
        val HDF5_DIMENSION_NAME = "NAME"
        val HDF5_REFERENCE_LIST = "REFERENCE_LIST"

        // debugging
        private val debugVlen = false
        private val debug1 = false
        private val debugDetail = false
        private val debugPos = false
        private val debugHeap = false
        private val debugV = false
        private val debugGroupBtree = false
        private val debugDataBtree = false
        private val debugBtree2 = false
        private val debugContinueMessage = false
        private val debugTracker = false
        private val debugSoftLink = false
        private val debugHardLink = false
        private val debugSymbolTable = false
        private val warnings = true
        private val debugReference = false
        private val debugCreationOrder = false
        private val debugStructure = false
        private val debugDimensionScales = false

        // NULL string value, following netCDF-C, set to NIL
        val NULL_STRING_VALUE = "NIL"

        private val magicHeader = byteArrayOf(
            0x89.toByte(),
            'H'.code.toByte(),
            'D'.code.toByte(),
            'F'.code.toByte(),
            '\r'.code.toByte(),
            '\n'.code.toByte(),
            0x1a,
            '\n'.code.toByte()
        )
        private val magicString = String(magicHeader, StandardCharsets.UTF_8)
        private val transformReference = true

        ////////////////////////////////////////////////////////////////////////////////
        /*
   * Implementation notes
   * any field called address is actually relative to the base address.
   * any field called filePos or dataPos is a byte offset within the file.
   *
   * it appears theres no sure fire way to tell if the file was written by netcdf4 library
   * 1) if one of the the NETCF4-XXX atts are set
   * 2) dimension scales:
   * 1) all dimensions have a dimension scale
   * 2) they all have the same length as the dimension
   * 3) all variables' dimensions have a dimension scale
   */
        private val KNOWN_FILTERS = 3
    }
}