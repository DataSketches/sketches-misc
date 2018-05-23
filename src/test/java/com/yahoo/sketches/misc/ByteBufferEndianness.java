/*
 * Copyright 2018, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.misc;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.testng.annotations.Test;

/**
 * @author Lee Rhodes
 */
public class ByteBufferEndianness {
  final boolean nativeIsBig = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;
  final ByteOrder BE = ByteOrder.BIG_ENDIAN;
  final ByteOrder LE = ByteOrder.LITTLE_ENDIAN;

  @Test
  public void checkBB() {
    ByteBuffer bb = ByteBuffer.allocate(8);
    assert bb.order() == BE;

    bb.order(LE);
    assert bb.order() == LE;
    ByteBuffer bb2 = bb.duplicate();
    assert bb2.order() == BE; //Wrong order

    ByteBuffer bb3 = bb.slice();
    assert bb3.order() == BE; //Wrong order

  }

}
