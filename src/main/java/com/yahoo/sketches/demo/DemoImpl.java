/*
 * Copyright 2016, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.demo;

import static com.yahoo.sketches.demo.Util.getMinSecFromMilli;
import static com.yahoo.sketches.demo.Util.nextLong;
import static com.yahoo.sketches.demo.Util.println;
import static com.yahoo.sketches.hash.MurmurHash3.hash;
import static java.lang.Math.sqrt;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Random;

import com.yahoo.sketches.Family;
import com.yahoo.sketches.ResizeFactor;
import com.yahoo.sketches.hll.HllSketch;
import com.yahoo.sketches.theta.Sketches;
import com.yahoo.sketches.theta.UpdateSketch;

/**
 * A simple demo that compares brute force counting of uniques vs. using sketches.
 *
 * <p>This demo computes a stream of values and feeds them first to
 * an exact sort-based method of computing the number of unique values
 * in the stream and then feeds a similar stream to two different types of
 * sketches from the library.
 *
 * <p>This demo becomes most significant in the case where the number of uniques in the
 * stream exceeds what the computer can hold in memory.
 *
 * <p>This demo utilizes the Unix sort and wc commands for the brute force compuation.
 * So this needs to be run on a linux or mac machine. A windows machine with a similar unix
 * library installed should also work, but it has not been tested.
 */
public class DemoImpl {
  //Static constants
  private static final String LS = System.getProperty("line.separator");
  private static final byte LS_BYTE = LS.getBytes(UTF_8)[0];
  private static Random rand = new Random(9001);
  private static StandardOpenOption C = StandardOpenOption.CREATE;
  private static StandardOpenOption W = StandardOpenOption.WRITE;
  private static StandardOpenOption TE = StandardOpenOption.TRUNCATE_EXISTING;

  //Stream Configuration
  private int byteBufCap_ = 1000000; //ByteBuffer capacity
  private long n_ = (long)1E8; //stream length
  private int batchSz_ = 1000; //batch size
  private final double uniquesFrac_; //fraction to be unique

  //Sketch configuration
  private int lgK_ = 14; //16K

  //Internal sketch values
  private int maxMemSkBytes_;
  private double rse2_;  //RSE for 95% confidence
  private UpdateSketch tSketch_ = null;
  private HllSketch hllSketch_ = null;

  //Other internal values
  private Path path = Paths.get("tmp/test.txt");
  private long[] vArr_ = new long[1]; //reuse this array
  private long fileBytes_ = 0;
  private long u_;    //global unique count;

  /**
   * Constuct the demo.
   * @param streamLen  The total stream length. Must be &gt; 1000. Will be rounded up to nearest
   * 1000.
   * @param uniquesFraction the fraction of streamLen values less than 1.0, that will be unique.
   * The actual # of uniques will vary around this value, because it is computed statistically.
   */
  public DemoImpl(final long streamLen, final double uniquesFraction) {
    if ((uniquesFraction <= 0.0) || (uniquesFraction > 1.0)) {
      throw new IllegalArgumentException(
          "uniquesFraction must be > 0.0 and <= 1.0: " + uniquesFraction);
    }
    uniquesFrac_ = uniquesFraction;
    final long m = streamLen / 1000;
    if (m * 1000 == streamLen) {
      n_ = streamLen;
    } else {
      n_ = (m + 1) * 1000;
    }
    lgK_ = 14; //Log-base 2 of the configured sketch size = 16K
    final File dir = new File("tmp"); //new directory tmp
    if (!dir.exists()) {
      try {
        dir.mkdir();
      } catch (final SecurityException e) {
        throw new SecurityException(e);
      }
    }
  }

