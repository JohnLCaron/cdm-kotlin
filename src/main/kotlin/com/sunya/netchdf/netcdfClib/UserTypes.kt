package com.sunya.netchdf.netcdfClib

import com.sunya.cdm.api.Attribute
import com.sunya.cdm.api.DataType
import com.sunya.cdm.api.EnumTypedef
import com.sunya.cdm.api.Group
import com.sunya.cdm.iosp.*
import com.sunya.netchdf.netcdf4.ffm.nc_vlen_t
import com.sunya.netchdf.netcdf4.ffm.netcdf_h.*
import java.io.IOException
import java.lang.foreign.MemoryAddress
import java.lang.foreign.MemorySegment
import java.lang.foreign.MemorySession
import java.lang.foreign.ValueLayout
import java.nio.ByteBuffer
import java.util.*

@Throws(IOException::class)
internal fun NCheader.readUserTypes(session: MemorySession, grpid: Int, gb: Group.Builder, userTypes : MutableMap<Int, UserType>) {
    val numUserTypes_p = session.allocate(C_INT, 0)
    val MAX_USER_TYPES = 1000L // bad API
    val userTypes_p = session.allocateArray(C_INT, MAX_USER_TYPES)
    checkErr("nc_inq_typeids", nc_inq_typeids(grpid, numUserTypes_p, userTypes_p))
    val numUserTypes = numUserTypes_p[C_INT, 0]
    if (numUserTypes == 0) {
        return
    }

    // for each defined "user type", get information, store in Map
    for (idx in 0 until numUserTypes) {
        val userTypeId = userTypes_p.getAtIndex(ValueLayout.JAVA_INT, idx.toLong())

        val name_p: MemorySegment = session.allocate(NC_MAX_NAME().toLong() + 1)
        val size_p = session.allocate(C_LONG, 0)
        val baseType_p: MemorySegment = session.allocate(C_INT, 0)
        val nfields_p = session.allocate(C_LONG, 0)
        val typeClass_p = session.allocate(C_INT, 0)

        // grpid: the group containing the user defined type.
        // userTypeId: The typeid for this type, as returned by nc_def_compound, nc_def_opaque, nc_def_enum,
        //   nc_def_vlen, or nc_inq_var.
        // name: If non-NULL, the name of the user defined type.. It will be NC_MAX_NAME bytes or less.
        // size: If non-NULL, the (in-memory) size of the type in bytes.. VLEN type size is the size of nc_vlen_t.
        // baseType: may be used to malloc space for the data, no matter what the type.
        // nfields: If non-NULL, the number of fields for enum and compound types.
        // typeClass: the class of the user defined type, NC_VLEN, NC_OPAQUE, NC_ENUM, or NC_COMPOUND.
        checkErr("nc_inq_user_type",
            nc_inq_user_type(grpid, userTypeId, name_p, size_p, baseType_p, nfields_p, typeClass_p)
        )
        val name: String = name_p.getUtf8String(0)
        val size = size_p[C_LONG, 0]
        val baseType = baseType_p[C_INT, 0]
        val nfields = nfields_p[C_LONG, 0]
        val typeClass = typeClass_p[C_INT, 0]

        val ut = UserType(session, grpid, userTypeId, name, size, baseType, nfields, typeClass)
        userTypes[userTypeId] = ut

        if (typeClass == NC_ENUM()) {
            val values: Map<Int, String> = readEnumTypedef(session, grpid, userTypeId)
            ut.enumTypdef = EnumTypedef(name, ut.enumBaseType, values)
            gb.typedefs.add(ut.enumTypdef!!)
        } else if (typeClass == NC_OPAQUE()) {
            val nameo_p: MemorySegment = session.allocate(NC_MAX_NAME().toLong() + 1)
            val sizeo_p = session.allocate(C_LONG, 0)
            checkErr("nc_inq_opaque", nc_inq_opaque(grpid, userTypeId, nameo_p, sizeo_p))
            ut.setSize(sizeo_p[C_LONG, 0].toInt())
            // doesnt seem to be any new info
            // String nameos = nameo_p.getUtf8String(0)
        }
    }
}

