// Generated by jextract

package com.sunya.netchdf.mfhdfClib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.foreign.*;

class constants$47 {

    static final FunctionDescriptor strcasecmp$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle strcasecmp$MH = RuntimeHelper.downcallHandle(
        "strcasecmp",
        constants$47.strcasecmp$FUNC
    );
    static final FunctionDescriptor strncasecmp$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle strncasecmp$MH = RuntimeHelper.downcallHandle(
        "strncasecmp",
        constants$47.strncasecmp$FUNC
    );
    static final FunctionDescriptor strcasecmp_l$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle strcasecmp_l$MH = RuntimeHelper.downcallHandle(
        "strcasecmp_l",
        constants$47.strcasecmp_l$FUNC
    );
    static final FunctionDescriptor strncasecmp_l$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle strncasecmp_l$MH = RuntimeHelper.downcallHandle(
        "strncasecmp_l",
        constants$47.strncasecmp_l$FUNC
    );
    static final FunctionDescriptor explicit_bzero$FUNC = FunctionDescriptor.ofVoid(
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle explicit_bzero$MH = RuntimeHelper.downcallHandle(
        "explicit_bzero",
        constants$47.explicit_bzero$FUNC
    );
    static final FunctionDescriptor strsep$FUNC = FunctionDescriptor.of(Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle strsep$MH = RuntimeHelper.downcallHandle(
        "strsep",
        constants$47.strsep$FUNC
    );
}


