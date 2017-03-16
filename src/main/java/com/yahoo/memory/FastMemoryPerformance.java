/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory;

import static com.yahoo.memory.UnsafeUtil.ARRAY_LONG_BASE_OFFSET;
import static com.yahoo.memory.UnsafeUtil.unsafe;
import static java.lang.Math.pow;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;

import com.yahoo.sketches.Util;

public class FastMemoryPerformance {
  private int arrLongs_;     //# entries in array
  private int lgMinTrials_;  //minimum # of trials
  private int lgMaxTrials_;  //maximum # of trials
  private int lgMinLongs_;   //minimum array longs
  private int lgMaxLongs_;   //maximum array longs
  private double ppo_;       //points per octave of arrLongs (x-axis)
  private double minGI_;     //min generating index
  private double maxGI_;     //max generating index
  private int lgMaxOps_;     //lgMaxLongs_ + lgMinTrials_
  private long address_ = 0; //used for unsafe

  /**
   * Evaluate Memory performancy under different scenarios
   */
  public FastMemoryPerformance() {
    //Configure
    lgMinTrials_ = 6;  //was 6
    lgMaxTrials_ = 24;  //was 24
    lgMinLongs_ = 5;   //was 5
    lgMaxLongs_ = 26;  //was 26
    ppo_ = 4.0;        //was 4
    //Compute
    lgMaxOps_ = lgMaxLongs_ + lgMinTrials_;
    maxGI_ = ppo_ * lgMaxLongs_;
    minGI_ = ppo_ * lgMinLongs_;

  }

  private static class Point {
    double ppo;
    double gi;
    int arrLongs;
    int trials;
    long sumReadTrials_nS = 0;
    long sumWriteTrials_nS = 0;

    Point(final double ppo, final double gi, final int arrLongs, final int trials) {
      this.ppo = ppo;
      this.gi = gi;
      this.arrLongs = arrLongs;
      this.trials = trials;
    }

    public static void printHeader() {
      println("LgLongs\tLongs\tTrials\t#Ops\tAvgRTrial_nS\tAvgROp_nS\tAvgWTrial_nS\tAvgWOp_nS");
    }

    public void printRow() {
      final long numOps = (long)((double)trials * arrLongs);
      final double logArrLongs = gi / ppo;
      final double rTrial_nS = (double)sumReadTrials_nS / trials;
      final double wTrial_nS = (double)sumWriteTrials_nS / trials;
      final double rOp_nS = rTrial_nS / arrLongs;
      final double wOp_nS = wTrial_nS / arrLongs;
      //Print
      final String out = String.format("%6.2f\t%d\t%d\t%d\t%.1f\t%8.3f\t%.1f\t%8.3f",
          logArrLongs, arrLongs, trials, numOps, rTrial_nS, rOp_nS, wTrial_nS, wOp_nS);
      println(out);
    }
  }

  private Point getNextPoint(final Point p) {
    final int lastArrLongs = (int)pow(2.0, p.gi / ppo_);
    int nextArrLongs;
    double logArrLongs;
    do {
      logArrLongs = (++p.gi) / ppo_;
      if (p.gi > maxGI_) { return null; }
      nextArrLongs = (int)pow(2.0, logArrLongs);
    } while (nextArrLongs <= lastArrLongs);
    p.arrLongs = nextArrLongs;
    //compute trials
    final double logTrials = Math.min(lgMaxOps_ - logArrLongs, lgMaxTrials_);
    p.trials = (int)pow(2.0, logTrials);
    return p;
  }

  /*************************************/
  // JAVA HEAP
  /*************************************/