@Throws(IOException::class)
private fun NCheader.readEnumTypedef(session: MemorySession, grpid: Int, xtype: Int): Map<Int, String> {
    val name_p: MemorySegment = session.allocate(NC_MAX_NAME().toLong())
    val baseType_p = session.allocate(C_INT)
    val baseSize_p = session.allocate(C_LONG, 0)
    val nmembers_p = session.allocate(C_LONG, 0)
    checkErr("nc_inq_enum", nc_inq_enum(grpid, xtype, name_p, baseType_p, baseSize_p, nmembers_p))

    val baseType = baseType_p[C_INT, 0]
    val baseSize = baseSize_p[C_LONG, 0]
    val nmembers = nmembers_p[C_LONG, 0]
    val values = mutableMapOf<Int, String>()
    for (i in 0 until nmembers.toInt()) {
        val value_p = session.allocate(C_INT)
        checkErr("nc_inq_enum_member", nc_inq_enum_member(grpid, xtype, i, name_p, value_p))
        val mname = name_p.getUtf8String(0)
        val value = value_p[C_INT, 0]
        values[value] = mname
    }
    return values
}

internal fun NCheader.readUserAttributeValues(session: MemorySession, grpid: Int, varid: Int, attname: String,
                                              userType: UserType, nelems: Long
): Attribute.Builder {
    return when (userType.typeClass) {
        NC_ENUM() -> readEnumAttValues(session, grpid, varid, attname, nelems, userType)
        NC_OPAQUE() -> readOpaqueAttValues(session, grpid, varid, attname, nelems, userType)
        NC_VLEN() -> readVlenAttValues(session, grpid, varid, attname, nelems, userType)
        //NC_COMPOUND() -> readCompoundAttValues(grpid, varid, attname, nelems, userType)
        else -> throw RuntimeException("Unsupported user attribute data type == ${userType.typeClass}")
    }
}

// Just flatten, attributes only support 1D arrays. Note need to do all array types
@Throws(IOException::class)
internal fun NCheader.readVlenAttValues(session: MemorySession, grpid: Int, varid: Int, attname: String, nelems: Long,
                                        userType: UserType
): Attribute.Builder {
    val attb = Attribute.Builder().setName(attname).setDataType(DataType.SEQUENCE)

    // fetch nelems of nc_vlen_t struct
    val attname_p: MemorySegment = session.allocateUtf8String(attname)
    val vlen_p = nc_vlen_t.allocateArray(nelems.toInt(), session)
    checkErr("nc_get_att", nc_get_att(grpid, varid, attname_p, vlen_p))

    // count the total
    var total = 0L
    for (index in 0 until nelems) {
        total += nc_vlen_t.getLength(vlen_p, index)
    }

    val dataType = getDataType(userType.baseTypeid)
    if (dataType == DataType.FLOAT) {
        val parray = mutableListOf<Float>()
        for (elem in 0 until nelems) {
            val count = nc_vlen_t.getLength(vlen_p, elem)
            val address: MemoryAddress = nc_vlen_t.getAddress(vlen_p, elem)
            for (idx in 0 until count) {
                val wtf = address.getAtIndex(C_FLOAT, idx)
                parray.add(wtf)
            }
        }
        attb.values = parray
    }

    return attb
}

@Throws(IOException::class)
private fun readOpaqueAttValues(session: MemorySession, grpid: Int, varid: Int, attname: String, nelems: Long, userType: UserType
): Attribute.Builder {
    val attb = Attribute.Builder().setName(attname).setDataType(DataType.OPAQUE)

    val attname_p: MemorySegment = session.allocateUtf8String(attname)
    val val_p = session.allocate(nelems * userType.size)

    // apparently have to call nc_get_att(), not readAttributeValues()
    checkErr("nc_get_att", nc_get_att(grpid, varid, attname_p, val_p))
    val raw = val_p.toArray(ValueLayout.JAVA_BYTE)
    val bb = ByteBuffer.wrap(raw)

    attb.values = listOf(bb)
    return attb
}

