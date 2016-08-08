/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the 
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.memory;

import static com.yahoo.sketches.Util.zeroPad;
import static com.yahoo.sketches.memory.UnsafeUtil.unsafe;
import static java.lang.Math.floor;
import static java.lang.Math.pow;

public final class MemoryPerformance {
  private int arrLongs_;    //# entries in array used for heap R/W
  private int lgMaxTrials_; //maximum X-axis
  private int ppo_;         //points per octave
  private int lastGI_;      //last generating index
  private long[] array_;    //array used for heap R/W
  private long checkSum_;
  private long address_ = 0; //used for unsafe
  
  public MemoryPerformance() {
    //Configure
    arrLongs_ = 1 << 14; //16K entries
    lgMaxTrials_ = 14; //16K trials
    ppo_ = 4;
    //Compute
    lastGI_ = ppo_ * lgMaxTrials_;
    array_ = new long[arrLongs_];
    checkSum_ = computeCorrectChecksum();
  }
  
  private final long computeCorrectChecksum() {
    long sum = 0L;
    for (int i=1; i<=arrLongs_; i++) { sum += i; }
    return sum;
  }
  
  /*************************************/
  // ON-HEAP
  /*************************************/
  
  private void loadHeapArray() {
    for (int i=0; i<arrLongs_; i++) { array_[i] = i+1; }
  }
  
  private void clearHeapArray() {
    for (int i=0; i<arrLongs_; i++) { array_[i] = 0L; }
  }
  /*************************************/
  
  private void testReadHeapArrayByIndex() {
    loadHeapArray();
    println("Trials\tTrial_nS\tOp_nS\t#Ops");
    int lastTrials = 0;
    for (int gi = 0; gi <= lastGI_; gi++) {
      int trials = (int)floor(pow(2.0, (double)gi/ppo_));
      if (trials == lastTrials) continue;
      lastTrials = trials;
      long sumTrialsTime_nS = 0;
      for (int t=0; t<trials; t++) { 
        //run a single trial
        sumTrialsTime_nS += readHeapArrayByIndexTrial(array_, checkSum_);
      }
      long trial_nS = (long)((double)sumTrialsTime_nS/trials);
      double numOps = (double)trials * arrLongs_;
      String numOpsStr = String.format("%.0f", numOps);
      double singleOp_nS = sumTrialsTime_nS/numOps;
      String readTimeStr = String.format("%6.3f", singleOp_nS);
      //output summary of trials per gi
      println(trials+"\t"+trial_nS+"\t"+readTimeStr+"\t"+numOpsStr);
    }
  }
  
  private static final long readHeapArrayByIndexTrial(long[] array, long checkSum) {
    long trialSum = 0;
    int size = array.length;
      //Timing interval for a single trial
      long startTime_nS = System.nanoTime();
      for (int i=0; i<size; i++) { trialSum += array[i]; }
      long trialTime_nS = System.nanoTime() - startTime_nS;
      
    if (trialSum != checkSum) throw new IllegalStateException("Bad checksum: "+trialSum+" != "+checkSum);
    return trialTime_nS;
  }
  
  /*************************************/
  
  private void testWriteHeapArrayByIndex() {
    println("Trials\tTrial_nS\tOp_nS\t#Ops");
    clearHeapArray();
    int lastTrials = 0;
    for (int gi = 0; gi <= lastGI_; gi++) {
      int trials = (int)floor(pow(2.0, (double)gi/ppo_));
      if (trials == lastTrials) continue;
      lastTrials = trials;
      long sumTrialsTime_nS = 0;
      for (int t=0; t<trials; t++) { 
        //run a single trial
        sumTrialsTime_nS += writeHeapArrayByIndexTrial(array_, checkSum_);
      }
      long trial_nS = (long)((double)sumTrialsTime_nS/trials);
      double numOps = (double)trials * arrLongs_;
      String numOpsStr = String.format("%.0f", numOps);
      double singleOp_nS = sumTrialsTime_nS/numOps;
      String readTimeStr = String.format("%6.3f", singleOp_nS);
      //output summary of trials per gi
      println(trials+"\t"+trial_nS+"\t"+readTimeStr+"\t"+numOpsStr);
    }
  }
  
  private static final long writeHeapArrayByIndexTrial(long[] array, long checkSum) {
    int size = array.length;
      //Timing interval for a single trial
      long startTime_nS = System.nanoTime();
      for (int i=0; i<size; i++) { array[i] = i+1; }
      long trialTime_nS = System.nanoTime() - startTime_nS;
    
    readHeapArrayByIndexTrial(array, checkSum); //does the checksum
    return trialTime_nS;
  }
  
  /*************************************/
  //  OFFHEAP
  /*************************************/
  
  private void allocateMemory() {
    if (address_ == 0) {
      address_ = unsafe.allocateMemory(arrLongs_ << 3);
    }
  }
  
