package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.*
import com.sunya.cdm.api.Section.Companion.computeSize
import com.sunya.cdm.array.StructureMember
import com.sunya.cdm.iosp.*
import com.sunya.netchdf.hdf5.H5builder.Companion.HDF5_CLASS
import com.sunya.netchdf.hdf5.H5builder.Companion.HDF5_DIMENSION_LIST
import com.sunya.netchdf.hdf5.H5builder.Companion.HDF5_DIMENSION_NAME
import com.sunya.netchdf.hdf5.H5builder.Companion.HDF5_DIMENSION_SCALE
import com.sunya.netchdf.hdf5.H5builder.Companion.HDF5_REFERENCE_LIST
import com.sunya.netchdf.hdf5.H5builder.Companion.HDF5_SPECIAL_ATTS
import com.sunya.netchdf.netcdf4.Netcdf4.Companion.NETCDF4_NON_COORD
import com.sunya.netchdf.netcdf4.Netcdf4.Companion.NETCDF4_SPECIAL_ATTS
import java.io.IOException
import java.nio.*

internal val includeOriginalAttributes = false
internal val debugDimensionScales = false

internal fun H5builder.buildCdm(h5root : H5Group) : Group {
    return buildGroup(h5root).build(null)
}

internal fun H5builder.buildGroup(group5 : H5Group) : Group.Builder {
    val groupb = Group.Builder(group5.name)

    makeDimensions(groupb, group5)

    group5.typedefs.forEach {
        val typedef = buildTypedef(groupb, it )
        if (this.addTypedef(it.mdtAddress, typedef, it.mdtHash)) {
            groupb.typedefs.add(typedef)
        }
    }

    group5.attributes().forEach { groupb.addAttribute( buildAttribute( it )) }

    group5.variables.filter{ it.isVariable }.forEach { groupb.addVariable( buildVariable( it )) }

    group5.nestedGroups.forEach { groupb.addGroup( buildGroup( it )) }

    if (strict) {
        val iter = groupb.attributes.iterator()
        while (iter.hasNext()) {
            val attname = iter.next().name
            if (NETCDF4_SPECIAL_ATTS.contains(attname) or HDF5_SPECIAL_ATTS.contains(attname)) {
                iter.remove()
            }
        }
    }

    return groupb
}

internal fun H5builder.buildTypedef(groupb : Group.Builder, typedef5: H5Typedef): Typedef {
    return when (typedef5.kind) {
        TypedefKind.Compound -> {
            val mess = typedef5.compoundMessage!!
            this.buildCompoundTypedef(groupb, typedef5.dataObject.name!!, mess)
        }
        TypedefKind.Enum -> {
            val mess = typedef5.enumMessage!!
            EnumTypedef(typedef5.dataObject.name!!, mess.datatype, mess.valuesMap)
        }
        TypedefKind.Opaque -> {
            val mess = typedef5.opaqueMessage!!
            OpaqueTypedef(typedef5.dataObject.name!!, mess.elemSize)
        }
        TypedefKind.Vlen -> {
            val mess = typedef5.vlenMessage!!
            val h5type = H5TypeInfo(mess.base)
            VlenTypedef(typedef5.dataObject.name!!, h5type.datatype(this))
        }
        else -> throw RuntimeException()
    }
}

// allow it to recurse
internal fun H5builder.buildCompoundTypedef(groupb : Group.Builder, name : String, mess: DatatypeCompound) : Typedef {
    // first look for embedded typedefs that need to be added
    mess.members.forEach { member ->
        val nestedTypedef = when (member.mdt.type) {
            Datatype5.Compound -> buildCompoundTypedef(groupb, member.name, member.mdt as DatatypeCompound)
            Datatype5.Enumerated -> buildEnumTypedef(member.name, member.mdt as DatatypeEnum)
            else -> null
        }
        if (nestedTypedef != null) {
            if (this.addTypedef(member.mdt.address, nestedTypedef, member.mdt.hashCode())) {
                groupb.typedefs.add(nestedTypedef)
            }
        }
    }

    // now build the typedef for the compound message
    val members = mess.members.map {
        val h5type = H5TypeInfo(it.mdt)
        val datatype = h5type.datatype(this)
        StructureMember(it.name, datatype, it.offset, it.dims)
    }
    return CompoundTypedef(name, members)
}

