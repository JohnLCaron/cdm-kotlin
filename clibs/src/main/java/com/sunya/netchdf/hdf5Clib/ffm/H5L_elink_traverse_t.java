// Generated by jextract

package com.sunya.netchdf.hdf5Clib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
public interface H5L_elink_traverse_t {

    int apply(java.lang.foreign.MemoryAddress parent_file_name, java.lang.foreign.MemoryAddress parent_group_name, java.lang.foreign.MemoryAddress child_file_name, java.lang.foreign.MemoryAddress child_object_name, java.lang.foreign.MemoryAddress acc_flags, long fapl_id, java.lang.foreign.MemoryAddress op_data);
    static MemorySegment allocate(H5L_elink_traverse_t fi, MemorySession session) {
        return RuntimeHelper.upcallStub(H5L_elink_traverse_t.class, fi, constants$37.H5L_elink_traverse_t$FUNC, session);
    }
    static H5L_elink_traverse_t ofAddress(MemoryAddress addr, MemorySession session) {
        MemorySegment symbol = MemorySegment.ofAddress(addr, 0, session);
        return (java.lang.foreign.MemoryAddress _parent_file_name, java.lang.foreign.MemoryAddress _parent_group_name, java.lang.foreign.MemoryAddress _child_file_name, java.lang.foreign.MemoryAddress _child_object_name, java.lang.foreign.MemoryAddress _acc_flags, long _fapl_id, java.lang.foreign.MemoryAddress _op_data) -> {
            try {
                return (int)constants$37.H5L_elink_traverse_t$MH.invokeExact((Addressable)symbol, (java.lang.foreign.Addressable)_parent_file_name, (java.lang.foreign.Addressable)_parent_group_name, (java.lang.foreign.Addressable)_child_file_name, (java.lang.foreign.Addressable)_child_object_name, (java.lang.foreign.Addressable)_acc_flags, _fapl_id, (java.lang.foreign.Addressable)_op_data);
            } catch (Throwable ex$) {
                throw new AssertionError("should not reach here", ex$);
            }
        };
    }
}


