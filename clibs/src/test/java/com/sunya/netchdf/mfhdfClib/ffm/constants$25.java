// Generated by jextract

package com.sunya.netchdf.mfhdfClib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.foreign.*;

class constants$25 {

    static final FunctionDescriptor __bswap_64$FUNC = FunctionDescriptor.of(Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle __bswap_64$MH = RuntimeHelper.downcallHandle(
        "__bswap_64",
        constants$25.__bswap_64$FUNC
    );
    static final FunctionDescriptor __uint16_identity$FUNC = FunctionDescriptor.of(Constants$root.C_SHORT$LAYOUT,
        Constants$root.C_SHORT$LAYOUT
    );
    static final MethodHandle __uint16_identity$MH = RuntimeHelper.downcallHandle(
        "__uint16_identity",
        constants$25.__uint16_identity$FUNC
    );
    static final FunctionDescriptor __uint32_identity$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle __uint32_identity$MH = RuntimeHelper.downcallHandle(
        "__uint32_identity",
        constants$25.__uint32_identity$FUNC
    );
    static final FunctionDescriptor __uint64_identity$FUNC = FunctionDescriptor.of(Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle __uint64_identity$MH = RuntimeHelper.downcallHandle(
        "__uint64_identity",
        constants$25.__uint64_identity$FUNC
    );
    static final FunctionDescriptor select$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle select$MH = RuntimeHelper.downcallHandle(
        "select",
        constants$25.select$FUNC
    );
    static final FunctionDescriptor pselect$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle pselect$MH = RuntimeHelper.downcallHandle(
        "pselect",
        constants$25.pselect$FUNC
    );
}

