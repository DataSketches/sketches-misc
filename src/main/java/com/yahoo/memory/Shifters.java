/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory;

public final class Shifters {

  //CHECKSTYLE.OFF: JavadocMethod

  public static int getByte0(final long long0) {
    final long mask = 0XFFL;
    return (int) (long0 & mask);
  }

  public static int getByte1(final long long0) {
    final int shift = 1 << 3;
    final long mask = 0XFFL;
    return (int) ((long0 >>> shift) & mask);
  }

  public static int getByte2(final long long0) {
    final int shift = 2 << 3;
    final long mask = 0XFFL;
    return (int) ((long0 >>> shift) & mask);
  }

  public static int getByte3(final long long0) {
    final int shift = 3 << 3;
    final long mask = 0XFFL;
    return (int) ((long0 >>> shift) & mask);
  }

  public static int getByte4(final long long0) {
    final int shift = 4 << 3;
    final long mask = 0XFFL;
    return (int) ((long0 >>> shift) & mask);
  }

  public static int getByte5(final long long0) {
    final int shift = 5 << 3;
    final long mask = 0XFFL;
    return (int) ((long0 >>> shift) & mask);
  }

  public static int getByte6(final long long0) {
    final int shift = 6 << 3;
    final long mask = 0XFFL;
    return (int) ((long0 >>> shift) & mask);
  }

  public static int getByte7(final long long0) {
    final int shift = 7 << 3;
    final long mask = 0XFFL;
    return (int) ((long0 >>> shift) & mask);
  }

  public static int getByte(final int bytePos, final long long0) {
    final int shift = bytePos << 3;
    final long mask = 0XFFL;
    return (int) ((long0 >>> shift) & mask);
  }

  public static long putByte0(final long value, final long long0) {
    final long mask = 0XFFL;
    return (value & mask) | (~mask & long0);
  }

  public static long putByte1(final long value, final long long0) {
    final int shift = 1 << 3;
    final long mask = 0XFFL;
    return ((value & mask) << shift) | (~(mask << shift) & long0);
  }

  public static long putByte2(final long value, final long long0) {
    final int shift = 2 << 3;
    final long mask = 0XFFL;
    return ((value & mask) << shift) | (~(mask << shift) & long0);
  }

  public static long putByte3(final long value, final long long0) {
    final int shift = 3 << 3;
    final long mask = 0XFFL;
    return ((value & mask) << shift) | (~(mask << shift) & long0);
  }

  public static long putByte4(final long value, final long long0) {
    final int shift = 4 << 3;
    final long mask = 0XFFL;
    return ((value & mask) << shift) | (~(mask << shift) & long0);
  }

  public static long putByte5(final long value, final long long0) {
    final int shift = 5 << 3;
    final long mask = 0XFFL;
    return ((value & mask) << shift) | (~(mask << shift) & long0);
  }

  public static long putByte6(final long value, final long long0) {
    final int shift = 6 << 3;
    final long mask = 0XFFL;
    return ((value & mask) << shift) | (~(mask << shift) & long0);
  }

  public static long putByte7(final long value, final long long0) {
    final int shift = 7 << 3;
    final long mask = 0XFFL;
    return ((value & mask) << shift) | (~(mask << shift) & long0);
  }

  public static long putByte(final int bytePos, final long value, final long long0) {
    final int shift = bytePos << 3;
    final long mask = 0XFFL;
    return ((value & mask) << shift) | (~(mask << shift) & long0);
  }
}
