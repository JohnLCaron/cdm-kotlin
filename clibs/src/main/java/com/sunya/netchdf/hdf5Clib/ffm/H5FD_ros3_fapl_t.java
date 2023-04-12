// Generated by jextract

package com.sunya.netchdf.hdf5Clib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
public class H5FD_ros3_fapl_t {

    static final  GroupLayout $struct$LAYOUT = MemoryLayout.structLayout(
        Constants$root.C_INT$LAYOUT.withName("version"),
        Constants$root.C_BOOL$LAYOUT.withName("authenticate"),
        MemoryLayout.sequenceLayout(33, Constants$root.C_CHAR$LAYOUT).withName("aws_region"),
        MemoryLayout.sequenceLayout(129, Constants$root.C_CHAR$LAYOUT).withName("secret_id"),
        MemoryLayout.sequenceLayout(129, Constants$root.C_CHAR$LAYOUT).withName("secret_key")
    ).withName("H5FD_ros3_fapl_t");
    public static MemoryLayout $LAYOUT() {
        return H5FD_ros3_fapl_t.$struct$LAYOUT;
    }
    static final VarHandle version$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("version"));
    public static VarHandle version$VH() {
        return H5FD_ros3_fapl_t.version$VH;
    }
    public static int version$get(MemorySegment seg) {
        return (int)H5FD_ros3_fapl_t.version$VH.get(seg);
    }
    public static void version$set( MemorySegment seg, int x) {
        H5FD_ros3_fapl_t.version$VH.set(seg, x);
    }
    public static int version$get(MemorySegment seg, long index) {
        return (int)H5FD_ros3_fapl_t.version$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void version$set(MemorySegment seg, long index, int x) {
        H5FD_ros3_fapl_t.version$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle authenticate$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("authenticate"));
    public static VarHandle authenticate$VH() {
        return H5FD_ros3_fapl_t.authenticate$VH;
    }
    public static boolean authenticate$get(MemorySegment seg) {
        return (boolean)H5FD_ros3_fapl_t.authenticate$VH.get(seg);
    }
    public static void authenticate$set( MemorySegment seg, boolean x) {
        H5FD_ros3_fapl_t.authenticate$VH.set(seg, x);
    }
    public static boolean authenticate$get(MemorySegment seg, long index) {
        return (boolean)H5FD_ros3_fapl_t.authenticate$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void authenticate$set(MemorySegment seg, long index, boolean x) {
        H5FD_ros3_fapl_t.authenticate$VH.set(seg.asSlice(index*sizeof()), x);
    }
    public static MemorySegment aws_region$slice(MemorySegment seg) {
        return seg.asSlice(5, 33);
    }
    public static MemorySegment secret_id$slice(MemorySegment seg) {
        return seg.asSlice(38, 129);
    }
    public static MemorySegment secret_key$slice(MemorySegment seg) {
        return seg.asSlice(167, 129);
    }
    public static long sizeof() { return $LAYOUT().byteSize(); }
    public static MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }
    public static MemorySegment allocateArray(int len, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));
    }
    public static MemorySegment ofAddress(MemoryAddress addr, MemorySession session) { return RuntimeHelper.asArray(addr, $LAYOUT(), 1, session); }
}

