/*
 * Copyright (c) 2023 John Caron
 * See LICENSE for license information.
 */

package com.sunya.array;

import java.util.Formatter;
import java.util.Iterator;

/** Compare two arrays. */
public class CompareArrayToArray {

  public static boolean compareData(String name, Array<?> org, Array<?> array) {
    Formatter f = new Formatter();
    boolean ok = compareData(f, name, org, array, false);
    if (f.toString().isEmpty()) {
      System.out.printf("%s%n", f);
    }
    return ok;
  }

  public static boolean compareData(Formatter f, String name, Array<?> org, Array<?> array, boolean justOne) {
    boolean ok = true;

    if (org.length() != array.length()) {
      f.format(" WARN  %s: data nelems %d !== %d%n", name, org.length(), array.length());
      // ok = false;
    }

    if (org.getArrayType() != array.getArrayType()) {
      f.format(" WARN  %s: dataType %s !== %s%n", name, org.getArrayType(), array.getArrayType());
      // ok = false;
    }

    if (!NumericCompare.compare(org.getShape(), array.getShape(), f)) {
      f.format(" WARN %s: data shape %s !== %s%n", name, java.util.Arrays.toString(org.getShape()),
          java.util.Arrays.toString(array.getShape()));
      // ok = false;
    }

    if (org.isVlen() != array.isVlen()) {
      f.format(" WARN %s: vlens dont match %s !~= %s%n", name, org.isVlen(), array.isVlen());
      ok = false;
    }

    if (!ok) {
      return false;
    }

    ArrayType dt = org.getArrayType();

    if (org instanceof ArrayVlen) {
      ArrayVlen<?> orgv = (ArrayVlen<?>) org;
      ArrayVlen<?> arrv = (ArrayVlen<?>) array;
      Iterator<?> iter1 = orgv.iterator();
      Iterator<?> iter2 = arrv.iterator();

      while (iter1.hasNext() && iter2.hasNext()) {
        Array<?> v1 = (Array<?>) iter1.next();
        Array<?> v2 = (Array<?>) iter2.next();
        ok &= compareData(f, name, v1, v2, justOne);
      }
      return ok;
    }

    switch (dt) {
      case CHAR, OPAQUE, BYTE, ENUM1, UBYTE -> {
        Iterator<Byte> iter1 = (Iterator<Byte>) org.iterator();
        Iterator<Byte> iter2 = (Iterator<Byte>) array.iterator();
        while (iter1.hasNext() && iter2.hasNext()) {
          byte v1 = iter1.next();
          byte v2 = iter2.next();
          if (v1 != v2) {
            f.format(createNumericDataDiffMessage(dt, name, v1, v2, 0));
            ok = false;
            if (justOne)
              break;
          }
        }
        break;
      }
      case DOUBLE -> {
        Iterator<Double> iter1 = (Iterator<Double>) org.iterator();
        Iterator<Double> iter2 = (Iterator<Double>) array.iterator();
        while (iter1.hasNext() && iter2.hasNext()) {
          double v1 = iter1.next();
          double v2 = iter2.next();
          if (!NumericCompare.nearlyEquals(v1, v2)) {
            f.format(createNumericDataDiffMessage(dt, name, v1, v2, 0));
            ok = false;
            if (justOne)
              break;
          }
        }
        break;
      }
      case FLOAT -> {
        Iterator<Float> iter1 = (Iterator<Float>) org.iterator();
        Iterator<Float> iter2 = (Iterator<Float>) array.iterator();
        while (iter1.hasNext() && iter2.hasNext()) {
          float v1 = iter1.next();
          float v2 = iter2.next();
          if (!NumericCompare.nearlyEquals(v1, v2)) {
            f.format(createNumericDataDiffMessage(dt, name, v1, v2, 0));
            ok = false;
            if (justOne)
              break;
          }
        }
        break;
      }
      case INT, ENUM4, UINT -> {
        Iterator<Integer> iter1 = (Iterator<Integer>) org.iterator();
        Iterator<Integer> iter2 = (Iterator<Integer>) array.iterator();
        while (iter1.hasNext() && iter2.hasNext()) {
          int v1 = iter1.next();
          int v2 = iter2.next();
          if (v1 != v2) {
            f.format(createNumericDataDiffMessage(dt, name, v1, v2, 0));
            ok = false;
            if (justOne)
              break;
          }
        }
        break;
      }
      case LONG, ULONG -> {
        Iterator<Long> iter1 = (Iterator<Long>) org.iterator();
        Iterator<Long> iter2 = (Iterator<Long>) array.iterator();
        while (iter1.hasNext() && iter2.hasNext()) {
          long v1 = iter1.next();
          long v2 = iter2.next();
          if (v1 != v2) {
            f.format(createNumericDataDiffMessage(dt, name, v1, v2, 0));
            ok = false;
            if (justOne)
              break;
          }
        }
        break;
      }


      /*
       * case OPAQUE: {
       * Iterator<Object> iter1 = (Iterator<Object>) org.iterator();
       * Iterator<Object> iter2 = (Iterator<Object>) array.iterator();
       * while (iter1.hasNext() && iter2.hasNext()) {
       * // Weve already unwrapped the VLEN part.
       * Array<Byte> v1 = (Array<Byte>) iter1.next();
       * Array<Byte> v2 = (Array<Byte>) iter2.next();
       * if (v1.length() != v2.length()) {
       * f.format(" DIFF %s: opaque sizes differ %d != %d%n", name, v1.length(), v2.length());
       * ok = false;
       * }
       * for (int idx = 0; idx < v1.length() && idx < v2.length(); idx++) {
       * if (!v1.get(idx).equals(v2.get(idx))) {
       * f.format(createNumericDataDiffMessage(dt, name, v1.get(idx), v2.get(idx), idx));
       * ok = false;
       * if (justOne)
       * break;
       * }
       * }
       * }
       * break;
       * }
       */

      case SHORT, ENUM2, USHORT -> {
        Iterator<Short> iter1 = (Iterator<Short>) org.iterator();
        Iterator<Short> iter2 = (Iterator<Short>) array.iterator();
        while (iter1.hasNext() && iter2.hasNext()) {
          short v1 = iter1.next();
          short v2 = iter2.next();
          if (v1 != v2) {
            f.format(createNumericDataDiffMessage(dt, name, v1, v2, 0));
            ok = false;
            if (justOne)
              break;
          }
        }
        break;
      }
      case STRING -> {
        Iterator<String> iter1 = (Iterator<String>) org.iterator();
        Iterator<String> iter2 = (Iterator<String>) array.iterator();
        while (iter1.hasNext() && iter2.hasNext()) {
          String v1 = iter1.next();
          String v2 = iter2.next();
          if (v1 == null || v2 == null) {
            iter1.next();
          }
          if (!v1.equals(v2)) {
            f.format(" DIFF string %s: %s != %s count=%s%n", name, v1, v2, 0);
            ok = false;
            if (justOne)
              break;
          }
        }
        break;
      }
      case STRUCTURE, SEQUENCE -> {
        Iterator<StructureData> iter1 = (Iterator<StructureData>) org.iterator();
        Iterator<StructureData> iter2 = (Iterator<StructureData>) array.iterator();
        int row = 0;
        while (iter1.hasNext() && iter2.hasNext()) {
          ok &= compareStructureData(f, iter1.next(), iter2.next(), justOne);
          row++;
        }
        break;
      }
      default -> {
        ok = false;
        f.format(" %s: Unknown data type %s%n", name, org.getArrayType());
      }
    }

    return ok;
  }

