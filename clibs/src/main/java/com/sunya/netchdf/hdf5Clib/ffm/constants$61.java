// Generated by jextract

package com.sunya.netchdf.hdf5Clib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
class constants$61 {

    static final  OfAddress stderr$LAYOUT = Constants$root.C_POINTER$LAYOUT;
    static final VarHandle stderr$VH = constants$61.stderr$LAYOUT.varHandle();
    static final MemorySegment stderr$SEGMENT = RuntimeHelper.lookupGlobalVariable("stderr", constants$61.stderr$LAYOUT);
    static final FunctionDescriptor remove$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle remove$MH = RuntimeHelper.downcallHandle(
        "remove",
        constants$61.remove$FUNC
    );
    static final FunctionDescriptor rename$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle rename$MH = RuntimeHelper.downcallHandle(
        "rename",
        constants$61.rename$FUNC
    );
    static final FunctionDescriptor renameat$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle renameat$MH = RuntimeHelper.downcallHandle(
        "renameat",
        constants$61.renameat$FUNC
    );
    static final FunctionDescriptor fclose$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle fclose$MH = RuntimeHelper.downcallHandle(
        "fclose",
        constants$61.fclose$FUNC
    );
    static final FunctionDescriptor tmpfile$FUNC = FunctionDescriptor.of(Constants$root.C_POINTER$LAYOUT);
    static final MethodHandle tmpfile$MH = RuntimeHelper.downcallHandle(
        "tmpfile",
        constants$61.tmpfile$FUNC
    );
}


