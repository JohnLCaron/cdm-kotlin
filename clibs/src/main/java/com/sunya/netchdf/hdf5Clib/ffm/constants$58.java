// Generated by jextract

package com.sunya.netchdf.hdf5Clib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
class constants$58 {

    static final FunctionDescriptor H5Dread_chunk$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle H5Dread_chunk$MH = RuntimeHelper.downcallHandle(
        "H5Dread_chunk",
        constants$58.H5Dread_chunk$FUNC
    );
    static final FunctionDescriptor H5Diterate$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle H5Diterate$MH = RuntimeHelper.downcallHandle(
        "H5Diterate",
        constants$58.H5Diterate$FUNC
    );
    static final FunctionDescriptor H5Dvlen_reclaim$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle H5Dvlen_reclaim$MH = RuntimeHelper.downcallHandle(
        "H5Dvlen_reclaim",
        constants$58.H5Dvlen_reclaim$FUNC
    );
    static final FunctionDescriptor H5Dvlen_get_buf_size$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle H5Dvlen_get_buf_size$MH = RuntimeHelper.downcallHandle(
        "H5Dvlen_get_buf_size",
        constants$58.H5Dvlen_get_buf_size$FUNC
    );
    static final FunctionDescriptor H5Dfill$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle H5Dfill$MH = RuntimeHelper.downcallHandle(
        "H5Dfill",
        constants$58.H5Dfill$FUNC
    );
    static final FunctionDescriptor H5Dset_extent$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle H5Dset_extent$MH = RuntimeHelper.downcallHandle(
        "H5Dset_extent",
        constants$58.H5Dset_extent$FUNC
    );
}


