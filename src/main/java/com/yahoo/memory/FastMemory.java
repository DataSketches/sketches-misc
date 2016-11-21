/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory;

import static com.yahoo.memory.UnsafeUtil.ARRAY_BYTE_BASE_OFFSET;
import static com.yahoo.memory.UnsafeUtil.ARRAY_LONG_BASE_OFFSET;
import static com.yahoo.memory.UnsafeUtil.ARRAY_LONG_INDEX_SCALE;
import static com.yahoo.memory.UnsafeUtil.LONG_SHIFT;
import static com.yahoo.memory.UnsafeUtil.LS;
import static com.yahoo.memory.UnsafeUtil.assertBounds;
import static com.yahoo.memory.UnsafeUtil.unsafe;

import java.nio.ByteBuffer;

import com.yahoo.sketches.SketchesArgumentException;

//@SuppressWarnings({"unused", "restriction"})
public class FastMemory {
  protected final long objectBaseOffset_;
  protected final Object memArray_;
  //holding on to this to make sure that it is not garbage collected before we are done with it.
  protected final ByteBuffer byteBuf_;
  protected long nativeRawStartAddress_;
  protected long capacityBytes_;
  protected MemoryRequest memReq_ = null; //set via AllocMemory

  //only sets the finals
  protected FastMemory(final long objectBaseOffset, final Object memArray, final ByteBuffer byteBuf) {
    objectBaseOffset_ = objectBaseOffset;
    memArray_ = memArray;
    byteBuf_ = byteBuf;
  }

  /**
   * Test for proposed new NativeMemory
   * @param mem the given NativeMemory
   */
  public FastMemory(final NativeMemory mem) {
    this.memArray_ = mem.array();
    if (memArray_ == null) {
      this.objectBaseOffset_ = 0L;
      this.nativeRawStartAddress_ = mem.getAddress(0L);
    } else {
      this.objectBaseOffset_ = mem.getAddress(0L);
      this.nativeRawStartAddress_ = 0L;
    }
    this.byteBuf_ = mem.byteBuffer();
    this.capacityBytes_ = mem.getCapacity();
    this.memReq_ = mem.getMemoryRequest();
  }

  /**
   * Provides access to the given byteArray using Memory interface
   * @param byteArray an on-heap byte array
   */
  public FastMemory(final byte[] byteArray) {
    this(ARRAY_BYTE_BASE_OFFSET, byteArray, null);
    if ((byteArray == null) || (byteArray.length == 0)) {
      throw new SketchesArgumentException(
          "Array must must not be null and have a length greater than zero.");
    }
    nativeRawStartAddress_ = 0L;
    capacityBytes_ = byteArray.length;
  }

  /**
   * Provides access to the given longArray using Memory interface
   * @param longArray an on-heap long array
   */
  public FastMemory(final long[] longArray) {
    this(ARRAY_LONG_BASE_OFFSET, longArray, null);
    if ((longArray == null) || (longArray.length == 0)) {
      throw new SketchesArgumentException(
          "Array must must not be null and have a length greater than zero.");
    }
    nativeRawStartAddress_ = 0L;
    capacityBytes_ = longArray.length << LONG_SHIFT;
  }

  /**
   * Provides access to the backing store of the given ByteBuffer using Memory interface
   * @param byteBuf the given ByteBuffer
   */
  public FastMemory(final ByteBuffer byteBuf) {
    if (byteBuf.isDirect()) {
      objectBaseOffset_ = 0L;
      memArray_ = null;
      byteBuf_ = byteBuf;
      nativeRawStartAddress_ = ((sun.nio.ch.DirectBuffer)byteBuf).address();
    }
    else { //must have array
      objectBaseOffset_ = ARRAY_BYTE_BASE_OFFSET;
      memArray_ = byteBuf.array();
      byteBuf_ = byteBuf;
      nativeRawStartAddress_ = 0L;
    }
    capacityBytes_ = byteBuf.capacity();
  }

  //No Interface
  public long getLong_I(final long offsetBytes) {
    assertBounds(offsetBytes, ARRAY_LONG_INDEX_SCALE, capacityBytes_); //
    return unsafe.getLong(memArray_, getAddress(offsetBytes));
  }

  //No Interface
  public void putLong_I(final long offsetBytes, final long srcValue) {
    assertBounds(offsetBytes, ARRAY_LONG_INDEX_SCALE, capacityBytes_);
    unsafe.putLong(memArray_, getAddress(offsetBytes), srcValue);
  }

  //No Interface, No Asserts
  public long getLong_IA(final long offsetBytes) {
    return unsafe.getLong(memArray_, getAddress(offsetBytes));
  }

  //No Interface, No Asserts
  public void putLong_IA(final long offsetBytes, final long srcValue) {
    unsafe.putLong(memArray_, getAddress(offsetBytes), srcValue);
  }

