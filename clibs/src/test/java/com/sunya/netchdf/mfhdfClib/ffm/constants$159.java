// Generated by jextract

package com.sunya.netchdf.mfhdfClib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.foreign.*;

class constants$159 {

    static final FunctionDescriptor SDfileinfo$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle SDfileinfo$MH = RuntimeHelper.downcallHandle(
        "SDfileinfo",
        constants$159.SDfileinfo$FUNC
    );
    static final FunctionDescriptor SDselect$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle SDselect$MH = RuntimeHelper.downcallHandle(
        "SDselect",
        constants$159.SDselect$FUNC
    );
    static final FunctionDescriptor SDgetinfo$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle SDgetinfo$MH = RuntimeHelper.downcallHandle(
        "SDgetinfo",
        constants$159.SDgetinfo$FUNC
    );
    static final FunctionDescriptor SDreaddata$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle SDreaddata$MH = RuntimeHelper.downcallHandle(
        "SDreaddata",
        constants$159.SDreaddata$FUNC
    );
    static final FunctionDescriptor SDgerefnumber$FUNC = FunctionDescriptor.of(Constants$root.C_SHORT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle SDgerefnumber$MH = RuntimeHelper.downcallHandle(
        "SDgerefnumber",
        constants$159.SDgerefnumber$FUNC
    );
    static final FunctionDescriptor SDnametoindex$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle SDnametoindex$MH = RuntimeHelper.downcallHandle(
        "SDnametoindex",
        constants$159.SDnametoindex$FUNC
    );
}

