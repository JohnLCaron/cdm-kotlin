// Generated by jextract

package com.sunya.netchdf.hdf5Clib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
class constants$23 {

    static final  OfLong H5T_NATIVE_UINT_FAST64_g$LAYOUT = Constants$root.C_LONG_LONG$LAYOUT;
    static final VarHandle H5T_NATIVE_UINT_FAST64_g$VH = constants$23.H5T_NATIVE_UINT_FAST64_g$LAYOUT.varHandle();
    static final MemorySegment H5T_NATIVE_UINT_FAST64_g$SEGMENT = RuntimeHelper.lookupGlobalVariable("H5T_NATIVE_UINT_FAST64_g", constants$23.H5T_NATIVE_UINT_FAST64_g$LAYOUT);
    static final FunctionDescriptor H5Tcreate$FUNC = FunctionDescriptor.of(Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle H5Tcreate$MH = RuntimeHelper.downcallHandle(
        "H5Tcreate",
        constants$23.H5Tcreate$FUNC
    );
    static final FunctionDescriptor H5Tcopy$FUNC = FunctionDescriptor.of(Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle H5Tcopy$MH = RuntimeHelper.downcallHandle(
        "H5Tcopy",
        constants$23.H5Tcopy$FUNC
    );
    static final FunctionDescriptor H5Tclose$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle H5Tclose$MH = RuntimeHelper.downcallHandle(
        "H5Tclose",
        constants$23.H5Tclose$FUNC
    );
    static final FunctionDescriptor H5Tequal$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle H5Tequal$MH = RuntimeHelper.downcallHandle(
        "H5Tequal",
        constants$23.H5Tequal$FUNC
    );
    static final FunctionDescriptor H5Tlock$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle H5Tlock$MH = RuntimeHelper.downcallHandle(
        "H5Tlock",
        constants$23.H5Tlock$FUNC
    );
}


