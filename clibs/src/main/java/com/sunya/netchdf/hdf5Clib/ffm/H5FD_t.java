// Generated by jextract

package com.sunya.netchdf.hdf5Clib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
public class H5FD_t {

    static final  GroupLayout $struct$LAYOUT = MemoryLayout.structLayout(
        Constants$root.C_LONG_LONG$LAYOUT.withName("driver_id"),
        Constants$root.C_POINTER$LAYOUT.withName("cls"),
        Constants$root.C_LONG_LONG$LAYOUT.withName("fileno"),
        Constants$root.C_INT$LAYOUT.withName("access_flags"),
        MemoryLayout.paddingLayout(32),
        Constants$root.C_LONG_LONG$LAYOUT.withName("feature_flags"),
        Constants$root.C_LONG_LONG$LAYOUT.withName("maxaddr"),
        Constants$root.C_LONG_LONG$LAYOUT.withName("base_addr"),
        Constants$root.C_LONG_LONG$LAYOUT.withName("threshold"),
        Constants$root.C_LONG_LONG$LAYOUT.withName("alignment"),
        Constants$root.C_BOOL$LAYOUT.withName("paged_aggr"),
        MemoryLayout.paddingLayout(56)
    ).withName("H5FD_t");
    public static MemoryLayout $LAYOUT() {
        return H5FD_t.$struct$LAYOUT;
    }
    static final VarHandle driver_id$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("driver_id"));
    public static VarHandle driver_id$VH() {
        return H5FD_t.driver_id$VH;
    }
    public static long driver_id$get(MemorySegment seg) {
        return (long)H5FD_t.driver_id$VH.get(seg);
    }
    public static void driver_id$set( MemorySegment seg, long x) {
        H5FD_t.driver_id$VH.set(seg, x);
    }
    public static long driver_id$get(MemorySegment seg, long index) {
        return (long)H5FD_t.driver_id$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void driver_id$set(MemorySegment seg, long index, long x) {
        H5FD_t.driver_id$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle cls$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("cls"));
    public static VarHandle cls$VH() {
        return H5FD_t.cls$VH;
    }
    public static MemoryAddress cls$get(MemorySegment seg) {
        return (java.lang.foreign.MemoryAddress)H5FD_t.cls$VH.get(seg);
    }
    public static void cls$set( MemorySegment seg, MemoryAddress x) {
        H5FD_t.cls$VH.set(seg, x);
    }
    public static MemoryAddress cls$get(MemorySegment seg, long index) {
        return (java.lang.foreign.MemoryAddress)H5FD_t.cls$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void cls$set(MemorySegment seg, long index, MemoryAddress x) {
        H5FD_t.cls$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle fileno$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("fileno"));
    public static VarHandle fileno$VH() {
        return H5FD_t.fileno$VH;
    }
    public static long fileno$get(MemorySegment seg) {
        return (long)H5FD_t.fileno$VH.get(seg);
    }
    public static void fileno$set( MemorySegment seg, long x) {
        H5FD_t.fileno$VH.set(seg, x);
    }
    public static long fileno$get(MemorySegment seg, long index) {
        return (long)H5FD_t.fileno$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void fileno$set(MemorySegment seg, long index, long x) {
        H5FD_t.fileno$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle access_flags$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("access_flags"));
    public static VarHandle access_flags$VH() {
        return H5FD_t.access_flags$VH;
    }
    public static int access_flags$get(MemorySegment seg) {
        return (int)H5FD_t.access_flags$VH.get(seg);
    }
    public static void access_flags$set( MemorySegment seg, int x) {
        H5FD_t.access_flags$VH.set(seg, x);
    }
    public static int access_flags$get(MemorySegment seg, long index) {
        return (int)H5FD_t.access_flags$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void access_flags$set(MemorySegment seg, long index, int x) {
        H5FD_t.access_flags$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle feature_flags$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("feature_flags"));
    public static VarHandle feature_flags$VH() {
        return H5FD_t.feature_flags$VH;
    }
    public static long feature_flags$get(MemorySegment seg) {
        return (long)H5FD_t.feature_flags$VH.get(seg);
    }
    public static void feature_flags$set( MemorySegment seg, long x) {
        H5FD_t.feature_flags$VH.set(seg, x);
    }
    public static long feature_flags$get(MemorySegment seg, long index) {
        return (long)H5FD_t.feature_flags$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void feature_flags$set(MemorySegment seg, long index, long x) {
        H5FD_t.feature_flags$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle maxaddr$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("maxaddr"));
    public static VarHandle maxaddr$VH() {
        return H5FD_t.maxaddr$VH;
    }
    public static long maxaddr$get(MemorySegment seg) {
        return (long)H5FD_t.maxaddr$VH.get(seg);
    }
    public static void maxaddr$set( MemorySegment seg, long x) {
        H5FD_t.maxaddr$VH.set(seg, x);
    }
    public static long maxaddr$get(MemorySegment seg, long index) {
        return (long)H5FD_t.maxaddr$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void maxaddr$set(MemorySegment seg, long index, long x) {
        H5FD_t.maxaddr$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle base_addr$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("base_addr"));
    public static VarHandle base_addr$VH() {
        return H5FD_t.base_addr$VH;
    }
    public static long base_addr$get(MemorySegment seg) {
        return (long)H5FD_t.base_addr$VH.get(seg);
    }
    public static void base_addr$set( MemorySegment seg, long x) {
        H5FD_t.base_addr$VH.set(seg, x);
    }
    public static long base_addr$get(MemorySegment seg, long index) {
        return (long)H5FD_t.base_addr$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void base_addr$set(MemorySegment seg, long index, long x) {
        H5FD_t.base_addr$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle threshold$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("threshold"));
    public static VarHandle threshold$VH() {
        return H5FD_t.threshold$VH;
    }
    public static long threshold$get(MemorySegment seg) {
        return (long)H5FD_t.threshold$VH.get(seg);
    }
    public static void threshold$set( MemorySegment seg, long x) {
        H5FD_t.threshold$VH.set(seg, x);
    }
    public static long threshold$get(MemorySegment seg, long index) {
        return (long)H5FD_t.threshold$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void threshold$set(MemorySegment seg, long index, long x) {
        H5FD_t.threshold$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle alignment$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("alignment"));
    public static VarHandle alignment$VH() {
        return H5FD_t.alignment$VH;
    }
    public static long alignment$get(MemorySegment seg) {
        return (long)H5FD_t.alignment$VH.get(seg);
    }
    public static void alignment$set( MemorySegment seg, long x) {
        H5FD_t.alignment$VH.set(seg, x);
    }
    public static long alignment$get(MemorySegment seg, long index) {
        return (long)H5FD_t.alignment$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void alignment$set(MemorySegment seg, long index, long x) {
        H5FD_t.alignment$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle paged_aggr$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("paged_aggr"));
    public static VarHandle paged_aggr$VH() {
        return H5FD_t.paged_aggr$VH;
    }
    public static boolean paged_aggr$get(MemorySegment seg) {
        return (boolean)H5FD_t.paged_aggr$VH.get(seg);
    }
    public static void paged_aggr$set( MemorySegment seg, boolean x) {
        H5FD_t.paged_aggr$VH.set(seg, x);
    }
    public static boolean paged_aggr$get(MemorySegment seg, long index) {
        return (boolean)H5FD_t.paged_aggr$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void paged_aggr$set(MemorySegment seg, long index, boolean x) {
        H5FD_t.paged_aggr$VH.set(seg.asSlice(index*sizeof()), x);
    }
    public static long sizeof() { return $LAYOUT().byteSize(); }
    public static MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }
    public static MemorySegment allocateArray(int len, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));
    }
    public static MemorySegment ofAddress(MemoryAddress addr, MemorySession session) { return RuntimeHelper.asArray(addr, $LAYOUT(), 1, session); }
}

