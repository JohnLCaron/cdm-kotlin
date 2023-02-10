package com.sunya.cdm.util

// Note that this method is closely related to the logarithm base 2. For all positive int values x:
//  floor(log2(x)) = 31 - numberOfLeadingZeros(x)
//  ceil(log2(x)) = 32 - numberOfLeadingZeros(x - 1)
// LOOK maybe want ceil ??
fun log2(n: Int): Int {
    require(n > 0)
    return 31 - Integer.numberOfLeadingZeros(n)
}

// Note that this method is closely related to the logarithm base 2. For all positive long values x:
//  floor(log2(x)) = 63 - numberOfLeadingZeros(x)
//  ceil(log2(x)) = 64 - numberOfLeadingZeros(x - 1)
// LOOK maybe want ceil ??
fun log2(n: Long): Int {
    require(n > 0)
    return 63 - java.lang.Long.numberOfLeadingZeros(n)
}