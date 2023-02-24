package com.sunya.netchdf.netcdfClib;

import java.lang.foreign.*;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static com.sunya.netchdf.netcdfClib.ffm.netcdf_h.*;

// sanity check for NClib primitives
public class NCJheader {

  public static void main(String[] args) {
    new NCJheader().testFailure("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/string_attrs.nc4");
  }

  void testFailure(String filename) {
    try (MemorySession session = MemorySession.openConfined()) {
      MemorySegment filenameSeg = session.allocateUtf8String(filename);
      MemorySegment fileHandle = session.allocate(JAVA_INT, 0);

      checkErr("nc_open", nc_open(filenameSeg, 0, fileHandle));
      int ncid = fileHandle.get(JAVA_INT, 0);

      MemorySegment name_p = session.allocateUtf8String("NULL_STR_GATTR");
      readAttributeValues(session, ncid, NC_GLOBAL(), name_p, 1);
    }
  }

  void readAttributeValues(MemorySession session, int grpid, int varid, MemorySegment name_p, int nelems) {
    MemorySegment strings_p = session.allocateArray(ValueLayout.ADDRESS, nelems);
    // MemorySegment strings_p = session.allocate(ValueLayout.ADDRESS);
    checkErr("nc_get_att_string", nc_get_att_string(grpid, varid, name_p, strings_p));
    MemoryAddress s2 = strings_p.getAtIndex(ValueLayout.ADDRESS, 0);
    // MemoryAddress s2 = strings_p.get(ValueLayout.ADDRESS, 0);
    String s = s2.getUtf8String(0);
    System.out.printf("readAttributeValues '%s'%n", s);
  }

  void checkErr (String where, int ret) {
    if (ret != 0) {
      throw new RuntimeException(
              String.format("%s return %d = %s%n", where, ret, nc_strerror(ret).toString()));
    }
  }
}
