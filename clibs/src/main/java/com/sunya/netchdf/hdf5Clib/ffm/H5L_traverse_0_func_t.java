// Generated by jextract

package com.sunya.netchdf.hdf5Clib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
public interface H5L_traverse_0_func_t {

    long apply(java.lang.foreign.MemoryAddress link_name, long cur_group, java.lang.foreign.MemoryAddress lnkdata, long lnkdata_size, long lapl_id);
    static MemorySegment allocate(H5L_traverse_0_func_t fi, MemorySession session) {
        return RuntimeHelper.upcallStub(H5L_traverse_0_func_t.class, fi, constants$36.H5L_traverse_0_func_t$FUNC, session);
    }
    static H5L_traverse_0_func_t ofAddress(MemoryAddress addr, MemorySession session) {
        MemorySegment symbol = MemorySegment.ofAddress(addr, 0, session);
        return (java.lang.foreign.MemoryAddress _link_name, long _cur_group, java.lang.foreign.MemoryAddress _lnkdata, long _lnkdata_size, long _lapl_id) -> {
            try {
                return (long)constants$36.H5L_traverse_0_func_t$MH.invokeExact((Addressable)symbol, (java.lang.foreign.Addressable)_link_name, _cur_group, (java.lang.foreign.Addressable)_lnkdata, _lnkdata_size, _lapl_id);
            } catch (Throwable ex$) {
                throw new AssertionError("should not reach here", ex$);
            }
        };
    }
}

