package com.sunya.array;

import com.google.common.base.Preconditions;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.Formatter;

public class PrintArray {

  /** Print named array to returned String. */
  public static String printArray(Array<?> array) {
    Formatter out = new Formatter();
    printArray(out, array, new Indent(2));
    return out.toString();
  }

  public static Formatter printArray(Formatter out, Array<?> array, Indent ilev) {
    if (null == out) {
      out = new Formatter();
    }

    if (array == null) {
      out.format("null array");
      return out;
    }

    if (array.getArrayType() == ArrayType.CHAR) {
      printCharArray(out, (Array<Byte>) array, ilev);

    } else if (array.getArrayType() == ArrayType.STRING) {
      printStringArray(out, (Array<String>) array, ilev);

    } else if (array instanceof StructureDataArray) {
      printStructureDataArray(out, (StructureDataArray) array, ilev);

    }
    else if (array instanceof ArrayVlen) {
      printVlenArray(out, (ArrayVlen<?>) array, ilev);
    } else {
      printRegularArray(out, array, ilev);
    }

    out.flush();
    return out;
  }

  private static void printRegularArray(Formatter out, Array<?> ma, Indent indent) {
    int rank = ma.getRank();

    // scalar
    if (rank == 0) {
      Object value = ma.getScalar();

      if (ma.getArrayType().isUnsigned()) {
        Preconditions.checkArgument(value instanceof Number, "A data type being unsigned implies that it is numeric.");

        // "value" is an unsigned number, but it will be treated as signed when we print it below, because Java only
        // has signed types. If it's large enough ( >= 2^(BIT_WIDTH-1) ), its most-significant bit will be interpreted
        // as the sign bit, which will result in an invalid (negative) value being printed. To prevent that, we're
        // going to widen the number before printing it, but only if the unsigned number is being seen as negative.
        value = ArrayType.widenNumberIfNegative((Number) value);
      }

      out.format("%s%s", indent, value);
      return;
    }

    int[] dims = ma.getShape();
    int last = dims[0];

    IndexFn indexFn = IndexFn.builder(ma.getShape()).build();

    if ((rank == 1) && (ma.getArrayType() != ArrayType.STRUCTURE)) {
      out.format("%s[", indent);
      for (int ii = 0; ii < last; ii++) {
        Object value = ma.get(indexFn.odometer(ii));

        if (ma.getArrayType().isUnsigned()) {
          Preconditions.checkArgument(value instanceof Number,
                  "A data type being unsigned implies that it is numeric.");
          value = ArrayType.widenNumberIfNegative((Number) value);
        }

        if (ii > 0) {
          out.format(", ");
        }
        out.format("%s", value);
      }
      out.format("]");
      return;
    }

    out.format("%s[", indent);
    indent.incr();
    for (int ii = 0; ii < last; ii++) {
      Array<?> slice = null;
      try {
        slice = Arrays.slice(ma, 0, ii);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
      }
      if (ii > 0) {
        out.format(", ");
      }
      out.format("\n");
      printArray(out, slice, indent);
    }
    indent.decr();
    out.format("\n%s]", indent);
  }

  /**
   * Print array as undifferentiated sequence of values.
   *
   * @param array any Array except StructureDataArray
   */
  public static String printArrayPlain(Array<?> array) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    for (Object val : array) {
      pw.print(val);
      pw.print(' ');
    }
    return sw.toString();
  }

  private static void printCharArray(Formatter out, Array<Byte> ma, Indent indent) {
    int rank = ma.getRank();

    if (rank == 1) {
      out.format("  \"%s\"", Arrays.makeStringFromChar(ma));
      return;
    }

    if (rank == 2) {
      boolean first = true;
      Array<String> strings = Arrays.makeStringsFromChar(ma);
      for (String s : strings) {
        if (!first)
          out.format(", ");
        out.format("  \"%s\"", s);
        first = false;
      }
      return;
    }

    int[] dims = ma.getShape();
    int last = dims[0];

    out.format("%n%s{", indent);
    indent.incr();
    for (int ii = 0; ii < last; ii++) {
      Array<Byte> slice = null;
      try {
        slice = Arrays.slice(ma, 0, ii);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
      }
      if (ii > 0)
        out.format(",");
      printCharArray(out, slice, indent);
    }
    indent.decr();

    out.format("%n%s}", indent);
  }

  private static void printByteBuffer(Formatter out, ByteBuffer bb, Indent indent) {
    out.format("%s0x", indent);
    int last = bb.limit() - 1;
    if (last < 0)
      out.format("00");
    else
      for (int i = bb.position(); i <= last; i++) {
        out.format("%02x", bb.get(i));
      }
  }

  private static void printStringArray(Formatter out, Array<String> ma, Indent indent) {
    int rank = ma.getRank();

    if (rank == 0) {
      out.format(" \"%s\"", ma.get(0));
      return;
    }

    if (rank == 1) {
      boolean first = true;
      for (int i = 0; i < ma.length(); i++) {
        if (!first)
          out.format(", ");
        out.format("\"%s\"", ma.get(i));
        first = false;
      }
      return;
    }

    int[] dims = ma.getShape();
    int last = dims[0];

    out.format("%n%s{", indent);
    indent.incr();
    for (int ii = 0; ii < last; ii++) {
      Array<String> slice = null;
      try {
        slice = Arrays.slice(ma, 0, ii);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
      }
      if (ii > 0)
        out.format(",");
      printStringArray(out, slice, indent);
    }
    indent.decr();
    out.format("%n%s}", indent);
  }

  private static void printStructureDataArray(Formatter out, StructureDataArray array, Indent indent) {
    int count = 0;
    for (StructureData sdata : array) {
      out.format("%n%s{%n", indent);
      printStructureData(out, sdata, indent);
      out.format("%s} %s(%d)", indent, sdata.getName(), count);
      count++;
    }
  }

  private static void printVlenArray(Formatter out, ArrayVlen<?> vlen, Indent indent) {
    out.format("%n%s{", indent);
    indent.incr();
    int count = 0;
    for (Array<?> inner : vlen) {
      out.format("%s%n", (count == 0) ? "," : ";"); // peek ahead
      printArray(out, inner, indent);
      count++;
    }
    indent.decr();
    out.format("%n%s}", indent);
  }

  /** Print StructureData. */
  public static String printStructureData(StructureData sdata) {
    Formatter out = new Formatter();
    for (StructureMembers.Member m : sdata.getStructureMembers()) {
      Array<?> memData = sdata.getMemberData(m);
      if (memData.getArrayType() == ArrayType.CHAR) {
        out.format("%s", Arrays.makeStringFromChar((Array<Byte>) memData));
      } else {
        printArray(out, memData, new Indent(2));
      }
      out.format(",");
    }
    return out.toString();
  }

  private static void printStructureData(Formatter out, StructureData sdata, Indent indent) {
    indent.incr();
    for (StructureMembers.Member m : sdata.getStructureMembers()) {
      Array<?> sdataArray = sdata.getMemberData(m);
      out.format("%s%s = ", indent, m.getName());
      printArray(out, sdataArray, indent);
    }
    indent.decr();
  }
}
