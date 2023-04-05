// Generated by jextract

package com.sunya.netchdf.hdf5Clib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
public class H5F_retry_info_t {

    static final  GroupLayout $struct$LAYOUT = MemoryLayout.structLayout(
        Constants$root.C_INT$LAYOUT.withName("nbins"),
        MemoryLayout.paddingLayout(32),
        MemoryLayout.sequenceLayout(21, Constants$root.C_POINTER$LAYOUT).withName("retries")
    ).withName("H5F_retry_info_t");
    public static MemoryLayout $LAYOUT() {
        return H5F_retry_info_t.$struct$LAYOUT;
    }
    static final VarHandle nbins$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("nbins"));
    public static VarHandle nbins$VH() {
        return H5F_retry_info_t.nbins$VH;
    }
    public static int nbins$get(MemorySegment seg) {
        return (int)H5F_retry_info_t.nbins$VH.get(seg);
    }
    public static void nbins$set( MemorySegment seg, int x) {
        H5F_retry_info_t.nbins$VH.set(seg, x);
    }
    public static int nbins$get(MemorySegment seg, long index) {
        return (int)H5F_retry_info_t.nbins$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void nbins$set(MemorySegment seg, long index, int x) {
        H5F_retry_info_t.nbins$VH.set(seg.asSlice(index*sizeof()), x);
    }
    public static MemorySegment retries$slice(MemorySegment seg) {
        return seg.asSlice(8, 168);
    }
    public static long sizeof() { return $LAYOUT().byteSize(); }
    public static MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }
    public static MemorySegment allocateArray(int len, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));
    }
    public static MemorySegment ofAddress(MemoryAddress addr, MemorySession session) { return RuntimeHelper.asArray(addr, $LAYOUT(), 1, session); }
}


