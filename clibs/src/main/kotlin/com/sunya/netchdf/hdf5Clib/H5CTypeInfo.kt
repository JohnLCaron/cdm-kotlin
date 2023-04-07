package com.sunya.netchdf.hdf5Clib

import com.sunya.cdm.api.*
import com.sunya.cdm.array.StructureMember
import com.sunya.netchdf.hdf5.*
import com.sunya.netchdf.hdf5Clib.ffm.hdf5_h.*
import java.lang.foreign.MemoryLayout
import java.lang.foreign.ValueLayout
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal fun H5Cbuilder.readH5CTypeInfo (context : GroupContext, type_id : Long, name : String) : H5CTypeInfo {
    // H5T_class_t H5Tget_class	(	hid_t 	type_id	)
    val tclass = H5Tget_class(type_id)
    val datatype5 = Datatype5.of(tclass)

    if (datatype5 == Datatype5.Compound || datatype5 == Datatype5.Enumerated || datatype5 == Datatype5.Vlen || datatype5 == Datatype5.Opaque) {
        // "Determines whether two datatype identifiers refer to the same datatype"
        // htri_t H5Tequal	(	hid_t 	type1_id, hid_t 	type2_id)
        // is it object identity, or content identity or?
        val existing = findTypeFromId(type_id)
        if (existing?.typedef != null) {
            return registerTypedef(existing, context.group)
        }
    }

    // H5Tget_size() returns the size of a datatype in bytes.
    //
    // For atomic datatypes, array datatypes, compound datatypes, and other datatypes of a constant size, this is the size of the actual datatype in bytes.
    // For variable-length string datatypes this is the size of the pointer to the actual string, or sizeof(char *).
    //   This function does not return the size of actual variable-length string data.
    // For variable-length sequence datatypes (see H5Tvlen_create()), the returned value is the size of the
    //    hvl_t struct, or sizeof(hvl_t). The hvl_t struct contains a pointer to the actual data and a size value.
    //    This function does not return the size of actual variable-length sequence data.
    // size_t H5Tget_size	(	hid_t 	type_id	)
    val type_size = H5Tget_size(type_id).toInt()

    // H5T_sign_t H5Tget_sign	(	hid_t 	type_id	)
    val type_sign = H5Tget_sign(type_id) == 1 // unsigned == 0, signed == 1

    // H5T_order_t H5Tget_order	(	hid_t 	type_id	)
    val type_endian = if (H5Tget_order(type_id) == 0) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN

    if (datatype5 == Datatype5.Compound) {
        val members = mutableListOf<StructureMember>()
        val nmembers = H5Tget_nmembers(type_id)
        repeat(nmembers) {membno ->
            // char* H5Tget_member_name	(	hid_t 	type_id, unsigned 	membno)
            val mname_p = H5Tget_member_name(type_id, membno) // LOOK is this a memory leak?
            val mname = mname_p.getUtf8String(0)

            // size_t H5Tget_member_offset	(	hid_t 	type_id, unsigned 	membno)
            val moffset = H5Tget_member_offset(type_id, membno)

            // hid_t H5Tget_member_type	(	hid_t 	type_id, unsigned 	membno)
            val mtype_id = H5Tget_member_type(type_id, membno)
            var basetype = readH5CTypeInfo(context, mtype_id, mname)
            var dims = intArrayOf()
            if (basetype.datatype5 == Datatype5.Array) {
                dims = basetype.dims!!
                val base_type_id = H5Tget_super(mtype_id) // in case its an array??
                val basetype2 = readH5CTypeInfo(context, base_type_id, mname)
                basetype = basetype2
            }
            // val name: String, val datatype : Datatype, val offset: Int, val dims : IntArray
            members.add(StructureMember(mname, basetype.datatype(), moffset.toInt(), dims)) // assume scalar for the moment
        }

        val typedef = CompoundTypedef(name, members)
        val result =  H5CTypeInfo(type_id, tclass, type_size, type_sign, type_endian, typedef, null)
        return registerTypedef(result, context.group)
    }

    if (datatype5 == Datatype5.Enumerated) {
        val base_type_id = H5Tget_super(type_id)
        val basetype = readH5CTypeInfo(context, base_type_id, name)
        val datatype = basetype.datatype().withSignedness(false) // for implicity, always unsigned

        val nmembers = H5Tget_nmembers(type_id)
        val members = mutableMapOf<Int, String>()
        repeat(nmembers) { membno ->
            // char* H5Tget_member_name	(	hid_t 	type_id, unsigned 	membno)
            val ename_p = H5Tget_member_name(type_id, membno) // LOOK is this a memory leak?
            val ename = ename_p.getUtf8String(0)

            // herr_t H5Tget_member_value	(	hid_t 	type_id, unsigned 	membno, void * 	value)
            val value_p = context.session.allocate(type_size.toLong()) // assume that the elem_size gives you the base type
            checkErr("H5Tget_member_name", H5Tget_member_value(type_id, membno, value_p))
            val raw = value_p.toArray(ValueLayout.JAVA_BYTE)
            val bb = ByteBuffer.wrap(raw)
            bb.order(basetype.endian)
            val value = when (basetype.datatype()) {
                Datatype.BYTE -> bb.get(0).toInt()
                Datatype.UBYTE -> bb.get(0).toUByte().toInt()
                Datatype.SHORT -> bb.asShortBuffer().get(0).toInt()
                Datatype.USHORT -> bb.asShortBuffer().get(0).toUShort().toInt()
                Datatype.UINT, Datatype.INT -> bb.asIntBuffer().get(0)
                else -> throw RuntimeException("unsupported datatype ${basetype.datatype()}")
            }
            members.put(value, ename)
        }
        // EnumTypedef(name : String, baseType : Datatype, val values : Map<Int, String>)
        val typedef = EnumTypedef(name, datatype, members)
        val result = H5CTypeInfo(type_id, tclass, type_size, type_sign, type_endian, typedef)
        return registerTypedef(result, context.group)
    }

    // LOOK not registering Opaque typedef
    if (datatype5 == Datatype5.Opaque) {
        // char* H5Tget_tag	(	hid_t 	type	)
        val tag_p = H5Tget_tag(type_id)
        val tag = tag_p.getUtf8String(0) // LOOK should we allow typedefs to have attributes ?
        // class OpaqueTypedef(name : String, val elemSize : Int)
        val typedef = OpaqueTypedef(name, type_size) // LOOK not making into an opague typedef. TBD
        // context.group.addTypedef(typedef)
        return H5CTypeInfo(type_id, tclass, type_size, type_sign, type_endian, null)
    }

    // LOOK not registering Vlen typedef
    if (datatype5 == Datatype5.Vlen) {
        // hid_t H5Tget_super	(	hid_t 	type	)
        val base_type_id = H5Tget_super(type_id)
        val basetype = readH5CTypeInfo(context, base_type_id, name)

        // class VlenTypedef(name : String, baseType : Datatype)
        val typedef = VlenTypedef(name, basetype.datatype())
        val result = H5CTypeInfo(type_id, tclass, type_size, type_sign, type_endian, typedef, basetype)
        return result // registerTypedef(result, context.group)
    }

    if (datatype5 == Datatype5.Array) {
        val base_type_id = H5Tget_super(type_id)
        val basetype = readH5CTypeInfo(context, base_type_id, name)

        val dims_p = context.session.allocateArray(C_LONG as MemoryLayout, MAX_DIMS)
        val ndims = H5Tget_array_dims2(type_id, dims_p)
        val dims = IntArray(ndims) { dims_p.getAtIndex(C_LONG, it.toLong()).toInt() } // where to put this ??
        return H5CTypeInfo(type_id, tclass, type_size, type_sign, type_endian, null, basetype, dims)
    }

    // regular
    return H5CTypeInfo(type_id, tclass, type_size, type_sign, type_endian)
}