  /**
   * Run the demo
   */
  public void runDemo() {
    println("# COMPUTE DISTINCT COUNT EXACTLY:");
    long exactTimeMS;

    exactTimeMS = buildFile();
    //exactTimeMS = buildFileAndSketch(); //used instead only for testing

    println("## SORT & REMOVE DUPLICATES");
    final String sortCmd = "sort -u -o tmp/sorted.txt tmp/test.txt";
    exactTimeMS += UnixCmd.run("sort", sortCmd);

    println("\n## LINE COUNT");
    final String wcCmd = "wc -l tmp/sorted.txt";
    exactTimeMS += UnixCmd.run("wc", wcCmd);

    println("Total Exact " + getMinSecFromMilli(exactTimeMS) + LS + LS);

    println("# COMPUTE DISTINCT COUNT USING SKETCHES");
    configureThetaSketch();
    long sketchTimeMS = buildSketch();
    double factor = exactTimeMS * 1.0 / sketchTimeMS;
    println("Speedup Factor " + String.format("%.1f", factor) + LS);

    configureHLLSketch();
    sketchTimeMS = buildSketch();
    factor = exactTimeMS * 1.0 / sketchTimeMS;
    println("Speedup Factor " + String.format("%.1f", factor));

  }

  /**
   * @return total test time in milliseconds
   */
  private long buildFile() {
    println("## BUILD FILE:");
    rand = new Random(9001);
    final ByteBuffer byteBuf = ByteBuffer.allocate(byteBufCap_);
    u_ = 1; //reset global unique counter
    fileBytes_ = 0;
    final long testStartTime_mS = System.currentTimeMillis();
    try (SeekableByteChannel sbc = Files.newByteChannel(path, C, W, TE)) {
      for (long i = 0; i < n_; i++) {
        final long v = nextValue();
        final String s = Long.toHexString(v);
        if (byteBuf.remaining() < 25) {
          byteBuf.flip();
          fileBytes_ += sbc.write(byteBuf);
          byteBuf.clear();
        }
        byteBuf.put(s.getBytes(UTF_8)).put(LS_BYTE);
      }
      if (byteBuf.position() > 0) { //write remainder
        byteBuf.flip();
        fileBytes_ += sbc.write(byteBuf);
        byteBuf.clear();
      }
    }
    catch (final IOException e) {
      e.printStackTrace();
    }
    final long testTime_mS = System.currentTimeMillis() - testStartTime_mS;
    //Print common results
    printCommon(testTime_mS, n_, u_);
    //Print file results
    println("File Size Bytes: " + String.format("%,d", fileBytes_) + LS);
    return testTime_mS;
  }

  /**
   * @return total test time in milliseconds
   */
  private long buildSketch() {
    rand = new Random(9001);
    u_ = 1; //reset global unique counter
    long stLen = 0;
    final long[] vArr = new long[batchSz_];
    long testTime_nS = 0;

    while (stLen < n_) {
      for (int i = 0; i < batchSz_; i++) { vArr[i] = nextValue(); }
      stLen += batchSz_;
      if (tSketch_ != null) { //Theta Sketch
        testTime_nS += timeThetaSketch(tSketch_, vArr);
      } else {
        testTime_nS += timeHllSketch(hllSketch_, vArr);
      }
    }
    final long testTime_mS = testTime_nS / 1000000;
    //Print sketch name
    final String sk = (tSketch_ != null) ? "THETA" : "HLL";
    println("## USING " + sk + " SKETCH");
    //Print common results
    printCommon(testTime_mS, n_, u_);

    //Print sketch results
    printSketchResults(u_, maxMemSkBytes_, rse2_);
    return testTime_mS;
  }

  //return nanoseconds
  private static long timeThetaSketch(final UpdateSketch tSketch, final long[] batchArr) {
    final int batLen = batchArr.length;
    final long testBatchStart_nS = System.nanoTime();
    for (int i = 0; i < batLen; i++) {
      tSketch.update(batchArr[i]);
    }
    return System.nanoTime() - testBatchStart_nS;
  }

  //return nanoseconds
  private static long timeHllSketch(final HllSketch hSketch, final long[] batchArr) {
    final int batLen = batchArr.length;
    final long testBatchStart_nS = System.nanoTime();
    for (int i = 0; i < batLen; i++) {
      hSketch.update(batchArr[i]);
    }
    return System.nanoTime() - testBatchStart_nS;
  }

