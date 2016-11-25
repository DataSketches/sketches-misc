/*
 * Copyright 2016, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.performance;

import static java.lang.Math.floor;
import static java.lang.Math.pow;

import com.yahoo.sketches.Family;
import com.yahoo.sketches.ResizeFactor;
import com.yahoo.sketches.hll.HllSketch;
import com.yahoo.sketches.hll.HllSketchBuilder;
import com.yahoo.sketches.theta.UpdateSketch;
import com.yahoo.sketches.theta.UpdateSketchBuilder;

//CHECKSTYLE.OFF: JavadocMethod
//CHECKSTYLE.OFF: WhitespaceAround
/**
 * Used to generate data for plotting the error distribution or speed performance of a sketch.
 * The X-axis is assumed to be the number of uniques fed to the sketch and varies from 1 to whatever
 * is specified in the lgMaxU parameter.  "lg" is shorthand for Log_base_2, so if lgMaxU is 12 then
 * the highest number of uniques on the X-axis would be 4096. An exponential series is used for the
 * unique values per trial so that a wide range of unique values (over many octaves) can be tested
 * using a constant number of points per octave. This dramatically reduces the number of plotting
 * points required and produces nice plots when plotted against a log axis.
 *
 * <p>See the main() method as an example of how to configure.
 *
 * @author Lee Rhodes
 */
public class SketchPerformance {

  /**
   * This method drives the whole process. An exponential series is used for the unique
   * counts per trial so that a wide range of unique values (over many octaves) can be tested using
   * a constant number of points per octave. This dramatically reduces the number of plotting points
   * required and produces nice plots when plotted against a log axis. See the main() method as an
   * example of how to configure this.
   *
   * @param trialMgr TrialManager to be used
   */
  public static void start(final TrialManager trialMgr) {
    final long testStartTime_mS = System.currentTimeMillis();
    final  int lastGI = trialMgr.getMaximumGeneratingIndex();
    final int ppo = trialMgr.getPPO();
    int lastU = 0;
    println(ProcessStats.getHeader());
    final StringBuilder dataStr = new StringBuilder();

    //Each generating index (gi) will generate a new row of data
    // representing N trials at a specific number of unique values.
    for (int gi = 0; gi <= lastGI; gi++) {
      final int u = (int)floor(pow(2.0, (double)gi/ppo));
      if (u == lastU) { continue; }//at the low end skips over duplicate values of u
      lastU = u;
      final int trials = trialMgr.getTrials(u);
      final int lgK = trialMgr.getLgK();
      final double p = trialMgr.getP();
      final Stats[] statsArr = processTrialSet(trialMgr, u, trials);
      ProcessStats.process(statsArr, u, lgK, p, dataStr);
      println(dataStr.toString());
    }
    final int testTime_S = (int)((System.currentTimeMillis() - testStartTime_mS)/1000.0);
    final int min = testTime_S/60;
    final int sec = testTime_S%60;
    println("TestTime: "+min+":"+sec);
  }

  /**
   * A Trial Set is a number of trials at number of uniques per trial, uPerTrial.
   * This is set up so that the number of trials may vary based on the number of uniques for the
   * trial set.
   * @param trialMgr manages the sketch and updating of a stats object
   * @param uPerTrial uniques for every trial of a trial set
   * @param trials number of trials per trial set
   * @return the Stats array contains measurements for each trial of the trial set
   */
  private static  Stats[] processTrialSet(final TrialManager trialMgr, final int uPerTrial,
      final int trials) {
    final Stats[] statsArr = new Stats[trials];
    //System.gc();
    for (int t=0; t < trials; t++) {
      if (statsArr[t] == null) { statsArr[t] = new Stats(); }
      trialMgr.doTrial(statsArr[t], uPerTrial);
    }
    return statsArr;
  }

  private static void println(final String s) { System.out.println(s); }


  /**
   * This main method sets the configuration of the sketches, the TrialManager profile, and
   * runs the test.
   * @param args not used.
   */
  @SuppressWarnings({"null", "unused"})
  public static void main(final String[] args) {
    //Common parameters
    final int lgK = 12; //4K
    final boolean udSketch = true;  //set true if you want to use a theta UpdateSketch, false for HLL

    //Theta UpdateSketch parameters
    final Family family = Family.QUICKSELECT;
    final ResizeFactor rf = ResizeFactor.X1;// See javadocs.
    final boolean direct = false; //See javadocs and the setSketchProfile code
    final float p = 1.0F;
    final boolean rebuild = false;  //set true if rebuild is desired to reduce size down to k.

    //HLL Parameters
    final boolean hip = true;
    final boolean dense = false;

    //Trials Profile Parameters
    //  For speed trials use min=4,5, max= 13,14,15,16
    //  For accuracy trials use min=max= 10 or more
    final int lgMinTrials = 4;
    final int lgMaxTrials = 13;
    final int lgMaxU = 20;
    final int ppo = 16;

    //INITIALIZE
    final TrialManager trialMgr = new TrialManager();
    trialMgr.setTrialsProfile(lgMinTrials, lgMaxTrials, lgMaxU, ppo);
    UpdateSketchBuilder udBldr = null;
    HllSketchBuilder hllBldr = null;

    if (udSketch) { //UpdateSketch Builder
      udBldr = UpdateSketch.builder().setNominalEntries(1 << lgK).setFamily(family).setP(p)
          .setResizeFactor(rf);
      trialMgr.setUpdateSketchBuilder(udBldr, direct, rebuild);
    }
    else { //HLL Builder
      hllBldr = HllSketch.builder().setLogBuckets(lgK).setHipEstimator(hip).setDenseMode(dense);
      trialMgr.setHllSketchBuilder(hllBldr);
    }

    //START THE TESTS
    SketchPerformance.start(trialMgr);

    //PRINT SUMMARY
    if (udBldr != null) { println(udBldr.toString()); }
    if (hllBldr != null) { println(hllBldr.toString()); }
    println(trialMgr.toString());
  }

}
