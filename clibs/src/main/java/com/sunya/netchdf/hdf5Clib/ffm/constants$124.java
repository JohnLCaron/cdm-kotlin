// Generated by jextract

package com.sunya.netchdf.hdf5Clib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
class constants$124 {

    static final FunctionDescriptor H5Gget_objinfo$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_BOOL$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle H5Gget_objinfo$MH = RuntimeHelper.downcallHandle(
        "H5Gget_objinfo",
        constants$124.H5Gget_objinfo$FUNC
    );
    static final FunctionDescriptor H5Gget_objname_by_idx$FUNC = FunctionDescriptor.of(Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle H5Gget_objname_by_idx$MH = RuntimeHelper.downcallHandle(
        "H5Gget_objname_by_idx",
        constants$124.H5Gget_objname_by_idx$FUNC
    );
    static final FunctionDescriptor H5Gget_objtype_by_idx$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle H5Gget_objtype_by_idx$MH = RuntimeHelper.downcallHandle(
        "H5Gget_objtype_by_idx",
        constants$124.H5Gget_objtype_by_idx$FUNC
    );
    static final FunctionDescriptor H5MM_allocate_t$FUNC = FunctionDescriptor.of(Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle H5MM_allocate_t$MH = RuntimeHelper.downcallHandle(
        constants$124.H5MM_allocate_t$FUNC
    );
    static final FunctionDescriptor H5MM_free_t$FUNC = FunctionDescriptor.ofVoid(
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
}