  private static String createNumericDataDiffMessage(ArrayType dt, String name, Number v1, Number v2, int idx) {
    return String.format(" DIFF %s %s: %s != %s;  count = %s, absDiff = %s, relDiff = %s %n", dt, name, v1, v2, idx,
            NumericCompare.absoluteDifference(v1.doubleValue(), v2.doubleValue()),
            NumericCompare.relativeDifference(v1.doubleValue(), v2.doubleValue()));
  }

  public static boolean compareStructureData(Formatter f, StructureData org, StructureData array, boolean justOne) {
    boolean ok = true;

    StructureMembers sm1 = org.getStructureMembers();
    StructureMembers sm2 = array.getStructureMembers();
    if (sm1.getMembers().size() != sm2.getMembers().size()) {
      f.format(" membersize %d !== %d%n", sm1.getMembers().size(), sm2.getMembers().size());
      ok = false;
    }

    for (StructureMembers.Member m1 : sm1.getMembers()) {
      StructureMembers.Member m2 = sm2.findMember(m1.getName());
      if (m2 == null) {
        System.out.printf("Cant find %s in copy%n", m1.getName());
        continue;
      }
      Array<?> data1 = org.getMemberData(m1);
      Array<?> data2 = array.getMemberData(m2);
      if (data2 != null) {
        f.format("    compare member %s %s%n", m1.getArrayType(), m1.getName());
        ok &= compareData(f, m1.getName(), data1, data2, justOne);
      }
    }

    return ok;
  }

  public static boolean compareSequence(Formatter f, String name, Iterator<StructureData> org,
      Iterator<StructureData> array) {
    boolean ok = true;
    int obsrow = 0;
    System.out.printf(" compareSequence %s%n", name);
    while (org.hasNext() && array.hasNext()) {
      ok &= compareStructureData(f, org.next(), array.next(), false);
      obsrow++;
    }
    return ok;
  }

}

