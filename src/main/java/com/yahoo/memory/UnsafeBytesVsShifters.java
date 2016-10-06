/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory;

import static com.yahoo.memory.UnsafeUtil.unsafe;
import static com.yahoo.sketches.TestingUtil.milliSecToString;
import static java.lang.Math.pow;

//import static com.yahoo.sketches.memory.MemoryPerformance.*;


public class UnsafeBytesVsShifters {
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

  public UnsafeBytesVsShifters() {
    //Configure
    lgMinTrials_ = 10;  //was 6
    lgMaxTrials_ = 20; //was 20
    lgMinLongs_ = 5;   //was 5
    lgMaxLongs_ = 20;  //was 26
    ppo_ = 1.0;        //was 4
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

    Point(double ppo, double gi, int arrLongs, int trials) {
      this.ppo = ppo;
      this.gi = gi;
      this.arrLongs = arrLongs;
      this.trials = trials;
    }

    public static void printHeader() {
      println("LgLongs\tLongs\tTrials\t#Ops\tAvgRTrial_nS\tAvgROp_nS\tAvgWTrial_nS\tAvgWOp_nS");
    }

    public void printRow() {
      long numOps = (long)((double)trials * arrLongs);
      double logArrLongs = gi/ppo;
      double rTrial_nS = (double)sumReadTrials_nS/trials;
      double wTrial_nS = (double)sumWriteTrials_nS/trials;
      double rOp_nS = rTrial_nS/arrLongs;
      double wOp_nS = wTrial_nS/arrLongs;
      //Print
      String out = String.format("%6.2f\t%d\t%d\t%d\t%.1f\t%8.3f\t%.1f\t%8.3f",
          logArrLongs, arrLongs, trials, numOps, rTrial_nS, rOp_nS, wTrial_nS, wOp_nS);
      println(out);
    }
  }

  private Point getNextPoint(Point p) {
    int lastArrLongs = (int)pow(2.0, p.gi/ppo_);
    int nextArrLongs;
    double logArrLongs;
    do {
      logArrLongs = (++p.gi)/ppo_;
      if (p.gi > maxGI_) return null;
      nextArrLongs = (int)pow(2.0, logArrLongs);
    } while (nextArrLongs <= lastArrLongs);
    p.arrLongs = lastArrLongs = nextArrLongs;
    //compute trials
    double logTrials = Math.min(lgMaxOps_- logArrLongs, lgMaxTrials_);
    p.trials = (int)pow(2.0, logTrials);
    return p;
  }

  /*************************************/
  // UNSAFE BYTE
  /*************************************/

  private void testUnsafeByte() {
    Point p = new Point(ppo_, minGI_-1, 1<<lgMinLongs_, 1<<lgMaxTrials_); //just below the start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      address_ = unsafe.allocateMemory(p.arrLongs << 3);
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t=0; t<p.trials; t++) { //do writes first
        p.sumWriteTrials_nS += trial_UnsafeByte(address_, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t=0; t<p.trials; t++) {
        p.sumReadTrials_nS += trial_UnsafeByte(address_, p.arrLongs, true); //a single trial read
      }
      p.printRow();
      freeMemory();
    }
  }

