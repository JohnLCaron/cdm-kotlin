// Generated by jextract

package com.sunya.netchdf.mfhdfClib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.foreign.*;

class constants$55 {

    static final FunctionDescriptor open$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle open$MH = RuntimeHelper.downcallHandleVariadic(
        "open",
        constants$55.open$FUNC
    );
    static final FunctionDescriptor openat$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle openat$MH = RuntimeHelper.downcallHandleVariadic(
        "openat",
        constants$55.openat$FUNC
    );
    static final FunctionDescriptor creat$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle creat$MH = RuntimeHelper.downcallHandle(
        "creat",
        constants$55.creat$FUNC
    );
    static final FunctionDescriptor lockf$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle lockf$MH = RuntimeHelper.downcallHandle(
        "lockf",
        constants$55.lockf$FUNC
    );
    static final FunctionDescriptor posix_fadvise$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle posix_fadvise$MH = RuntimeHelper.downcallHandle(
        "posix_fadvise",
        constants$55.posix_fadvise$FUNC
    );
    static final FunctionDescriptor posix_fallocate$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle posix_fallocate$MH = RuntimeHelper.downcallHandle(
        "posix_fallocate",
        constants$55.posix_fallocate$FUNC
    );
}


