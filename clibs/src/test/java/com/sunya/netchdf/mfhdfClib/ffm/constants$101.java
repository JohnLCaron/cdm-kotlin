// Generated by jextract

package com.sunya.netchdf.mfhdfClib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.foreign.*;

class constants$101 {

    static final FunctionDescriptor DFR8addimage$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_SHORT$LAYOUT
    );
    static final MethodHandle DFR8addimage$MH = RuntimeHelper.downcallHandle(
        "DFR8addimage",
        constants$101.DFR8addimage$FUNC
    );
    static final FunctionDescriptor DFR8nimages$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle DFR8nimages$MH = RuntimeHelper.downcallHandle(
        "DFR8nimages",
        constants$101.DFR8nimages$FUNC
    );
    static final FunctionDescriptor DFR8readref$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_SHORT$LAYOUT
    );
    static final MethodHandle DFR8readref$MH = RuntimeHelper.downcallHandle(
        "DFR8readref",
        constants$101.DFR8readref$FUNC
    );
    static final FunctionDescriptor DFR8writeref$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_SHORT$LAYOUT
    );
    static final MethodHandle DFR8writeref$MH = RuntimeHelper.downcallHandle(
        "DFR8writeref",
        constants$101.DFR8writeref$FUNC
    );
    static final FunctionDescriptor DFR8restart$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT);
    static final MethodHandle DFR8restart$MH = RuntimeHelper.downcallHandle(
        "DFR8restart",
        constants$101.DFR8restart$FUNC
    );
    static final FunctionDescriptor DFR8lastref$FUNC = FunctionDescriptor.of(Constants$root.C_SHORT$LAYOUT);
    static final MethodHandle DFR8lastref$MH = RuntimeHelper.downcallHandle(
        "DFR8lastref",
        constants$101.DFR8lastref$FUNC
    );
}