  private void testHeapArrayByIndex() {
    Point p = new Point(ppo_, minGI_ - 1, 1 << lgMinLongs_, 1 << lgMaxTrials_); //just below the start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      final long[] array = new long[p.arrLongs];
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumWriteTrials_nS += trial_HeapArrayByIndex(array, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumReadTrials_nS += trial_HeapArrayByIndex(array, p.arrLongs, true); //a single trial read
      }

      p.printRow();
    }
  }

  //Must do write trial first
  private static final long trial_HeapArrayByIndex(final long[] array, final int arrLongs,
      final boolean read) {
    final long checkSum = (arrLongs * (arrLongs - 1L)) / 2L;
    final long startTime_nS, stopTime_nS;
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { trialSum += array[i]; }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        throw new IllegalStateException("Bad checksum: " + trialSum + " != " + checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { array[i] = i; }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }

  /*************************************/
  //  UNSAFE DIRECT
  /*************************************/

  private void testNativeArrayByUnsafe() {
    Point p = new Point(ppo_, minGI_ - 1, 1 << lgMinLongs_, 1 << lgMaxTrials_); //just below the start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      address_ = unsafe.allocateMemory(p.arrLongs << 3);
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t = 0; t < p.trials; t++) { //do writes first
        p.sumWriteTrials_nS += trial_NativeArrayByUnsafe(address_, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumReadTrials_nS += trial_NativeArrayByUnsafe(address_, p.arrLongs, true); //a single trial read
      }
      p.printRow();
      freeMemory();
    }
  }

  //Must do writes first
  private static final long trial_NativeArrayByUnsafe(final long address, final int arrLongs,
      final boolean read) {
    final long checkSum = (arrLongs * (arrLongs - 1L)) / 2L;
    final long startTime_nS, stopTime_nS;
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { trialSum += unsafe.getLong(address + (i << 3)); }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        if (address != 0) { unsafe.freeMemory(address); }
        throw new IllegalStateException("Bad checksum: " + trialSum + " != " + checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { unsafe.putLong(address + (i << 3), i); }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }

  /*************************************/
  //  BYTE BUFFER - HEAP
  /*************************************/

  private void testByteBufferHeap() {
    Point p = new Point(ppo_, minGI_ - 1, 1 << lgMinLongs_, 1 << lgMaxTrials_); //just below the start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      final ByteBuffer buf = ByteBuffer.allocate(p.arrLongs << 3);
      buf.order(ByteOrder.nativeOrder());
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t = 0; t < p.trials; t++) { //do writes first
        p.sumWriteTrials_nS += trial_ByteBufferHeap(buf, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumReadTrials_nS += trial_ByteBufferHeap(buf, p.arrLongs, true); //a single trial read
      }
      p.printRow();
    }
  }

  //Must do writes first
  private static final long trial_ByteBufferHeap(final ByteBuffer buf, final int arrLongs,
      final boolean read) {
    final long checkSum = (arrLongs * (arrLongs - 1L)) / 2L;
    final long startTime_nS, stopTime_nS;
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { trialSum += buf.getLong(i << 3); }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        throw new IllegalStateException("Bad checksum: " + trialSum + " != " + checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { buf.putLong(i << 3, i); }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }

  /*************************************/
  //  BYTE BUFFER - DIRECT
  /*************************************/

  private void testByteBufferDirect() {
    Point p = new Point(ppo_, minGI_ - 1, 1 << lgMinLongs_, 1 << lgMaxTrials_); //just below the start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      final ByteBuffer buf = ByteBuffer.allocateDirect(p.arrLongs << 3);
      buf.order(ByteOrder.LITTLE_ENDIAN);
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t = 0; t < p.trials; t++) { //do writes first
        p.sumWriteTrials_nS += trial_ByteBufferDirect(buf, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumReadTrials_nS += trial_ByteBufferDirect(buf, p.arrLongs, true); //a single trial read
      }
      p.printRow();
    }
  }

  //Must do writes first
  private static final long trial_ByteBufferDirect(final ByteBuffer buf, final int arrLongs,
      final boolean read) {
    final long checkSum = (arrLongs * (arrLongs - 1L)) / 2L;
    final long startTime_nS, stopTime_nS;
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { trialSum += buf.getLong(i << 3); }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        throw new IllegalStateException("Bad checksum: " + trialSum + " != " + checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { buf.putLong(i << 3, i); }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }

  /*************************************/
  //  LONG BUFFER - HEAP
  /*************************************/

  private void testLongBufferHeap() {
    Point p = new Point(ppo_, minGI_ - 1, 1 << lgMinLongs_, 1 << lgMaxTrials_); //just below the start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      final long[] arr = new long[p.arrLongs];
      final LongBuffer buf = LongBuffer.wrap(arr);
      //buf.order(ByteOrder.LITTLE_ENDIAN);
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t = 0; t < p.trials; t++) { //do writes first
        p.sumWriteTrials_nS += trial_LongBufferHeap(buf, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumReadTrials_nS += trial_LongBufferHeap(buf, p.arrLongs, true); //a single trial read
      }
      p.printRow();
    }
  }

  //Must do writes first
  private static final long trial_LongBufferHeap(final LongBuffer buf, final int arrLongs,
      final boolean read) {
    final long checkSum = (arrLongs * (arrLongs - 1L)) / 2L;
    final long startTime_nS, stopTime_nS;
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { trialSum += buf.get(i); }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        throw new IllegalStateException("Bad checksum: " + trialSum + " != " + checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { buf.put(i, i); }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }

  /*************************************/
  //  LONG BUFFER - DIRECT
  /*************************************/

  private void testLongBufferDirect() {
    Point p = new Point(ppo_, minGI_ - 1, 1 << lgMinLongs_, 1 << lgMaxTrials_); //just below the start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      final ByteBuffer buf = ByteBuffer.allocateDirect(p.arrLongs << 3);
      final LongBuffer lbuf = buf.asLongBuffer();
      buf.order(ByteOrder.LITTLE_ENDIAN);
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t = 0; t < p.trials; t++) { //do writes first
        p.sumWriteTrials_nS += trial_LongBufferDirect(lbuf, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumReadTrials_nS += trial_LongBufferDirect(lbuf, p.arrLongs, true); //a single trial read
      }
      p.printRow();
    }
  }

  //Must do writes first
  private static final long trial_LongBufferDirect(final LongBuffer buf, final int arrLongs,
      final boolean read) {
    final long checkSum = (arrLongs * (arrLongs - 1L)) / 2L;
    final long startTime_nS, stopTime_nS;
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { trialSum += buf.get(i); }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        throw new IllegalStateException("Bad checksum: " + trialSum + " != " + checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { buf.put(i, i); }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }

  /*************************************/
  //  NATIVE MEMORY FROM LIBRARY - HEAP
  /*************************************/

  private void testMemoryHeap() {
    Point p = new Point(ppo_, minGI_ - 1, 1 << lgMinLongs_, 1 << lgMaxTrials_); //just below the start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      final byte[] array = new byte[p.arrLongs << 3];
      final NativeMemory mem = new NativeMemory(array);
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t = 0; t < p.trials; t++) { //do writes first
        p.sumWriteTrials_nS += trial_MemoryHeap(mem, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumReadTrials_nS += trial_MemoryHeap(mem, p.arrLongs, true); //a single trial read
      }
      p.printRow();
    }
  }

  //Must do writes first
  private static final long trial_MemoryHeap(final NativeMemory mem, final int arrLongs,
      final boolean read) {
    final long checkSum = (arrLongs * (arrLongs - 1L)) / 2L;
    final long startTime_nS, stopTime_nS;
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { trialSum += mem.getLong(i << 3); }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        throw new IllegalStateException("Bad checksum: " + trialSum + " != " + checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { mem.putLong(i << 3, i); }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }

  /*************************************/
  //  NATIVE MEMORY FROM LIBRARY - DIRECT
  /*************************************/

  private void testMemoryDirect() {
    Point p = new Point(ppo_, minGI_ - 1, 1 << lgMinLongs_, 1 << lgMaxTrials_); //just below the start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      final NativeMemory mem = new AllocMemory(p.arrLongs << 3);
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t = 0; t < p.trials; t++) { //do writes first
        p.sumWriteTrials_nS += trial_MemoryDirect(mem, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumReadTrials_nS += trial_MemoryDirect(mem, p.arrLongs, true); //a single trial read
      }
      p.printRow();
      mem.freeMemory();
    }
  }

  //Must do writes first
  private static final long trial_MemoryDirect(final NativeMemory mem, final int arrLongs,
      final boolean read) {
    final long checkSum = (arrLongs * (arrLongs - 1L)) / 2L;
    final long startTime_nS, stopTime_nS;
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { trialSum += mem.getLong(i << 3); }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        throw new IllegalStateException("Bad checksum: " + trialSum + " != " + checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { mem.putLong(i << 3, i); }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }

  /*************************************/
  //  FAST MEMORY - HEAP, NO INTERFACE
  /*************************************/

  private void testFastMemoryHeap_I() {
    Point p = new Point(ppo_, minGI_ - 1, 1 << lgMinLongs_, 1 << lgMaxTrials_); //just below the start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      final byte[] array = new byte[p.arrLongs << 3];
      final FastMemory mem = new FastMemory(array);
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t = 0; t < p.trials; t++) { //do writes first
        p.sumWriteTrials_nS += trial_FastMemoryHeap_I(mem, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumReadTrials_nS += trial_FastMemoryHeap_I(mem, p.arrLongs, true); //a single trial read
      }
      p.printRow();
    }
  }

  //Must do writes first
  private static final long trial_FastMemoryHeap_I(final FastMemory mem, final int arrLongs,
      final boolean read) {
    final long checkSum = (arrLongs * (arrLongs - 1L)) / 2L;
    final long startTime_nS, stopTime_nS;
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { trialSum += mem.getLong_I(i << 3); }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        throw new IllegalStateException("Bad checksum: " + trialSum + " != " + checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { mem.putLong_I(i << 3, i); }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }

  /*************************************/
  //  FAST MEMORY - DIRECT, NO INTERFACE
  /*************************************/

  private void testFastMemoryDirect_I() {
    Point p = new Point(ppo_, minGI_ - 1, 1 << lgMinLongs_, 1 << lgMaxTrials_); //just below the start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      final FastMemory mem = new AllocFastMemory(p.arrLongs << 3);
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t = 0; t < p.trials; t++) { //do writes first
        p.sumWriteTrials_nS += trial_FastMemoryDirect_I(mem, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumReadTrials_nS += trial_FastMemoryDirect_I(mem, p.arrLongs, true); //a single trial read
      }
      p.printRow();
      mem.freeMemory();
    }
  }

  //Must do writes first
  private static final long trial_FastMemoryDirect_I(final FastMemory mem, final int arrLongs,
      final boolean read) {
    final long checkSum = (arrLongs * (arrLongs - 1L)) / 2L;
    final long startTime_nS, stopTime_nS;
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { trialSum += mem.getLong_I(i << 3); }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        throw new IllegalStateException("Bad checksum: " + trialSum + " != " + checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { mem.putLong_I(i << 3, i); }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }

  /*************************************/
  //  FAST MEMORY - HEAP, NO INTERFACE, NO ASSERTS
  /*************************************/

  private void testFastMemoryHeap_IA() {
    Point p = new Point(ppo_, minGI_ - 1, 1 << lgMinLongs_, 1 << lgMaxTrials_); //just below the start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      final byte[] array = new byte[p.arrLongs << 3];
      final FastMemory mem = new FastMemory(array);
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int  t = 0; t < p.trials; t++) { //do writes first
        p.sumWriteTrials_nS += trial_FastMemoryHeap_IA(mem, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumReadTrials_nS += trial_FastMemoryHeap_IA(mem, p.arrLongs, true); //a single trial read
      }
      p.printRow();
    }
  }

  //Must do writes first
  private static final long trial_FastMemoryHeap_IA(final FastMemory mem, final int arrLongs,
      final boolean read) {
    final long checkSum = (arrLongs * (arrLongs - 1L)) / 2L;
    final long startTime_nS, stopTime_nS;
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { trialSum += mem.getLong_IA(i << 3); }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        throw new IllegalStateException("Bad checksum: " + trialSum + " != " + checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { mem.putLong_IA(i << 3, i); }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }

  /*************************************/
  //  FAST MEMORY - DIRECT, NO INTERFACE, NO ASSERTS
  /*************************************/

  private void testFastMemoryDirect_IA() {
    Point p = new Point(ppo_, minGI_ - 1, 1 << lgMinLongs_, 1 << lgMaxTrials_); //just below the start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      final FastMemory mem = new AllocFastMemory(p.arrLongs << 3);
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t = 0; t < p.trials; t++) { //do writes first
        p.sumWriteTrials_nS += trial_FastMemoryDirect_IA(mem, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumReadTrials_nS += trial_FastMemoryDirect_IA(mem, p.arrLongs, true); //a single trial read
      }
      p.printRow();
      mem.freeMemory();
    }
  }

  //Must do writes first
  private static final long trial_FastMemoryDirect_IA(final FastMemory mem, final int arrLongs,
      final boolean read) {
    final long checkSum = (arrLongs * (arrLongs - 1L)) / 2L;
    final long startTime_nS, stopTime_nS;
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { trialSum += mem.getLong_IA(i << 3); }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        throw new IllegalStateException("Bad checksum: " + trialSum + " != " + checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { mem.putLong_IA(i << 3, i); }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }

  /*************************************/
  //  FAST MEMORY - HEAP, NO INTERFACE, NO ASSERTS, STATIC
  /*************************************/

  private void testFastMemoryHeap_IAS() {
    Point p = new Point(ppo_, minGI_ - 1, 1 << lgMinLongs_, 1 << lgMaxTrials_); //just below the start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      final long[] array = new long[p.arrLongs];
      //FastMemory mem = new FastMemory(array);
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t = 0; t < p.trials; t++) { //do writes first
        p.sumWriteTrials_nS += trial_FastMemoryHeap_IAS(array, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumReadTrials_nS += trial_FastMemoryHeap_IAS(array, p.arrLongs, true); //a single trial read
      }
      p.printRow();
    }
  }

  //Must do writes first
  private static final long trial_FastMemoryHeap_IAS(final long[] array, final int arrLongs,
      final boolean read) {
    final long checkSum = (arrLongs * (arrLongs - 1L)) / 2L;
    final long startTime_nS, stopTime_nS;
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) {
        trialSum += FastMemory.getLong_IAS(array, (i << 3) + ARRAY_LONG_BASE_OFFSET); }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        throw new IllegalStateException("Bad checksum: " + trialSum + " != " + checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) {
        FastMemory.putLong_IAS(array, (i << 3) + ARRAY_LONG_BASE_OFFSET, i);
      }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }

  /*************************************/
  //  FAST MEMORY - DIRECT, NO INTERFACE, NO ASSERTS, STATIC
  /*************************************/

  private void testFastMemoryDirect_IAS() {
    Point p = new Point(ppo_, minGI_ - 1, 1 << lgMinLongs_, 1 << lgMaxTrials_); //just below the start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      final FastMemory mem = new AllocFastMemory(p.arrLongs << 3); //AllocDirect
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t = 0; t < p.trials; t++) { //do writes first
        p.sumWriteTrials_nS += trial_FastMemoryDirect_IAS(mem, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumReadTrials_nS += trial_FastMemoryDirect_IAS(mem, p.arrLongs, true); //a single trial read
      }
      p.printRow();
      mem.freeMemory();
    }
  }

  //Must do writes first
  private static final long trial_FastMemoryDirect_IAS(final FastMemory mem, final int arrLongs,
      final boolean read) {
    final long checkSum = (arrLongs * (arrLongs - 1L)) / 2L;
    final long rawBaseAdd = mem.getAddress(0L);
    final long startTime_nS, stopTime_nS;
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) {
        trialSum += FastMemory.getLong_IAS(null, (i << 3) + rawBaseAdd);
      }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        throw new IllegalStateException("Bad checksum: " + trialSum + " != " + checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) {
        FastMemory.putLong_IAS(null, (i << 3) + rawBaseAdd, i);
      }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }

  /*************************************/
  //  FAST MEMORY - HEAP, NO INTERFACE, NO ASSERTS, STATIC, FINAL
  /*************************************/

  private void testFastMemoryHeap_IASF() {
    Point p = new Point(ppo_, minGI_ - 1, 1 << lgMinLongs_, 1 << lgMaxTrials_); //just below the start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      final long[] array = new long[p.arrLongs];
      //FastMemory mem = new FastMemory(array);
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t = 0; t < p.trials; t++) { //do writes first
        p.sumWriteTrials_nS += trial_FastMemoryHeap_IASF(array, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumReadTrials_nS += trial_FastMemoryHeap_IASF(array, p.arrLongs, true); //a single trial read
      }
      p.printRow();
    }
  }

  //Must do writes first
  private static final long trial_FastMemoryHeap_IASF(final long[] array, final int arrLongs,
      final boolean read) {
    final long checkSum = (arrLongs * (arrLongs - 1L)) / 2L;
    final long startTime_nS, stopTime_nS;
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) {
        trialSum += FastMemory.getLong_IASF(array, (i << 3) + ARRAY_LONG_BASE_OFFSET); }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        throw new IllegalStateException("Bad checksum: " + trialSum + " != " + checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) {
        FastMemory.putLong_IASF(array, (i << 3) + ARRAY_LONG_BASE_OFFSET, i);
      }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }

  /*************************************/
  //  FAST MEMORY - DIRECT, NO INTERFACE, NO ASSERTS, STATIC, FINAL
  /*************************************/

  private void testFastMemoryDirect_IASF() {
    Point p = new Point(ppo_, minGI_ - 1, 1 << lgMinLongs_, 1 << lgMaxTrials_); //just below the start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      final FastMemory mem = new AllocFastMemory(p.arrLongs << 3); //AllocDirect
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t = 0; t < p.trials; t++) { //do writes first
        p.sumWriteTrials_nS += trial_FastMemoryDirect_IASF(mem, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumReadTrials_nS += trial_FastMemoryDirect_IASF(mem, p.arrLongs, true); //a single trial read
      }
      p.printRow();
      mem.freeMemory();
    }
  }

  //Must do writes first
  private static final long trial_FastMemoryDirect_IASF(final FastMemory mem, final int arrLongs,
      final boolean read) {
    final long checkSum = (arrLongs * (arrLongs - 1L)) / 2L;
    final long rawBaseAdd = mem.getAddress(0L);
    final long startTime_nS, stopTime_nS;
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) {
        trialSum += FastMemory.getLong_IASF(null, (i << 3) + rawBaseAdd);
      }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        throw new IllegalStateException("Bad checksum: " + trialSum + " != " + checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { FastMemory.putLong_IASF(null, (i << 3) + rawBaseAdd, i); }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }

  /*************************************/
  //  FAST MEMORY - DIRECT, NO INTERFACE, NO ASSERTS, STATIC, FINAL, No Object, For DirectOnly
  /*************************************/

  private void testFastMemoryDirect_IASFO() {
    Point p = new Point(ppo_, minGI_ - 1, 1 << lgMinLongs_, 1 << lgMaxTrials_); //just below the start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      final FastMemory mem = new AllocFastMemory(p.arrLongs << 3); //AllocDirect
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t = 0; t < p.trials; t++) { //do writes first
        p.sumWriteTrials_nS += trial_FastMemoryDirect_IASFO(mem, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumReadTrials_nS += trial_FastMemoryDirect_IASFO(mem, p.arrLongs, true); //a single trial read
      }
      p.printRow();
      mem.freeMemory();
    }
  }

  //Must do writes first
  private static final long trial_FastMemoryDirect_IASFO(final FastMemory mem, final int arrLongs,
      final boolean read) {
    final long checkSum = (arrLongs * (arrLongs - 1L)) / 2L;
    final long rawBaseAdd = mem.getAddress(0L);
    final long startTime_nS, stopTime_nS;
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) {
        trialSum += FastMemory.getLong_IASFO((i << 3) + rawBaseAdd);
      }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        throw new IllegalStateException("Bad checksum: " + trialSum + " != " + checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) { FastMemory.putLong_IASFO((i << 3) + rawBaseAdd, i); }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }

  /*************************************/
  //  FAST MEMORY - HEAP, NO INTERFACE, STATIC, FINAL, PASS FAST MEMORY
  /*************************************/

  private void testFastMemoryHeap_ISF() {
    Point p = new Point(ppo_, minGI_ - 1, 1 << lgMinLongs_, 1 << lgMaxTrials_); //just below the start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      final long[] array = new long[p.arrLongs];
      final FastMemory mem = new FastMemory(array);
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t = 0; t < p.trials; t++) { //do writes first
        p.sumWriteTrials_nS += trial_FastMemoryHeap_ISF(mem, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumReadTrials_nS += trial_FastMemoryHeap_ISF(mem, p.arrLongs, true); //a single trial read
      }
      p.printRow();
    }
  }

  //Must do writes first
  private static final long trial_FastMemoryHeap_ISF(final FastMemory mem, final int arrLongs,
      final boolean read) {
    final long checkSum = (arrLongs * (arrLongs - 1L)) / 2L;
    final long startTime_nS, stopTime_nS;
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) {
        trialSum += FastMemory.getLong_ISF(mem, (i << 3));
      }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        throw new IllegalStateException("Bad checksum: " + trialSum + " != " + checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) {
        FastMemory.putLong_ISF(mem, (i << 3), i);
      }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }

  /*************************************/
  //  FAST MEMORY - DIRECT, NO INTERFACE, STATIC, FINAL, PASS FAST MEMORY
  /*************************************/

  private void testFastMemoryDirect_ISF() {
    Point p = new Point(ppo_, minGI_ - 1, 1 << lgMinLongs_, 1 << lgMaxTrials_); //just below the start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      final FastMemory mem = new AllocFastMemory(p.arrLongs << 3); //AllocDirect
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t = 0; t < p.trials; t++) { //do writes first
        p.sumWriteTrials_nS += trial_FastMemoryDirect_ISF(mem, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumReadTrials_nS += trial_FastMemoryDirect_ISF(mem, p.arrLongs, true); //a single trial read
      }
      p.printRow();
      mem.freeMemory();
    }
  }

  //Must do writes first
  private static final long trial_FastMemoryDirect_ISF(final FastMemory mem, final int arrLongs,
      final boolean read) {
    final long checkSum = (arrLongs * (arrLongs - 1L)) / 2L;
    //final long rawBaseAdd = mem.getAddress(0L);
    final long startTime_nS, stopTime_nS;
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) {
        trialSum += FastMemory.getLong_ISF(mem, (i << 3));
      }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        throw new IllegalStateException("Bad checksum: " + trialSum + " != " + checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) {
        FastMemory.putLong_ISF(mem, (i << 3), i);
      }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }

  /*************************************/
  // FAST MEMORY - HEAP, NO INTERFACE, STATIC, FINAL, PASS ALL
  /*************************************/

  private void testFastMemoryHeap_ISF2() {
    Point p = new Point(ppo_, minGI_ - 1, 1 << lgMinLongs_, 1 << lgMaxTrials_); //just below the start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      final long[] array = new long[p.arrLongs];
      final FastMemory mem = new FastMemory(array);
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t = 0; t < p.trials; t++) { //do writes first
        p.sumWriteTrials_nS += trial_FastMemoryHeap_ISF2(mem, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumReadTrials_nS += trial_FastMemoryHeap_ISF2(mem, p.arrLongs, true); //a single trial read
      }
      p.printRow();
    }
  }

  //Must do writes first
  private static final long trial_FastMemoryHeap_ISF2(final FastMemory mem, final int arrLongs,
      final boolean read) {
    final long checkSum = (arrLongs * (arrLongs - 1L)) / 2L;
    final long startTime_nS, stopTime_nS;
    final Object array = mem.memArray_;
    final long objectBaseOffset = mem.objectBaseOffset_;
    final long nativeRawStartAddress = mem.nativeRawStartAddress_;
    final long capacityBytes = mem.capacityBytes_;

    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (long i = 0; i < arrLongs; i++) {
        trialSum += FastMemory.getLong_ISF2(
            array, objectBaseOffset, nativeRawStartAddress, capacityBytes, (i << 3));
      }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        throw new IllegalStateException("Bad checksum: " + trialSum + " != " + checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (long i = 0; i < arrLongs; i++) {
        FastMemory.putLong_ISF2(
            array, objectBaseOffset, nativeRawStartAddress, capacityBytes, (i << 3), i);
      }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }

  /*************************************/
  // FAST MEMORY - DIRECT, NO INTERFACE, STATIC, FINAL, PASS ALL
  /*************************************/

  private void testFastMemoryDirect_ISF2() {
    Point p = new Point(ppo_, minGI_ - 1, 1 << lgMinLongs_, 1 << lgMaxTrials_); //just below the start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      final FastMemory mem = new AllocFastMemory(p.arrLongs << 3); //AllocDirect
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t = 0; t < p.trials; t++) { //do writes first
        p.sumWriteTrials_nS += trial_FastMemoryDirect_ISF2(mem, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t = 0; t < p.trials; t++) {
        p.sumReadTrials_nS += trial_FastMemoryDirect_ISF2(mem, p.arrLongs, true); //a single trial read
      }
      p.printRow();
      mem.freeMemory();
    }
  }

  //Must do writes first
  private static final long trial_FastMemoryDirect_ISF2(final FastMemory mem, final int arrLongs,
      final boolean read) {
    final long checkSum = (arrLongs * (arrLongs - 1L)) / 2L;
    //final long rawBaseAdd = mem.getAddress(0L);
    final long startTime_nS, stopTime_nS;
    final Object array = mem.memArray_;
    final long objectBaseOffset = mem.objectBaseOffset_;
    final long nativeRawStartAddress = mem.nativeRawStartAddress_;
    final long capacityBytes = mem.capacityBytes_;

    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) {
        trialSum += FastMemory.getLong_ISF2(
            array, objectBaseOffset, nativeRawStartAddress, capacityBytes, (i << 3));
      }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        throw new IllegalStateException("Bad checksum: " + trialSum + " != " + checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i = 0; i < arrLongs; i++) {
        FastMemory.putLong_ISF2(
            array, objectBaseOffset, nativeRawStartAddress, capacityBytes, (i << 3), i);
      }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }




  /*************************************/

  private final void freeMemory() {
    if (address_ != 0) {
      unsafe.freeMemory(address_);
      address_ = 0L;
    }
  }

  /**
   * If the JVM calls this method and a "freeMemory() has not been called" a <i>System.err</i>
   * message will be logged.
   */
  @Override
  protected void finalize() {
    if (address_ > 0L) {
      System.err.println("ERROR: freeMemory() has not been called: Address: " + address_
        + ", capacity: " + (arrLongs_ << 3));
      final java.lang.StackTraceElement[] arr = Thread.currentThread().getStackTrace();
      for (int i = 0; i < arr.length; i++) {
          System.err.println(arr[i].toString());
      }
      unsafe.freeMemory(address_);
      address_ = 0L;
    }
  }

  /**
   * Start the testing
   */
  public void go() {
    final long startMillis = System.currentTimeMillis();
    println("Test Long Array On Heap");
    testHeapArrayByIndex();
    println("\nTest Direct Memory By Unsafe");
    testNativeArrayByUnsafe();
    println("\nTest ByteBuffer Heap");
    testByteBufferHeap();
    println("\nTest ByteBuffer Direct");
    testByteBufferDirect();
    println("\nTest LongBuffer Heap");
    testLongBufferHeap();
    println("\nTest LongBuffer Direct");
    testLongBufferDirect();
    println("\nTest Memory Heap");
    testMemoryHeap();
    println("\nTest Memory Direct");
    testMemoryDirect();
    println("\nTest FastMemory Heap, I");
    testFastMemoryHeap_I();
    println("\nTest FastMemory Direct, I");
    testFastMemoryDirect_I();
    println("\nTest FastMemory Heap, IA");
    testFastMemoryHeap_IA();
    println("\nTest FastMemory Direct, IA");
    testFastMemoryDirect_IA();
    println("\nTest FastMemory Heap, IAS");
    testFastMemoryHeap_IAS();
    println("\nTest FastStaticMemory Direct, IAS");
    testFastMemoryDirect_IAS();
    println("\nTest FastMemory Heap, IASF");
    testFastMemoryHeap_IASF();
    println("\nTest FastStaticMemory Direct, IASF");
    testFastMemoryDirect_IASF();
    println("\nTest FastStaticMemory Direct, IASFO");
    testFastMemoryDirect_IASFO();

    println("\nTest FastMemory Heap, ISF, Pass FastMemory");
    testFastMemoryHeap_ISF();
    println("\nTest FastStaticMemory Direct, ISF, Pass FastMemroy");
    testFastMemoryDirect_ISF();

    println("\nTest FastMemory Heap, ISF, Pass All");
    testFastMemoryHeap_ISF2();
    println("\nTest FastStaticMemory Direct, ISF, Pass All");
    testFastMemoryDirect_ISF2();

    final long testMillis = System.currentTimeMillis() - startMillis;
    println("Total Time: " + milliSecToString(testMillis));
  }

  /**
   * MAIN
   * @param args not used
   */
  public static void main(final String[] args) {
    final MemoryPerformance test = new MemoryPerformance();
    test.go();
    //testMilliSec();
    //testNanoSec();
  }

  //Handy utils

  public static void println(final String s) { System.out.println(s); }

  /**
   * copied from com.yahoo.sketches.TestingUtil which is a test class not in the main jar
   * @param mS milliseconds
   * @return string
   */
  public static String milliSecToString(final long mS) {
    final long rem_mS = (long)(mS % 1000.0D);
    final long rem_sec = (long)(mS / 1000.0D % 60.0D);
    final long rem_min = (long)(mS / 60000.0D % 60.0D);
    final long hr = (long)(mS / 3600000.0D);
    final String mSstr = Util.zeroPad(Long.toString(rem_mS), 3);
    final String secStr = Util.zeroPad(Long.toString(rem_sec), 2);
    final String minStr = Util.zeroPad(Long.toString(rem_min), 2);
    return String.format("%d:%2s:%2s.%3s", hr, minStr, secStr, mSstr);
  }
}