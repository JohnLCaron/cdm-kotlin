// Generated by jextract

package com.sunya.netchdf.mfhdfClib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.foreign.*;

class constants$97 {

    static final FunctionDescriptor DFCIimcomp$FUNC = FunctionDescriptor.ofVoid(
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle DFCIimcomp$MH = RuntimeHelper.downcallHandle(
        "DFCIimcomp",
        constants$97.DFCIimcomp$FUNC
    );
    static final FunctionDescriptor DFCIunimcomp$FUNC = FunctionDescriptor.ofVoid(
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle DFCIunimcomp$MH = RuntimeHelper.downcallHandle(
        "DFCIunimcomp",
        constants$97.DFCIunimcomp$FUNC
    );
    static final FunctionDescriptor DFCIjpeg$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_SHORT$LAYOUT,
        Constants$root.C_SHORT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_SHORT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle DFCIjpeg$MH = RuntimeHelper.downcallHandle(
        "DFCIjpeg",
        constants$97.DFCIjpeg$FUNC
    );
    static final FunctionDescriptor DFCIunjpeg$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_SHORT$LAYOUT,
        Constants$root.C_SHORT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_SHORT$LAYOUT
    );
    static final MethodHandle DFCIunjpeg$MH = RuntimeHelper.downcallHandle(
        "DFCIunjpeg",
        constants$97.DFCIunjpeg$FUNC
    );
    static final FunctionDescriptor DFdiread$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_SHORT$LAYOUT,
        Constants$root.C_SHORT$LAYOUT
    );
    static final MethodHandle DFdiread$MH = RuntimeHelper.downcallHandle(
        "DFdiread",
        constants$97.DFdiread$FUNC
    );
    static final FunctionDescriptor DFdiget$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle DFdiget$MH = RuntimeHelper.downcallHandle(
        "DFdiget",
        constants$97.DFdiget$FUNC
    );
}

