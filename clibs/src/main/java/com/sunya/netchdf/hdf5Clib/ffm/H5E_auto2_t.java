// Generated by jextract

package com.sunya.netchdf.hdf5Clib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
public interface H5E_auto2_t {

    int apply(long estack, java.lang.foreign.MemoryAddress client_data);
    static MemorySegment allocate(H5E_auto2_t fi, MemorySession session) {
        return RuntimeHelper.upcallStub(H5E_auto2_t.class, fi, constants$104.H5E_auto2_t$FUNC, session);
    }
    static H5E_auto2_t ofAddress(MemoryAddress addr, MemorySession session) {
        MemorySegment symbol = MemorySegment.ofAddress(addr, 0, session);
        return (long _estack, java.lang.foreign.MemoryAddress _client_data) -> {
            try {
                return (int)constants$104.H5E_auto2_t$MH.invokeExact((Addressable)symbol, _estack, (java.lang.foreign.Addressable)_client_data);
            } catch (Throwable ex$) {
                throw new AssertionError("should not reach here", ex$);
            }
        };
    }
}

