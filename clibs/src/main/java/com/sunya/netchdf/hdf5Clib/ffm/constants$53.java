// Generated by jextract

package com.sunya.netchdf.hdf5Clib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
class constants$53 {

    static final FunctionDescriptor H5Aopen_name$FUNC = FunctionDescriptor.of(Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle H5Aopen_name$MH = RuntimeHelper.downcallHandle(
        "H5Aopen_name",
        constants$53.H5Aopen_name$FUNC
    );
    static final FunctionDescriptor H5Aopen_idx$FUNC = FunctionDescriptor.of(Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle H5Aopen_idx$MH = RuntimeHelper.downcallHandle(
        "H5Aopen_idx",
        constants$53.H5Aopen_idx$FUNC
    );
    static final FunctionDescriptor H5Aget_num_attrs$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle H5Aget_num_attrs$MH = RuntimeHelper.downcallHandle(
        "H5Aget_num_attrs",
        constants$53.H5Aget_num_attrs$FUNC
    );
    static final FunctionDescriptor H5Aiterate1$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle H5Aiterate1$MH = RuntimeHelper.downcallHandle(
        "H5Aiterate1",
        constants$53.H5Aiterate1$FUNC
    );
    static final FunctionDescriptor H5D_append_cb_t$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle H5D_append_cb_t$MH = RuntimeHelper.downcallHandle(
        constants$53.H5D_append_cb_t$FUNC
    );
}


