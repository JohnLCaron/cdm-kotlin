// Generated by jextract

package com.sunya.netchdf.hdf5Clib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
class constants$49 {

    static final FunctionDescriptor H5Awrite$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle H5Awrite$MH = RuntimeHelper.downcallHandle(
        "H5Awrite",
        constants$49.H5Awrite$FUNC
    );
    static final FunctionDescriptor H5Aread$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle H5Aread$MH = RuntimeHelper.downcallHandle(
        "H5Aread",
        constants$49.H5Aread$FUNC
    );
    static final FunctionDescriptor H5Aclose$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle H5Aclose$MH = RuntimeHelper.downcallHandle(
        "H5Aclose",
        constants$49.H5Aclose$FUNC
    );
    static final FunctionDescriptor H5Aget_space$FUNC = FunctionDescriptor.of(Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle H5Aget_space$MH = RuntimeHelper.downcallHandle(
        "H5Aget_space",
        constants$49.H5Aget_space$FUNC
    );
    static final FunctionDescriptor H5Aget_type$FUNC = FunctionDescriptor.of(Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle H5Aget_type$MH = RuntimeHelper.downcallHandle(
        "H5Aget_type",
        constants$49.H5Aget_type$FUNC
    );
    static final FunctionDescriptor H5Aget_create_plist$FUNC = FunctionDescriptor.of(Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle H5Aget_create_plist$MH = RuntimeHelper.downcallHandle(
        "H5Aget_create_plist",
        constants$49.H5Aget_create_plist$FUNC
    );
}

