// Generated by jextract

package sunya.cdm.netcdf4.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
class constants$23 {

    static final FunctionDescriptor nc_get_att_longlong$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle nc_get_att_longlong$MH = RuntimeHelper.downcallHandle(
        "nc_get_att_longlong",
        constants$23.nc_get_att_longlong$FUNC
    );
    static final FunctionDescriptor nc_put_att_ulonglong$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle nc_put_att_ulonglong$MH = RuntimeHelper.downcallHandle(
        "nc_put_att_ulonglong",
        constants$23.nc_put_att_ulonglong$FUNC
    );
    static final FunctionDescriptor nc_get_att_ulonglong$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle nc_get_att_ulonglong$MH = RuntimeHelper.downcallHandle(
        "nc_get_att_ulonglong",
        constants$23.nc_get_att_ulonglong$FUNC
    );
    static final FunctionDescriptor nc_def_var$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle nc_def_var$MH = RuntimeHelper.downcallHandle(
        "nc_def_var",
        constants$23.nc_def_var$FUNC
    );
    static final FunctionDescriptor nc_inq_var$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle nc_inq_var$MH = RuntimeHelper.downcallHandle(
        "nc_inq_var",
        constants$23.nc_inq_var$FUNC
    );
    static final FunctionDescriptor nc_inq_varid$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle nc_inq_varid$MH = RuntimeHelper.downcallHandle(
        "nc_inq_varid",
        constants$23.nc_inq_varid$FUNC
    );
}


