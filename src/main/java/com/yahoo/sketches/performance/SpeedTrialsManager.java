/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.performance;

import static com.yahoo.sketches.Util.pwr2LawNext;
import static java.lang.Math.log;
import static java.lang.Math.pow;

/**
 * @author Lee Rhodes
 */
public class SpeedTrialsManager implements TrialsManager {
  private static final char TAB = '\t';
  private static final double LN2 = log(2.0);
  private PerformanceJob perf;
  private SketchTrial trial;
  private int lgMinT;
  private int minT;
  private int lgMaxT;
  private int maxT;
  private int lgMinBpU;
  private int minBpU;
  private int lgMaxBpU;
  private int maxBpU;
  private int lgMaxU;
  private int maxU;
  private int lgMinU;
  private int minU;
  private int uPPO;
  private double slope;

  //Global counter that increments for every new input value.
  //This ensures that every trial is based on a different set of uniques
  private long vIn = 0;
  private Properties prop;
  //private SpeedStats[] sArr; ??

  public SpeedTrialsManager(PerformanceJob perf) {
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
    StringBuilder dataStr = new StringBuilder();
    perf.println(getHeader());
    while (lastU < maxU) { //for each U point on X-axis, OR one row on output
      int nextU = (lastU == 0) ? minU : pwr2LawNext(uPPO, lastU);
      lastU = nextU;
      int trials = getNumTrials(nextU);
      SpeedStats[] statsArr = new SpeedStats[trials];

      for (int t = 0; t < trials; t++) { //do # trials
        SpeedStats stats = statsArr[t];
        if (stats == null) {
          stats = statsArr[t] = new SpeedStats();
        }
        vIn = trial.doSpeedTrial(stats, nextU, vIn); //at this # of uniques
      }
      process(statsArr, nextU, dataStr);
      perf.println(dataStr.toString());
    }
  }

  /**
   * Process the Stats[] array
   *
   * @param statsArr the input Stats array
   * @param uPerTrial the number of uniques per trial for this trial set.
   * @param dataStr The StringBuilder object that is reused for each row of output
   */
  public static void process(SpeedStats[] statsArr, int uPerTrial, StringBuilder dataStr) {
    int trials = statsArr.length;
    double sumUpdateTimePerU_nS = 0;

    for (int t = 0; t < trials; t++) {
      sumUpdateTimePerU_nS += statsArr[t].updateTimePerU_nS;
    }
    double meanUpdateTimePerU_nS = sumUpdateTimePerU_nS / trials;

    // OUTPUT
    dataStr.setLength(0);
    dataStr.append(uPerTrial).append(TAB);
    dataStr.append(trials).append(TAB);
    dataStr.append(meanUpdateTimePerU_nS);
  }

  /**
   * Computes the number of trials for a given current number of uniques for a
   * trial set.
   *
   * @param curU the given current number of uniques for a trial set.
   * @return the number of trials for a given current number of uniques for a
   * trial set.
   */
  public int getNumTrials(int curU) {
    if ((lgMinT == lgMaxT) || (curU <= (minBpU))) {
      return maxT;
    }
    if (curU >= maxBpU) {
      return minT;
    }
    double lgCurU = log(curU) / LN2;
    double lgTrials = (slope * (lgCurU - lgMinBpU)) + lgMaxT;
    return (int) pow(2.0, lgTrials);
  }

  /**
   * Returns a column header row
   *
   * @return a column header row
   */
  public static String getHeader() {
    StringBuilder sb = new StringBuilder();
    sb.append("InU").append(TAB);
    sb.append("Trials").append(TAB);
    sb.append("nS/u");
    return sb.toString();
  }

}