internal fun buildEnumTypedef(name : String, mess: DatatypeEnum): EnumTypedef {
    return EnumTypedef(name, mess.datatype, mess.valuesMap)
}

internal fun H5builder.buildAttribute(att5 : AttributeMessage) : Attribute {
    val typedef = this.findTypedef(att5.mdt.address, att5.mdt.hashCode())
    if (debugTypedefs and (typedef != null)) {
        println(" made attribute ${att5.name} from typedef ${typedef!!.name}@${att5.mdt.address}")
    }
    val h5type = H5TypeInfo(att5.mdt)
    val dc = DataContainerAttribute(att5.name, h5type, att5.dataPos, att5.mdt, att5.mds)
    val values = this.readRegularData(dc, null)
    var useType = h5type.datatype(this)
    if (useType == Datatype.CHAR) useType = Datatype.STRING // LOOK
    return Attribute(att5.name, useType, values.toList())
}

internal fun H5builder.buildVariable(v5 : H5Variable) : Variable.Builder {
    // what the cdm variable looks like
    val builder = Variable.Builder()
    builder.name = v5.name.substringAfter(NETCDF4_NON_COORD)

    val h5type = H5TypeInfo(v5.mdt)
    builder.datatype = h5type.datatype(this) // typedefs added here
    // LOOK why doesnt h5type.datatype() do this ?
    //if (builder.datatype == Datatype.CHAR && v5.mdt.elemSize > 1) {
    //    builder.datatype = Datatype.STRING
    //}

    if (v5.dimList != null) {
        builder.dimList = v5.dimList!!.trim().split(" ")
    } else if (v5.mds.dims.size > 0) {
        // LOOK non-shared, integrate with shared ??
        v5.mds.dims.forEach{builder.dimensions.add(Dimension(it))}
    }

    for (att5 in v5.attributes()) {
        builder.attributes.add(buildAttribute(att5))
    }
    if (strict) {
        val iter = builder.attributes.iterator()
        while (iter.hasNext()) {
            val attname = iter.next().name
            if (NETCDF4_SPECIAL_ATTS.contains(attname) or HDF5_SPECIAL_ATTS.contains(attname)) {
                iter.remove()
            }
        }
    }

    // stuff needed to read hdf5
    require (v5.dataObject.mdl != null)
    val vdata = DataContainerVariable(builder.name!!, h5type, v5, this)
    builder.spObject = vdata

    return builder
}

internal interface DataContainer {
    val name: String
    val h5type: H5TypeInfo
    val dataPos: Long
    val mdt: DatatypeMessage
    val mds: DataspaceMessage
    val storageDims : IntArray
}

internal open class DataContainerAttribute(
    override val name : String,
    override val h5type: H5TypeInfo,
    override val dataPos : Long,
    override val mdt: DatatypeMessage,
    override val mds: DataspaceMessage,
    ) : DataContainer {
        override val storageDims = mds.dims
    }


