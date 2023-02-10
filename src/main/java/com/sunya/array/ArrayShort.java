/*
 * Copyright (c) 2023 John Caron
 * See LICENSE for license information.
 */
package com.sunya.array;

import com.google.common.base.Preconditions;

import java.util.Arrays;
import java.util.Iterator;

;

/** Concrete implementation of Array specialized for Short. */
@Immutable
final class ArrayShort extends Array<Short> {
  private final Storage<Short> storage;

  /** Create an empty Array of type Short and the given shape. */
  ArrayShort(ArrayType dtype, int[] shape, short fillValue) {
    super(dtype, shape);
    short[] parray = new short[(int) indexFn.length()];
    Arrays.fill(parray, fillValue);
    storage = new StorageS(parray);
  }

  /** Create an Array of type Short and the given shape and storage. */
  ArrayShort(ArrayType dtype, int[] shape, Storage<Short> storage) {
    super(dtype, shape);
    Preconditions.checkArgument(indexFn.length() <= storage.length());
    this.storage = storage;
  }

  /** Create an Array of type Short and the given indexFn and storage. */
  private ArrayShort(ArrayType dtype, IndexFn indexFn, Storage<Short> storageD) {
    super(dtype, indexFn);
    Preconditions.checkArgument(indexFn.length() <= storageD.length());
    this.storage = storageD;
  }

  @Override
  Iterator<Short> fastIterator() {
    return storage.iterator();
  }

  @Override
  public Iterator<Short> iterator() {
    return indexFn.isCanonicalOrder() ? fastIterator() : new CanonicalIterator();
  }

  @Override
  public Short get(int... index) {
    return storage.get(indexFn.get(index));
  }

  @Override
  public Short get(Index index) {
    return get(index.getCurrentIndex());
  }

  @Override
  void arraycopy(int srcPos, Object dest, int destPos, long length) {
    if (indexFn.isCanonicalOrder()) {
      storage.arraycopy(srcPos, dest, destPos, length);
    } else {
      short[] ddest = (short[]) dest;
      int destIndex = destPos;
      Iterator<Integer> iter = indexFn.iterator(srcPos, length);
      while (iter.hasNext()) {
        ddest[destIndex++] = storage.get(iter.next());
      }
    }
  }

  @Override
  Storage<Short> storage() {
    return storage;
  }

  /** create new Array with given IndexFn and the same backing store */
  @Override
  protected ArrayShort createView(IndexFn view) {
    return new ArrayShort(this.arrayType, view, this.storage);
  }

  // used when the data is not in canonical order
  private class CanonicalIterator implements Iterator<Short> {
    private final Iterator<Integer> iter = indexFn.iterator();

    @Override
    public boolean hasNext() {
      return iter.hasNext();
    }

    @Override
    public Short next() {
      return storage.get(iter.next());
    }
  }

  // standard storage using short[] primitive array
  @Immutable
  static class StorageS implements Storage<Short> {
    private final short[] storage;

    StorageS(short[] storage) {
      this.storage = storage;
    }

    @Override
    public long length() {
      return storage.length;
    }

    @Override
    public Short get(long elem) {
      return storage[(int) elem];
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      System.arraycopy(storage, srcPos, dest, destPos, (int) length);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof StorageS shorts)) return false;
      return Arrays.equals(storage, shorts.storage);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(storage);
    }

    @Override
    public Iterator<Short> iterator() {
      return new StorageIter();
    }

    private final class StorageIter implements Iterator<Short> {
      private int count = 0;

      @Override
      public final boolean hasNext() {
        return count < storage.length;
      }

      @Override
      public final Short next() {
        return storage[count++];
      }
    }
  }

}
