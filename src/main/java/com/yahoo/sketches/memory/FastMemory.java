/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the 
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.memory;

import static com.yahoo.sketches.memory.UnsafeUtil.*;

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
  protected FastMemory(long objectBaseOffset, Object memArray, ByteBuffer byteBuf) {
    objectBaseOffset_ = objectBaseOffset;
    memArray_ = memArray;
    byteBuf_ = byteBuf;
  }

  public FastMemory(NativeMemory mem) {
    this.objectBaseOffset_ = mem.objectBaseOffset_;
    this.memArray_ = mem.memArray_;
    this.byteBuf_ = mem.byteBuf_;
    this.nativeRawStartAddress_ = mem.nativeRawStartAddress_;
    this.capacityBytes_ = mem.capacityBytes_;
    this.memReq_ = mem.memReq_;
  }
  
  /**
   * Provides access to the given byteArray using Memory interface
   * @param byteArray an on-heap byte array
   */
  public FastMemory(byte[] byteArray) {
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
  public FastMemory(long[] longArray) {
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
  public FastMemory(ByteBuffer byteBuf) {
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
  public long getLong_I(long offsetBytes) {
    assertBounds(offsetBytes, ARRAY_LONG_INDEX_SCALE, capacityBytes_); //
    return unsafe.getLong(memArray_, getAddress(offsetBytes));
  }

  //No Interface
  public void putLong_I(long offsetBytes, long srcValue) {
    assertBounds(offsetBytes, ARRAY_LONG_INDEX_SCALE, capacityBytes_);
    unsafe.putLong(memArray_, getAddress(offsetBytes), srcValue);
  }
  
  //No Interface, No Asserts
  public long getLong_IA(long offsetBytes) {
    return unsafe.getLong(memArray_, getAddress(offsetBytes));
  }

  //No Interface, No Asserts
  public void putLong_IA(long offsetBytes, long srcValue) {
    unsafe.putLong(memArray_, getAddress(offsetBytes), srcValue);
  }
  
  //No Interface, No Asserts, Static
  public static long getLong_IAS(Object array, long rawAddress) {
    return unsafe.getLong(array, rawAddress);
  }
  
  //No Interface, No Asserts, Static
  public static void putLong_IAS(Object array, long rawAddress, long value) {
    unsafe.putLong(array, rawAddress, value);
  }
  
  //No Interface, No Asserts, Static, Final
  public static final long getLong_IASF(Object array, long rawAddress) {
    return unsafe.getLong(array, rawAddress);
  }
  
  //No Interface, No Asserts, Static, Final
  public static final void putLong_IASF(Object array, long rawAddress, long value) {
    unsafe.putLong(array, rawAddress, value);
  }
  
  //No Interface, No Asserts, Static, Final, no Object, For Direct Only
  public static final long getLong_IASFO(long rawAddress) {
    return unsafe.getLong(rawAddress);
  }
  
  //No Interface, No Asserts, Static, Final, no Object, For Direct Only
  public static final void putLong_IASFO(long rawAddress, long value) {
    unsafe.putLong(rawAddress, value);
  }

  //Non-data Memory interface methods

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

  public void setMemoryRequest(MemoryRequest memReq) {
    memReq_ = memReq;
  }

  public String toHexString(String header, long offsetBytes, int lengthBytes) {
    StringBuilder sb = new StringBuilder();
    sb.append(header).append(LS);
    String s1 = String.format("(..., %d, %d)", offsetBytes, lengthBytes);
    sb.append(this.getClass().getSimpleName()).append(".toHexString")
      .append(s1).append(", hash: ").append(this.hashCode()).append(LS);
    sb.append("  MemoryRequest: ");
    if (memReq_ != null) {
      sb.append(memReq_.getClass().getSimpleName()).append(", hash: ").append(memReq_.hashCode());
    } else sb.append("null");
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
  private String toHex(String header, long offsetBytes, int lengthBytes) {
    assertBounds(offsetBytes, lengthBytes, capacityBytes_);
    long unsafeRawAddress = getAddress(offsetBytes);
    StringBuilder sb = new StringBuilder();
    sb.append(header).append(LS);
    sb.append("Raw Address         : ").append(nativeRawStartAddress_).append(LS);
    sb.append("Object Offset       : ").append(objectBaseOffset_).append(": ");
    sb.append( (memArray_ == null) ? "null" : memArray_.getClass().getSimpleName()).append(LS);
    sb.append("Relative Offset     : ").append(offsetBytes).append(LS);
    sb.append("Total Offset        : ").append(unsafeRawAddress).append(LS);
    sb.append("Native Region       :  0  1  2  3  4  5  6  7");
    long j = offsetBytes;
    StringBuilder sb2 = new StringBuilder();
    for (long i = 0; i < lengthBytes; i++) {
      int b = unsafe.getByte(memArray_, unsafeRawAddress + i) & 0XFF;
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
