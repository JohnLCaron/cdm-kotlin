// Generated by jextract

package com.sunya.netchdf.hdf5Clib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
public interface H5L_copy_func_t {

    int apply(java.lang.foreign.MemoryAddress new_name, long new_loc, java.lang.foreign.MemoryAddress lnkdata, long lnkdata_size);
    static MemorySegment allocate(H5L_copy_func_t fi, MemorySession session) {
        return RuntimeHelper.upcallStub(H5L_copy_func_t.class, fi, constants$35.H5L_copy_func_t$FUNC, session);
    }
    static H5L_copy_func_t ofAddress(MemoryAddress addr, MemorySession session) {
        MemorySegment symbol = MemorySegment.ofAddress(addr, 0, session);
        return (java.lang.foreign.MemoryAddress _new_name, long _new_loc, java.lang.foreign.MemoryAddress _lnkdata, long _lnkdata_size) -> {
            try {
                return (int)constants$35.H5L_copy_func_t$MH.invokeExact((Addressable)symbol, (java.lang.foreign.Addressable)_new_name, _new_loc, (java.lang.foreign.Addressable)_lnkdata, _lnkdata_size);
            } catch (Throwable ex$) {
                throw new AssertionError("should not reach here", ex$);
            }
        };
    }
}


