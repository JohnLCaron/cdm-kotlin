// Generated by jextract

package com.sunya.netchdf.mfhdfClib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.foreign.*;

class constants$6 {

    static final FunctionDescriptor __tolower_l$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle __tolower_l$MH = RuntimeHelper.downcallHandle(
        "__tolower_l",
        constants$6.__tolower_l$FUNC
    );
    static final FunctionDescriptor tolower_l$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle tolower_l$MH = RuntimeHelper.downcallHandle(
        "tolower_l",
        constants$6.tolower_l$FUNC
    );
    static final FunctionDescriptor __toupper_l$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle __toupper_l$MH = RuntimeHelper.downcallHandle(
        "__toupper_l",
        constants$6.__toupper_l$FUNC
    );
    static final FunctionDescriptor toupper_l$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle toupper_l$MH = RuntimeHelper.downcallHandle(
        "toupper_l",
        constants$6.toupper_l$FUNC
    );
    static final FunctionDescriptor imaxabs$FUNC = FunctionDescriptor.of(Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle imaxabs$MH = RuntimeHelper.downcallHandle(
        "imaxabs",
        constants$6.imaxabs$FUNC
    );
    static final FunctionDescriptor imaxdiv$FUNC = FunctionDescriptor.of(MemoryLayout.structLayout(
        Constants$root.C_LONG_LONG$LAYOUT.withName("quot"),
        Constants$root.C_LONG_LONG$LAYOUT.withName("rem")
    ),
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle imaxdiv$MH = RuntimeHelper.downcallHandle(
        "imaxdiv",
        constants$6.imaxdiv$FUNC
    );
}


