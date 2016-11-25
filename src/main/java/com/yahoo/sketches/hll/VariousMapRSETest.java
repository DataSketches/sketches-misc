/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.hll;

import com.yahoo.sketches.ResizeFactor;
import com.yahoo.sketches.Util;
import com.yahoo.sketches.theta.Sketches;
import com.yahoo.sketches.theta.UpdateSketch;
import com.yahoo.sketches.theta.UpdateSketchBuilder;

//CHECKSTYLE.OFF: JavadocMethod
//CHECKSTYLE.OFF: WhitespaceAround
/**
 * Characterizes the RSE on the whole UniqueCountMap as the key internal maps.
 * For comparison, this can also characterize the internal HLL and Theta sketches.
 * Choose the sketch to characterize in the main method. At the top of the testRSE method
 * are the parameters for the test and additional parameters for the HllMap config.
 *
 */
public class VariousMapRSETest {
  private static final String LS = System.getProperty("line.separator");
  private static final int LG_K = 10;
  private static final int K = 1 << LG_K;
  private static final int KEY_SIZE = 4;

  enum SketchEnum { THETA, HLL, HLL_MAP, COUPON_HASH_MAP, UNIQUE_COUNT_MAP }

  public void testHllMap() {
    testRSE(SketchEnum.HLL_MAP);
    println(LS);
  }

  public void testTheta() {
    testRSE(SketchEnum.THETA);
    println(LS);
  }

  public void testHll() {
    testRSE(SketchEnum.HLL);
    println(LS);
  }

  public void testCouponHashMap() { //must limit u to 192 or less
    testRSE(SketchEnum.COUPON_HASH_MAP);
    println(LS);
  }

  public void testUniqueCountMap() {
    testRSE(SketchEnum.UNIQUE_COUNT_MAP);
    println(LS);
  }

