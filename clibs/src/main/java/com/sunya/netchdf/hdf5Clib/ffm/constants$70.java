// Generated by jextract

package com.sunya.netchdf.hdf5Clib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
class constants$70 {

    static final FunctionDescriptor fputs$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle fputs$MH = RuntimeHelper.downcallHandle(
        "fputs",
        constants$70.fputs$FUNC
    );
    static final FunctionDescriptor puts$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle puts$MH = RuntimeHelper.downcallHandle(
        "puts",
        constants$70.puts$FUNC
    );
    static final FunctionDescriptor ungetc$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle ungetc$MH = RuntimeHelper.downcallHandle(
        "ungetc",
        constants$70.ungetc$FUNC
    );
    static final FunctionDescriptor fread$FUNC = FunctionDescriptor.of(Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle fread$MH = RuntimeHelper.downcallHandle(
        "fread",
        constants$70.fread$FUNC
    );
    static final FunctionDescriptor fwrite$FUNC = FunctionDescriptor.of(Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle fwrite$MH = RuntimeHelper.downcallHandle(
        "fwrite",
        constants$70.fwrite$FUNC
    );
    static final FunctionDescriptor fread_unlocked$FUNC = FunctionDescriptor.of(Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle fread_unlocked$MH = RuntimeHelper.downcallHandle(
        "fread_unlocked",
        constants$70.fread_unlocked$FUNC
    );
}


