/* <p>Copyright (c) 2005-2013, N. Lee Rhodes, Los Altos, California.
 * All Rights Reserved.<br>
 * THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THE ECLIPSE PUBLIC
 * LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION OF THE PROGRAM
 * CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.</p>
 * <p>You should have received a copy of the Eclipse Public License, v1.0 along
 * with this program.  Otherwise, a copy can be obtained from
 * http://www.eclipse.org/org/documents/epl-v10.php.</p>
 */

package com.yahoo.sketches;

import static java.nio.channels.FileChannel.MapMode.READ_ONLY;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * A collection of useful static file handlers that conveniently convert the
 * java.io checked exceptions into runtime exceptions.
 *
 * @author Lee Rhodes
 */
public final class Files {
  private static final String LS = System.getProperty("line.separator");
  private static final byte CR = 0xD, LF = 0xA;
  public static final int DEFAULT_BUFSIZE = 8192;

  // Common IO & NIO file methods

  /**
   * If the fileName string is null or empty, this method throws a
   * RuntimeException.
   *
   * @param fileName the given fileName
   * @throws RuntimeException if fileName is null or empty.
   */
  public static void checkFileName(String fileName) {
    if (fileName == null) {
      throw new RuntimeException(LS + "FileName is null.");
    }
    if (fileName.length() == 0) {
      throw new RuntimeException(LS + "FileName is empty.");
    }
    return;
  }

  /**
   * Gets an existing normal file as a File. If fileName is null, or empty, or
   * if the file is actually a directory, or doesn't exist this will throw a
   * Runtime Exception; otherwise it will return the fileName as a File object.
   *
   * @param fileName the given fileName
   * @return a File object
   * @throws RuntimeException if fileName cannot resolve to a existing normal
   * file.
   */
  public static File getExistingFile(String fileName) {
    checkFileName(fileName);
    File file = new File(fileName);
    if (file.isFile()) {
      return file;
    }
    if (file.isDirectory()) {
      throw new RuntimeException(LS + "FileName is a Directory: " + fileName);
    }

    throw new RuntimeException(LS + "FileName does not exist: " + fileName);
  }

  /**
   * Returns true if file is a normal file and not a directory.
   *
   * @param fileName the given fileName
   * @return true if file is a normal file and not a directory.
   * @throws RuntimeException if fileName is null or empty.
   */
  public static boolean isFileValid(String fileName) {
    checkFileName(fileName);
    File file = new File(fileName);
    return file.isFile();
  }

  /**
   * Gets the System.getProperty("user.dir"), which is the expected location of
   * the user root directory.
   *
   * @return location of user root directory
   */
  public static String getUserDir() {
    return System.getProperty("user.dir");
  }

  /**
   * Opens a RandomAccessFile given a File object and String mode: "r", "rw",
   * "rws" or "rwd". Remember to close this file when you are done!
   *
   * @param file the given file
   * @param mode the given mode
   * @return RandomAccessFile
   * @throws RuntimeException if an IOException occurs.
   */
  public static RandomAccessFile openRandomAccessFile(File file, String mode) {
    RandomAccessFile raf;
    try {
      raf = new RandomAccessFile(file, mode);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(LS + "Failed to open RandomAccessFile " + LS + e);
    }
    return raf;
  }

  /**
   * Gets the FileDescriptor from the given RandomAccessFile.
   *
   * @param raf RandomAccessFile
   * @return the FileDescriptor
   * @throws RuntimeException if an IOException occurs.
   */
  public static FileDescriptor getFD(RandomAccessFile raf) {
    FileDescriptor fd = null;
    try {
      fd = raf.getFD();
    } catch (IOException e) {
      throw new RuntimeException(LS + "RandomAccessFile.getFD() failure" + LS + e);
    }
    return fd;
  }

  /**
   * Sets the position of the given RandomAccessFile to the given position.
   *
   * @param raf RandomAccessFile
   * @param position the given position
   * @throws RuntimeException if an IOException occurs.
   */
  public static void seek(RandomAccessFile raf, long position) {
    try {
      raf.seek(position);
    } catch (IOException e) {
      throw new RuntimeException(LS + "RandomAccessFile seek failure" + LS + e);
    }
  }

