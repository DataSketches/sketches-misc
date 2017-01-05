/*
 * Copyright 2015-16, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.quantiles;

/**
 * Utility functions for computing space consumed by the QuantileSketch.
 *
 * @author Kevin Lang
 * @author Lee Rhodes
 */
public final class Space {
  private static final String LS = System.getProperty("line.separator");

  Space() {}

  /**
   * Returns a pretty print string of a table of the maximum sizes of a QuantileSketch
   * data structure configured as a single array over a range of <i>n</i> and <i>k</i>.
   * @param lgKlo the starting value of k expressed as log_base2(k)
   * @param lgKhi the ending value of k expressed as log_base2(k)
   * @param maxLgN the ending value of N expressed as log_base2(N)
   * @return a pretty print string of a table of the maximum sizes of a QuantileSketch
   */
  public static String spaceTableGuide(final int lgKlo, final int lgKhi, final int maxLgN) {
    final int cols = lgKhi - lgKlo + 1;
    final int maxUBbytes = elemCapacity(1 << lgKhi, (1L << maxLgN) - 1L);
    final int tblColWidth = String.format("%,d", maxUBbytes).length() + 1;
    final int leftColWidth = 16;
    final String leftColStrFmt = "%" + leftColWidth + "s";
    final String dFmt = "%," + tblColWidth + "d";
    final String fFmt = "%" + (tblColWidth - 1) + ".3f%%";
    final StringBuilder sb = new StringBuilder();
    sb.append(
        "Table Guide for Quantiles DoublesSketch Size in Bytes and Approximate Error:").append(LS);
    sb.append(String.format(leftColStrFmt, "K => |"));
    for (int kpow = lgKlo; kpow <= lgKhi; kpow++) { //the header row of k values
      final int k = 1 << kpow;
      sb.append(String.format(dFmt, k));
    }
    sb.append(LS);
    sb.append(String.format(leftColStrFmt,"~ Error => |"));
    //sb.append("    ~ Error => |");
    for (int kpow = lgKlo; kpow <= lgKhi; kpow++) { //the header row of k values
      final int k = 1 << kpow;
      sb.append(String.format(fFmt, 100 * getEpsilon(k)));
    }
    sb.append(LS);
    sb.append(String.format(leftColStrFmt, "N |"));
    sb.append(" Size in Bytes ->").append(LS);
    final int numDashes = leftColWidth + tblColWidth * cols;
    final StringBuilder sb2 = new StringBuilder();
    for (int i = 0; i < numDashes; i++) { sb2.append("-"); }
    sb.append(sb2.toString()).append(LS);
    final String leftColNumFmt = "%," + (leftColWidth - 2) + "d |";
    for (int npow = 0; npow <= maxLgN; npow++) {
      final long n = (1L << npow) - 1L;
      sb.append(String.format(leftColNumFmt, n)); //first column
      for (int kpow = lgKlo; kpow <= lgKhi; kpow++) { //table columns
        final int k = 1 << kpow;
        final int ubBytes = elemCapacity(k, n);
        sb.append(String.format(dFmt, ubBytes));
      }
      sb.append(LS);
    }
    return sb.toString();
  }

  //External calls
  private static int elemCapacity(final int k, final long n) {
    return (n == 0) ? 8
        : (Util.computeCombinedBufferItemCapacity(k, n, true) + 4) * Double.BYTES;
  }

  private static double getEpsilon(final int k) {
    return Util.EpsilonFromK.getAdjustedEpsilon(k);
  }

  /**
   * Pretty prints a table of the maximum sizes of a QuantileSketch
   * data structure configured as a single array over a range of <i>n</i> and <i>k</i> and given
   * an element size of 8 bytes.
   * @param args Not used.
   */
  public static void main(final String[] args) {
    println(com.yahoo.sketches.quantiles.Space.spaceTableGuide(4, 15, 32));
  }

  static void println(final String s) { System.out.println(s); }
}
