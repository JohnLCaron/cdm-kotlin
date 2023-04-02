package com.sunya.netchdf.netcdfClib;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;

import com.sunya.netchdf.netcdfClib.ffm.netcdf_h;
import org.junit.jupiter.api.Test;
import com.sunya.testdata.TestFilesKt;

public class TestNetcdfClib {

  @Test
  public void cantOpenProblem() throws IOException {
    open(TestFilesKt.testData + "devcdm/netcdf3/simple_xy.nc");
  }

  private void open(String filename) {
    try (var session = MemorySession.openConfined()) {
      MemorySegment filenameSeg = session.allocateUtf8String(filename);
      MemorySegment fileHandle = session.allocate(netcdf_h.C_INT, 0);

      // nc_open(const char *path, int mode, int *ncidp);
      // public static int nc_open ( Addressable path,  int mode,  Addressable ncidp) {
      int ret = netcdf_h.nc_open(filenameSeg, 0, fileHandle);
      int ncid = fileHandle.get(netcdf_h.C_INT, 0);
      System.out.printf("nc_open %s ret %d fileHandle %d%n", filename, ret, ncid);

    }
  }

}