internal class DataContainerVariable(
    override val name: String,
    override val h5type: H5TypeInfo,
    v5 : H5Variable,
    h5 : H5builder,
) : DataContainer {
    override val dataPos : Long
    override val mdt: DatatypeMessage = v5.mdt
    override val mds: DataspaceMessage = v5.mds
    override val storageDims : IntArray // dimensions

    val mdl = v5.mdl
    val mfp = v5.mfp

    val isChunked : Boolean
    val elementSize : Int // total length in bytes on disk of one element
    val onlyFillValue : Boolean // no data at all
    val fillValue : Any?

    init {
        // TODO if compact, do not use fileOffset
        dataPos = when (mdl) {
            is DataLayoutContiguous -> h5.getFileOffset(mdl.dataAddress)
            is DataLayoutContiguous3 -> h5.getFileOffset(mdl.dataAddress)
            is DataLayoutChunked -> mdl.btreeAddress // offset will be added in BTreeData
            else -> -1 // LOOK compact?
        }

        // deal with unallocated data
        fillValue = getFillValueNonDefault(h5, v5, h5type)
        onlyFillValue = (dataPos == -1L)

        isChunked = (mdl.layoutClass == LayoutClass.Chunked)
        when (mdl) {
            is DataLayoutCompact -> {
                this.storageDims = mds.dims
                this.elementSize = mdt.elemSize
            }
            is DataLayoutCompact3 -> {
                this.storageDims = mds.dims
                this.elementSize = mdt.elemSize
            }
            is DataLayoutContiguous -> {
                this.storageDims = mds.dims
                val nelems = computeSize(this.storageDims).toInt()
                this.elementSize = (mdt.elemSize / nelems)
            }
            is DataLayoutContiguous3 -> {
                this.storageDims = mds.dims
                val nelems = computeSize(this.storageDims).toInt()
                this.elementSize = (mdt.elemSize / nelems)
            }
            is DataLayoutChunked -> {
                this.storageDims = mdl.dims // LOOK
                this.elementSize = storageDims[storageDims.size - 1] // last number is element size
                // make the data btree, entries are not read in, but the point is to cache it ??
                // this.btree = DataBTree(h5, this.dataPos, shape, this.storageSize, null)
            }
            else -> throw RuntimeException()
        }
    }
}

internal fun getFillValueNonDefault(h5 : H5builder, v5 : H5Variable, h5type: H5TypeInfo): Any? {
    // look for fill value message
    var fillValueBB : ByteBuffer? = null
    for (mess in v5.dataObject.messages) {
        if (mess.mtype === MessageType.FillValue) {
            val fvm = mess as FillValueMessage
            if (fvm.hasFillValue) {
                fillValueBB = fvm.value // val value: ByteBuffer?
            }
        } else if (mess.mtype === MessageType.FillValueOld) {
            val fvm = mess as FillValueOldMessage
            if (fvm.size > 0) {
                fillValueBB = fvm.value // val value: ByteBuffer?
            }
        }
    }
    if (fillValueBB == null) return null
    fillValueBB.position(0)
    fillValueBB.order(h5type.endian)

    // a single data value, same datatype as the dataset
    return when (h5type.datatype(h5)) {
        Datatype.BYTE -> fillValueBB.get()
        Datatype.CHAR, Datatype.UBYTE, Datatype.ENUM1 -> fillValueBB.get().toUByte()
        Datatype.SHORT -> fillValueBB.getShort()
        Datatype.USHORT, Datatype.ENUM2 -> fillValueBB.getShort().toUShort()
        Datatype.INT -> fillValueBB.getInt()
        Datatype.UINT, Datatype.ENUM4 -> fillValueBB.getInt().toUInt()
        Datatype.FLOAT -> fillValueBB.getFloat()
        Datatype.DOUBLE -> fillValueBB.getDouble()
        Datatype.LONG -> fillValueBB.getLong()
        Datatype.ULONG -> fillValueBB.getLong().toULong()
        Datatype.OPAQUE -> fillValueBB
        else -> null
    }
}


///////////////////////// Attributes

/*
   * from https://www.unidata.ucar.edu/software/netcdf/docs/netcdf.html#NetCDF_002d4-Format
   * C.3.7 Attributes
   *
   * Attributes in HDF5 and netCDF-4 correspond very closely. Each attribute in an HDF5 file is represented as an
   * attribute in the netCDF-4 file, with the exception of the attributes below, which are ignored by the netCDF-4 API.
   *
   * 1) _Netcdf4Coordinates An integer array containing the dimension IDs of a variable which is a multi-dimensional
   * coordinate variable.
   * 2) _nc3_strict When this (scalar, H5T_NATIVE_INT) attribute exists in the root group of the HDF5 file, the netCDF API
   * will enforce the netCDF classic model on the data file.
   * 3) REFERENCE_LIST This attribute is created and maintained by the HDF5 dimension scale API.
   * 4) CLASS This attribute is created and maintained by the HDF5 dimension scale API.
   * 5) DIMENSION_LIST This attribute is created and maintained by the HDF5 dimension scale API.
   * 6) NAME This attribute is created and maintained by the HDF5 dimension scale API.
 */

