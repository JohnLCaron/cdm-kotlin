// Generated by jextract

package com.sunya.netchdf.mfhdfClib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
class constants$12 {

    static final FunctionDescriptor vprintf$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle vprintf$MH = RuntimeHelper.downcallHandle(
        "vprintf",
        constants$12.vprintf$FUNC
    );
    static final FunctionDescriptor vsprintf$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle vsprintf$MH = RuntimeHelper.downcallHandle(
        "vsprintf",
        constants$12.vsprintf$FUNC
    );
    static final FunctionDescriptor snprintf$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle snprintf$MH = RuntimeHelper.downcallHandleVariadic(
        "snprintf",
        constants$12.snprintf$FUNC
    );
    static final FunctionDescriptor vsnprintf$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle vsnprintf$MH = RuntimeHelper.downcallHandle(
        "vsnprintf",
        constants$12.vsnprintf$FUNC
    );
    static final FunctionDescriptor vdprintf$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle vdprintf$MH = RuntimeHelper.downcallHandle(
        "vdprintf",
        constants$12.vdprintf$FUNC
    );
    static final FunctionDescriptor dprintf$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle dprintf$MH = RuntimeHelper.downcallHandleVariadic(
        "dprintf",
        constants$12.dprintf$FUNC
    );
}

