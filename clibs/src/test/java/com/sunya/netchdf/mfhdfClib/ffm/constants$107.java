// Generated by jextract

package com.sunya.netchdf.mfhdfClib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.foreign.*;

class constants$107 {

    static final FunctionDescriptor DF24setcompress$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle DF24setcompress$MH = RuntimeHelper.downcallHandle(
        "DF24setcompress",
        constants$107.DF24setcompress$FUNC
    );
    static final FunctionDescriptor DF24restart$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT);
    static final MethodHandle DF24restart$MH = RuntimeHelper.downcallHandle(
        "DF24restart",
        constants$107.DF24restart$FUNC
    );
    static final FunctionDescriptor DF24addimage$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle DF24addimage$MH = RuntimeHelper.downcallHandle(
        "DF24addimage",
        constants$107.DF24addimage$FUNC
    );
    static final FunctionDescriptor DF24putimage$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle DF24putimage$MH = RuntimeHelper.downcallHandle(
        "DF24putimage",
        constants$107.DF24putimage$FUNC
    );
    static final FunctionDescriptor DF24nimages$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle DF24nimages$MH = RuntimeHelper.downcallHandle(
        "DF24nimages",
        constants$107.DF24nimages$FUNC
    );
    static final FunctionDescriptor DF24readref$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_SHORT$LAYOUT
    );
    static final MethodHandle DF24readref$MH = RuntimeHelper.downcallHandle(
        "DF24readref",
        constants$107.DF24readref$FUNC
    );
}

