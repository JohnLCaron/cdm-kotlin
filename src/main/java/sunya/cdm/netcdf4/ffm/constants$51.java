// Generated by jextract

package sunya.cdm.netcdf4.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
class constants$51 {

    static final FunctionDescriptor ncopen$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle ncopen$MH = RuntimeHelper.downcallHandle(
        "ncopen",
        constants$51.ncopen$FUNC
    );
    static final FunctionDescriptor ncsetfill$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle ncsetfill$MH = RuntimeHelper.downcallHandle(
        "ncsetfill",
        constants$51.ncsetfill$FUNC
    );
    static final FunctionDescriptor ncredef$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle ncredef$MH = RuntimeHelper.downcallHandle(
        "ncredef",
        constants$51.ncredef$FUNC
    );
    static final FunctionDescriptor ncendef$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle ncendef$MH = RuntimeHelper.downcallHandle(
        "ncendef",
        constants$51.ncendef$FUNC
    );
    static final FunctionDescriptor ncsync$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle ncsync$MH = RuntimeHelper.downcallHandle(
        "ncsync",
        constants$51.ncsync$FUNC
    );
    static final FunctionDescriptor ncabort$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle ncabort$MH = RuntimeHelper.downcallHandle(
        "ncabort",
        constants$51.ncabort$FUNC
    );
}