  //No Interface, No Asserts, Static
  public static long getLong_IAS(final Object array, final long rawAddress) {
    return unsafe.getLong(array, rawAddress);
  }

  //No Interface, No Asserts, Static
  public static void putLong_IAS(final Object array, final long rawAddress, final long value) {
    unsafe.putLong(array, rawAddress, value);
  }

  //No Interface, No Asserts, Static, Final
  public static final long getLong_IASF(final Object array, final long rawAddress) {
    return unsafe.getLong(array, rawAddress);
  }

  //No Interface, No Asserts, Static, Final
  public static final void putLong_IASF(final Object array, final long rawAddress, final long value) {
    unsafe.putLong(array, rawAddress, value);
  }

  //No Interface, No Asserts, Static, Final, no Object, For Direct Only
  public static final long getLong_IASFO(final long rawAddress) {
    return unsafe.getLong(rawAddress);
  }

  //No Interface, No Asserts, Static, Final, no Object, For Direct Only
  public static final void putLong_IASFO(final long rawAddress, final long value) {
    unsafe.putLong(rawAddress, value);
  }

  /**
   * NEW: No Interface, Static, Final, PASS FastMemory
   * @param mem the given FastMemory
   * @param offsetBytes the offset
   * @return a long
   */
  public static final long getLong_ISF(final FastMemory mem, final long offsetBytes) {
    final long capBytes = mem.capacityBytes_;
    assertBounds(offsetBytes, ARRAY_LONG_INDEX_SCALE, capBytes);
    final long add = mem.getAddress(offsetBytes);
    return unsafe.getLong(mem.memArray_, add);
  }

  /**
   * NEW: No Interface, Static, Final, PASS FastMemory
   * @param mem the given FastMemory
   * @param offsetBytes the offset
   * @param srcValue the value to put
   */
  public static final void putLong_ISF(final FastMemory mem, final long offsetBytes,
      final long srcValue) {
    final long capBytes = mem.capacityBytes_;
    assertBounds(offsetBytes, ARRAY_LONG_INDEX_SCALE, capBytes);
    final long add = mem.getAddress(offsetBytes);
    unsafe.putLong(mem.memArray_, add, srcValue);
  }

  /**
   * NEW: No Interface, Static, Final, PASS All
   * @param array array object
   * @param objectBaseOffset Base offset
   * @param nativeRawStartAddress raw or relative offset
   * @param capacityBytes memory capacity to check bounds
   * @param offsetBytes the long offset
   * @return the long
   */
  public static final long getLong_ISF2(final Object array, final long objectBaseOffset,
      final long nativeRawStartAddress, final long capacityBytes, final long offsetBytes) {
    assertBounds(offsetBytes, ARRAY_LONG_INDEX_SCALE, capacityBytes);
    assert (nativeRawStartAddress > 0) ^ (objectBaseOffset > 0); //only one must be zero
    assert (nativeRawStartAddress > 0) ^ (array != null); //only one must exist
    final long add = nativeRawStartAddress + objectBaseOffset + offsetBytes;
    return unsafe.getLong(array, add);
  }

  /**
   * NEW: No Interface, Static, Final, PASS All
   * @param array object array
   * @param objectBaseOffset base offset
   * @param nativeRawStartAddress raw or relative offset
   * @param capacityBytes memory capacity to check bounds
   * @param offsetBytes the long offset
   * @param srcValue the value to put
   */
  public static final void putLong_ISF2(final Object array, final long objectBaseOffset,
      final long nativeRawStartAddress, final long capacityBytes, final long offsetBytes,
      final long srcValue) {
    assertBounds(offsetBytes, ARRAY_LONG_INDEX_SCALE, capacityBytes);
    assert (nativeRawStartAddress > 0) ^ (objectBaseOffset > 0); //only one must be zero
    assert (nativeRawStartAddress > 0) ^ (array != null); //only one must exist
    final long add = nativeRawStartAddress + objectBaseOffset + offsetBytes;
    unsafe.putLong(array, add, srcValue);
  }

  //Non-data Memory interface methods

  /**
   * Get the effective address
   * @param offsetBytes the current offset
   * @return the address
   */
  public final long getAddress(final long offsetBytes) {
    assertBounds(offsetBytes, 0, capacityBytes_);
    assert (nativeRawStartAddress_ > 0) ^ (objectBaseOffset_ > 0); //only one must be zero
    return nativeRawStartAddress_ + objectBaseOffset_ + offsetBytes;
  }

  public long getCapacity() {
    return capacityBytes_;
  }

  public MemoryRequest getMemoryRequest() {
    return memReq_;
  }

  public Object getParent() {
    return memArray_;
  }

  public void setMemoryRequest(final MemoryRequest memReq) {
    memReq_ = memReq;
  }

