// Generated by jextract

package com.sunya.netchdf.mfhdfClib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
class constants$164 {

    static final FunctionDescriptor SDgetexternalfile$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle SDgetexternalfile$MH = RuntimeHelper.downcallHandle(
        "SDgetexternalfile",
        constants$164.SDgetexternalfile$FUNC
    );
    static final FunctionDescriptor SDgetexternalinfo$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle SDgetexternalinfo$MH = RuntimeHelper.downcallHandle(
        "SDgetexternalinfo",
        constants$164.SDgetexternalinfo$FUNC
    );
    static final FunctionDescriptor SDsetexternalfile$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle SDsetexternalfile$MH = RuntimeHelper.downcallHandle(
        "SDsetexternalfile",
        constants$164.SDsetexternalfile$FUNC
    );
    static final FunctionDescriptor SDsetnbitdataset$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle SDsetnbitdataset$MH = RuntimeHelper.downcallHandle(
        "SDsetnbitdataset",
        constants$164.SDsetnbitdataset$FUNC
    );
    static final FunctionDescriptor SDsetcompress$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle SDsetcompress$MH = RuntimeHelper.downcallHandle(
        "SDsetcompress",
        constants$164.SDsetcompress$FUNC
    );
    static final FunctionDescriptor SDgetcompress$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle SDgetcompress$MH = RuntimeHelper.downcallHandle(
        "SDgetcompress",
        constants$164.SDgetcompress$FUNC
    );
}

