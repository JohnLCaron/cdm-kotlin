// Generated by jextract

package com.sunya.netchdf.mfhdfClib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.foreign.*;

class constants$165 {

    static final FunctionDescriptor SDgetcompinfo$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle SDgetcompinfo$MH = RuntimeHelper.downcallHandle(
        "SDgetcompinfo",
        constants$165.SDgetcompinfo$FUNC
    );
    static final FunctionDescriptor SDgetcomptype$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle SDgetcomptype$MH = RuntimeHelper.downcallHandle(
        "SDgetcomptype",
        constants$165.SDgetcomptype$FUNC
    );
    static final FunctionDescriptor SDfindattr$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle SDfindattr$MH = RuntimeHelper.downcallHandle(
        "SDfindattr",
        constants$165.SDfindattr$FUNC
    );
    static final FunctionDescriptor SDidtoref$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle SDidtoref$MH = RuntimeHelper.downcallHandle(
        "SDidtoref",
        constants$165.SDidtoref$FUNC
    );
    static final FunctionDescriptor SDreftoindex$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle SDreftoindex$MH = RuntimeHelper.downcallHandle(
        "SDreftoindex",
        constants$165.SDreftoindex$FUNC
    );
    static final FunctionDescriptor SDisrecord$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle SDisrecord$MH = RuntimeHelper.downcallHandle(
        "SDisrecord",
        constants$165.SDisrecord$FUNC
    );
}


