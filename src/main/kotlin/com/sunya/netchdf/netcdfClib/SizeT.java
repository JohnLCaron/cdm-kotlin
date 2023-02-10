package com.sunya.netchdf.netcdfClib;

/*
Make equivilent of
/home/snake/dev/github/netcdf/netcdf-java/netcdf4/src/main/java/ucar/nc2/jni/netcdf/SizeTByReference.java

Clients must be aware of the current platform if they target C functions which use scalar types such as long, int,
and size_t. This is because the association of scalar C types with layout constants varies by platform.
On Windows/x64, a C long is associated with the JAVA_INT layout, so the required FunctionDescriptor would be
FunctionDescriptor.of(JAVA_INT, JAVA_INT) and the type of the downcall method handle would be the Java signature int to int.
 */
public class SizeT {
}
