// Generated by jextract

package com.sunya.netchdf.hdf5Clib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
class constants$157 {

    static final FunctionDescriptor H5Pset_nbit$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle H5Pset_nbit$MH = RuntimeHelper.downcallHandle(
        "H5Pset_nbit",
        constants$157.H5Pset_nbit$FUNC
    );
    static final FunctionDescriptor H5Pset_scaleoffset$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle H5Pset_scaleoffset$MH = RuntimeHelper.downcallHandle(
        "H5Pset_scaleoffset",
        constants$157.H5Pset_scaleoffset$FUNC
    );
    static final FunctionDescriptor H5Pset_fill_value$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle H5Pset_fill_value$MH = RuntimeHelper.downcallHandle(
        "H5Pset_fill_value",
        constants$157.H5Pset_fill_value$FUNC
    );
    static final FunctionDescriptor H5Pget_fill_value$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle H5Pget_fill_value$MH = RuntimeHelper.downcallHandle(
        "H5Pget_fill_value",
        constants$157.H5Pget_fill_value$FUNC
    );
    static final FunctionDescriptor H5Pfill_value_defined$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle H5Pfill_value_defined$MH = RuntimeHelper.downcallHandle(
        "H5Pfill_value_defined",
        constants$157.H5Pfill_value_defined$FUNC
    );
    static final FunctionDescriptor H5Pset_alloc_time$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle H5Pset_alloc_time$MH = RuntimeHelper.downcallHandle(
        "H5Pset_alloc_time",
        constants$157.H5Pset_alloc_time$FUNC
    );
}


