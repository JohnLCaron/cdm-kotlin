package com.sunya.array;

/** Maintains indentation level for printing nested structures. */
public class Indent {
  private final String blanks = " ".repeat(100);
  private final int nspaces;

  private int level;

  /** Create an Indent with nspaces per level. */
  public Indent(int nspaces) {
    this.nspaces = nspaces;
    this.level = 1;
  }

  /** Create an Indent with nspaces per level. */
  public Indent(int nspaces, int level) {
    this.nspaces = nspaces;
    this.level = level;
  }

  public Indent incr() {
    level++;
    return this;
  }

  /** Decrement the indent level */
  public Indent decr() {
    level--;
    return this;
  }

  /** Increment the indent level, return new object */
  public Indent incrNew() {
    return new Indent(nspaces, level + 1);
  }

  /** Decrement the indent level, , return new object */
  public Indent decrNew() {
    return new Indent(nspaces, level - 1);
  }

  /** Return a String of nspaces * level blanks which is the indentation. */
  public String toString() {
    return blanks.substring(0, nspaces * level);
  }
}
