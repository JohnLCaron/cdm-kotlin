// Generated by jextract

package sunya.cdm.netcdf4.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
class constants$11 {

    static final FunctionDescriptor nc_get_varm$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle nc_get_varm$MH = RuntimeHelper.downcallHandle(
        "nc_get_varm",
        constants$11.nc_get_varm$FUNC
    );
    static final FunctionDescriptor nc_def_var_deflate$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle nc_def_var_deflate$MH = RuntimeHelper.downcallHandle(
        "nc_def_var_deflate",
        constants$11.nc_def_var_deflate$FUNC
    );
    static final FunctionDescriptor nc_inq_var_deflate$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle nc_inq_var_deflate$MH = RuntimeHelper.downcallHandle(
        "nc_inq_var_deflate",
        constants$11.nc_inq_var_deflate$FUNC
    );
    static final FunctionDescriptor nc_def_var_szip$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle nc_def_var_szip$MH = RuntimeHelper.downcallHandle(
        "nc_def_var_szip",
        constants$11.nc_def_var_szip$FUNC
    );
    static final FunctionDescriptor nc_inq_var_szip$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle nc_inq_var_szip$MH = RuntimeHelper.downcallHandle(
        "nc_inq_var_szip",
        constants$11.nc_inq_var_szip$FUNC
    );
    static final FunctionDescriptor nc_def_var_fletcher32$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle nc_def_var_fletcher32$MH = RuntimeHelper.downcallHandle(
        "nc_def_var_fletcher32",
        constants$11.nc_def_var_fletcher32$FUNC
    );
}


