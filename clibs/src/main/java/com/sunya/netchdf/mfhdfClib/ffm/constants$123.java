// Generated by jextract

package com.sunya.netchdf.mfhdfClib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.foreign.*;

class constants$123 {

    static final FunctionDescriptor ANendaccess$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle ANendaccess$MH = RuntimeHelper.downcallHandle(
        "ANendaccess",
        constants$123.ANendaccess$FUNC
    );
    static final FunctionDescriptor ANget_tagref$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle ANget_tagref$MH = RuntimeHelper.downcallHandle(
        "ANget_tagref",
        constants$123.ANget_tagref$FUNC
    );
    static final FunctionDescriptor ANid2tagref$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle ANid2tagref$MH = RuntimeHelper.downcallHandle(
        "ANid2tagref",
        constants$123.ANid2tagref$FUNC
    );
    static final FunctionDescriptor ANtagref2id$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_INT$LAYOUT,
        Constants$root.C_SHORT$LAYOUT,
        Constants$root.C_SHORT$LAYOUT
    );
    static final MethodHandle ANtagref2id$MH = RuntimeHelper.downcallHandle(
        "ANtagref2id",
        constants$123.ANtagref2id$FUNC
    );
    static final FunctionDescriptor ANatype2tag$FUNC = FunctionDescriptor.of(Constants$root.C_SHORT$LAYOUT,
        Constants$root.C_INT$LAYOUT
    );
    static final MethodHandle ANatype2tag$MH = RuntimeHelper.downcallHandle(
        "ANatype2tag",
        constants$123.ANatype2tag$FUNC
    );
    static final FunctionDescriptor ANtag2atype$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_SHORT$LAYOUT
    );
    static final MethodHandle ANtag2atype$MH = RuntimeHelper.downcallHandle(
        "ANtag2atype",
        constants$123.ANtag2atype$FUNC
    );
}


