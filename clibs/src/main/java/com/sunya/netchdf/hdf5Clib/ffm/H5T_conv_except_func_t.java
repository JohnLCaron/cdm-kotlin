// Generated by jextract

package com.sunya.netchdf.hdf5Clib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
public interface H5T_conv_except_func_t {

    int apply(int except_type, long src_id, long dst_id, java.lang.foreign.MemoryAddress src_buf, java.lang.foreign.MemoryAddress dst_buf, java.lang.foreign.MemoryAddress user_data);
    static MemorySegment allocate(H5T_conv_except_func_t fi, MemorySession session) {
        return RuntimeHelper.upcallStub(H5T_conv_except_func_t.class, fi, constants$8.H5T_conv_except_func_t$FUNC, session);
    }
    static H5T_conv_except_func_t ofAddress(MemoryAddress addr, MemorySession session) {
        MemorySegment symbol = MemorySegment.ofAddress(addr, 0, session);
        return (int _except_type, long _src_id, long _dst_id, java.lang.foreign.MemoryAddress _src_buf, java.lang.foreign.MemoryAddress _dst_buf, java.lang.foreign.MemoryAddress _user_data) -> {
            try {
                return (int)constants$8.H5T_conv_except_func_t$MH.invokeExact((Addressable)symbol, _except_type, _src_id, _dst_id, (java.lang.foreign.Addressable)_src_buf, (java.lang.foreign.Addressable)_dst_buf, (java.lang.foreign.Addressable)_user_data);
            } catch (Throwable ex$) {
                throw new AssertionError("should not reach here", ex$);
            }
        };
    }
}

