// Generated by jextract

package com.sunya.netchdf.mfhdfClib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.foreign.*;

class constants$84 {

    static final FunctionDescriptor Hcache$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle Hcache$MH = RuntimeHelper.downcallHandle(
        "Hcache",
        constants$84.Hcache$FUNC
    );
    static final FunctionDescriptor Hgetlibversion$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle Hgetlibversion$MH = RuntimeHelper.downcallHandle(
        "Hgetlibversion",
        constants$84.Hgetlibversion$FUNC
    );
    static final FunctionDescriptor Hgetfileversion$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle Hgetfileversion$MH = RuntimeHelper.downcallHandle(
        "Hgetfileversion",
        constants$84.Hgetfileversion$FUNC
    );
    static final FunctionDescriptor Hsetaccesstype$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle Hsetaccesstype$MH = RuntimeHelper.downcallHandle(
        "Hsetaccesstype",
        constants$84.Hsetaccesstype$FUNC
    );
    static final FunctionDescriptor HDmake_special_tag$FUNC = FunctionDescriptor.of(Constants$root.C_SHORT$LAYOUT,
        Constants$root.C_SHORT$LAYOUT
    );
    static final MethodHandle HDmake_special_tag$MH = RuntimeHelper.downcallHandle(
        "HDmake_special_tag",
        constants$84.HDmake_special_tag$FUNC
    );
    static final FunctionDescriptor HDis_special_tag$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_SHORT$LAYOUT
    );
    static final MethodHandle HDis_special_tag$MH = RuntimeHelper.downcallHandle(
        "HDis_special_tag",
        constants$84.HDis_special_tag$FUNC
    );
}


