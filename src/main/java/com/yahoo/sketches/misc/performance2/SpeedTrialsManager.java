/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.misc.performance2;

import static com.yahoo.sketches.Util.pwr2LawNext;
import static java.lang.Math.log;
import static java.lang.Math.pow;

/**
 * @author Lee Rhodes
 */
public class SpeedTrialsManager implements TrialsManager {
  static final char TAB = '\t';
  static final double LN2 = log(2.0);
  PerformanceJob perf;
  SketchTrial trial;
  int lgMinT;
  int minT;
  int lgMaxT;
  int maxT;
  int lgMinBpU;
  int minBpU;
  int lgMaxBpU;
  int maxBpU;
  int lgMaxU;
  int maxU;
  int lgMinU;
  int minU;
  int uPPO;
  double slope;

  //Global counter that increments for every new input value.
  //This ensures that every trial is based on a different set of uniques
  long vIn = 0;
  Properties prop;
  //private SpeedStats[] sArr; ??

  /**
   *
   * @param perf the given PerformanceJob
   */
  public SpeedTrialsManager(final PerformanceJob perf) {
    this.perf = perf;
    prop = perf.getProperties();
    trial = perf.getSketchTrial();
    trial.configureSketch(prop);

    lgMinT = Integer.parseInt(prop.mustGet("Trials_lgMinT"));
    minT = 1 << lgMinT;
    lgMaxT = Integer.parseInt(prop.mustGet("Trials_lgMaxT"));
    maxT = 1 << lgMaxT;
    lgMinBpU = Integer.parseInt(prop.mustGet("Trials_lgMinBpU"));
    minBpU = 1 << lgMinBpU;
    lgMaxBpU = Integer.parseInt(prop.mustGet("Trials_lgMaxBpU"));
    maxBpU = 1 << lgMaxBpU;
    lgMinU = Integer.parseInt(prop.mustGet("Trials_lgMinU"));
    minU = 1 << lgMinU;
    lgMaxU = Integer.parseInt(prop.mustGet("Trials_lgMaxU"));
    maxU = 1 << lgMaxU;
    uPPO = Integer.parseInt(prop.mustGet("Trials_UPPO"));
    slope = (double) (lgMaxT - lgMinT) / (lgMinBpU - lgMaxBpU);
  }

  /**
   * Traverses all the unique axis points and performs trials(u) at each point
   * and outputs a row per unique axis point.
   */
  @Override
  public void doTrials() {
    int lastU = 0;
    final StringBuilder dataStr = new StringBuilder();
    perf.println(getHeader());
    while (lastU < maxU) { //Trial for each U point on X-axis, and one row on output
      final int nextU = (lastU == 0) ? minU : pwr2LawNext(uPPO, lastU);
      lastU = nextU;
      final int trials = getNumTrials(nextU);
      //Build stats arr
      final SpeedStats[] statsArr = new SpeedStats[trials];
      for (int t = 0; t < trials; t++) { //do # trials
        SpeedStats stats = statsArr[t];
        if (stats == null) {
          stats = statsArr[t] = new SpeedStats();
        }
      }

      System.gc();
      for (int t = 0; t < trials; t++) {
        vIn = trial.doUpdateSpeedTrial(statsArr[t], nextU, vIn); //at this # of uniques
      }
      process(statsArr, trials, nextU, dataStr);
      perf.println(dataStr.toString());
    }
  }

  /**
   * Process the Stats[] array
   *
   * @param statsArr the input Stats array
   * @param uPerTrial the number of uniques per trial for this trial set.
   * @param sb The StringBuilder object that is reused for each row of output
   */
  private static void process(final SpeedStats[] statsArr, final int trials, final int uPerTrial,
      final StringBuilder sb) {
    double sumUpdateTimePerU_nS = 0;

    for (int t = 0; t < trials; t++) {
      sumUpdateTimePerU_nS += (statsArr[t].updateTime_nS * 1.0) / uPerTrial;
    }
    final double meanUpdateTimePerU_nS = sumUpdateTimePerU_nS / trials;

    // OUTPUT
    sb.setLength(0);
    sb.append(uPerTrial).append(TAB);
    sb.append(trials).append(TAB);
    sb.append(meanUpdateTimePerU_nS);
  }

  /**
   * Computes the number of trials for a given current number of uniques for a
   * trial set.
   *
   * @param curU the given current number of uniques for a trial set.
   * @return the number of trials for a given current number of uniques for a
   * trial set.
   */
  int getNumTrials(final int curU) {
    if ((lgMinT == lgMaxT) || (curU <= (minBpU))) {
      return maxT;
    }
    if (curU >= maxBpU) {
      return minT;
    }
    final double lgCurU = log(curU) / LN2;
    final double lgTrials = (slope * (lgCurU - lgMinBpU)) + lgMaxT;
    return (int) pow(2.0, lgTrials);
  }

  /**
   * Returns a column header row
   *
   * @return a column header row
   */
  private static String getHeader() {
    final StringBuilder sb = new StringBuilder();
    sb.append("InU").append(TAB);
    sb.append("Trials").append(TAB);
    sb.append("nS/u");
    return sb.toString();
  }

}