@Throws(IOException::class)
private fun NCheader.readEnumAttValues(session: MemorySession, grpid: Int, varid: Int, attname: String, nelems: Long, userType: UserType): Attribute.Builder {
    val attb = Attribute.Builder().setName(attname).setDataType(userType.enumBaseType)

    val attname_p: MemorySegment = session.allocateUtf8String(attname)
    val val_p = session.allocate(nelems)

    // apparently have to call nc_get_att(), not readAttributeValues()
    checkErr("nc_get_att", nc_get_att(grpid, varid, attname_p, val_p))
    val raw = val_p.toArray(ValueLayout.JAVA_BYTE)
    val bb = ByteBuffer.wrap(raw)

    val result = mutableListOf<String>()
    val map = userType.enumTypdef!!.values
    for (i in 0 until nelems.toInt()) {
        val num = when (userType.enumBasePrimitiveType) {
            DataType.UBYTE -> bb.get(i).toUByte().toInt()
            DataType.USHORT -> bb.getShort(i).toUShort().toInt()
            DataType.UINT -> bb.getInt(i)
            else -> throw RuntimeException("convertEnums unknown type = ${userType.enumBasePrimitiveType}")
        }
        val s = map[num] ?: throw RuntimeException("convertEnums unknown num = ${num}")
        result.add(s)
    }
    attb.values = result
    return attb
}

internal class UserType(session: MemorySession,
                        val grpid: Int,
                        val typeid: Int,
                        val name: String,
                        size: Long, // the size of the user defined type
                        val baseTypeid: Int, // the base typeid for vlen and enum types
                        val nfields: Long, // the number of fields for enum and compound types
                        val typeClass: Int // the class of the user defined type: NC_VLEN, NC_OPAQUE, NC_ENUM, or NC_COMPOUND.
) {
    var size : Int // the size of the user defined type
    var enumTypdef: EnumTypedef? = null
    var flds = mutableListOf<CompoundField>()

    init {
        this.size = size.toInt()
        if (typeClass == NC_COMPOUND()) {
            readFields(session)
        }
    }

    // Allow size override for e.g. opaque
    fun setSize(size: Int): UserType {
        this.size = size
        return this
    }

    val enumBaseType: DataType
        get() {
            // set the enum's basetype
            if (baseTypeid in 1..NC_MAX_ATOMIC_TYPE()) {
                val cdmtype = when (baseTypeid) {
                    NC_CHAR(), NC_UBYTE(), NC_BYTE() -> DataType.ENUM1
                    NC_USHORT(), NC_SHORT() -> DataType.ENUM2
                    NC_UINT(), NC_INT() -> DataType.ENUM4
                    else -> throw RuntimeException("enumBaseType illegal = $baseTypeid")
                }
                return cdmtype
            }
            throw RuntimeException("enumBaseType illegal = $baseTypeid")
        }

    val enumBasePrimitiveType: DataType
        get() {
            // set the enum's basetype
            if (baseTypeid in 1..NC_MAX_ATOMIC_TYPE()) {
                val cdmtype = when (baseTypeid) {
                    NC_CHAR(), NC_UBYTE(), NC_BYTE() -> DataType.UBYTE
                    NC_USHORT(), NC_SHORT() -> DataType.USHORT
                    NC_UINT(), NC_INT() -> DataType.UINT
                    else -> throw RuntimeException("enumBaseType illegal = $baseTypeid")
                }
                return cdmtype
            }
            throw RuntimeException("enumBaseType illegal = $baseTypeid")
        }


    fun addField(fld: CompoundField) {
        flds.add(fld)
    }

    fun setFields(flds: List<CompoundField>) {
        this.flds.addAll(flds)
    }

    override fun toString(): String {
        return ("UserType" + "{grpid=" + grpid + ", typeid=" + typeid + ", name='" + name + '\'' + ", size=" + size
                + ", baseTypeid=" + baseTypeid + ", nfields=" + nfields + ", typeClass=" + typeClass + '}')
    }

    @Throws(IOException::class)
    fun readFields(session: MemorySession) {
        for (fldidx in 0 until nfields.toInt()) {
            val fldname_p: MemorySegment = session.allocate(NC_MAX_NAME().toLong() + 1)
            val offset_p = session.allocate(C_LONG, 0)
            val fldtypeid_p = session.allocate(C_INT, 0)
            val ndims_p = session.allocate(C_INT, 0)

            val dims_p = session.allocateArray(C_INT, NC_MAX_DIMS().toLong())
            checkErr("nc_inq_compound_field",
                nc_inq_compound_field(grpid, typeid, fldidx, fldname_p, offset_p, fldtypeid_p, ndims_p, dims_p)
            )
            val fldname: String = fldname_p.getUtf8String(0)
            val offset = offset_p[C_LONG, 0]
            val fldtypeid = fldtypeid_p[C_INT, 0]
            val ndims = ndims_p[C_INT, 0]
            val dims = IntArray(ndims) {idx -> dims_p.getAtIndex(C_INT, idx.toLong())}

            addField(CompoundField(grpid, typeid, fldidx, fldname, offset.toInt(), fldtypeid, ndims, dims))
        }
    }
}

