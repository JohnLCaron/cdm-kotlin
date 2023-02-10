/*
 * Copyright (c) 2023 John Caron
 * See LICENSE for license information.
 */
package com.sunya.array;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/** A Composite of other RangeIterators. Iterate over them in sequence. */
@Immutable
public class RangeComposite implements RangeIterator {
  private final List<RangeIterator> ranges;
  private final String name;

  public RangeComposite(String name, List<RangeIterator> ranges) {
    this.name = name;
    this.ranges = ranges;
  }

  @Override
  public String name() {
    return name;
  }

  public List<RangeIterator> getRanges() {
    return ranges;
  }

  @Override
  public RangeIterator copyWithName(String name) {
    if (name.equals(this.name()))
      return this;
    return new RangeComposite(name, ranges);
  }

  @Override
  public Iterator<Integer> iterator() {
    return new CompositeIterator<>(new ArrayList<>(ranges));
  }

  @Override
  public int length() {
    int result = 0;
    for (RangeIterator r : ranges)
      result += r.length();
    return result;
  }

  // generic could be moved to utils
  private static class CompositeIterator<T> implements Iterator<T> {
    Iterator<Iterable<T>> iters;
    Iterator<T> current;

    CompositeIterator(Collection<Iterable<T>> iters) {
      this.iters = iters.iterator();
      current = this.iters.next().iterator();
    }

    @Override
    public boolean hasNext() {
      if (current.hasNext())
        return true;
      if (!iters.hasNext())
        return false;
      current = iters.next().iterator();
      return hasNext();
    }

    @Override
    public T next() {
      return current.next();
    }
  }

}
