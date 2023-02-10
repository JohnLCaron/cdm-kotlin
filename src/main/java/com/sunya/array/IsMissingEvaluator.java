/*
 * Copyright (c) 2023 John Caron
 * See LICENSE for license information.
 */
package com.sunya.array;

/** A mix-in interface for evaluating if a value is missing. */
public interface IsMissingEvaluator {
  /** true if there may be missing data */
  boolean hasMissing();

  /** Test if val is a missing data value */
  boolean isMissing(double val);
}