  private void loadNativeMemory() {
    allocateMemory();
    for (long i=0; i<arrLongs_; i++) {
      unsafe.putLong(address_ + (i << 3), i+1);
    }
  }
  
  private void clearNativeMemory() {
    if (address_ == 0) {
      unsafe.setMemory(null, address_, arrLongs_ << 3, (byte) 0);
    }
  }
  
  /*************************************/
  
  private void testReadNativeArrayByUnsafe() {
    loadNativeMemory();
    println("Trials\tTrial_nS\tOp_nS\t#Ops");
    int lastTrials = 0;
    for (int gi = 0; gi <= lastGI_; gi++) {
      int trials = (int)floor(pow(2.0, (double)gi/ppo_));
      if (trials == lastTrials) continue;
      lastTrials = trials;
      long sumTrialsTime_nS = 0;
      for (int t=0; t<trials; t++) { 
        //run a single trial
        sumTrialsTime_nS += readNativeArrayByUnsafeTrial(address_, arrLongs_, checkSum_);
      }
      long trial_nS = (long)((double)sumTrialsTime_nS/trials);
      double numOps = (double)trials * arrLongs_;
      String numOpsStr = String.format("%.0f", numOps);
      double singleOp_nS = sumTrialsTime_nS/numOps;
      String readTimeStr = String.format("%6.3f", singleOp_nS);
      //output summary of trials per gi
      println(trials+"\t"+trial_nS+"\t"+readTimeStr+"\t"+numOpsStr);
    }
    unsafe.freeMemory(address_);
    address_ = 0L;
  }
  
  //Includes 4 variations of the for-loop.
  @SuppressWarnings("unused")
  private static final long readNativeArrayByUnsafeTrial(long address, int arrLongs, long checkSum) {
    long trialSum = 0;
    long capBytes = arrLongs << 3;
      //Timing interval for a single trial
      long startTime_nS = System.nanoTime(); //time to execute a trial
      for (int i=0; i<arrLongs; i++)  { trialSum += unsafe.getLong(address + (i << 3)); } //fastest
      //for (int i=0; i<arrLongs; i++)  { trialSum += unsafe.getLong(address +  i * 8); } //faster
      //for (int i=arrLongs; i-- >0; )  { trialSum += unsafe.getLong(address + (i << 3)); }//slower
      //for (int i=0; i<capBytes; i+=8) { trialSum += unsafe.getLong(address + i); } //slowest
      long trialTime_nS = System.nanoTime() - startTime_nS;
    
    if (trialSum != checkSum) throw new IllegalStateException("Bad checksum: "+trialSum+" != "+checkSum);
    return trialTime_nS;
  }
  
  /*************************************/
  
  private void testWriteNativeArrayByUnsafe() {
    allocateMemory();
    clearNativeMemory();
    println("Trials\tTrial_nS\tOp_nS\t#Ops");
    int lastTrials = 0;
    for (int gi = 0; gi <= lastGI_; gi++) {
      int trials = (int)floor(pow(2.0, (double)gi/ppo_));
      if (trials == lastTrials) continue;
      lastTrials = trials;
      long sumTrialsTime_nS = 0;
      for (int t=0; t<trials; t++) { 
        //run a single trial
        sumTrialsTime_nS += writeNativeArrayByUnsafeTrial(address_, arrLongs_, checkSum_);
      }
      long trial_nS = (long)((double)sumTrialsTime_nS/trials);
      double numOps = (double)trials * arrLongs_;
      String numOpsStr = String.format("%.0f", numOps);
      double singleOp_nS = sumTrialsTime_nS/numOps;
      String readTimeStr = String.format("%6.3f", singleOp_nS);
      //output summary of trials per gi
      println(trials+"\t"+trial_nS+"\t"+readTimeStr+"\t"+numOpsStr);
    }
    unsafe.freeMemory(address_);
    address_ = 0L;
    
  }
  
  private static final long writeNativeArrayByUnsafeTrial(long address, int arrLongs, long checkSum) {
      //Timing interval for a single trial
      long startTime_nS = System.nanoTime();
      for (int i=0; i<arrLongs; i++)  { unsafe.putLong(address + (i << 3), i+1); }
      long trialTime_nS = System.nanoTime() - startTime_nS;
    
    readNativeArrayByUnsafeTrial(address, arrLongs, checkSum); //does the checksum
    return trialTime_nS;
  }
  
  /*************************************/
  
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
    println("Test Read Heap Array By Index");
    test.testReadHeapArrayByIndex();
    println("\nTest Write Heap Array By Index");
    test.testWriteHeapArrayByIndex();
    println("\nTest Read Native Array By Unsafe");
    test.testReadNativeArrayByUnsafe();
    println("\nTest Write Native Array By Unsafe");
    test.testWriteNativeArrayByUnsafe();
  }
  
}
