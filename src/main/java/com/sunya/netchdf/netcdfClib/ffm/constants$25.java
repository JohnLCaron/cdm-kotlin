// Generated by jextract

package com.sunya.netchdf.netcdfClib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
class constants$25 {

    static final FunctionDescriptor nc_inq_varndims$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle nc_inq_varndims$MH = RuntimeHelper.downcallHandle(
        "nc_inq_varndims",
        constants$25.nc_inq_varndims$FUNC
    );
    static final FunctionDescriptor nc_inq_vardimid$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle nc_inq_vardimid$MH = RuntimeHelper.downcallHandle(
        "nc_inq_vardimid",
        constants$25.nc_inq_vardimid$FUNC
    );
    static final FunctionDescriptor nc_inq_varnatts$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle nc_inq_varnatts$MH = RuntimeHelper.downcallHandle(
        "nc_inq_varnatts",
        constants$25.nc_inq_varnatts$FUNC
    );
    static final FunctionDescriptor nc_rename_var$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle nc_rename_var$MH = RuntimeHelper.downcallHandle(
        "nc_rename_var",
        constants$25.nc_rename_var$FUNC
    );
    static final FunctionDescriptor nc_copy_var$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle nc_copy_var$MH = RuntimeHelper.downcallHandle(
        "nc_copy_var",
        constants$25.nc_copy_var$FUNC
    );
    static final FunctionDescriptor nc_put_var1_text$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle nc_put_var1_text$MH = RuntimeHelper.downcallHandle(
        "nc_put_var1_text",
        constants$25.nc_put_var1_text$FUNC
    );
}


