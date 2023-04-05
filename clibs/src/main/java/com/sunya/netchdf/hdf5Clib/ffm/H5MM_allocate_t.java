// Generated by jextract

package com.sunya.netchdf.hdf5Clib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
public interface H5MM_allocate_t {

    java.lang.foreign.Addressable apply(long size, java.lang.foreign.MemoryAddress alloc_info);
    static MemorySegment allocate(H5MM_allocate_t fi, MemorySession session) {
        return RuntimeHelper.upcallStub(H5MM_allocate_t.class, fi, constants$124.H5MM_allocate_t$FUNC, session);
    }
    static H5MM_allocate_t ofAddress(MemoryAddress addr, MemorySession session) {
        MemorySegment symbol = MemorySegment.ofAddress(addr, 0, session);
        return (long _size, java.lang.foreign.MemoryAddress _alloc_info) -> {
            try {
                return (java.lang.foreign.Addressable)(java.lang.foreign.MemoryAddress)constants$124.H5MM_allocate_t$MH.invokeExact((Addressable)symbol, _size, (java.lang.foreign.Addressable)_alloc_info);
            } catch (Throwable ex$) {
                throw new AssertionError("should not reach here", ex$);
            }
        };
    }
}