  /**
   * Get hex string
   * @param header optional header
   * @param offsetBytes the offset in bytes
   * @param lengthBytes the length in bytes
   * @return the hex string
   */
  public String toHexString(final String header, final long offsetBytes, final int lengthBytes) {
    final StringBuilder sb = new StringBuilder();
    sb.append(header).append(LS);
    final String s1 = String.format("(..., %d, %d)", offsetBytes, lengthBytes);
    sb.append(this.getClass().getSimpleName()).append(".toHexString")
      .append(s1).append(", hash: ").append(this.hashCode()).append(LS);
    sb.append("  MemoryRequest: ");
    if (memReq_ != null) {
      sb.append(memReq_.getClass().getSimpleName()).append(", hash: ").append(memReq_.hashCode());
    } else { sb.append("null"); }
    return toHex(sb.toString(), offsetBytes, lengthBytes);
  }

  //NativeMemory only methods

  /**
   * Returns the backing on-heap primitive array if there is one, otherwise returns null
   * @return the backing on-heap primitive array if there is one, otherwise returns null
   */
  public Object array() {
    return memArray_;
  }

  /**
   * Returns the backing ByteBuffer if there is one, otherwise returns null
   * @return the backing ByteBuffer if there is one, otherwise returns null
   */
  public ByteBuffer byteBuffer() {
    return byteBuf_;
  }

  /**
   * Returns true if this NativeMemory is accessing native (off-heap) memory directly.
   * This includes the case of a Direct ByteBuffer.
   * @return true if this NativeMemory is accessing native (off-heap) memory directly.
   */
  public boolean isDirect() {
    return nativeRawStartAddress_ > 0;
  }

  /**
   * This frees this Memory only if it is required. This always sets the capacity to zero
   * and the reference to MemoryRequest to null, which effectively disables this class.
   * However,
   *
   * <p>It is always safe to call this method when you are done with this class.
   */
  public void freeMemory() {
    if (requiresFree()) {
        unsafe.freeMemory(nativeRawStartAddress_);
        nativeRawStartAddress_ = 0L;
    }
    capacityBytes_ = 0L;
    memReq_ = null;
  }

  /**
   * Returns true if this Memory is backed by an on-heap primitive array
   * @return true if this Memory is backed by an on-heap primitive array
   */
  public boolean hasArray() {
    return (memArray_ != null);
  }

  /**
   * Returns true if this Memory is backed by a ByteBuffer
   * @return true if this Memory is backed by a ByteBuffer
   */
  public boolean hasByteBuffer() {
    return (byteBuf_ != null);
  }

  /**
   * Returns true if the underlying memory of this Memory has a capacity greater than zero
   * @return true if the underlying memory of this Memory has a capacity greater than zero
   */
  public boolean isAllocated() {
    return (capacityBytes_ > 0L);
  }

  //Restricted methods

  /**
   * Returns a formatted hex string of an area of this Memory.
   * Used primarily for testing.
   * @param header a descriptive header
   * @param offsetBytes offset bytes relative to the Memory start
   * @param lengthBytes number of bytes to convert to a hex string
   * @return a formatted hex string in a human readable array
   */
  private String toHex(final String header, final long offsetBytes, final int lengthBytes) {
    assertBounds(offsetBytes, lengthBytes, capacityBytes_);
    final long unsafeRawAddress = getAddress(offsetBytes);
    final StringBuilder sb = new StringBuilder();
    sb.append(header).append(LS);
    sb.append("Raw Address         : ").append(nativeRawStartAddress_).append(LS);
    sb.append("Object Offset       : ").append(objectBaseOffset_).append(": ");
    sb.append( (memArray_ == null) ? "null" : memArray_.getClass().getSimpleName()).append(LS);
    sb.append("Relative Offset     : ").append(offsetBytes).append(LS);
    sb.append("Total Offset        : ").append(unsafeRawAddress).append(LS);
    sb.append("Native Region       :  0  1  2  3  4  5  6  7");
    long j = offsetBytes;
    final StringBuilder sb2 = new StringBuilder();
    for (long i = 0; i < lengthBytes; i++) {
      final int b = unsafe.getByte(memArray_, unsafeRawAddress + i) & 0XFF;
      if ((i != 0) && ((i % 8) == 0)) {
        sb.append(String.format("%n%20s: ", j)).append(sb2);
        j += 8;
        sb2.setLength(0);
      }
      sb2.append(String.format("%02x ", b));
    }
    sb.append(String.format("%n%20s: ", j)).append(sb2).append(LS);
    return sb.toString();
  }

  /**
   * Returns true if the object requires being freed.
   * This method exists to standardize the check between freeMemory() and finalize()
   *
   * @return true if the object should be freed when it is no longer needed
   */
  protected boolean requiresFree() {
    return (nativeRawStartAddress_ != 0L) && (byteBuf_ == null);
  }


}
