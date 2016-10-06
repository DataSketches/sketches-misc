/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory;

import static com.yahoo.memory.UnsafeUtil.unsafe;

public class AllocFastMemory extends FastMemory {

  /**
   * Constructor for allocate native memory.
   *
   * <p>Allocates and provides access to capacityBytes directly in native (off-heap) memory
   * leveraging the Memory interface.  The MemoryRequest callback is set to null.
   * @param capacityBytes the size in bytes of the native memory
   */
  public AllocFastMemory(long capacityBytes) {
    super(0L, null, null);
    super.nativeRawStartAddress_ = unsafe.allocateMemory(capacityBytes);
    super.capacityBytes_ = capacityBytes;
    super.memReq_ = null;
  }

  @Override
  public void freeMemory() {
    super.freeMemory();
  }

  /**
   * If the JVM calls this method and a "freeMemory() has not been called" a <i>System.err</i>
   * message will be logged.
   */
  @Override
  protected void finalize() {
      if (requiresFree()) {
          System.err.println(
                  "ERROR: freeMemory() has not been called: Address: " + nativeRawStartAddress_
                  + ", capacity: " + capacityBytes_);
          java.lang.StackTraceElement[] arr = Thread.currentThread().getStackTrace();
          for (int i = 0; i < arr.length; i++) {
              System.err.println(arr[i].toString());
          }
      }
  }

}