///////////////////////// Dimensions

/*
https://www.unidata.ucar.edu/blogs/news/entry/netcdf_shared_dimensions_vs_hdf5

A Dimension Scale is a special variable containing a set of references to dimensions in variables.
Each referenced variable has a DIMENSION_LIST attribute that contains, for each dimension, a list of references to Dimension Scales.
So we have a two-way, many-to-many linking between Dimension Scales and Dimensions.
 */

/*

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

// find dimensions in h5group, add them to parentGroup
internal fun H5builder.makeDimensions(parentGroup: Group.Builder, h5group: H5Group) {

    // 1. find all objects with CLASS = "DIMENSION_SCALE", make into a dimension. use shape(0) as length.
    h5group.variables.forEach { findDimensionScales(parentGroup, h5group, it) }

    // 2. if also a variable (NAME != "This is a ...") then first dim = itself, second matches length, if multiple
    // match, use :_Netcdf4Coordinates = 0, 3 and order of dimensions.
    h5group.variables.filter { it.is2DCoordinate }.forEach { findDimensionScales2D(h5group, it) }

    // 3. use DIMENSION_LIST to assign dimensions to other variables.
    h5group.variables.forEach { findSharedDimensions(parentGroup, h5group, it) }

    for (d in h5group.dimList) {
        parentGroup.addDimensionIfNotExists(d)
    }
}

// find the Dimension Scale objects, turn them into shared dimensions
// always has attribute CLASS = "DIMENSION_SCALE"
// note that we dont bother looking at their REFERENCE_LIST
@Throws(IOException::class)
internal fun H5builder.findDimensionScales(g: Group.Builder, h5group: H5Group, h5variable: H5Variable) {

    val removeAtts = mutableListOf<AttributeMessage>()
    h5variable.attributes().filter { it.name.equals(HDF5_CLASS) }.forEach {
        val att = buildAttribute(it)
        check(att.isString)
        val value: String = att.values[0] as String
        if (value == HDF5_DIMENSION_SCALE && h5variable.mds.rank() > 0) {
            // create a dimension - always use the first dataspace length
            h5variable.dimList = addSharedDimension(
                g,
                h5group,
                h5variable.name,
                h5variable.mds.dims[0],
                h5variable.mds.isUnlimited
            )
            h5variable.hasNetcdfDimensions = true
            if (!includeOriginalAttributes) {
                removeAtts.add(it)
            }
            if (h5variable.mds.rank() > 1) {
                h5variable.is2DCoordinate = true
            }
        }
    }
    removeAtts.forEach { h5variable.removeAtt(it)}
}

// add a dimension, return its name
private fun addSharedDimension(
    parent: Group.Builder,
    h5group: H5Group,
    name: String,
    length: Int,
    isUnlimited: Boolean
): String {
    val dimName = name.substringAfterLast('/')
    var d = h5group.dimMap[dimName] // first look in current group
    if (d == null) { // create if not exist
        d = Dimension(name, length, isUnlimited, true)
        h5group.dimMap[dimName] = d
        h5group.dimList.add(d)
        parent.addDimension(d)
        if (debugDimensionScales) {
            println("addDimension name=" + name + " dim= " + d + " to group " + parent.name)
        }
    } else { // check has correct length
        check(d.length == length) { "addDimension: DimScale has different length than dimension it references dimScale=$dimName" }
    }
    return d.name
}

// look for unlimited dimensions without dimension scale - must get length from the variable
// LOOK this implies that different variables might have different dimension lengths.
//   so, underlying "h5dataset" not same as cdm variable
private fun extendDimension(parent: Group.Builder, h5group: H5Group, name: String, length: Int): String {
    val dimName = name.substringAfterLast('/')
    val d = h5group.findDimension(dimName) // first look in current group
    if (d != null) {
        // if (d.isUnlimited && length > d.length) {
        if (length > d.length) {
            parent.replaceDimension(d.copy(length = length))
        }
        // check(!(!d.isUnlimited && length != d.length)) { "extendDimension: DimScale has different length than dimension it references dimScale=$dimName" }
        return d.name
    }
    return dimName
}

internal fun findDimensionScales2D(h5group: H5Group, h5variable: H5Variable) {
    val lens: IntArray = h5variable.mds.dims
    if (debugDimensionScales and (lens.size > 2)) {
        println("DIMENSION_LIST: dimension scale > 2 = ${h5variable.name}")
        return
    }

    // first dimension is itself
    val name: String = h5variable.name
    val pos = name.lastIndexOf('/')
    val dimName = if (pos >= 0) name.substring(pos + 1) else name
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
    for (d in h5group.dimList) {
        if (d.length == want_len) {
            if (match == null) {
                match = d
            } else {
                unique = false
            }
        }
    }
    if (match != null && unique) {
        sbuff.append(match.name) // 2. if length matches and unique, use it
    } else {
        if (match == null) { // 3. if no length matches or multiple matches, then use anonymous
            if (debugDimensionScales) println("DIMENSION_LIST: dimension scale ${h5variable.name} has second dimension ${want_len} but no match")
            // based on /media/twobee/netch/gilmore/data.nc, just ignore this second dimension
            // sbuff.append(want_len)
        } else {
            if (debugDimensionScales) println("DIMENSION_LIST: dimension scale ${h5variable.name} has second dimension ${want_len} but multiple matches")
            sbuff.append(want_len)
        }
    }
    h5variable.dimList = sbuff.toString()
}

// look for references to dimension scales, ie the variables that use them
// return true if this variable is compatible with netcdf4 data model
@Throws(IOException::class)
internal fun H5builder.findSharedDimensions(parentGroup: Group.Builder, h5group: H5Group, h5variable: H5Variable): Boolean {

    val removeAtts = mutableListOf<AttributeMessage>()
    h5variable.attributes().forEach { matt ->
        when (matt.name) {
            HDF5_DIMENSION_LIST -> {
                // references : may extend the dimension rank?
                val att: Attribute = buildAttribute(matt) // this reads in the data
                if (att.values.size != h5variable.mds.rank()) {
                    // some attempts to writing hdf5 directly fail here
                    if (debugDimensionScales) println("DIMENSION_LIST: must have same number of dimension scales as dimensions att=${att} on variable ${h5variable.name}")
                } else {
                    val sbuff = StringBuilder()
                    var i = 0
                    while (i < att.values.size) {
                        val name: String = att.values[i] as String // LOOK assumes string
                        val dimName: String = extendDimension(parentGroup, h5group, name, h5variable.mds.dims[i])
                        sbuff.append(dimName).append(" ")
                        i++
                    }
                    h5variable.dimList = sbuff.toString()
                    h5variable.hasNetcdfDimensions = true
                    if (debugDimensionScales) {
                        println("Found dimList '${h5variable.dimList}' for var '${h5variable.name}'")
                    }
                }
                removeAtts.add(matt)

            }

            HDF5_DIMENSION_NAME -> {
                val att = buildAttribute(matt)
                val value: String = att.values[0] as String
                if (value.startsWith("This is a netCDF dimension but not a netCDF variable")) {
                    h5variable.isVariable = false
                    isNetcdf4 = true
                }
                removeAtts.add(matt)
                if (debugDimensionScales) {
                    println("Found $HDF5_DIMENSION_NAME='$value'")
                }
            }

            // HDF5_DIMENSION_LABELS,
            HDF5_REFERENCE_LIST -> removeAtts.add(matt)
        }
    }
    if (!includeOriginalAttributes) {
        removeAtts.forEach { h5variable.removeAtt(it) }
    }

    return h5variable.hasNetcdfDimensions || h5variable.mds.rank() == 0
}