  /**
   * Used in testing
   * @return total test time in milliseconds
   */
  @SuppressWarnings("unused")
  private long buildFileAndSketch() {
    println("## BUILD FILE AND SKETCH:");
    final ByteBuffer byteBuf = ByteBuffer.allocate(byteBufCap_);
    u_ = 1;
    fileBytes_ = 0;
    final long testStartTime_mS = System.currentTimeMillis();
    try (SeekableByteChannel sbc = Files.newByteChannel(path, C, W, TE)) {
      if (tSketch_ != null) {
        final long v = nextValue();
        tSketch_.update(v);

        //build file
        final String s = Long.toHexString(v);
        if (byteBuf.remaining() < 25) {
          byteBuf.flip();
          fileBytes_ += sbc.write(byteBuf);
          byteBuf.clear();
        }
        byteBuf.put(s.getBytes(UTF_8)).put(LS_BYTE);
      }
      else { //HLL Sketch
        final long v = nextValue();
        hllSketch_.update(v);

        //build file
        final String s = Long.toHexString(v);
        if (byteBuf.remaining() < 25) {
          byteBuf.flip();
          fileBytes_ += sbc.write(byteBuf);
          byteBuf.clear();
        }
        byteBuf.put(s.getBytes(UTF_8)).put(LS_BYTE);
      }

      if (byteBuf.position() > 0) {
        byteBuf.flip();
        fileBytes_ += sbc.write(byteBuf);
        byteBuf.clear();
      }
    }
    catch (final IOException e) {
      e.printStackTrace();
    }
    final long testTime_mS = System.currentTimeMillis() - testStartTime_mS;

    //Print common results
    printCommon(testTime_mS, n_, u_);
    //Print file results
    println("File Size Bytes: " + String.format("%,d", fileBytes_));

    //Print sketch results
    printSketchResults(u_, maxMemSkBytes_, rse2_);
    return testTime_mS;
  }

  /**
   * @return next hashed long value with (1.0 - uniqueFrac_) as duplicates
   */
  private long nextValue() {
    final long out;
    if ( (rand.nextDouble() < uniquesFrac_) || (u_ <= 1)) {
      out = u_++; //unique
    } else {
      out = nextLong(0, u_); //duplicate
    }
    //return out;
    vArr_[0] = out;
    return hash(vArr_, 0L)[0];
  }

  private final void configureThetaSketch() {
    final int k = 1 << lgK_; //14
    hllSketch_ = null;
    maxMemSkBytes_ = k * 16; //includes full hash table
    rse2_ = 2.0 / sqrt(k);    //Error for 95% confidence
    tSketch_ = Sketches.updateSketchBuilder()
        .setResizeFactor(ResizeFactor.X1)
        .setFamily(Family.ALPHA).build(k );
  }

  private final void configureHLLSketch() {
    final int k = 1 << lgK_; //14
    final boolean compressed = true;
    final boolean hipEstimator = true;
    final boolean denseMode = true;
    tSketch_ = null;
    maxMemSkBytes_ = (compressed) ? k / 2 : k;
    rse2_ = 2.0 * ((hipEstimator) ? 0.836 / sqrt(k) : 1.04 / sqrt(k)); //for 95% confidence
    hllSketch_ = HllSketch.builder().setLogBuckets(lgK_)
        .setHipEstimator(hipEstimator)
        .setDenseMode(denseMode)
        .setCompressedDense(compressed)
        .build();
  }

  private static void printCommon(final long testTimeMilli, final long n, final long u) {
    println(getMinSecFromMilli(testTimeMilli));
    println("Total Values: " + String.format("%,d",n));
    final int nSecRate = (int) (testTimeMilli * 1000000.0 / n);
    println("Build Rate: " + String.format("%d nSec/Value", nSecRate));
    println("Exact Uniques: " + String.format("%,d", u));
  }

  private void printSketchResults(final long u, final int maxMemSkBytes, final double rse2) {
    println("## SKETCH STATS");
    final double rounded = Math.round((tSketch_ != null)
        ? tSketch_.getEstimate() : hllSketch_.getEstimate());
    println("Sketch Estimate of Uniques: " + String.format("%,d", (long)rounded));
    final double err = (u == 0) ? 0 : (rounded / u - 1.0);
    println("Sketch Actual Relative Error: " + String.format("%.3f%%", err * 100));
    println("Sketch 95%ile Error Bounds  : " + String.format("+/- %.3f%%", rse2 * 100));
    println("Max Sketch Size Bytes: " + String.format("%,d", maxMemSkBytes));
  }

}
