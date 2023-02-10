/*
 * Copyright (c) 2023 John Caron
 * See LICENSE for license information.
 */
package com.sunya.array;

import com.google.common.base.Preconditions;

import java.util.Arrays;
import java.util.Iterator;

;

/**
 * Concrete implementation of Array specialized for Strings.
 * A String might be null.
 */
@Immutable
final class ArrayString extends Array<String> {
  private final Storage<String> storage;

  /** Create an empty Array of type String and the given shape. */
  ArrayString(int[] shape) {
    super(ArrayType.STRING, shape);
    storage = new StorageS(new String[(int) indexFn.length()]);
  }

  /** Create an Array of type String and the given shape and storage. */
  ArrayString(int[] shape, Storage<String> storage) {
    super(ArrayType.STRING, shape);
    Preconditions.checkArgument(indexFn.length() <= storage.length());
    this.storage = storage;
  }

  /** Create an Array of type String and the given indexFn and storage. */
  private ArrayString(IndexFn indexFn, Storage<String> storageD) {
    super(ArrayType.STRING, indexFn);
    Preconditions.checkArgument(indexFn.length() <= storageD.length());
    this.storage = storageD;
  }

  @Override
  Iterator<String> fastIterator() {
    return storage.iterator();
  }

  @Override
  public Iterator<String> iterator() {
    return indexFn.isCanonicalOrder() ? fastIterator() : new CanonicalIterator();
  }

  @Override
  public String get(int... index) {
    return storage.get(indexFn.get(index));
  }

  @Override
  public String get(Index index) {
    return get(index.getCurrentIndex());
  }

  @Override
  void arraycopy(int srcPos, Object dest, int destPos, long length) {
    if (indexFn.isCanonicalOrder()) {
      storage.arraycopy(srcPos, dest, destPos, length);
    } else {
      String[] ddest = (String[]) dest;
      int destIndex = destPos;
      Iterator<Integer> iter = indexFn.iterator(srcPos, length);
      while (iter.hasNext()) {
        ddest[destIndex++] = storage.get(iter.next());
      }
    }
  }

  @Override
  Storage<String> storage() {
    return storage;
  }

  /** create new Array with given IndexFn and the same backing store */
  @Override
  protected ArrayString createView(IndexFn view) {
    return new ArrayString(view, storage);
  }

  // used when the data is not in canonical order
  private class CanonicalIterator implements Iterator<String> {
    private final Iterator<Integer> iter = indexFn.iterator();

    @Override
    public boolean hasNext() {
      return iter.hasNext();
    }

    @Override
    public String next() {
      return storage.get(iter.next());
    }
  }

  // standard storage using String[] primitive array
  @Immutable
  static final class StorageS implements Storage<String> {
    private final String[] storage;

    StorageS(String[] storage) {
      this.storage = storage;
    }

    @Override
    public long length() {
      return storage.length;
    }

    @Override
    public String get(long elem) {
      return storage[(int) elem];
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      System.arraycopy(storage, srcPos, dest, destPos, (int) length);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof StorageS strings)) return false;
      return Arrays.equals(storage, strings.storage);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(storage);
    }

    @Override
    public Iterator<String> iterator() {
      return new StorageIter();
    }

    private final class StorageIter implements Iterator<String> {
      private int count = 0;

      @Override
      public final boolean hasNext() {
        return count < storage.length;
      }

      @Override
      public final String next() {
        return storage[count++];
      }
    }
  }

}
