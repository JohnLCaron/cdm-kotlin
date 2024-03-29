// Generated by jextract

package com.sunya.netchdf.hdf5Clib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
public class H5FD_free_t {

    static final  GroupLayout $struct$LAYOUT = MemoryLayout.structLayout(
        Constants$root.C_LONG_LONG$LAYOUT.withName("addr"),
        Constants$root.C_LONG_LONG$LAYOUT.withName("size"),
        Constants$root.C_POINTER$LAYOUT.withName("next")
    ).withName("H5FD_free_t");
    public static MemoryLayout $LAYOUT() {
        return H5FD_free_t.$struct$LAYOUT;
    }
    static final VarHandle addr$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("addr"));
    public static VarHandle addr$VH() {
        return H5FD_free_t.addr$VH;
    }
    public static long addr$get(MemorySegment seg) {
        return (long)H5FD_free_t.addr$VH.get(seg);
    }
    public static void addr$set( MemorySegment seg, long x) {
        H5FD_free_t.addr$VH.set(seg, x);
    }
    public static long addr$get(MemorySegment seg, long index) {
        return (long)H5FD_free_t.addr$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void addr$set(MemorySegment seg, long index, long x) {
        H5FD_free_t.addr$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle size$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("size"));
    public static VarHandle size$VH() {
        return H5FD_free_t.size$VH;
    }
    public static long size$get(MemorySegment seg) {
        return (long)H5FD_free_t.size$VH.get(seg);
    }
    public static void size$set( MemorySegment seg, long x) {
        H5FD_free_t.size$VH.set(seg, x);
    }
    public static long size$get(MemorySegment seg, long index) {
        return (long)H5FD_free_t.size$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void size$set(MemorySegment seg, long index, long x) {
        H5FD_free_t.size$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle next$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("next"));
    public static VarHandle next$VH() {
        return H5FD_free_t.next$VH;
    }
    public static MemoryAddress next$get(MemorySegment seg) {
        return (java.lang.foreign.MemoryAddress)H5FD_free_t.next$VH.get(seg);
    }
    public static void next$set( MemorySegment seg, MemoryAddress x) {
        H5FD_free_t.next$VH.set(seg, x);
    }
    public static MemoryAddress next$get(MemorySegment seg, long index) {
        return (java.lang.foreign.MemoryAddress)H5FD_free_t.next$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void next$set(MemorySegment seg, long index, MemoryAddress x) {
        H5FD_free_t.next$VH.set(seg.asSlice(index*sizeof()), x);
    }
    public static long sizeof() { return $LAYOUT().byteSize(); }
    public static MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }
    public static MemorySegment allocateArray(int len, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));
    }
    public static MemorySegment ofAddress(MemoryAddress addr, MemorySession session) { return RuntimeHelper.asArray(addr, $LAYOUT(), 1, session); }
}


