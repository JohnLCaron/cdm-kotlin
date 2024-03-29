// Generated by jextract

package com.sunya.netchdf.hdf5Clib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
public class H5T_cdata_t {

    static final  GroupLayout $struct$LAYOUT = MemoryLayout.structLayout(
        Constants$root.C_INT$LAYOUT.withName("command"),
        Constants$root.C_INT$LAYOUT.withName("need_bkg"),
        Constants$root.C_BOOL$LAYOUT.withName("recalc"),
        MemoryLayout.paddingLayout(56),
        Constants$root.C_POINTER$LAYOUT.withName("priv")
    ).withName("H5T_cdata_t");
    public static MemoryLayout $LAYOUT() {
        return H5T_cdata_t.$struct$LAYOUT;
    }
    static final VarHandle command$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("command"));
    public static VarHandle command$VH() {
        return H5T_cdata_t.command$VH;
    }
    public static int command$get(MemorySegment seg) {
        return (int)H5T_cdata_t.command$VH.get(seg);
    }
    public static void command$set( MemorySegment seg, int x) {
        H5T_cdata_t.command$VH.set(seg, x);
    }
    public static int command$get(MemorySegment seg, long index) {
        return (int)H5T_cdata_t.command$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void command$set(MemorySegment seg, long index, int x) {
        H5T_cdata_t.command$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle need_bkg$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("need_bkg"));
    public static VarHandle need_bkg$VH() {
        return H5T_cdata_t.need_bkg$VH;
    }
    public static int need_bkg$get(MemorySegment seg) {
        return (int)H5T_cdata_t.need_bkg$VH.get(seg);
    }
    public static void need_bkg$set( MemorySegment seg, int x) {
        H5T_cdata_t.need_bkg$VH.set(seg, x);
    }
    public static int need_bkg$get(MemorySegment seg, long index) {
        return (int)H5T_cdata_t.need_bkg$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void need_bkg$set(MemorySegment seg, long index, int x) {
        H5T_cdata_t.need_bkg$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle recalc$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("recalc"));
    public static VarHandle recalc$VH() {
        return H5T_cdata_t.recalc$VH;
    }
    public static boolean recalc$get(MemorySegment seg) {
        return (boolean)H5T_cdata_t.recalc$VH.get(seg);
    }
    public static void recalc$set( MemorySegment seg, boolean x) {
        H5T_cdata_t.recalc$VH.set(seg, x);
    }
    public static boolean recalc$get(MemorySegment seg, long index) {
        return (boolean)H5T_cdata_t.recalc$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void recalc$set(MemorySegment seg, long index, boolean x) {
        H5T_cdata_t.recalc$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle priv$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("priv"));
    public static VarHandle priv$VH() {
        return H5T_cdata_t.priv$VH;
    }
    public static MemoryAddress priv$get(MemorySegment seg) {
        return (java.lang.foreign.MemoryAddress)H5T_cdata_t.priv$VH.get(seg);
    }
    public static void priv$set( MemorySegment seg, MemoryAddress x) {
        H5T_cdata_t.priv$VH.set(seg, x);
    }
    public static MemoryAddress priv$get(MemorySegment seg, long index) {
        return (java.lang.foreign.MemoryAddress)H5T_cdata_t.priv$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void priv$set(MemorySegment seg, long index, MemoryAddress x) {
        H5T_cdata_t.priv$VH.set(seg.asSlice(index*sizeof()), x);
    }
    public static long sizeof() { return $LAYOUT().byteSize(); }
    public static MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }
    public static MemorySegment allocateArray(int len, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));
    }
    public static MemorySegment ofAddress(MemoryAddress addr, MemorySession session) { return RuntimeHelper.asArray(addr, $LAYOUT(), 1, session); }
}


