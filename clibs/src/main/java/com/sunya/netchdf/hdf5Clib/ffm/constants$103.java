// Generated by jextract

package com.sunya.netchdf.hdf5Clib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
class constants$103 {

    static final  OfLong H5E_CANTMODIFY_g$LAYOUT = Constants$root.C_LONG_LONG$LAYOUT;
    static final VarHandle H5E_CANTMODIFY_g$VH = constants$103.H5E_CANTMODIFY_g$LAYOUT.varHandle();
    static final MemorySegment H5E_CANTMODIFY_g$SEGMENT = RuntimeHelper.lookupGlobalVariable("H5E_CANTMODIFY_g", constants$103.H5E_CANTMODIFY_g$LAYOUT);
    static final  OfLong H5E_CANTREMOVE_g$LAYOUT = Constants$root.C_LONG_LONG$LAYOUT;
    static final VarHandle H5E_CANTREMOVE_g$VH = constants$103.H5E_CANTREMOVE_g$LAYOUT.varHandle();
    static final MemorySegment H5E_CANTREMOVE_g$SEGMENT = RuntimeHelper.lookupGlobalVariable("H5E_CANTREMOVE_g", constants$103.H5E_CANTREMOVE_g$LAYOUT);
    static final  OfLong H5E_CANTCONVERT_g$LAYOUT = Constants$root.C_LONG_LONG$LAYOUT;
    static final VarHandle H5E_CANTCONVERT_g$VH = constants$103.H5E_CANTCONVERT_g$LAYOUT.varHandle();
    static final MemorySegment H5E_CANTCONVERT_g$SEGMENT = RuntimeHelper.lookupGlobalVariable("H5E_CANTCONVERT_g", constants$103.H5E_CANTCONVERT_g$LAYOUT);
    static final  OfLong H5E_BADSIZE_g$LAYOUT = Constants$root.C_LONG_LONG$LAYOUT;
    static final VarHandle H5E_BADSIZE_g$VH = constants$103.H5E_BADSIZE_g$LAYOUT.varHandle();
    static final MemorySegment H5E_BADSIZE_g$SEGMENT = RuntimeHelper.lookupGlobalVariable("H5E_BADSIZE_g", constants$103.H5E_BADSIZE_g$LAYOUT);
    static final FunctionDescriptor H5E_walk2_t$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle H5E_walk2_t$MH = RuntimeHelper.downcallHandle(
        constants$103.H5E_walk2_t$FUNC
    );
}


