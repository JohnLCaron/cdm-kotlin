// Generated by jextract

package sunya.cdm.netcdf4.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
class constants$6 {

    static final FunctionDescriptor nc_inq_compound_fieldoffset$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle nc_inq_compound_fieldoffset$MH = RuntimeHelper.downcallHandle(
        "nc_inq_compound_fieldoffset",
        constants$6.nc_inq_compound_fieldoffset$FUNC
    );
    static final FunctionDescriptor nc_inq_compound_fieldtype$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle nc_inq_compound_fieldtype$MH = RuntimeHelper.downcallHandle(
        "nc_inq_compound_fieldtype",
        constants$6.nc_inq_compound_fieldtype$FUNC
    );
    static final FunctionDescriptor nc_inq_compound_fieldndims$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle nc_inq_compound_fieldndims$MH = RuntimeHelper.downcallHandle(
        "nc_inq_compound_fieldndims",
        constants$6.nc_inq_compound_fieldndims$FUNC
    );
    static final FunctionDescriptor nc_inq_compound_fielddim_sizes$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle nc_inq_compound_fielddim_sizes$MH = RuntimeHelper.downcallHandle(
        "nc_inq_compound_fielddim_sizes",
        constants$6.nc_inq_compound_fielddim_sizes$FUNC
    );
    static final FunctionDescriptor nc_def_vlen$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle nc_def_vlen$MH = RuntimeHelper.downcallHandle(
        "nc_def_vlen",
        constants$6.nc_def_vlen$FUNC
    );
    static final FunctionDescriptor nc_inq_vlen$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle nc_inq_vlen$MH = RuntimeHelper.downcallHandle(
        "nc_inq_vlen",
        constants$6.nc_inq_vlen$FUNC
    );
}


