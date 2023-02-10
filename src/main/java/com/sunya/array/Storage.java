/*
 * Copyright (c) 2023 John Caron
 * See LICENSE for license information.
 */
package com.sunya.array;

/** Abstraction for storing Array data. Always deals with storage as 1-dimensional. */
public interface Storage<T> extends Iterable<T> {

  /** Number of elements. */
  long length();

  /** Get the ith element. */
  T get(long elem);

  /**
   * Copy all or a portion to dest array.
   * Mimic of System.arraycopy(Object src, int srcPos, Object dest, int destPos, int length);
   * 
   * @param srcPos starting pos in this source.
   * @param dest destination primitive array of type T.
   * @param destPos starting pos in destination.
   * @param length copy these number of elements.
   */
  void arraycopy(int srcPos, Object dest, int destPos, long length);
}
