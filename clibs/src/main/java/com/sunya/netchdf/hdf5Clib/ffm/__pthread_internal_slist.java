// Generated by jextract

package com.sunya.netchdf.hdf5Clib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
public class __pthread_internal_slist {

    static final  GroupLayout $struct$LAYOUT = MemoryLayout.structLayout(
        Constants$root.C_POINTER$LAYOUT.withName("__next")
    ).withName("__pthread_internal_slist");
    public static MemoryLayout $LAYOUT() {
        return __pthread_internal_slist.$struct$LAYOUT;
    }
    static final VarHandle __next$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("__next"));
    public static VarHandle __next$VH() {
        return __pthread_internal_slist.__next$VH;
    }
    public static MemoryAddress __next$get(MemorySegment seg) {
        return (java.lang.foreign.MemoryAddress)__pthread_internal_slist.__next$VH.get(seg);
    }
    public static void __next$set( MemorySegment seg, MemoryAddress x) {
        __pthread_internal_slist.__next$VH.set(seg, x);
    }
    public static MemoryAddress __next$get(MemorySegment seg, long index) {
        return (java.lang.foreign.MemoryAddress)__pthread_internal_slist.__next$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void __next$set(MemorySegment seg, long index, MemoryAddress x) {
        __pthread_internal_slist.__next$VH.set(seg.asSlice(index*sizeof()), x);
    }
    public static long sizeof() { return $LAYOUT().byteSize(); }
    public static MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }
    public static MemorySegment allocateArray(int len, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));
    }
    public static MemorySegment ofAddress(MemoryAddress addr, MemorySession session) { return RuntimeHelper.asArray(addr, $LAYOUT(), 1, session); }
}


