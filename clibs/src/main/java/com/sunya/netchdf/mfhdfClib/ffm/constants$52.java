// Generated by jextract

package com.sunya.netchdf.mfhdfClib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
class constants$52 {

    static final  OfInt daylight$LAYOUT = Constants$root.C_INT$LAYOUT;
    static final VarHandle daylight$VH = constants$52.daylight$LAYOUT.varHandle();
    static final MemorySegment daylight$SEGMENT = RuntimeHelper.lookupGlobalVariable("daylight", constants$52.daylight$LAYOUT);
    static final  OfLong timezone$LAYOUT = Constants$root.C_LONG_LONG$LAYOUT;
    static final VarHandle timezone$VH = constants$52.timezone$LAYOUT.varHandle();
    static final MemorySegment timezone$SEGMENT = RuntimeHelper.lookupGlobalVariable("timezone", constants$52.timezone$LAYOUT);
    static final FunctionDescriptor timegm$FUNC = FunctionDescriptor.of(Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle timegm$MH = RuntimeHelper.downcallHandle(
        "timegm",
        constants$52.timegm$FUNC
    );
    static final FunctionDescriptor timelocal$FUNC = FunctionDescriptor.of(Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle timelocal$MH = RuntimeHelper.downcallHandle(
        "timelocal",
        constants$52.timelocal$FUNC
    );
    static final FunctionDescriptor dysize$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle dysize$MH = RuntimeHelper.downcallHandle(
        "dysize",
        constants$52.dysize$FUNC
    );
    static final FunctionDescriptor nanosleep$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle nanosleep$MH = RuntimeHelper.downcallHandle(
        "nanosleep",
        constants$52.nanosleep$FUNC
    );
}