// A field in a compound type
internal class CompoundField(
    val grpid: Int,
    val typeid: Int,
    val fldidx: Int,
    val name: String,
    val offset: Int,
    val fldtypeid: Int,
    val ndims: Int,
    val dims: IntArray
) {
    var ctype: NCheader.ConvertedType? = null // wtf?

    // int total_size;
    var data: List<Array<*>>? = null

    init {
        // ctype = convertArrayType(fldtypeid)
        /* if (isVlen(fldtypeid)) {
            val edims = IntArray(ndims + 1)
            if (ndims > 0) {
                System.arraycopy(dimz, 0, edims, 0, ndims)
            }
            edims[ndims] = -1
            dims = edims
            this.ndims++
        } */
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o == null || javaClass != o.javaClass) {
            return false
        }
        val field = o as CompoundField
        return (grpid == field.grpid && typeid == field.typeid && fldidx == field.fldidx && offset == field.offset && fldtypeid == field.fldtypeid && ndims == field.ndims && name == field.name
                && Arrays.equals(dims, field.dims))
    }

    override fun hashCode(): Int {
        return Objects.hash(grpid, typeid, fldidx, name, offset, fldtypeid, ndims, dims)
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("Field")
        sb.append("{grpid=").append(grpid)
        sb.append(", typeid=").append(typeid)
        sb.append(", fldidx=").append(fldidx)
        sb.append(", name='").append(name).append('\'')
        sb.append(", offset=").append(offset)
        sb.append(", fldtypeid=").append(fldtypeid)
        sb.append(", ndims=").append(ndims)
        sb.append(", dims=").append(if (dims == null) "null" else "")
        var i = 0
        while (dims != null && i < dims!!.size) {
            sb.append(if (i == 0) "" else ", ").append(dims!![i])
            ++i
        }
        sb.append(", dtype=").append(ctype?.dt)
        // if (ctype.isVlen) sb.append("(vlen)")
        sb.append('}')
        return sb.toString()
    }

    /*
    @Throws(IOException::class)
    fun makeMemberVariable(g: Group.Builder?): Variable.Builder {
        val v = readVariable(g, name, fldtypeid, "")
        v.setDimensionsAnonymous(dims)
        return v
    }

     */
}