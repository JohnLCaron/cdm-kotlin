/*
 * Copyright (c) 2023 John Caron
 * See LICENSE for license information.
 */
package com.sunya.array;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import java.util.Arrays;
import java.util.Iterator;

;

/** Concrete implementation of Array specialized for Integer. */
@Immutable
class ArrayInteger extends Array<Integer> {
  private final Storage<Integer> storage;

  /** Create an empty Array of type Integer and the given shape. */
  ArrayInteger(ArrayType dtype, int[] shape, int fillValue) {
    super(dtype, shape);
    int[] parray = new int[(int) indexFn.length()];
    Arrays.fill(parray, fillValue);
    storage = new StorageS(parray);
  }

  /** Create an Array of type Integer and the given shape and storage. */
  ArrayInteger(ArrayType dtype, int[] shape, Storage<Integer> storage) {
    super(dtype, shape);
    if (indexFn.length() > storage.length()) {
      throw new IllegalArgumentException(String.format("shape %d > storage %d", indexFn.length(), storage.length()));
    }
    this.storage = storage;
  }

  /** Create an Array of type Integer and the given indexFn and storage. */
  private ArrayInteger(ArrayType dtype, IndexFn indexFn, Storage<Integer> storageD) {
    super(dtype, indexFn);
    Preconditions.checkArgument(indexFn.length() <= storageD.length());
    this.storage = storageD;
  }

  @Override
  Iterator<Integer> fastIterator() {
    return storage.iterator();
  }

  @Override
  public Iterator<Integer> iterator() {
    return indexFn.isCanonicalOrder() ? fastIterator() : new CanonicalIterator();
  }

  @Override
  public Integer get(int... index) {
    return storage.get(indexFn.get(index));
  }

  @Override
  public Integer get(Index index) {
    return get(index.getCurrentIndex());
  }

  @Override
  void arraycopy(int srcPos, Object dest, int destPos, long length) {
    if (indexFn.isCanonicalOrder()) {
      storage.arraycopy(srcPos, dest, destPos, length);
    } else {
      int[] ddest = (int[]) dest;
      int destIndex = destPos;
      Iterator<Integer> iter = indexFn.iterator(srcPos, length);
      while (iter.hasNext()) {
        ddest[destIndex++] = storage.get(iter.next());
      }
    }
  }

  @Override
  Storage<Integer> storage() {
    return storage;
  }

  /** create new Array with given IndexFn and the same backing store */
  @Override
  protected ArrayInteger createView(IndexFn view) {
    return new ArrayInteger(this.arrayType, view, this.storage);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), storage.hashCode());
  }

  // used when the data is not in canonical order
  private class CanonicalIterator implements Iterator<Integer> {
    private final Iterator<Integer> iter = indexFn.iterator();

    @Override
    public boolean hasNext() {
      return iter.hasNext();
    }

    @Override
    public Integer next() {
      return storage.get(iter.next());
    }
  }

  // standard storage using int[] primitive array
  @Immutable
  static final class StorageS implements Storage<Integer> {
    private final int[] storage;

    StorageS(int[] storage) {
      this.storage = storage;
    }

    @Override
    public long length() {
      return storage.length;
    }

    @Override
    public Integer get(long elem) {
      return storage[(int) elem];
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      System.arraycopy(storage, srcPos, dest, destPos, (int) length);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof StorageS integers)) return false;
      return Arrays.equals(storage, integers.storage);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(storage);
    }

    @Override
    public Iterator<Integer> iterator() {
      return new StorageIter();
    }

    private final class StorageIter implements Iterator<Integer> {
      private int count = 0;

      @Override
      public boolean hasNext() {
        return count < storage.length;
      }

      @Override
      public Integer next() {
        return storage[count++];
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (!super.equals(o)) {
      return false;
    }
    Array<Integer> a2 = (Array<Integer>) o;
    for (int idx = 0; idx < getSize(); idx++) {
      if (!this.get(idx).equals(a2.get(idx))) {
        return false;
      }
    }
    return true;
  }

}
