// Generated by jextract

package com.sunya.netchdf.mfhdfClib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.foreign.*;

class constants$149 {

    static final FunctionDescriptor VSgetexternalfile$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle VSgetexternalfile$MH = RuntimeHelper.downcallHandle(
        "VSgetexternalfile",
        constants$149.VSgetexternalfile$FUNC
    );
    static final FunctionDescriptor VSgetexternalinfo$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle VSgetexternalinfo$MH = RuntimeHelper.downcallHandle(
        "VSgetexternalinfo",
        constants$149.VSgetexternalinfo$FUNC
    );
    static final FunctionDescriptor VSfpack$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle VSfpack$MH = RuntimeHelper.downcallHandle(
        "VSfpack",
        constants$149.VSfpack$FUNC
    );
    static final FunctionDescriptor VSPshutdown$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT);
    static final MethodHandle VSPshutdown$MH = RuntimeHelper.downcallHandle(
        "VSPshutdown",
        constants$149.VSPshutdown$FUNC
    );
    static final FunctionDescriptor VSseek$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle VSseek$MH = RuntimeHelper.downcallHandle(
        "VSseek",
        constants$149.VSseek$FUNC
    );
    static final FunctionDescriptor VSread$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle VSread$MH = RuntimeHelper.downcallHandle(
        "VSread",
        constants$149.VSread$FUNC
    );
}


