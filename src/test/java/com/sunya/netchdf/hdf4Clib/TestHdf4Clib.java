package com.sunya.netchdf.hdf4Clib;

import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;

import static com.sunya.netchdf.mfhdfClib.ffm.mfhdf_h.DFACC_READ;
import static com.sunya.netchdf.mfhdfClib.ffm.mfhdf_h.SDstart;
import static com.sunya.netchdf.mfhdfClib.ffm.mfhdf_h.SDend;
import static test.util.TestFilesKt.testData;

public class TestHdf4Clib {

  @Test
  public void testOpenLibrary() {
    open(testData + "netchdf/hdf4/chlora/MODSCW_P2009168_C4_1805_1810_1940_1945_GM03_closest_chlora.hdf");
  }

  private void open(String filename) {
    try (var session = MemorySession.openConfined()) {
      MemorySegment filenameSeg = session.allocateUtf8String(filename);

      int sd_id = SDstart(filenameSeg, DFACC_READ());
      System.out.printf("SDstart %s sd_id=%d %n", filename, sd_id);

      int ret = SDend(sd_id);
      System.out.printf("SDend ret=%d %n", ret);
    }
  }

}
