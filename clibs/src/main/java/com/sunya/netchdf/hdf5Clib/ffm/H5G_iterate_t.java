// Generated by jextract

package com.sunya.netchdf.hdf5Clib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
public interface H5G_iterate_t {

    int apply(long group, java.lang.foreign.MemoryAddress name, java.lang.foreign.MemoryAddress op_data);
    static MemorySegment allocate(H5G_iterate_t fi, MemorySession session) {
        return RuntimeHelper.upcallStub(H5G_iterate_t.class, fi, constants$121.H5G_iterate_t$FUNC, session);
    }
    static H5G_iterate_t ofAddress(MemoryAddress addr, MemorySession session) {
        MemorySegment symbol = MemorySegment.ofAddress(addr, 0, session);
        return (long _group, java.lang.foreign.MemoryAddress _name, java.lang.foreign.MemoryAddress _op_data) -> {
            try {
                return (int)constants$121.H5G_iterate_t$MH.invokeExact((Addressable)symbol, _group, (java.lang.foreign.Addressable)_name, (java.lang.foreign.Addressable)_op_data);
            } catch (Throwable ex$) {
                throw new AssertionError("should not reach here", ex$);
            }
        };
    }
}


