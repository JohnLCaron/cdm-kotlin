// Generated by jextract

package com.sunya.netchdf.hdf5Clib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
class constants$55 {

    static final FunctionDescriptor H5Dcreate2$FUNC = FunctionDescriptor.of(Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle H5Dcreate2$MH = RuntimeHelper.downcallHandle(
        "H5Dcreate2",
        constants$55.H5Dcreate2$FUNC
    );
    static final FunctionDescriptor H5Dcreate_anon$FUNC = FunctionDescriptor.of(Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle H5Dcreate_anon$MH = RuntimeHelper.downcallHandle(
        "H5Dcreate_anon",
        constants$55.H5Dcreate_anon$FUNC
    );
    static final FunctionDescriptor H5Dopen2$FUNC = FunctionDescriptor.of(Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle H5Dopen2$MH = RuntimeHelper.downcallHandle(
        "H5Dopen2",
        constants$55.H5Dopen2$FUNC
    );
    static final FunctionDescriptor H5Dclose$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle H5Dclose$MH = RuntimeHelper.downcallHandle(
        "H5Dclose",
        constants$55.H5Dclose$FUNC
    );
    static final FunctionDescriptor H5Dget_space$FUNC = FunctionDescriptor.of(Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle H5Dget_space$MH = RuntimeHelper.downcallHandle(
        "H5Dget_space",
        constants$55.H5Dget_space$FUNC
    );
    static final FunctionDescriptor H5Dget_space_status$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle H5Dget_space_status$MH = RuntimeHelper.downcallHandle(
        "H5Dget_space_status",
        constants$55.H5Dget_space_status$FUNC
    );
}


