// Generated by jextract

package com.sunya.netchdf.mfhdfClib.ffm;

import java.lang.invoke.VarHandle;
import java.lang.foreign.*;

public class hdf_varlist {

    static final  GroupLayout $struct$LAYOUT = MemoryLayout.structLayout(
        Constants$root.C_INT$LAYOUT.withName("var_index"),
        Constants$root.C_INT$LAYOUT.withName("var_type")
    ).withName("hdf_varlist");
    public static MemoryLayout $LAYOUT() {
        return hdf_varlist.$struct$LAYOUT;
    }
    static final VarHandle var_index$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("var_index"));
    public static VarHandle var_index$VH() {
        return hdf_varlist.var_index$VH;
    }
    public static int var_index$get(MemorySegment seg) {
        return (int)hdf_varlist.var_index$VH.get(seg);
    }
    public static void var_index$set( MemorySegment seg, int x) {
        hdf_varlist.var_index$VH.set(seg, x);
    }
    public static int var_index$get(MemorySegment seg, long index) {
        return (int)hdf_varlist.var_index$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void var_index$set(MemorySegment seg, long index, int x) {
        hdf_varlist.var_index$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle var_type$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("var_type"));
    public static VarHandle var_type$VH() {
        return hdf_varlist.var_type$VH;
    }
    public static int var_type$get(MemorySegment seg) {
        return (int)hdf_varlist.var_type$VH.get(seg);
    }
    public static void var_type$set( MemorySegment seg, int x) {
        hdf_varlist.var_type$VH.set(seg, x);
    }
    public static int var_type$get(MemorySegment seg, long index) {
        return (int)hdf_varlist.var_type$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void var_type$set(MemorySegment seg, long index, int x) {
        hdf_varlist.var_type$VH.set(seg.asSlice(index*sizeof()), x);
    }
    public static long sizeof() { return $LAYOUT().byteSize(); }
    public static MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }
    public static MemorySegment allocateArray(int len, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));
    }
    public static MemorySegment ofAddress(MemoryAddress addr, MemorySession session) { return RuntimeHelper.asArray(addr, $LAYOUT(), 1, session); }
}