internal data class H5CTypeInfo(val type_id: Long, val type_class : Int, val elemSize : Int, val signed : Boolean, val endian : ByteOrder,
                           val typedef : Typedef?  = null, val base : H5CTypeInfo? = null, val dims : IntArray? = null) {
    val datatype5 = Datatype5.of(type_class)
    val isVlenString = H5Tis_variable_str(type_id) > 0

    // Call this after all the typedefs have been found
    fun datatype(): Datatype {
        return when (datatype5) {
            Datatype5.Fixed, Datatype5.BitField ->
                when (this.elemSize) {
                    1 -> Datatype.BYTE.withSignedness(signed)
                    2 -> Datatype.SHORT.withSignedness(signed)
                    4 -> Datatype.INT.withSignedness(signed)
                    8 -> Datatype.LONG.withSignedness(signed)
                    else -> throw RuntimeException("Bad hdf5 integer type ($datatype5) with size= ${this.elemSize}")
                }

            Datatype5.Floating ->
                when (this.elemSize) {
                    4 -> Datatype.FLOAT
                    8 -> Datatype.DOUBLE
                    else -> throw RuntimeException("Bad hdf5 float type with size= ${this.elemSize}")
                }

            Datatype5.Time -> Datatype.LONG.withSignedness(true) // LOOK use bitPrecision i suppose?
            Datatype5.String -> if ((isVlenString) or (elemSize > 1)) Datatype.STRING else Datatype.CHAR
            Datatype5.Reference -> Datatype.REFERENCE // "object" gets converted to dataset path, "region" ignored
            Datatype5.Opaque -> if (typedef != null) Datatype.OPAQUE.withTypedef(typedef) else Datatype.OPAQUE
            Datatype5.Compound -> Datatype.COMPOUND.withTypedef(typedef!!)
            Datatype5.Enumerated -> {
                when (this.elemSize) {
                    1 -> Datatype.ENUM1.withTypedef(typedef)
                    2 -> Datatype.ENUM2.withTypedef(typedef)
                    4 -> Datatype.ENUM4.withTypedef(typedef)
                    else -> throw RuntimeException("Bad hdf5 enum type with size= ${this.elemSize}")
                }
            }

            Datatype5.Vlen -> {
                if (isVlenString or this.base!!.isVlenString or (this.base.datatype5 == Datatype5.Reference)) Datatype.STRING
                else Datatype.VLEN.withTypedef(typedef)
            }

            Datatype5.Array -> {
                return this.base!!.datatype() // ??
            }
        }
    }
}

