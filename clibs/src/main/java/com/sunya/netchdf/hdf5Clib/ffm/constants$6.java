// Generated by jextract

package com.sunya.netchdf.hdf5Clib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
class constants$6 {

    static final FunctionDescriptor H5Iget_name$FUNC = FunctionDescriptor.of(Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle H5Iget_name$MH = RuntimeHelper.downcallHandle(
        "H5Iget_name",
        constants$6.H5Iget_name$FUNC
    );
    static final FunctionDescriptor H5Iinc_ref$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle H5Iinc_ref$MH = RuntimeHelper.downcallHandle(
        "H5Iinc_ref",
        constants$6.H5Iinc_ref$FUNC
    );
    static final FunctionDescriptor H5Idec_ref$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle H5Idec_ref$MH = RuntimeHelper.downcallHandle(
        "H5Idec_ref",
        constants$6.H5Idec_ref$FUNC
    );
    static final FunctionDescriptor H5Iget_ref$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle H5Iget_ref$MH = RuntimeHelper.downcallHandle(
        "H5Iget_ref",
        constants$6.H5Iget_ref$FUNC
    );
    static final FunctionDescriptor H5Iregister_type$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle H5Iregister_type$MH = RuntimeHelper.downcallHandle(
        "H5Iregister_type",
        constants$6.H5Iregister_type$FUNC
    );
    static final FunctionDescriptor H5Iclear_type$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_BOOL$LAYOUT
    );
    static final MethodHandle H5Iclear_type$MH = RuntimeHelper.downcallHandle(
        "H5Iclear_type",
        constants$6.H5Iclear_type$FUNC
    );
}


