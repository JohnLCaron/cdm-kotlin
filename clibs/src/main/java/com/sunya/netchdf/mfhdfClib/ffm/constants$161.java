// Generated by jextract

package com.sunya.netchdf.mfhdfClib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.foreign.*;

class constants$161 {

    static final FunctionDescriptor SDendaccess$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle SDendaccess$MH = RuntimeHelper.downcallHandle(
        "SDendaccess",
        constants$161.SDendaccess$FUNC
    );
    static final FunctionDescriptor SDsetrange$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle SDsetrange$MH = RuntimeHelper.downcallHandle(
        "SDsetrange",
        constants$161.SDsetrange$FUNC
    );
    static final FunctionDescriptor SDsetattr$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle SDsetattr$MH = RuntimeHelper.downcallHandle(
        "SDsetattr",
        constants$161.SDsetattr$FUNC
    );
    static final FunctionDescriptor SDattrinfo$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle SDattrinfo$MH = RuntimeHelper.downcallHandle(
        "SDattrinfo",
        constants$161.SDattrinfo$FUNC
    );
    static final FunctionDescriptor SDreadattr$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle SDreadattr$MH = RuntimeHelper.downcallHandle(
        "SDreadattr",
        constants$161.SDreadattr$FUNC
    );
    static final FunctionDescriptor SDwritedata$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle SDwritedata$MH = RuntimeHelper.downcallHandle(
        "SDwritedata",
        constants$161.SDwritedata$FUNC
    );
}