/** References from H5T.c
    /* Object reference (i.e. object header address in file) */
    H5T_INIT_TYPE(OBJREF, H5T_STD_REF_OBJ_g, ALLOC, -, SET, H5R_OBJ_REF_BUF_SIZE)
    objref = dt;    /* Keep type for later */

    /* Dataset Region reference (i.e. selection inside a dataset) */
    H5T_INIT_TYPE(REGREF, H5T_STD_REF_DSETREG_g, ALLOC, -, SET, H5R_DSET_REG_REF_BUF_SIZE)

    rbuf = malloc(sizeof(hobj_ref_t)*SPACE1_DIM1);
    // Read selection from disk
    ret=H5Dread(dataset, H5T_STD_REF_OBJ, H5S_ALL, H5S_ALL, H5P_DEFAULT, rbuf);
    // Open dataset object
    dset2 = H5Rdereference(dataset, H5R_OBJECT, &rbuf[0]);

   H5 is returning the name of the object, instead of the object. so "cdmUnitTest/formats/hdf5/msg/test.h5" has an
   attribute thats a reference, intended to point to another object. instead we are returning the name:
           :PALETTE = color_palette ;
   so i guess we have to add references? or read in the dataset and store into teh attribute??
   and then, there are several "color_palette" datasets in the file: ubyte color_palette(256, 3) with their own attributes.
    group: visualisation5 {
        variables:
            ubyte color_palette(256, 3) ;
                :PAL_COLORMODEL = "RGB" ;
                :PAL_TYPE = "STANDARD8" ;
                :PAL_VERSION = "1.2" ;
    }
   so would need the full path

   could just return the object name, except the name is in the link, not the object.
 */