// Generated by jextract

package com.sunya.netchdf.hdf5Clib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
class constants$152 {

    static final FunctionDescriptor H5Pget_metadata_read_attempts$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle H5Pget_metadata_read_attempts$MH = RuntimeHelper.downcallHandle(
        "H5Pget_metadata_read_attempts",
        constants$152.H5Pget_metadata_read_attempts$FUNC
    );
    static final FunctionDescriptor H5Pset_object_flush_cb$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle H5Pset_object_flush_cb$MH = RuntimeHelper.downcallHandle(
        "H5Pset_object_flush_cb",
        constants$152.H5Pset_object_flush_cb$FUNC
    );
    static final FunctionDescriptor H5Pget_object_flush_cb$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle H5Pget_object_flush_cb$MH = RuntimeHelper.downcallHandle(
        "H5Pget_object_flush_cb",
        constants$152.H5Pget_object_flush_cb$FUNC
    );
    static final FunctionDescriptor H5Pset_mdc_log_options$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_BOOL$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_BOOL$LAYOUT
    );
    static final MethodHandle H5Pset_mdc_log_options$MH = RuntimeHelper.downcallHandle(
        "H5Pset_mdc_log_options",
        constants$152.H5Pset_mdc_log_options$FUNC
    );
    static final FunctionDescriptor H5Pget_mdc_log_options$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle H5Pget_mdc_log_options$MH = RuntimeHelper.downcallHandle(
        "H5Pget_mdc_log_options",
        constants$152.H5Pget_mdc_log_options$FUNC
    );
    static final FunctionDescriptor H5Pset_evict_on_close$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_BOOL$LAYOUT
    );
    static final MethodHandle H5Pset_evict_on_close$MH = RuntimeHelper.downcallHandle(
        "H5Pset_evict_on_close",
        constants$152.H5Pset_evict_on_close$FUNC
    );
}