  @SuppressWarnings("null")
  public void testRSE(final SketchEnum skEnum) {

    /*****TEST CONFIG***********************/
    final int lgUniques = 18;  //Max Log2 # of uniques per trial, except for CouponHashMap
    final int startLgTrials = 20; //for the maps this is also the # of Keys
    final int endLgTrials   = 12; //gradually decrese #trials down to this to speed up overall test time
    final int ppo = 4; //Points per Octave on the X axis, produces nice plots.
    /***************************************/

    final int points = ppo * lgUniques + 1;
    final int[] xPoints = Util.evenlyLgSpaced(0, lgUniques, points);
    final int[] tPoints = Util.evenlyLgSpaced(startLgTrials, endLgTrials, points);

    Map map = null;
    UniqueCountMap ucMap = null;
    final UpdateSketchBuilder thBldr =
        Sketches.updateSketchBuilder().setResizeFactor(ResizeFactor.X1).setNominalEntries(K);
    UpdateSketch thSketch = null;
    final HllSketchBuilder hllBldr = HllSketch.builder().setLogBuckets(LG_K).setHipEstimator(true);
    HllSketch hllSk = null;
    long v = 0;
    byte[] ipv4bytes = new byte[4];
    byte[] valBytes = new byte[8];

    if (skEnum == SketchEnum.HLL_MAP) {
      println("HllMap: k:\t" + K);
    }
    else if (skEnum == SketchEnum.THETA) {
      thSketch = thBldr.build();       //set here because we can reset it
      println("Theta Sketch: k:\t" + K);
    }
    else if (skEnum == SketchEnum.HLL){
      println("HLL Sketch: k:\t" + K);
    }
    else if (skEnum == SketchEnum.COUPON_HASH_MAP) {
      println("CouponHashMap: k:\t" + K);
    }
    else { //Unique Count Map
      println("UniqueCountMap: k:\t" + K);
    }

    println("U\tTrials\tMean\tBias\tRSE\tMemUsage");
    double sum=0, sumErr=0,sumErrSq=0;

    //at each point do multiple trials.
    final long startMs = System.currentTimeMillis(); //start the overall process timing
    long totnS = 0;
    long lastX = 0; //remove duplicate x values due to rounding early on
    long memUsage = 0;
    for (int pt = 0; pt < points ; pt++) {
      final int x = xPoints[pt];
      if (x == lastX) { continue; }
      if ((skEnum == SketchEnum.COUPON_HASH_MAP) && (x > 192)) { break; }
      lastX = x;
      sum = sumErr = sumErrSq = 0;
      final int trials = tPoints[pt];
      int ipv4 = 10 << 24; //10.0.0.0

      if (skEnum == SketchEnum.HLL_MAP) {
        map = HllMap.getInstance(KEY_SIZE, K); //renew per trial set
      }

      else if (skEnum == SketchEnum.COUPON_HASH_MAP) {
        map = CouponHashMap.getInstance(KEY_SIZE, 256); //renew per trial set
      }

      else if (skEnum == SketchEnum.UNIQUE_COUNT_MAP) {
        ucMap = new UniqueCountMap(KEY_SIZE); //renew per trial set
      }
      //else do nothing to the other sketches

      for (int t = 0; t < trials; t++) { //each trial
        double est = 0;
        long startnS = 0, endnS = 0;

        if (skEnum == SketchEnum.HLL_MAP) {
          ipv4++;  //different IP for each trial
          final HllMap hllMap = (HllMap) map;
          ipv4bytes = Util.intToBytes(ipv4, ipv4bytes);
          startnS = System.nanoTime();
          final int index = hllMap.findOrInsertKey(ipv4bytes);
          for (long i=0; i< x; i++) { //x is the #uniques per trial
            v++;  //next unique
            valBytes = Util.longToBytes(v, valBytes);
            final int coupon = Map.coupon16(valBytes);
            est = hllMap.update(index, (short)coupon);
          }
          endnS = System.nanoTime();
          memUsage = hllMap.getMemoryUsageBytes();
        }

        else if (skEnum == SketchEnum.COUPON_HASH_MAP) {
          ipv4++;  //different IP for each trial
          ipv4bytes = Util.intToBytes(ipv4, ipv4bytes);
          startnS = System.nanoTime();
          final int index = map.findOrInsertKey(ipv4bytes);
          for (long i=0; i< x; i++) { //x is the #uniques per trial
            v++;  //next unique
            valBytes = Util.longToBytes(v, valBytes);
            final int coupon = Map.coupon16(valBytes);
            est = map.update(index, (short)coupon);
          }
          endnS = System.nanoTime();
          memUsage = map.getMemoryUsageBytes();
        }

        else if (skEnum == SketchEnum.UNIQUE_COUNT_MAP) {
          ipv4++;  //different IP for each trial
          ipv4bytes = Util.intToBytes(ipv4, ipv4bytes);
          startnS = System.nanoTime();
          for (long i=0; i< x; i++) { //x is the #uniques per trial
            v++;  //next unique
            valBytes = Util.longToBytes(v, valBytes);
            est = ucMap.update(ipv4bytes, valBytes);
          }
          endnS = System.nanoTime();
          memUsage = ucMap.getMemoryUsageBytes();
        }

        else if (skEnum == SketchEnum.THETA) {
          thSketch.reset();
          startnS = System.nanoTime();
          for (long i=0; i< x; i++) { //x is the #uniques per trial
            v++;  //next unique
            thSketch.update(v);
          }
          endnS = System.nanoTime();
          memUsage = thSketch.getCurrentBytes(false);
        }

        else { // if (skEnum == SketchEnum.HLL) {
          hllSk = hllBldr.build(); //no reset on HLL !
          startnS = System.nanoTime();
          for (long i=0; i< x; i++) { //x is the #uniques per trial
            v++;  //next unique
            hllSk.update(v);
          }
          endnS = System.nanoTime();
          memUsage = K + 24;
        }

        totnS += endnS - startnS;

        if (skEnum == SketchEnum.THETA) {
          thSketch.rebuild();
          est = thSketch.getEstimate();
        }

        else if (skEnum == SketchEnum.HLL) {
          est = hllSk.getEstimate();
        }

        sum += est;
        final double err = est - x;
        sumErr += err;
        sumErrSq += err * err;
      }
      final double mean = sum /trials;
      final double meanErr = sumErr/trials;
      final double varErr = (sumErrSq - meanErr * sumErr/trials)/(trials -1);
      final double relErr = Math.sqrt(varErr)/x;
      final double bias = mean/x - 1.0;

      final String line = String.format("%d\t%d\t%.2f\t%.2f%%\t%.2f%%\t%,d",
          x, trials, mean, bias*100, relErr*100, memUsage);
      println(line);
    }
    println("");
    println(String.format("Updates          :\t%,d", v));
    if ((skEnum == SketchEnum.HLL_MAP) || (skEnum == SketchEnum.COUPON_HASH_MAP)) {
      println(              "Entry bytes      :\t" + map.getEntrySizeBytes());
    }

    final long endMs = System.currentTimeMillis();
    final long deltamS = endMs - startMs;
    final double updnS2 = ((double)totnS)/v;
    println(String.format(  "nS/Update        :\t%.1f", updnS2));
    println(                "Total: H:M:S.mS  :\t"+Util.milliSecToString(deltamS));
  }

  @SuppressWarnings("unused")
  private static void printIPandValue(final byte[] ip, final byte[] value) {
    println("IP:\t"+Util.bytesToString(ip, false, false, ".")
      + "\tVal:\t"+Util.bytesToString(value, false, false, "."));
  }

  public static void main(final String[] args) {
    final VariousMapRSETest test = new VariousMapRSETest();
    test.testHllMap();
    //    test.testTheta();
    //    test.testHll();
    //    test.testCouponHashMap();
    //    test.testUniqueCountMap();
  }

  static void println(final String s) { System.out.println(s); }

  static void print(final String s) { System.out.print(s); }
}
