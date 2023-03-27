
package com.sunya.io;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteOrder;

public class RandomAccessFile implements Closeable {
  public static final int BIG_ENDIAN = 0;
  public static final int LITTLE_ENDIAN = 1;

  protected static final int defaultBufferSize = 8092; // The default buffer size, in bytes.

  protected String location;

  /**
   * The underlying java.io.RandomAccessFile.
   */
  protected java.io.RandomAccessFile file;
  protected java.nio.channels.FileChannel fileChannel;

  /**
   * The offset in bytes from the file start, of the next read or
   * write operation.
   */
  protected long filePosition;

  /**
   * The buffer used for reading the data.
   */
  protected byte[] buffer;

  /**
   * The offset in bytes of the start of the buffer, from the start of the file.
   */
  protected long bufferStart;

  /**
   * The offset in bytes of the end of the data in the buffer, from
   * the start of the file. This can be calculated from
   * <code>bufferStart + dataSize</code>, but it is cached to speed
   * up the read( ) method.
   */
  protected long dataEnd;

  /**
   * The size of the data stored in the buffer, in bytes. This may be
   * less than the size of the buffer.
   */
  protected int dataSize;

  /**
   * True if we are at the end of the file.
   */
  protected boolean endOfFile;

  /**
   * The current endian (big or little) mode of the file.
   */
  protected ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

  /**
   * Constructor, default buffer size.
   *
   * @param location location of the file
   * @param mode same as for java.io.RandomAccessFile, usually "r" or "rw"
   * @throws IOException on open error
   */
  public RandomAccessFile(String location, String mode) throws IOException {
    this(location, mode, defaultBufferSize);
    this.location = location;
  }

  /**
   * Constructor.
   *
   * @param location location of the file
   * @param mode same as for java.io.RandomAccessFile
   * @param bufferSize size of buffer to use.
   * @throws IOException on open error
   */
  public RandomAccessFile(String location, String mode, int bufferSize) throws IOException {
    if (bufferSize < 0)
      bufferSize = defaultBufferSize;
    this.location = location;
    this.file = new java.io.RandomAccessFile(location, mode);
    init(bufferSize);
  }

  private void init(int bufferSize) {
    // Initialise the buffer
    bufferStart = 0;
    dataEnd = 0;
    dataSize = 0;
    filePosition = 0;
    buffer = new byte[bufferSize];
    endOfFile = false;
  }

  /**
   * Close the file, and release any associated system resources.
   *
   * @throws IOException if an I/O error occurrs.
   */
  public synchronized void close() throws IOException {
    if (file == null)
      return;

    // Close the underlying file object.
    file.close();
    file = null; // help the gc
  }

  /**
   * Set the position in the file for the next read or write.
   *
   * @param pos the offset (in bytes) from the start of the file.
   * @throws IOException if an I/O error occurrs.
   */
  public void seek(long pos) throws IOException {
    if (pos < 0)
      throw new IOException("Negative seek offset");

    // If the seek is into the buffer, just update the file pointer.
    if ((pos >= bufferStart) && (pos < dataEnd)) {
      filePosition = pos;
      endOfFile = false;
      return;
    }

    // need new buffer, starting at pos
    readBuffer(pos);
  }

  protected void readBuffer(long pos) throws IOException {
    bufferStart = pos;
    filePosition = pos;

    dataSize = read_(pos, buffer, 0, buffer.length);

    if (dataSize <= 0) {
      dataSize = 0;
      endOfFile = true;
    } else {
      endOfFile = false;
    }

    // Cache the position of the buffer end.
    dataEnd = bufferStart + dataSize;
  }

  /**
   * Get the file location, or name.
   *
   * @return file location
   */
  public String getLocation() {
    return location;
  }

  /**
   * Get the length of the file. The data in the buffer (which may not
   * have been written the disk yet) is taken into account.
   *
   * @return the length of the file in bytes.
   * @throws IOException if an I/O error occurrs.
   */
  public long length() throws IOException {
    long fileLength = (file == null) ? -1L : file.length(); // GRIB has closed the data raf
    return Math.max(fileLength, dataEnd);
  }

