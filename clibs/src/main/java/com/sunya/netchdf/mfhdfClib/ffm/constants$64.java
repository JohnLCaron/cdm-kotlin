// Generated by jextract

package com.sunya.netchdf.mfhdfClib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.foreign.*;

class constants$64 {

    static final FunctionDescriptor getuid$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT);
    static final MethodHandle getuid$MH = RuntimeHelper.downcallHandle(
        "getuid",
        constants$64.getuid$FUNC
    );
    static final FunctionDescriptor geteuid$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT);
    static final MethodHandle geteuid$MH = RuntimeHelper.downcallHandle(
        "geteuid",
        constants$64.geteuid$FUNC
    );
    static final FunctionDescriptor getgid$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT);
    static final MethodHandle getgid$MH = RuntimeHelper.downcallHandle(
        "getgid",
        constants$64.getgid$FUNC
    );
    static final FunctionDescriptor getegid$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT);
    static final MethodHandle getegid$MH = RuntimeHelper.downcallHandle(
        "getegid",
        constants$64.getegid$FUNC
    );
    static final FunctionDescriptor getgroups$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle getgroups$MH = RuntimeHelper.downcallHandle(
        "getgroups",
        constants$64.getgroups$FUNC
    );
    static final FunctionDescriptor setuid$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle setuid$MH = RuntimeHelper.downcallHandle(
        "setuid",
        constants$64.setuid$FUNC
    );
}


