// Generated by jextract

package com.sunya.netchdf.hdf5Clib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
public interface H5D_operator_t {

    int apply(java.lang.foreign.MemoryAddress elem, long type_id, int ndim, java.lang.foreign.MemoryAddress point, java.lang.foreign.MemoryAddress operator_data);
    static MemorySegment allocate(H5D_operator_t fi, MemorySession session) {
        return RuntimeHelper.upcallStub(H5D_operator_t.class, fi, constants$54.H5D_operator_t$FUNC, session);
    }
    static H5D_operator_t ofAddress(MemoryAddress addr, MemorySession session) {
        MemorySegment symbol = MemorySegment.ofAddress(addr, 0, session);
        return (java.lang.foreign.MemoryAddress _elem, long _type_id, int _ndim, java.lang.foreign.MemoryAddress _point, java.lang.foreign.MemoryAddress _operator_data) -> {
            try {
                return (int)constants$54.H5D_operator_t$MH.invokeExact((Addressable)symbol, (java.lang.foreign.Addressable)_elem, _type_id, _ndim, (java.lang.foreign.Addressable)_point, (java.lang.foreign.Addressable)_operator_data);
            } catch (Throwable ex$) {
                throw new AssertionError("should not reach here", ex$);
            }
        };
    }
}


