// Generated by jextract

package sunya.cdm.netcdf4.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
public class nc_vlen_t {

    static final  GroupLayout $struct$LAYOUT = MemoryLayout.structLayout(
        Constants$root.C_LONG_LONG$LAYOUT.withName("len"),
        Constants$root.C_POINTER$LAYOUT.withName("p")
    );
    public static MemoryLayout $LAYOUT() {
        return nc_vlen_t.$struct$LAYOUT;
    }
    static final VarHandle len$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("len"));
    public static VarHandle len$VH() {
        return nc_vlen_t.len$VH;
    }
    public static long len$get(MemorySegment seg) {
        return (long)nc_vlen_t.len$VH.get(seg);
    }
    public static void len$set( MemorySegment seg, long x) {
        nc_vlen_t.len$VH.set(seg, x);
    }
    public static long len$get(MemorySegment seg, long index) {
        return (long)nc_vlen_t.len$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void len$set(MemorySegment seg, long index, long x) {
        nc_vlen_t.len$VH.set(seg.asSlice(index*sizeof()), x);
    }
    static final VarHandle p$VH = $struct$LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("p"));
    public static VarHandle p$VH() {
        return nc_vlen_t.p$VH;
    }
    public static MemoryAddress p$get(MemorySegment seg) {
        return (java.lang.foreign.MemoryAddress)nc_vlen_t.p$VH.get(seg);
    }
    public static void p$set( MemorySegment seg, MemoryAddress x) {
        nc_vlen_t.p$VH.set(seg, x);
    }
    public static MemoryAddress p$get(MemorySegment seg, long index) {
        return (java.lang.foreign.MemoryAddress)nc_vlen_t.p$VH.get(seg.asSlice(index*sizeof()));
    }
    public static void p$set(MemorySegment seg, long index, MemoryAddress x) {
        nc_vlen_t.p$VH.set(seg.asSlice(index*sizeof()), x);
    }
    public static long sizeof() { return $LAYOUT().byteSize(); }
    public static MemorySegment allocate(SegmentAllocator allocator) { return allocator.allocate($LAYOUT()); }
    public static MemorySegment allocateArray(int len, SegmentAllocator allocator) {
        return allocator.allocate(MemoryLayout.sequenceLayout(len, $LAYOUT()));
    }
    public static MemorySegment ofAddress(MemoryAddress addr, MemorySession session) { return RuntimeHelper.asArray(addr, $LAYOUT(), 1, session); }
}


