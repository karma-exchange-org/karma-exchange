package org.karmaexchange.util;

import java.util.HashSet;

@SuppressWarnings("serial")
public class BoundedHashSet<E> extends HashSet<E> {

  private int limit;

  public static <E> BoundedHashSet<E> create(int limit) {
    return new BoundedHashSet<E>(limit);
  }

  private BoundedHashSet(int limit) {
    this.limit = limit;
  }

  @Override
  public boolean add(E e) {
    if (limitReached()) {
      throw new IllegalStateException("Bounded hash set is full: limit=" + limit);
    }
    return super.add(e);
  }

  public void addIfSpace(E e) {
    if (!limitReached()) {
      add(e);
    }
  }

  public boolean limitReached() {
    return size() >= limit;
  }
}
