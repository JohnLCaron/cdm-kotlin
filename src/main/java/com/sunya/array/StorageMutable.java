/*
 * Copyright (c) 2023 John Caron
 * See LICENSE for license information.
 */
package com.sunya.array;

/** Storage that can be changed. */
public interface StorageMutable<T> extends Storage<T> {
  /** Set the ith element. */
  void setPrimitiveArray(int index, Object value);

  /** Get the ith element. */
  Object getPrimitiveArray(int index);
}
