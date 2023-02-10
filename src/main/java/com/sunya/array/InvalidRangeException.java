/*
 * Copyright (c) 2023 John Caron
 * See LICENSE for license information.
 */
package com.sunya.array;

/** Thrown if an attempt is made to use an invalid Range to index an array. */
public class InvalidRangeException extends Exception {
  public InvalidRangeException(String s) {
    super(s);
  }
}
