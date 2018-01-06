/*
 * Copyright 2018, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.misc.performance;

import static com.yahoo.sketches.Util.pwr2LawNext;
import static java.lang.Math.log;
import static java.lang.Math.pow;

import com.yahoo.memory.WritableMemory;
import com.yahoo.sketches.hll.HllSketch;
import com.yahoo.sketches.hll.TgtHllType;

/**
 * @author Lee Rhodes
 */
public class HllUpdateSpeedProfile implements JobProfile {
  private static final char TAB = '\t';
  private static final double LN2 = log(2.0);
  private Properties prop;
  private long vIn = 0;
  private int lgMinT;
  private int lgMaxT;
  private int lgMinBpU;
  private int lgMaxBpU;
  private double slope;
  private HllSketch sketch;

  @Override
  public void start(final Job job) {
    prop = job.getProperties();
    lgMinT = Integer.parseInt(prop.mustGet("Trials_lgMinT"));
    lgMaxT = Integer.parseInt(prop.mustGet("Trials_lgMaxT"));
    lgMinBpU = Integer.parseInt(prop.mustGet("Trials_lgMinBpU"));
    lgMaxBpU = Integer.parseInt(prop.mustGet("Trials_lgMaxBpU"));
    slope = (double) (lgMaxT - lgMinT) / (lgMinBpU - lgMaxBpU);
    configure();
    doTrials(job, this);
  }

  void configure() {

    final int lgK = Integer.parseInt(prop.mustGet("LgK"));
    final boolean direct = Boolean.parseBoolean(prop.mustGet("HLL_direct"));

    final TgtHllType tgtHllType;
    final String type = prop.mustGet("HLL_tgtHllType");
    if (type.equalsIgnoreCase("HLL4")) { tgtHllType = TgtHllType.HLL_4; }
    else if (type.equalsIgnoreCase("HLL6")) { tgtHllType = TgtHllType.HLL_6; }
    else { tgtHllType = TgtHllType.HLL_8; }

    if (direct) {
      final int bytes = HllSketch.getMaxUpdatableSerializationBytes(lgK, tgtHllType);
      final WritableMemory wmem = WritableMemory.allocate(bytes);
      sketch = new HllSketch(lgK, tgtHllType, wmem);
    } else {
      sketch = new HllSketch(lgK, tgtHllType);
    }
  }

  double doTrial(final int uPerTrial) {
    sketch.reset(); // reuse the same sketch
    final long startUpdateTime_nS = System.nanoTime();

    for (int u = uPerTrial; u-- > 0;) {
      sketch.update(++vIn);
    }
    final long updateTime_nS = System.nanoTime() - startUpdateTime_nS;
    return updateTime_nS / uPerTrial;
  }

  /**
   * Traverses all the unique axis points and performs trials(u) at each point
   * and outputs a row per unique axis point.
   *
   * @param job the given job
   * @param profile the given profile
   */
  static void doTrials(final Job job, final HllUpdateSpeedProfile profile) {
    final Properties prop = job.getProperties();
    final int maxU = 1 << Integer.parseInt(prop.mustGet("Trials_lgMaxU"));
    final int minU = 1 << Integer.parseInt(prop.mustGet("Trials_lgMinU"));
    final int uPPO = Integer.parseInt(prop.mustGet("Trials_UPPO"));
    int lastU = 0;
    final StringBuilder dataStr = new StringBuilder();
    job.println(getHeader());
    while (lastU < maxU) { //Trial for each U point on X-axis, and one row on output
      final int nextU = (lastU == 0) ? minU : pwr2LawNext(uPPO, lastU);
      lastU = nextU;
      final int trials = profile.getNumTrials(nextU);
      //Build stats arr
      final SpeedStats[] statsArr = new SpeedStats[trials];
      for (int t = 0; t < trials; t++) { //do # trials
        SpeedStats stats = statsArr[t];
        if (stats == null) {
          stats = statsArr[t] = new SpeedStats();
        }
      }

      System.gc();
      double sumUpdateTimePerU_nS = 0;
      for (int t = 0; t < trials; t++) {
        sumUpdateTimePerU_nS += profile.doTrial(nextU);
      }
      final double meanUpdateTimePerU_nS = sumUpdateTimePerU_nS / trials;
      process(meanUpdateTimePerU_nS, trials, nextU, dataStr);
      job.println(dataStr.toString());
    }
  }

  /**
   * Computes the number of trials for a given current number of uniques for a
   * trial set. This is used in speed trials and decreases the number of trials
   * as the number of uniques increase.
   *
   * @param curU the given current number of uniques for a trial set.
   * @return the number of trials for a given current number of uniques for a
   * trial set.
   */
  int getNumTrials(final int curU) {
    final int minBpU = 1 << lgMinBpU;
    final int maxBpU = 1 << lgMaxBpU;
    final int maxT = 1 << lgMaxT;
    final int minT = 1 << lgMinT;
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
   * Process the results
   *
   * @param statsArr the input Stats array
   * @param uPerTrial the number of uniques per trial for this trial set.
   * @param sb The StringBuilder object that is reused for each row of output
   */
  private static void process(final double meanUpdateTimePerU_nS, final int trials,
      final int uPerTrial, final StringBuilder sb) {
    // OUTPUT
    sb.setLength(0);
    sb.append(uPerTrial).append(TAB);
    sb.append(trials).append(TAB);
    sb.append(meanUpdateTimePerU_nS);
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
