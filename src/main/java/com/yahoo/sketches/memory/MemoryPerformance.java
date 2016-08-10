/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the 
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.memory;

import static com.yahoo.sketches.Util.zeroPad;
import static com.yahoo.sketches.memory.UnsafeUtil.unsafe;
import static java.lang.Math.pow;

public final class MemoryPerformance {
  private int arrLongs_;    //# entries in array
  private int lgMinTrials_; //minimum # of trials
  private int lgMaxTrials_; //maximum # of trials
  private int lgMinLongs_;  //minimum array longs
  private int lgMaxLongs_;  //maximum array longs
  private int ppo_;         //points per octave of arrLongs (x-axis)
  private int minGI_;     //min generating index
  private int maxGI_;     //max generating index
  private int lgMaxOps_;    //lgMaxLongs_ + lgMinTrials_
  private long address_ = 0; //used for unsafe
  
  public MemoryPerformance() {
    //Configure
    lgMinTrials_ = 6;  //6
    lgMaxTrials_ = 20; //20
    lgMinLongs_ = 5;   //5
    lgMaxLongs_ = 26;  //26
    ppo_ = 4; //4
    //Compute
    maxGI_ = ppo_ * lgMaxLongs_;
    minGI_ = ppo_ * lgMinLongs_;
    lgMaxOps_ = lgMaxLongs_ + lgMinTrials_;
  }
  
  private class Point {
    int gi;
    int arrLongs;
    int trials;
    
    Point(int gi, int arrLongs, int trials) {
      this.gi = gi;
      this.arrLongs = arrLongs;
      this.trials = trials;
    }
  }
  
  private Point getNextPoint(Point p) {
    int lastArrLongs = (int)pow(2.0, (double)p.gi/ppo_);
    int nextArrLongs;
    double logArrLongs;
    do {
      logArrLongs = (++p.gi)/(double)ppo_;
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
  // ON-HEAP
  /*************************************/
  
  private void testHeapArrayByIndex() {
    println("LgLongs\tLongs\tTrials\t#Ops\tAvgRTrial_nS\tAvgROp_nS\tAvgWTrial_nS\tAvgWOp_nS");
    Point p = new Point(minGI_-1, 1<<lgMinLongs_, 1<<lgMaxTrials_); //just below the start
    while ((p = getNextPoint(p)) != null) {
      long[] array = new long[p.arrLongs];
      
      //Do all write trials at this array size point
      long sumWriteTrials_nS = 0;
      for (int t=0; t<p.trials; t++) {
        sumWriteTrials_nS += trial_HeapArrayByIndex(array, false); //a single trial write
      }
      //Do all read trials at this array size point
      long sumReadTrials_nS  = 0;
      for (int t=0; t<p.trials; t++) {
        sumReadTrials_nS += trial_HeapArrayByIndex(array, true); //a single trial read
      }
      //Print Results
      printOut(p.gi, sumReadTrials_nS, sumWriteTrials_nS, p.trials, p.arrLongs);
    }
  }
  
  //Must do writes first
  private static final long trial_HeapArrayByIndex(long[] array, boolean read) {
    int arrLongs = array.length;
    long checkSum = (arrLongs * (arrLongs - 1L)) /2L;
    long startTime_nS, stopTime_nS; 
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i=0; i<arrLongs; i++) { trialSum += array[i]; }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        throw new IllegalStateException("Bad checksum: "+trialSum+" != "+checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i=0; i<arrLongs; i++) { array[i] = i; }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }
  
  /*************************************/
  //  OFFHEAP
  /*************************************/
  
  private void testNativeArrayByUnsafe() {
    println("LgLongs\tLongs\tTrials\t#Ops\tAvgRTrial_nS\tAvgROp_nS\tAvgWTrial_nS\tAvgWOp_nS");
    Point p = new Point(minGI_-1, 1<<lgMinLongs_, 1<<lgMaxTrials_); //just below the start
    address_ = 0L;
    while ((p = getNextPoint(p)) != null) {
      address_ = unsafe.allocateMemory(p.arrLongs << 3);
      //Do all write trials at this array size point
      long sumWriteTrials_nS = 0;
      for (int t=0; t<p.trials; t++) { //do writes first
        sumWriteTrials_nS += trial_NativeArrayByUnsafe(address_, p.arrLongs, false); //a single trial write
      }
      //Do all read trials at this array size point
      long sumReadTrials_nS  = 0;
      for (int t=0; t<p.trials; t++) {
        sumReadTrials_nS += trial_NativeArrayByUnsafe(address_, p.arrLongs, true); //a single trial read
      }
      //Print Results
      printOut(p.gi, sumReadTrials_nS, sumWriteTrials_nS, p.trials, p.arrLongs);
      freeMemory();
    }
  }
  
  //Must do writes first
  private static final long trial_NativeArrayByUnsafe(long address, int arrLongs, boolean read) {
    long checkSum = (arrLongs * (arrLongs - 1L)) /2L;
    long startTime_nS, stopTime_nS; 
    if (read) {
      //Timing interval for a single trial
      long trialSum = 0;
      startTime_nS = System.nanoTime();
      for (int i=0; i<arrLongs; i++) { trialSum += unsafe.getLong(address + (i << 3)); }
      stopTime_nS = System.nanoTime();
      if (trialSum != checkSum) {
        if (address != 0) { unsafe.freeMemory(address); }
        throw new IllegalStateException("Bad checksum: "+trialSum+" != "+checkSum);
      }
    } else { //write
      startTime_nS = System.nanoTime();
      for (int i=0; i<arrLongs; i++) { unsafe.putLong(address + (i << 3), i); }
      stopTime_nS = System.nanoTime();
    }
    return stopTime_nS - startTime_nS;
  }
  
  /*************************************/
  
  private void printOut(int gi, long sumReadTrials_nS, long sumWriteTrials_nS, int trials, int arrLongs) {
    double logArrLongs = gi/(double)ppo_;
    double rTrial_nS = (double)sumReadTrials_nS/trials;
    double wTrial_nS = (double)sumWriteTrials_nS/trials;
    long numOps = (long)((double)trials * arrLongs);
    double rOp_nS = sumReadTrials_nS/(double)numOps;
    double wOp_nS = sumWriteTrials_nS/(double)numOps;
    //Print
    String out = String.format("%6.3f\t%d\t%d\t%d\t%.1f\t%8.3f\t%.1f\t%8.3f", 
        logArrLongs, arrLongs, trials, numOps, rTrial_nS, rOp_nS, wTrial_nS, wOp_nS);
    println(out);
  }
  
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
  
  //Handy utils
  
  public static String timeToString(double nanoSec) {
    long nS = (long)nanoSec;
    long rem_nS = nS % 1000;
    long rem_uS = (nS / 1000) % 1000;
    long rem_mS = (nS / 1000000) % 1000;
    long sec    = nS / 1000000000;
    String nSstr = zeroPad(Long.toString(rem_nS), 3);
    String uSstr = zeroPad(Long.toString(rem_uS), 3);
    String mSstr = zeroPad(Long.toString(rem_mS), 3);
    return String.format("%d.%3s %3s %3s", sec, mSstr, uSstr, nSstr);
  }
  
  public static void println(String s) {System.out.println(s); }
  
  //MAIN
  public static void main(String[] args) {
    MemoryPerformance test = new MemoryPerformance();
    println("Test Heap Array By Index");
    test.testHeapArrayByIndex();
    println("\nTest Native Memory By Unsafe");
    test.testNativeArrayByUnsafe();
  }
  
}