//Must do writes first
  private static final long trial_UnsafeByte(long address, int arrLongs, boolean read) {
    long checkSum = (arrLongs * (arrLongs - 1L)) /2L;
    long startTime_nS, stopTime_nS;
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i=0; i<arrLongs; i++) {
        int j = i << 3;
        long k = 0;
        k |= (unsafe.getByte(address + j) & 0XFFL);
        k |= (unsafe.getByte(address + j + 1) & 0XFFL) << 8;
        k |= (unsafe.getByte(address + j + 2) & 0XFFL) << 16;
        k |= (unsafe.getByte(address + j + 3) & 0XFFL) << 24;
        k |= (unsafe.getByte(address + j + 4) & 0XFFL) << 32;
        k |= (unsafe.getByte(address + j + 5) & 0XFFL) << 40;
        k |= (unsafe.getByte(address + j + 6) & 0XFFL) << 48;
        k |= (unsafe.getByte(address + j + 7) & 0XFFL) << 56;
        trialSum += k;
      }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        if (address != 0) { unsafe.freeMemory(address); }
        throw new IllegalStateException("Bad checksum: "+trialSum+" != "+checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i=0; i<arrLongs; i++) {
        long j = i << 3;
        long k = i;
        unsafe.putByte(address + j, (byte) k);
        unsafe.putByte(address + j + 1, (byte) (k >>>= 8));
        unsafe.putByte(address + j + 2, (byte) (k >>>= 8));
        unsafe.putByte(address + j + 3, (byte) (k >>>= 8));
        unsafe.putByte(address + j + 4, (byte) (k >>>= 8));
        unsafe.putByte(address + j + 5, (byte) (k >>>= 8));
        unsafe.putByte(address + j + 6, (byte) (k >>>= 8));
        unsafe.putByte(address + j + 7, (byte) (k >>>= 8));
      }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }

  /*************************************/
  // BYTES VIA SHIFTERS
  /*************************************/

  private void testBytesByShifters() {
    Point p = new Point(ppo_, minGI_-1, 1<<lgMinLongs_, 1<<lgMaxTrials_); //just below the start
    Point.printHeader();
    while ((p = getNextPoint(p)) != null) { //an array size point
      address_ = unsafe.allocateMemory(p.arrLongs << 3);
      //Do all write trials first
      p.sumWriteTrials_nS = 0;
      for (int t=0; t<p.trials; t++) { //do writes first
        p.sumWriteTrials_nS += trial_BytesByShifters(address_, p.arrLongs, false); //a single trial write
      }
      //Do all read trials
      p.sumReadTrials_nS  = 0;
      for (int t=0; t<p.trials; t++) {
        p.sumReadTrials_nS += trial_BytesByShifters(address_, p.arrLongs, true); //a single trial read
      }
      p.printRow();
      freeMemory();
    }
  }

//Must do writes first
  private static final long trial_BytesByShifters(long address, int arrLongs, boolean read) {
    long checkSum = (arrLongs * (arrLongs - 1L)) /2L;
    long startTime_nS, stopTime_nS;
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;

      startTime_nS = System.nanoTime();
      for (int i=0; i<arrLongs; i++) {
        long k = 0;
        long long0 = unsafe.getLong(address + (i << 3));
        k |= (Shifters.getByte0(long0) & 0XFFL);
        k |= (Shifters.getByte1(long0) & 0XFFL) << 8;
        k |= (Shifters.getByte2(long0) & 0XFFL) << 16;
        k |= (Shifters.getByte3(long0) & 0XFFL) << 24;
        k |= (Shifters.getByte4(long0) & 0XFFL) << 32;
        k |= (Shifters.getByte5(long0) & 0XFFL) << 40;
        k |= (Shifters.getByte6(long0) & 0XFFL) << 48;
        k |= (Shifters.getByte7(long0) & 0XFFL) << 56;
        trialSum += k;
      }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        if (address != 0) { unsafe.freeMemory(address); }
        throw new IllegalStateException("Bad checksum: "+trialSum+" != "+checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i=0; i<arrLongs; i++) {
        //int j = i << 3;
        long long0 = 0L;
        long k = i;
        long0 = Shifters.putByte0(k, long0);
        long0 = Shifters.putByte1(k >>>= 8, long0);
        long0 = Shifters.putByte2(k >>>= 8, long0);
        long0 = Shifters.putByte3(k >>>= 8, long0);
        long0 = Shifters.putByte4(k >>>= 8, long0);
        long0 = Shifters.putByte5(k >>>= 8, long0);
        long0 = Shifters.putByte6(k >>>= 8, long0);
        long0 = Shifters.putByte7(k >>>= 8, long0);
        unsafe.putLong(address + (i << 3), long0);
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
      java.lang.StackTraceElement[] arr = Thread.currentThread().getStackTrace();
      for (int i = 0; i < arr.length; i++) {
          System.err.println(arr[i].toString());
      }
      unsafe.freeMemory(address_);
      address_ = 0L;
    }
  }

  public void go() {
    long startMillis = System.currentTimeMillis();
    println("Test Unsafe Byte");
    testUnsafeByte();
    println("\nTest Bytes By Shifters");
    testBytesByShifters();
    long testMillis = System.currentTimeMillis() - startMillis;
    println("Total Time: "+ milliSecToString(testMillis));
  }

  //MAIN
  public static void main(String[] args) {
    UnsafeBytesVsShifters test = new UnsafeBytesVsShifters();
    test.go();
  }

  public static void println(String s) {System.out.println(s); }
}