  public void order(ByteOrder bo) {
    this.byteOrder = bo;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////
  // Read primitives.
  //

  /**
   * Read a byte of data from the file, blocking until data is
   * available.
   *
   * @return the next byte of data, or -1 if the end of the file is
   *         reached.
   * @throws IOException if an I/O error occurrs.
   */
  public int read() throws IOException {

    // If the file position is within the data, return the byte...
    if (filePosition < dataEnd) {
      int pos = (int) (filePosition - bufferStart);
      filePosition++;
      return (buffer[pos] & 0xff);

      // ...or should we indicate EOF...
    } else if (endOfFile) {
      return -1;

      // ...or seek to fill the buffer, and try again.
    } else {
      seek(filePosition);
      return read();
    }
  }

  /**
   * Read up to <code>len</code> bytes into an array, at a specified
   * offset. This will block until at least one byte has been read.
   *
   * @param b the byte array to receive the bytes.
   * @param off the offset in the array where copying will start.
   * @param len the number of bytes to copy.
   * @return the actual number of bytes read, or -1 if there is not
   *         more data due to the end of the file being reached.
   * @throws IOException if an I/O error occurrs.
   */
  public int readBytes(byte[] b, int off, int len) throws IOException {

    // Check for end of file.
    if (endOfFile) {
      return -1;
    }

    // See how many bytes are available in the buffer - if none,
    // seek to the file position to update the buffer and try again.
    int bytesAvailable = (int) (dataEnd - filePosition);
    if (bytesAvailable < 1) {
      seek(filePosition);
      return readBytes(b, off, len);
    }

    // Copy as much as we can.
    int copyLength = Math.min(bytesAvailable, len);
    System.arraycopy(buffer, (int) (filePosition - bufferStart), b, off, copyLength);
    filePosition += copyLength;

    // If there is more to copy...
    if (copyLength < len) {
      int extraCopy = len - copyLength;

      // If the amount remaining is more than a buffer's length, read it
      // directly from the file.
      if (extraCopy > buffer.length) {
        extraCopy = read_(filePosition, b, off + copyLength, len - copyLength);

        // ...or read a new buffer full, and copy as much as possible...
      } else {
        seek(filePosition);
        if (!endOfFile) {
          extraCopy = Math.min(extraCopy, dataSize);
          System.arraycopy(buffer, 0, b, off + copyLength, extraCopy);
        } else {
          extraCopy = -1;
        }
      }

      // If we did manage to copy any more, update the file position and
      // return the amount copied.
      if (extraCopy > 0) {
        filePosition += extraCopy;
        return copyLength + extraCopy;
      }
    }

    // Return the amount copied.
    return copyLength;
  }

  public java.nio.channels.FileChannel getFileChannel() {
    if (fileChannel == null) {
      fileChannel = file.getChannel();
    }
    return fileChannel;
  }

  /**
   * Read directly from file, without going through the buffer.
   * All reading goes through here or readToByteChannel;
   *
   * @param pos start here in the file
   * @param b put data into this buffer
   * @param offset buffer offset
   * @param len this number of bytes
   * @return actual number of bytes read
   * @throws IOException on io error
   */
  protected int read_(long pos, byte[] b, int offset, int len) throws IOException {
    file.seek(pos);
    return file.read(b, offset, len);
  }

  /**
   * Read up to <code>len</code> bytes into an array, at a specified
   * offset. This will block until at least one byte has been read.
   *
   * @param b the byte array to receive the bytes.
   * @param off the offset in the array where copying will start.
   * @param len the number of bytes to copy.
   * @return the actual number of bytes read, or -1 if there is not
   *         more data due to the end of the file being reached.
   * @throws IOException if an I/O error occurrs.
   */
  public int read(byte[] b, int off, int len) throws IOException {
    return readBytes(b, off, len);
  }

  /**
   * Read up to <code>b.length( )</code> bytes into an array. This
   * will block until at least one byte has been read.
   *
   * @param b the byte array to receive the bytes.
   * @return the actual number of bytes read, or -1 if there is not
   *         more data due to the end of the file being reached.
   * @throws IOException if an I/O error occurrs.
   */
  public int read(byte[] b) throws IOException {
    return readBytes(b, 0, b.length);
  }

  /**
   * Reads a signed 8-bit value from this file. This method reads a
   * byte from the file. If the byte read is <code>b</code>, where
   * <code>0&nbsp;&lt;=&nbsp;b&nbsp;&lt;=&nbsp;255</code>,
   * then the result is:
   * <ul>
   * <code>
   * (byte)(b)
   * </code>
   * </ul>
   * <p/>
   * This method blocks until the byte is read, the end of the stream
   * is detected, or an exception is thrown.
   *
   * @return the next byte of this file as a signed 8-bit
   *         <code>byte</code>.
   * @throws EOFException if this file has reached the end.
   * @throws IOException if an I/O error occurs.
   */
  public final byte readByte() throws IOException {
    int ch = this.read();
    if (ch < 0) {
      throw new EOFException();
    }
    return (byte) (ch);
  }

  /**
   * Reads a signed 16-bit number from this file. The method reads 2
   * bytes from this file. If the two bytes read, in order, are
   * <code>b1</code> and <code>b2</code>, where each of the two values is
   * between <code>0</code> and <code>255</code>, inclusive, then the
   * result is equal to:
   * <ul>
   * <code>
   * (short)((b1 &lt;&lt; 8) | b2)
   * </code>
   * </ul>
   * <p/>
   * This method blocks until the two bytes are read, the end of the
   * stream is detected, or an exception is thrown.
   *
   * @return the next two bytes of this file, interpreted as a signed
   *         16-bit number.
   * @throws EOFException if this file reaches the end before reading
   *         two bytes.
   * @throws IOException if an I/O error occurs.
   */
  public final short readShort() throws IOException {
    int ch1 = this.read();
    int ch2 = this.read();
    if ((ch1 | ch2) < 0) {
      throw new EOFException();
    }
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      return (short) ((ch1 << 8) + (ch2));
    } else {
      return (short) ((ch2 << 8) + (ch1));
    }
  }