  /**
   * Reads buf.length bytes into the given buf.
   *
   * @param raf RandomAccessFile
   * @param buf the size of this buffer is the number of bytes requested.
   * @return the number of bytes actually read.
   * @throws RuntimeException if an IOException occurs.
   */
  public static int readBytes(RandomAccessFile raf, byte[] buf) {
    int len = buf.length;
    int read = 0;
    try {
      read = raf.read(buf);
    } catch (IOException e) {
      throw new RuntimeException(LS + "RandomAccessFile read failure" + LS + e);
    }
    if (read < len) {
      Arrays.fill(buf, read, len, (byte) 0);
    }
    return read;
  }

  // ************************
  // NIO OPERATIONS
  // ************************
  // ByteBuffer methods
  /**
   * Gets a MappedByteBuffer from the given FileChannel, mode, position and
   * size.
   *
   * @param fChan the given FileChannel
   * @param mmode the given MapMode
   * @param position the given position
   * @param size the given size
   * @return a MappedByteBuffer
   * @throws RuntimeException if an IOException occurs.
   */
  public static MappedByteBuffer getMappedByteBuffer(FileChannel fChan, FileChannel.MapMode mmode,
      long position, long size) {
    MappedByteBuffer mbBuf;
    try {
      mbBuf = fChan.map(mmode, position, size);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return mbBuf;
  }

  /**
   * Gets a MappedByteBuffer from the given FileChannel and mode. Assumes a
   * start position of zero and size of the length of the file (via the
   * FileChannel. Equivalent to:<br/>
   * getMappedByteBuffer(FileChannel, FileChanel.MapMode, 0L, size(fChan));
   *
   * @param fChan the given FileChannel
   * @param mmode the given MapMode
   * @return a MappedByteBuffer
   * @throws RuntimeException if an IOException occurs.
   */
  public static MappedByteBuffer getMappedByteBuffer(FileChannel fChan, FileChannel.MapMode mmode) {
    return getMappedByteBuffer(fChan, mmode, 0L, size(fChan));
  }

  /**
   * Reads bytes from the given (Mapped)ByteBuffer until either a CR, LF or CRLF
   * is detected in the byte stream and then converts the captured bytes,
   * excluding the CR and LF characters into a string. This method will work
   * with US-ASCII, ISO-8859 families, Windows 1252, and UTF-8. encodings. In
   * general any character encoding that is isomorphic with respect to the
   * exclusive use of the CR (0xD) and the LF (0xA) codes. Equivalent to:<br/>
   * readLine(ByteBuffer, ByteArrayBuilder, Charset.defaultCharset());
   *
   * @param mbBuf Given ByteBuffer or MappedByteBuffer
   * @param bab an optional ByteArrayBuilder for internal reuse, which will
   * improve multi-line reading performance. The result of the read as an array
   * of bytes is available from the bab.
   * @return the line as a string
   *
   */
  public static String readLine(ByteBuffer mbBuf, ByteArrayBuilder bab) {
    return readLine(mbBuf, bab, Charset.defaultCharset());
  }

  /**
   * Reads bytes from the given (Mapped)ByteBuffer until either a CR, LF or CRLF
   * is detected in the byte stream and then converts the captured bytes,
   * excluding the CR and LF characters into a string. This method will work
   * with US-ASCII, ISO-8859 families, Windows 1252, and UTF-8. encodings. In
   * general any character encoding that is isomorphic with respect to the
   * exclusive use of the CR (0xD) and the LF (0xA) codes.
   *
   * @param mbBuf Given ByteBuffer or MappedByteBuffer
   * @param bab an optional ByteArrayBuilder for internal reuse, which will
   * improve multiline reading performance. The result of the read as an array
   * of bytes is available from the bab.
   * @param charset The Charset to use when converting arrays of bytes from the
   * source to a Unicode String (UTF-16).
   * @return The characters of a line, or NULL if End-of-File, or "" if line was
   * empty.
   */
  public static String readLine(ByteBuffer mbBuf, ByteArrayBuilder bab, Charset charset) {
    if (!mbBuf.hasRemaining()) {
      return null;
    }
    ByteArrayBuilder bab1;
    if (bab == null) {
      bab1 = new ByteArrayBuilder();
    } else {
      bab1 = bab;
      bab1.setLength(0);
    }
    while (mbBuf.hasRemaining()) {
      byte b = mbBuf.get();
      if (b == LF) {
        break; // EOL
      }
      if (b == CR) {
        if (mbBuf.hasRemaining()) {
          // peek next byte without moving position
          if (mbBuf.get(mbBuf.position()) == LF) {
            mbBuf.get(); // consume it
          }
        }
        break; // EOL
      }
      bab1.append(b); // transfer the byte
    }
    if (bab1.length() == 0) {
      if (!mbBuf.hasRemaining()) {
        return null;
      }
      return "";
    }
    byte[] out = bab1.toByteArray();
    String s = new String(out, charset);
    return s;
  }

  /**
   * Reads a ByteBuffer (or subclass) with a request for numBytes. The result is
   * stuffed into the provided byte[] array (required), which must be larger or
   * equal to numBytes.
   *
   * @param bb The ByteBuffer to read from
   * @param numBytes The requested number of bytes to read.
   * @param out The target array for the bytes.
   * @return the actual number of bytes read.
   * @throws BufferUnderflowException if numBytes is greater than bytes
   * available in the buffer.
   */
  public static int readByteBuffer(ByteBuffer bb, int numBytes, byte[] out) {
    int rem = bb.remaining();
    if (rem == 0) {
      return 0;
    }
    int nBytes = (rem < numBytes) ? rem : numBytes;
    bb.get(out);
    return nBytes;
  }

  // FileChannel methods
  /**
   * Sets the FileChannel position.
   *
   * @param fc FileChannel
   * @param position the position
   * @throws RuntimeException if an IOException occurs.
   */
  public static void position(FileChannel fc, long position) {
    try {
      fc.position(position);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Gets the current size of the FileChannel.
   *
   * @param fc FileChannel
   * @return the size in bytes.
   * @throws RuntimeException if an IOException occurs.
   */
  public static long size(FileChannel fc) {
    long sz;
    try {
      sz = fc.size();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return sz;
  }

  /**
   * Appends the given string to the end of the file specified via the given
   * FileChannel. Equivalent to:<br/>
   * append(String, FileChannel, Charset.defaultCharset());
   *
   * @param out the string to append
   * @param fc the given FileChannel
   * @return the number of bytes actually appended.
   * @throws RuntimeException if an IOException occurs.
   */
  public static int append(String out, FileChannel fc) {
    return append(out, fc, Charset.defaultCharset());
  }

  /**
   * Appends the given string to the end of the file specified via the given
   * FileChannel.
   *
   * @param out the string to append
   * @param fc the given FileChannel
   * @param charset The Charset to use when converting the source string
   * (UTF-16) to a sequence of encoded bytes of the Charset.
   * @return the number of bytes actually appended.
   * @throws RuntimeException if an IOException occurs.
   */
  public static int append(String out, FileChannel fc, Charset charset) {
    int bytes;
    ByteBuffer bb = ByteBuffer.wrap(out.getBytes(charset));
    try {
      bytes = fc.write(bb, fc.size());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return bytes;
  }

  /**
   * Appends the given byteArr to the end of the file specified via the given
   * FileChannel.
   *
   * @param byteArr the byte[] to append
   * @param fc the given FileChannel
   * @return the number of bytes actually appended.
   * @throws RuntimeException if an IOException occurs.
   */
  public static int append(byte[] byteArr, FileChannel fc) {
    int bytes;
    ByteBuffer bb = ByteBuffer.wrap(byteArr);
    try {
      bytes = fc.write(bb, fc.size());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return bytes;
  }

  /**
   * Writes the given string out to the file specified via the given FileChannel
   * starting at the given file position. Equivalent to:<br/>
   * write(String, FileChannel, long, Charset.defaultCharset());
   *
   * @param out the given string
   * @param fc FileChannel
   * @param position the position
   * @return the total number of bytes written.
   * @throws RuntimeException if an IOException occurs.
   */
  public static int write(String out, FileChannel fc, long position) {
    return write(out, fc, position, Charset.defaultCharset());
  }

  /**
   * Writes the given string out to the file specified via the given FileChannel
   * starting at the given file position.
   *
   * @param out the given string
   * @param fc FileChannel
   * @param position the given position
   * @param charset The Charset to use when converting the source string
   * (UTF-16) to a sequence of encoded bytes of the Charset.
   * @return the total number of bytes written.
   * @throws RuntimeException if an IOException occurs.
   */
  public static int write(String out, FileChannel fc, long position, Charset charset) {
    int bytes;
    ByteBuffer bb = ByteBuffer.wrap(out.getBytes(charset));
    try {
      bytes = fc.write(bb, position);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return bytes;
  }

  /**
   * Writes the given byteArr to the file specified via the given FileChannel at
   * the given position.
   *
   * @param byteArr the byte[] to append
   * @param fc the given FileChannel
   * @param position the given position
   * @return the number of bytes actually appended.
   * @throws RuntimeException if an IOException occurs.
   */
  public static int write(byte[] byteArr, FileChannel fc, long position) {
    int bytes;
    ByteBuffer bb = ByteBuffer.wrap(byteArr);
    try {
      bytes = fc.write(bb, position);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return bytes;
  }

  // Complete NIO BASED FILE WRITE OPERATIONS
  /**
   * Writes the given String text to the fileName using NIO.
   *
   * @param text Source string to place in a file. Equivalent to: <br/>
   * stringToFileNIO(String, String, Charset.defaultCharset());
   * @param fileName name of target file
   * @return the total number of bytes written.
   * @throws RuntimeException if an IOException occurs.
   */
  public static int stringToFileNIO(String text, String fileName) {
    return stringToFileNIO(text, fileName, Charset.defaultCharset());
  }

  /**
   * Writes the given String text to the fileName using NIO.
   *
   * @param text Source string to place in a file.
   * @param fileName name of target file
   * @param charset The Charset to use when converting the source string
   * (UTF-16) to a sequence of encoded bytes of the Charset.
   * @return the total number of bytes written.
   * @throws RuntimeException if an IOException occurs.
   */
  public static int stringToFileNIO(String text, String fileName, Charset charset) {
    checkFileName(fileName);
    File file = new File(fileName);
    int bytes = 0;
    if (!file.isFile()) {
      try {
        file.createNewFile();
      } catch (IOException e) {
        throw new RuntimeException("Cannot create new file: " + fileName + LS + e);
      }
      try (FileChannel fc = openRandomAccessFile(file, "rw").getChannel()) {
        bytes = write(text, fc, 0L, charset);
      } catch (IOException e) {
        throw new RuntimeException("Cannot create File Channel: " + fileName + LS + e);
      }
    }

    return bytes;
  }

  /**
   * Appends a String to a file using NIO. If fileName does not exist, this
   * creates a new empty file of that name. This closes the file after
   * appending.
   *
   * @param text is the source String. Equivalent to: <br/>
   * appendStringToFileNIO(String, String, Charset.defautCharset());
   * @param fileName the given fileName
   * @return the total number of bytes written
   * @throws RuntimeException if IOException or SecurityException occurs, or if
   * fileName is null or empty.
   */
  public static int appendStringToFileNIO(String text, String fileName) {
    return appendStringToFileNIO(text, fileName, Charset.defaultCharset());
  }

  /**
   * Appends a String to a file using NIO and a Charset. If fileName does not
   * exist, this creates a new empty file of that name. This closes the file
   * after appending.
   *
   * @param text is the source String.
   * @param fileName the given fileName
   * @param charset The Charset to use when converting the source string
   * (UTF-16) to a sequence of encoded bytes of the Charset.
   * @return the total number of bytes written
   * @throws RuntimeException if IOException or SecurityException occurs, or if
   * fileName is null or empty.
   */
  public static int appendStringToFileNIO(String text, String fileName, Charset charset) {
    checkFileName(fileName);
    File file = new File(fileName);
    if (!file.isFile()) { // does not exist
      try {
        file.createNewFile();
      } catch (Exception e) {
        throw new RuntimeException("Cannot create file: " + fileName + LS + e);
      }
    }
    int bytes = 0;
    try (FileChannel fc = openRandomAccessFile(file, "rw").getChannel()) {
      bytes = append(text, fc, charset);
    } catch (IOException e) {
      throw new RuntimeException("Cannot create File Channel: " + fileName + LS + e);
    }
    return bytes;
  }

  // Complete NIO BASED FILE READ OPERATIONS
  /**
   * Reads a file into a char array using NIO, then closes the file. Useful when
   * special handling of Line-Separation characters is required. Equivalent to:
   * <br/>
   * fileToCharArrayNIO(String, Charset.defaultCharset());
   *
   * @param fileName the given fileName
   * @return a char[]
   * @throws RuntimeException if IOException occurs.
   * @throws IllegalArgumentException if File size is greater than
   * Integer.MAX_VALUE.
   */
  public static char[] fileToCharArrayNIO(String fileName) {
    return fileToCharArrayNIO(fileName, Charset.defaultCharset());
  }

  /**
   * Reads a file into a char array using NIO, then closes the file. Useful when
   * special handling of Line-Separation characters is required.
   *
   * @param fileName the given fileName
   * @param charset The Charset to use when converting the source string
   * (UTF-16) to a sequence of encoded bytes of the Charset.
   * @return a char[]
   * @throws RuntimeException if IOException occurs.
   * @throws IllegalArgumentException if File size is greater than
   * Integer.MAX_VALUE.
   */
  public static char[] fileToCharArrayNIO(String fileName, Charset charset) {
    File file = getExistingFile(fileName);
    char[] chArr = null;
    try (RandomAccessFile raf = openRandomAccessFile(file, "r");
        FileChannel fc = raf.getChannel();) {
      MappedByteBuffer mbBuf = getMappedByteBuffer(fc, READ_ONLY);
      long len = size(fc);
      if (len > Integer.MAX_VALUE) {
        fc.close();
        throw new IllegalArgumentException("File size cannot exceed Integer.MAX_VALUE.");
      }
      byte[] in = new byte[(int) len];
      mbBuf.get(in); // fill the buffer
      String out = new String(in, charset);
      chArr = out.toCharArray();
    } catch (IOException e) {
      throw new RuntimeException("Could not create or close File Channel.");
    }
    return chArr;
  }

  /**
   * Reads a file into a String using NIO. Each line of the file is delimited by
   * the current operating systems's "line.separator" characters. Closes the
   * file. This method is equivalent to<br/>
   * fileToStringNIO(String fileName, Charset.defaultCharset())
   *
   * @param fileName given fileName
   * @return a String
   * @throws RuntimeException if IOException occurs.
   */
  public static String fileToStringNIO(String fileName) {
    return fileToStringNIO(fileName, Charset.defaultCharset());
  }

  /**
   * Reads a file into a String using NIO. Each line of the file is delimited by
   * the current operating systems's "line.separator" characters. Closes the
   * file.
   *
   * @param fileName given fileName
   * @param charset The Charset to use when converting arrays of bytes from the
   * source to a Unicode String (UTF-16).
   * @return a String
   * @throws RuntimeException if IOException occurs.
   */
  public static String fileToStringNIO(String fileName, Charset charset) {
    File file = getExistingFile(fileName);
    StringBuilder sb = new StringBuilder(1024);
    try (RandomAccessFile raf = openRandomAccessFile(file, "r");
        FileChannel fChan = raf.getChannel();) {
      MappedByteBuffer mbBuf = getMappedByteBuffer(fChan, READ_ONLY);
      ByteArrayBuilder bab = new ByteArrayBuilder();

      String s;
      while ((s = readLine(mbBuf, bab, charset)) != null) {
        sb.append(s);
        sb.append(LS);
      }
    } catch (IOException e) {
      throw new RuntimeException("Cannot create File Channel.");
    }
    return sb.toString();
  }

  // *******************************
  // STANDARD IO READER OPERATIONS
  // *******************************
  /**
   * Opens a BufferedReader wrapped around a File. Equivalent to the call<br>
   * openBufferedReader(file, DEFAULT_BUFSIZE) Rethrows any IOException as a
   * RuntimeException.
   *
   * @param file the given file
   * @return BufferedReader object
   * @throws RuntimeException if File Not Found.
   */
  public static BufferedReader openBufferedReader(File file) {
    return openBufferedReader(file, DEFAULT_BUFSIZE, Charset.defaultCharset());
  }

  /**
   * Opens a BufferedReader wrapped around a FileReader with a specified file
   * and buffer size. If bufSize is less than the default (8192) the default
   * will be used.
   *
   * @param file the given file
   * @param bufSize the given buffer size
   * @return a BufferedReader object
   * @throws RuntimeException if File Not Found.
   */
  public static BufferedReader openBufferedReader(File file, int bufSize) {
    return openBufferedReader(file, bufSize, Charset.defaultCharset());
  }

  /**
   * Opens a BufferedReader, which specifies a bufSize, wrapped around an
   * InputStreamReader, which specifies a Charset. The InputStreamReader wraps a
   * FileInputStream. If bufSize is less than the default (8192) the default
   * will be used. If the charset is null, Charset.defaultCharset() will be
   * used.
   *
   * @param file the given file
   * @param bufSize the given buffer size
   * @param charset the given Charset
   * @return a BufferedReader object
   * @throws RuntimeException if FileNotFoundException occurs.
   */
  public static BufferedReader openBufferedReader(File file, int bufSize, Charset charset) {
    int bufSz = (bufSize < DEFAULT_BUFSIZE) ? DEFAULT_BUFSIZE : bufSize;
    Charset cSet = (charset == null) ? Charset.defaultCharset() : charset;
    BufferedReader in = null; // default bufsize is 8192.
    try {
      FileInputStream fis = new FileInputStream(file);
      InputStreamReader isr = new InputStreamReader(fis, cSet);
      in = new BufferedReader(isr, bufSz);
    } catch (FileNotFoundException e) { // from FileInputStream
      // never opened, so don't close it.
      throw new RuntimeException(LS + "File Not Found: " + file.getPath() + LS + e);
    }
    return in;
  }

  /**
   * Tests a Reader object if it is ready.
   *
   * @param in the given Reader
   * @return boolean true if ready.
   * @throws RuntimeException if IOException occurs.
   */
  public static boolean ready(Reader in) {
    boolean out = false;
    try {
      out = in.ready();
    } catch (IOException e) {
      throw new RuntimeException(LS + "Reader.ready() unsuccessful: " + LS + e);
    }
    return out;
  }

  /**
   * Skips bytes in the given Reader object.
   *
   * @param in the given Reader
   * @param skipLen in bytes.
   * @throws RuntimeException if IOException occurs.
   */
  public static void skip(Reader in, long skipLen) {
    try {
      in.skip(skipLen);
    } catch (IOException e) {
      try {
        in.close();
      } catch (IOException f) {
        throw new RuntimeException(LS + "Close Unsuccessful" + LS + f);
      }
      throw new RuntimeException(LS + "Reader.skip(len) unsuccessful: " + LS + e);
    }
  }

  /**
   * Reads characters from the given Reader into the given character array.
   *
   * @param in the given Reader
   * @param length number of characters to read
   * @param cArr Array must be equal to or larger than length
   * @return number of characters actually read from Reader
   * @throws RuntimeException if IOException occurs.
   */
  public static int readCharacters(Reader in, int length, char[] cArr) {
    int readLen = 0;
    try {
      readLen = in.read(cArr, 0, length);
    } catch (IOException e) {
      try {
        in.close();
      } catch (IOException f) {
        throw new RuntimeException(LS + "Close Unsuccessful" + LS + f);
      }
      throw new RuntimeException(LS + "Reader.read(char[],0,len) unsuccessful: " + LS + e);
    }
    return readLen;
  }

  /**
   * Reads a file into a char array, then closes the file. Useful when special
   * handling of Line-Separation characters is required. Equivalent to: <br/>
   * fileToCharArray(String, int, Charset.defaultCharset());
   *
   * @param fileName the given fileName
   * @return a char[]
   * @throws RuntimeException if IOException occurs.
   * @throws IllegalArgumentException if File size is greater than
   * Integer.MAX_VALUE.
   */
  public static char[] fileToCharArray(String fileName) {
    return fileToCharArray(fileName, DEFAULT_BUFSIZE, Charset.defaultCharset());
  }

  /**
   * Reads a file into a char array, then closes the file. Useful when special
   * handling of Line-Separation characters is required.
   *
   * @param fileName the given fileName
   * @param bufSize if less than 8192 it defaults to 8192
   * @param charset The Charset to use when converting arrays of bytes from the
   * source to a Unicode String (UTF-16).
   * @return a char[]
   * @throws RuntimeException if IOException occurs.
   * @throws IllegalArgumentException if File size is greater than
   * Integer.MAX_VALUE.
   */
  public static char[] fileToCharArray(String fileName, int bufSize, Charset charset) {
    File file = getExistingFile(fileName);
    char[] cArr = null;
    long fileLen = (long) (file.length() * 1.1); // 10% headroom
    if (fileLen > Integer.MAX_VALUE) {
      throw new IllegalArgumentException(
          LS + "File Size is too large: " + fileLen + " >" + " Max: " + Integer.MAX_VALUE);
    }
    int len = (int) fileLen;
    try (BufferedReader in = openBufferedReader(file, bufSize, charset)) {
      cArr = new char[len];
      in.read(cArr, 0, len);
    } catch (IOException e) { // thrown by read()
      throw new RuntimeException(LS + "BufferedReader.read(char[],0,len) unsuccessful: " + LS + e);
    }
    return cArr;
  }

  /**
   * Reads a file into a String. Each line of the file is delimited by the
   * current operating systems's "line.separator" characters. Closes the file.
   * Equivalent to: <br/>
   * fileToString(String, 8192, Charset.defaultCharset());
   *
   * @param fileName the given fileName
   * @return a String
   * @throws RuntimeException if IOException occurs.
   */
  public static String fileToString(String fileName) {
    return fileToString(fileName, DEFAULT_BUFSIZE, Charset.defaultCharset());
  }

  /**
   * Reads a file into a String. Each line of the file is delimited by the
   * current operating systems's "line.separator" characters. Closes the file.
   *
   * @param fileName the given fileName
   * @param bufSize if less than 8192 it defaults to 8192
   * @param charset The Charset to use when converting arrays of bytes from the
   * source to a Unicode String (UTF-16).
   * @return String
   * @throws RuntimeException if IOException occurs.
   */
  public static String fileToString(String fileName, int bufSize, Charset charset) {
    StringBuilder sb = new StringBuilder();
    File file = getExistingFile(fileName);
    try (BufferedReader in = openBufferedReader(file, bufSize, charset)) {
      String s;
      while ((s = in.readLine()) != null) {
        sb.append(s);
        sb.append(LS);
      }
    } catch (IOException e) { // thrown by readLine()
      throw new RuntimeException(LS + "BufferedReader.readLine() unsuccessful: " + LS + e);
    }
    return sb.toString();
  }

  // STANDARD IO WRITE OPERATIONS
  /**
   * Opens a BufferedWriter wrapped around a FileWriter with a specified file
   * and buffer size. If bufSize is less than the default (8192) the default
   * will be used.
   *
   * @param file the given file
   * @param bufSize the given buffer size
   * @param append to existing file if true.
   * @return BufferedWriter object
   * @throws RuntimeException if IOException occurs.
   */
  public static BufferedWriter openBufferedWriter(File file, int bufSize, boolean append) {
    return openBufferedWriter(file, bufSize, append, Charset.defaultCharset());
  }

  /**
   * Opens a BufferedWriter wrapped around a FileWriter with a specified file
   * and buffer size. If bufSize is less than the default (8192) the default
   * will be used.
   *
   * @param file the given file
   * @param bufSize if less than 8192 it defaults to 8192.
   * @param append to existing file if true.
   * @param charset The Charset to use when converting the source string
   * (UTF-16) to a sequence of encoded bytes of the Charset.
   * @return BufferedWriter object
   * @throws RuntimeException if IOException occurs.
   */
  public static BufferedWriter openBufferedWriter(File file, int bufSize, boolean append,
      Charset charset) {
    int bufSz = (bufSize < DEFAULT_BUFSIZE) ? DEFAULT_BUFSIZE : bufSize;
    BufferedWriter out = null; // default bufsize is 8192.
    try {
      FileOutputStream fos = new FileOutputStream(file, append);
      OutputStreamWriter osw = new OutputStreamWriter(fos, charset);
      out = new BufferedWriter(osw, bufSz);
    } catch (IOException e) {
      // never opened, so don't close it.
      throw new RuntimeException(LS + "Could not create: " + file.getPath() + LS + e);
    }
    return out;
  }

  /**
   * Writes a String to a file using a BufferedWriter. Closes the file.
   *
   * @param text is the source String.
   * @param fileName the given fileName
   * @throws RuntimeException if IOException occurs or if fileName is null or
   * empty.
   */
  public static void stringToFile(String text, String fileName) {
    stringToFile(text, fileName, DEFAULT_BUFSIZE, Charset.defaultCharset());
  }

  /**
   * Writes a String to a file using a BufferedWriter. Closes the file.
   *
   * @param text is the source String.
   * @param fileName the given fileName
   * @param bufSize if less than 8192 it defaults to 8192.
   * @param charset The Charset to use when converting the source string
   * (UTF-16) to a sequence of encoded bytes of the Charset.
   * @throws RuntimeException if IOException occurs or if fileName is null or
   * empty.
   */
  public static void stringToFile(String text, String fileName, int bufSize, Charset charset) {
    checkFileName(fileName);
    File file = new File(fileName);
    try (BufferedWriter bw = openBufferedWriter(file, bufSize, false, charset);
        PrintWriter out = new PrintWriter(bw);) {
      out.print(text);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Appends a String to a file using a BufferedWriter. If fileName does not
   * exist, this creates a new empty file of that name. This closes the file
   * after appending.
   *
   * @param text is the source String.
   * @param fileName the given fileName
   * @throws RuntimeException if IOException or SecurityException occurs, or if
   * fileName is null or empty.
   */
  public static void appendStringToFile(String text, String fileName) {
    appendStringToFile(text, fileName, DEFAULT_BUFSIZE, Charset.defaultCharset());
  }

  /**
   * Appends a String to a file using a BufferedWriter, bufSize and Charset. If
   * fileName does not exist, this creates a new empty file of that name. This
   * closes the file after appending.
   *
   * @param text is the source String.
   * @param fileName the given fileName
   * @param bufSize if less than 8192 it defaults to 8192.
   * @param charset The Charset to use when converting the source string
   * (UTF-16) to a sequence of encoded bytes of the Charset.
   * @throws RuntimeException if IOException or SecurityException occurs, or if
   * fileName is null or empty.
   */
  public static void appendStringToFile(String text, String fileName, int bufSize,
      Charset charset) {
    checkFileName(fileName);
    File file = new File(fileName);
    if (!file.isFile()) { // does not exist
      try {
        file.createNewFile();
      } catch (Exception e) {
        throw new RuntimeException("Cannot create file: " + fileName + LS + e);
      }
    }
    try (BufferedWriter bw = openBufferedWriter(file, bufSize, true, charset);
        PrintWriter out = new PrintWriter(bw);) {
      out.print(text);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
