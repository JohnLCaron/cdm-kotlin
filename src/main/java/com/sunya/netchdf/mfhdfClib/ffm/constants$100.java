// Generated by jextract

package com.sunya.netchdf.mfhdfClib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
class constants$100 {

    static final FunctionDescriptor DFPlastref$FUNC = FunctionDescriptor.of(Constants$root.C_SHORT$LAYOUT);
    static final MethodHandle DFPlastref$MH = RuntimeHelper.downcallHandle(
        "DFPlastref",
        constants$100.DFPlastref$FUNC
    );
    static final FunctionDescriptor DFR8setcompress$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle DFR8setcompress$MH = RuntimeHelper.downcallHandle(
        "DFR8setcompress",
        constants$100.DFR8setcompress$FUNC
    );
    static final FunctionDescriptor DFR8getdims$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle DFR8getdims$MH = RuntimeHelper.downcallHandle(
        "DFR8getdims",
        constants$100.DFR8getdims$FUNC
    );
    static final FunctionDescriptor DFR8getimage$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle DFR8getimage$MH = RuntimeHelper.downcallHandle(
        "DFR8getimage",
        constants$100.DFR8getimage$FUNC
    );
    static final FunctionDescriptor DFR8setpalette$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle DFR8setpalette$MH = RuntimeHelper.downcallHandle(
        "DFR8setpalette",
        constants$100.DFR8setpalette$FUNC
    );
    static final FunctionDescriptor DFR8putimage$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_SHORT$LAYOUT
    );
    static final MethodHandle DFR8putimage$MH = RuntimeHelper.downcallHandle(
        "DFR8putimage",
        constants$100.DFR8putimage$FUNC
    );
}

