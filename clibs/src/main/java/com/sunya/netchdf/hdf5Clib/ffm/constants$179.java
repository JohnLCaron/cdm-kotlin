// Generated by jextract

package com.sunya.netchdf.hdf5Clib.ffm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.lang.foreign.*;
import static java.lang.foreign.ValueLayout.*;
class constants$179 {

    static final FunctionDescriptor H5FD_family_init$FUNC = FunctionDescriptor.of(Constants$root.C_LONG_LONG$LAYOUT);
    static final MethodHandle H5FD_family_init$MH = RuntimeHelper.downcallHandle(
        "H5FD_family_init",
        constants$179.H5FD_family_init$FUNC
    );
    static final FunctionDescriptor H5Pset_fapl_family$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT
    );
    static final MethodHandle H5Pset_fapl_family$MH = RuntimeHelper.downcallHandle(
        "H5Pset_fapl_family",
        constants$179.H5Pset_fapl_family$FUNC
    );
    static final FunctionDescriptor H5Pget_fapl_family$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle H5Pget_fapl_family$MH = RuntimeHelper.downcallHandle(
        "H5Pget_fapl_family",
        constants$179.H5Pget_fapl_family$FUNC
    );
    static final FunctionDescriptor H5FD_hdfs_init$FUNC = FunctionDescriptor.of(Constants$root.C_LONG_LONG$LAYOUT);
    static final MethodHandle H5FD_hdfs_init$MH = RuntimeHelper.downcallHandle(
        "H5FD_hdfs_init",
        constants$179.H5FD_hdfs_init$FUNC
    );
    static final FunctionDescriptor H5Pget_fapl_hdfs$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle H5Pget_fapl_hdfs$MH = RuntimeHelper.downcallHandle(
        "H5Pget_fapl_hdfs",
        constants$179.H5Pget_fapl_hdfs$FUNC
    );
    static final FunctionDescriptor H5Pset_fapl_hdfs$FUNC = FunctionDescriptor.of(Constants$root.C_INT$LAYOUT,
        Constants$root.C_LONG_LONG$LAYOUT,
        Constants$root.C_POINTER$LAYOUT
    );
    static final MethodHandle H5Pset_fapl_hdfs$MH = RuntimeHelper.downcallHandle(
        "H5Pset_fapl_hdfs",
        constants$179.H5Pset_fapl_hdfs$FUNC
    );
}

