// Generated by jextract

package com.sunya.netchdf.hdf5Clib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
public interface H5P_prp_get_func_t {

    int apply(long _x0, java.lang.foreign.MemoryAddress _x1, long _x2, java.lang.foreign.MemoryAddress _x3);
    static MemorySegment allocate(H5P_prp_get_func_t fi, MemorySession session) {
        return RuntimeHelper.upcallStub(H5P_prp_get_func_t.class, fi, constants$129.H5P_prp_get_func_t$FUNC, session);
    }
    static H5P_prp_get_func_t ofAddress(MemoryAddress addr, MemorySession session) {
        MemorySegment symbol = MemorySegment.ofAddress(addr, 0, session);
        return (long __x0, java.lang.foreign.MemoryAddress __x1, long __x2, java.lang.foreign.MemoryAddress __x3) -> {
            try {
                return (int)constants$129.H5P_prp_get_func_t$MH.invokeExact((Addressable)symbol, __x0, (java.lang.foreign.Addressable)__x1, __x2, (java.lang.foreign.Addressable)__x3);
            } catch (Throwable ex$) {
                throw new AssertionError("should not reach here", ex$);
            }
        };
    }
}


