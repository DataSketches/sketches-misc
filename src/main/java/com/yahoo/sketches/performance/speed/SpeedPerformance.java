/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.performance.speed;

import static java.lang.Math.floor;
import static java.lang.Math.pow;

import com.yahoo.sketches.Family;
import com.yahoo.sketches.ResizeFactor;
import com.yahoo.sketches.hll.TgtHllType;
import com.yahoo.sketches.performance.SpeedStats;
import com.yahoo.sketches.theta.UpdateSketch;
import com.yahoo.sketches.theta.UpdateSketchBuilder;

/**
 * Used to generate data for plotting the error distribution or speed
 * performance of a sketch. The X-axis is assumed to be the number of uniques
 * fed to the sketch and varies from 1 to whatever is specified in the lgMaxU
 * parameter. "lg" is shorthand for Log_base_2, so if lgMaxU is 12 then the
 * highest number of uniques on the X-axis would be 4096. An exponential series
 * is used for the unique values per trial so that a wide range of unique values
 * (over many octaves) can be tested using a constant number of points per
 * octave. This dramatically reduces the number of plotting points required and
 * produces nice plots when plotted against a log axis.
 *
 * <p>
 * See the main() method as an example of how to configure.
 *
 * @author Lee Rhodes
 */
public class SpeedPerformance {

  /**
   * This method drives the whole process. An exponential series is used for the
   * unique counts per trial so that a wide range of unique values (over many
   * octaves) can be tested using a constant number of points per octave. This
   * dramatically reduces the number of plotting points required and produces
   * nice plots when plotted against a log axis. See the main() method as an
   * example of how to configure this.
   *
   * @param trialMgr TrialManager to be used
   */
  public static void start(SpeedTrialManager trialMgr) {
    long testStartTime_mS = System.currentTimeMillis();
    int maxGI = trialMgr.getMaxGenIndex();
    int minGI = trialMgr.getMinGenIndex();
    int ppo = trialMgr.getPPO();
    int lastU = 0;
    println(ProcessSpeedStats.getHeader());
    StringBuilder dataStr = new StringBuilder();

    // Each generating index (gi) will generate a new row of data
    // representing N trials at a specific number of unique values.
    for (int gi = minGI; gi <= maxGI; gi++) {
      int u = (int) floor(pow(2.0, (double) gi / ppo));
      if (u == lastU) {
        continue; // at the low end skips over duplicate values of u
      }
      lastU = u;
      int trials = trialMgr.getNumTrials(u);
      SpeedStats[] statsArr = processTrialSet(trialMgr, u, trials);
      ProcessSpeedStats.process(statsArr, u, dataStr);
      println(dataStr.toString());
    }
    int testTime_S = (int) ((System.currentTimeMillis() - testStartTime_mS) / 1000.0);
    int min = testTime_S / 60;
    int sec = testTime_S % 60;
    println("TestTime: " + min + ":" + sec);
  }

  /**
   * A Trial Set is a number of trials at number of uniques per trial,
   * uPerTrial. This is set up so that the number of trials may vary based on
   * the number of uniques for the trial set.
   *
   * @param trialMgr manages the sketch and updating of a stats object
   * @param uPerTrial uniques for every trial of a trial set
   * @param trials number of trials per trial set
   * @return the Stats array contains measurements for each trial of the trial
   * set
   */
  private static SpeedStats[] processTrialSet(SpeedTrialManager trialMgr, int uPerTrial,
      int trials) {
    SpeedStats[] statsArr = new SpeedStats[trials];
    System.gc();
    for (int t = 0; t < trials; t++) {
      if (statsArr[t] == null) {
        statsArr[t] = new SpeedStats();
      }
      trialMgr.doTrial(statsArr[t], uPerTrial);
    }
    return statsArr;
  }

  private static void println(String s) {
    System.out.println(s);
  }

  /**
   * This main method sets the configuration of the sketches, the TrialManager
   * profile, and runs the test.
   *
   * @param args not used.
   */
  public static void main(String[] args) {

    // Theta UpdateSketch parameters
    Family family = Family.ALPHA;
    ResizeFactor rf = ResizeFactor.X1;// See javadocs.
    boolean direct = true; // See javadocs and the setSketchProfile code
    float p = 1.0F;
    boolean rebuild = false; // set true if rebuild is desired to reduce size
                             // down to k.

    // HLL Parameters
    TgtHllType tgtHllType = TgtHllType.HLL_4;
    boolean useComposite = false;

    // Common parameters
    int lgK = 12;
    boolean udSketch = false; // set true if you want to use a theta
                              // UpdateSketch, false for HLL

    // Trials Profile Parameters
    // For speed trials use min=4, max= up to 23
    // For accuracy trials use min=max= 12 or more
    int lgMinTrials = 4;
    int lgMaxTrials = 23;
    int lgMinU = 0;
    int lgMaxU = 20;
    int lgBpMinU = lgMinU; // start the slope along the U axis
    int lgBpMaxU = lgMaxU; // stop the slope along the U axis

    int ppo = 16;

    // INITIALIZE
    SpeedTrialManager trialMgr = new SpeedTrialManager();
    trialMgr.setTrialsProfile(lgMinTrials, lgMaxTrials, lgMinU, lgMaxU, lgBpMinU, lgBpMaxU, ppo);
    UpdateSketchBuilder udBldr = null;

    if (udSketch) { // UpdateSketch Builder
      udBldr = UpdateSketch.builder().setNominalEntries(1 << lgK).setFamily(family).setP(p)
          .setResizeFactor(rf);
      trialMgr.setUpdateSketchBuilder(udBldr, direct, rebuild);
    } else { // HLL Sketch
      trialMgr.setHllSketchParam(lgK, tgtHllType, useComposite, direct);
    }

    // START THE TESTS
    SpeedPerformance.start(trialMgr);

    // PRINT SUMMARY
    if (udBldr != null) {
      println(udBldr.toString());
    }
    println(trialMgr.toString());
  }

}