  /**
   * Reads a signed 32-bit integer from this file. This method reads 4
   * bytes from the file. If the bytes read, in order, are <code>b1</code>,
   * <code>b2</code>, <code>b3</code>, and <code>b4</code>, where
   * <code>0&nbsp;&lt;=&nbsp;b1, b2, b3, b4&nbsp;&lt;=&nbsp;255</code>,
   * then the result is equal to:
   * <ul>
   * <code>
   * (b1 &lt;&lt; 24) | (b2 &lt;&lt; 16) + (b3 &lt;&lt; 8) + b4
   * </code>
   * </ul>
   * <p/>
   * This method blocks until the four bytes are read, the end of the
   * stream is detected, or an exception is thrown.
   *
   * @return the next four bytes of this file, interpreted as an
   *         <code>int</code>.
   * @throws EOFException if this file reaches the end before reading
   *         four bytes.
   * @throws IOException if an I/O error occurs.
   */
  public final int readInt() throws IOException {
    int ch1 = this.read();
    int ch2 = this.read();
    int ch3 = this.read();
    int ch4 = this.read();
    if ((ch1 | ch2 | ch3 | ch4) < 0) {
      throw new EOFException();
    }

    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4));
    } else {
      return ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1));
    }
  }

  /**
   * Reads a signed 64-bit integer from this file. This method reads eight
   * bytes from the file. If the bytes read, in order, are
   * <code>b1</code>, <code>b2</code>, <code>b3</code>,
   * <code>b4</code>, <code>b5</code>, <code>b6</code>,
   * <code>b7</code>, and <code>b8,</code> where:
   * <ul>
   * <code>
   * 0 &lt;= b1, b2, b3, b4, b5, b6, b7, b8 &lt;=255,
   * </code>
   * </ul>
   * <p/>
   * then the result is equal to:
   * <p>
   * <blockquote>
   * 
   * <pre>
   * ((long) b1 &lt;&lt; 56) + ((long) b2 &lt;&lt; 48) + ((long) b3 &lt;&lt; 40) + ((long) b4 &lt;&lt; 32) + ((long) b5 &lt;&lt; 24)
   *     + ((long) b6 &lt;&lt; 16) + ((long) b7 &lt;&lt; 8) + b8
   * </pre>
   * 
   * </blockquote>
   * <p/>
   * This method blocks until the eight bytes are read, the end of the
   * stream is detected, or an exception is thrown.
   *
   * @return the next eight bytes of this file, interpreted as a
   *         <code>long</code>.
   * @throws EOFException if this file reaches the end before reading
   *         eight bytes.
   * @throws IOException if an I/O error occurs.
   */
  public final long readLong() throws IOException {
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      return ((long) (readInt()) << 32) + (readInt() & 0xFFFFFFFFL); // tested ok
    } else {
      return ((readInt() & 0xFFFFFFFFL) + ((long) readInt() << 32)); // not tested yet ??
    }

    /*
     * int ch1 = this.read();
     * int ch2 = this.read();
     * int ch3 = this.read();
     * int ch4 = this.read();
     * int ch5 = this.read();
     * int ch6 = this.read();
     * int ch7 = this.read();
     * int ch8 = this.read();
     * if ((ch1 | ch2 | ch3 | ch4 | ch5 | ch6 | ch7 | ch8) < 0)
     * throw new EOFException();
     * 
     * if (bigEndian)
     * return ((long)(ch1 << 56)) + (ch2 << 48) + (ch3 << 40) + (ch4 << 32) + (ch5 << 24) + (ch6 << 16) + (ch7 << 8) +
     * (ch8 << 0));
     * else
     * return ((long)(ch8 << 56) + (ch7 << 48) + (ch6 << 40) + (ch5 << 32) + (ch4 << 24) + (ch3 << 16) + (ch2 << 8) +
     * (ch1 << 0));
     */
  }

  /**
   * Reads a <code>float</code> from this file. This method reads an
   * <code>int</code> value as if by the <code>readInt</code> method
   * and then converts that <code>int</code> to a <code>float</code>
   * using the <code>intBitsToFloat</code> method in class
   * <code>Float</code>.
   * <p/>
   * This method blocks until the four bytes are read, the end of the
   * stream is detected, or an exception is thrown.
   *
   * @return the next four bytes of this file, interpreted as a
   *         <code>float</code>.
   * @throws EOFException if this file reaches the end before reading
   *         four bytes.
   * @throws IOException if an I/O error occurs.
   * @see java.io.RandomAccessFile#readInt()
   * @see Float#intBitsToFloat(int)
   */
  public final float readFloat() throws IOException {
    return Float.intBitsToFloat(readInt());
  }


  /**
   * Reads a <code>double</code> from this file. This method reads a
   * <code>long</code> value as if by the <code>readLong</code> method
   * and then converts that <code>long</code> to a <code>double</code>
   * using the <code>longBitsToDouble</code> method in
   * class <code>Double</code>.
   * <p/>
   * This method blocks until the eight bytes are read, the end of the
   * stream is detected, or an exception is thrown.
   *
   * @return the next eight bytes of this file, interpreted as a
   *         <code>double</code>.
   * @throws EOFException if this file reaches the end before reading
   *         eight bytes.
   * @throws IOException if an I/O error occurs.
   * @see java.io.RandomAccessFile#readLong()
   * @see Double#longBitsToDouble(long)
   */
  public final double readDouble() throws IOException {
    return Double.longBitsToDouble(readLong());
  }

}