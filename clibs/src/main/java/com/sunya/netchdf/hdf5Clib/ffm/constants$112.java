// Generated by jextract

package com.sunya.netchdf.hdf5Clib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
class constants$112 {

    static final FunctionDescriptor H5Funmount$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle H5Funmount$MH = RuntimeHelper.downcallHandle(
        "H5Funmount",
        constants$112.H5Funmount$FUNC
    );
    static final FunctionDescriptor H5Fget_freespace$FUNC = FunctionDescriptor.of(Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle H5Fget_freespace$MH = RuntimeHelper.downcallHandle(
        "H5Fget_freespace",
        constants$112.H5Fget_freespace$FUNC
    );
    static final FunctionDescriptor H5Fget_filesize$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle H5Fget_filesize$MH = RuntimeHelper.downcallHandle(
        "H5Fget_filesize",
        constants$112.H5Fget_filesize$FUNC
    );
    static final FunctionDescriptor H5Fget_eoa$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle H5Fget_eoa$MH = RuntimeHelper.downcallHandle(
        "H5Fget_eoa",
        constants$112.H5Fget_eoa$FUNC
    );
    static final FunctionDescriptor H5Fincrement_filesize$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle H5Fincrement_filesize$MH = RuntimeHelper.downcallHandle(
        "H5Fincrement_filesize",
        constants$112.H5Fincrement_filesize$FUNC
    );
    static final FunctionDescriptor H5Fget_file_image$FUNC = FunctionDescriptor.of(Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle H5Fget_file_image$MH = RuntimeHelper.downcallHandle(
        "H5Fget_file_image",
        constants$112.H5Fget_file_image$FUNC
    );
}


