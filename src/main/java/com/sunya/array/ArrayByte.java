/*
 * Copyright (c) 2023 John Caron
 * See LICENSE for license information.
 */
package com.sunya.array;

import com.google.common.base.Preconditions;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;

;

/** Concrete implementation of Array specialized for Byte. */
@Immutable
final class ArrayByte extends Array<Byte> {
  private final Storage<Byte> storage;

  /** Create an Array of type Byte and the given shape, filled with the given value. */
  ArrayByte(ArrayType dtype, int[] shape, byte fillValue) {
    super(dtype, shape);
    byte[] parray = new byte[(int) indexFn.length()];
    java.util.Arrays.fill(parray, fillValue);
    storage = new StorageS(parray);
  }

  /** Create an Array of type Byte and the given shape and storage. */
  ArrayByte(ArrayType dtype, int[] shape, Storage<Byte> storage) {
    super(dtype, shape);
    Preconditions.checkArgument(indexFn.length() <= storage.length());
    this.storage = storage;
  }

  /** Create an Array of type Byte and the given indexFn and storage. */
  private ArrayByte(ArrayType dtype, IndexFn indexFn, Storage<Byte> storageD) {
    super(dtype, indexFn);
    Preconditions.checkArgument(indexFn.length() <= storageD.length());
    this.storage = storageD;
  }

  @Override
  Iterator<Byte> fastIterator() {
    return storage.iterator();
  }

  @Override
  public Iterator<Byte> iterator() {
    return indexFn.isCanonicalOrder() ? fastIterator() : new CanonicalIterator();
  }

  @Override
  public Byte get(int... index) {
    return storage.get(indexFn.get(index));
  }

  @Override
  public Byte get(Index index) {
    return get(index.getCurrentIndex());
  }

  @Override
  void arraycopy(int srcPos, Object dest, int destPos, long length) {
    if (indexFn.isCanonicalOrder()) {
      storage.arraycopy(srcPos, dest, destPos, length);
    } else {
      byte[] ddest = (byte[]) dest;
      int destIndex = destPos;
      Iterator<Integer> iter = indexFn.iterator(srcPos, length);
      while (iter.hasNext()) {
        ddest[destIndex++] = storage.get(iter.next());
      }
    }
  }

  /**
   * Create a String out of this Array, collapsing all dimensions into one.
   * If there is a null (zero) value in the array, the String will end there.
   * The null is not returned as part of the String.
   */
  String makeStringFromChar() {
    int count = 0;
    for (byte c : this) {
      if (c == 0) {
        break;
      }
      count++;
    }
    byte[] carr = new byte[count];
    int idx = 0;
    for (byte c : this) {
      if (c == 0) {
        break;
      }
      carr[idx++] = c;
    }
    return new String(carr, StandardCharsets.UTF_8);
  }

  /**
   * Create an Array of Strings out of this Array of any rank.
   * If there is a null (zero) value in the Array array, the String will end there.
   * The null is not returned as part of the String.
   *
   * @return Array of Strings of rank - 1.
   */
  Array<String> makeStringsFromChar() {
    if (getRank() < 2) {
      return Arrays.factory(ArrayType.STRING, new int[] {1}, new String[] {makeStringFromChar()});
    }
    int innerLength = this.indexFn.getShape(this.rank - 1);
    int outerLength = (int) this.length() / innerLength;
    int[] outerShape = new int[this.rank - 1];
    System.arraycopy(this.getShape(), 0, outerShape, 0, this.rank - 1);

    String[] result = new String[outerLength];
    byte[] carr = new byte[innerLength];

    int cidx = 0;
    int sidx = 0;

    IndexFn indexFn = IndexFn.builder(getShape()).build();
    while (sidx < outerLength) {
      int idx = sidx * innerLength + cidx;
      byte c = get(indexFn.odometer(idx));
      if (c == 0) {
        result[sidx++] = new String(carr, 0, cidx, StandardCharsets.UTF_8);
        cidx = 0;
        continue;
      }
      carr[cidx++] = c;
      if (cidx == innerLength) {
        result[sidx++] = new String(carr, StandardCharsets.UTF_8);
        cidx = 0;
      }
    }
    return Arrays.factory(ArrayType.STRING, outerShape, result);
  }

  @Override
  Storage<Byte> storage() {
    return storage;
  }

  /** create new Array with given IndexFn and the same backing store */
  @Override
  protected ArrayByte createView(IndexFn view) {
    return new ArrayByte(this.arrayType, view, this.storage);
  }

  // used when the data is not in canonical order
  private class CanonicalIterator implements Iterator<Byte> {
    private final Iterator<Integer> iter = indexFn.iterator();

    @Override
    public boolean hasNext() {
      return iter.hasNext();
    }

    @Override
    public Byte next() {
      return storage.get(iter.next());
    }
  }

  // standard storage using byte[] primitive array
  @Immutable
  static final class StorageS implements Storage<Byte> {
    private final byte[] storage;

    StorageS(byte[] storage) {
      this.storage = storage;
    }

    @Override
    public long length() {
      return storage.length;
    }

    @Override
    public Byte get(long elem) {
      return storage[(int) elem];
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      System.arraycopy(storage, srcPos, dest, destPos, (int) length);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof StorageS bytes)) return false;
      return java.util.Arrays.equals(storage, bytes.storage);
    }

    @Override
    public int hashCode() {
      return java.util.Arrays.hashCode(storage);
    }

    @Override
    public Iterator<Byte> iterator() {
      return new StorageIter();
    }

    private final class StorageIter implements Iterator<Byte> {
      private int count = 0;

      @Override
      public final boolean hasNext() {
        return count < storage.length;
      }

      @Override
      public final Byte next() {
        return storage[count++];
      }
    }
  }

}
