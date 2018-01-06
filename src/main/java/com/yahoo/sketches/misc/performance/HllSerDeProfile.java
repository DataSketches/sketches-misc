/*
 * Copyright 2018, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.misc.performance;

import static com.yahoo.sketches.Util.pwr2LawNext;
import static java.lang.Math.log;
import static java.lang.Math.pow;

import com.yahoo.memory.Memory;
import com.yahoo.memory.WritableMemory;
import com.yahoo.sketches.hll.HllSketch;
import com.yahoo.sketches.hll.TgtHllType;

/**
 * @author Lee Rhodes
 */
public class HllSerDeProfile implements JobProfile {
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

  private boolean compact; //serde
  private boolean wrap; //serde

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
    compact = Boolean.parseBoolean(prop.mustGet("HLL_compact"));
    wrap = Boolean.parseBoolean(prop.mustGet("HLL_wrap"));

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

  void doTrial(final SerDeStats stats, final int uPerTrial) {
    sketch.reset(); // reuse the same sketch

    for (int u = uPerTrial; u-- > 0;) {
      sketch.update(++vIn);
    }
    final double est1 = sketch.getEstimate();

    final byte[] byteArr;
    final long startSerTime_nS, stopSerTime_nS;
    if (compact) {
      startSerTime_nS = System.nanoTime();
      byteArr = sketch.toCompactByteArray();
      stopSerTime_nS = System.nanoTime();
    } else {
      startSerTime_nS = System.nanoTime();
      byteArr = sketch.toUpdatableByteArray();
      stopSerTime_nS = System.nanoTime();
    }

    final Memory mem = Memory.wrap(byteArr);
    final HllSketch sketch2;
    final long startDeserTime_nS, stopDeserTime_nS;
    if (wrap) {
      startDeserTime_nS = System.nanoTime();
      sketch2 = HllSketch.wrap(mem);
      stopDeserTime_nS = System.nanoTime();
    } else {
      startDeserTime_nS = System.nanoTime();
      sketch2 = HllSketch.heapify(mem);
      stopDeserTime_nS = System.nanoTime();
    }

    final double est2 = sketch2.getEstimate();
    assert est1 == est2;

    final long serTime_nS = stopSerTime_nS - startSerTime_nS;
    final long deTime_nS = stopDeserTime_nS - startDeserTime_nS;
    stats.update(serTime_nS, deTime_nS);
  }

  static void doTrials(final Job job, final HllSerDeProfile profile) {
    final Properties prop = job.getProperties();
    final int maxU = 1 << Integer.parseInt(prop.mustGet("Trials_lgMaxU"));
    final int minU = 1 << Integer.parseInt(prop.mustGet("Trials_lgMinU"));
    final int uPPO = Integer.parseInt(prop.mustGet("Trials_UPPO"));
    int lastU = 0;
    final StringBuilder dataStr = new StringBuilder();
    job.println(getHeader());
    while (lastU < maxU) { //for each U point on X-axis, OR one row on output
      final int nextU = (lastU == 0) ? minU : pwr2LawNext(uPPO, lastU);
      lastU = nextU;
      final int trials = profile.getNumTrials(nextU);
      //Build stats arr
      final SerDeStats[] statsArr = new SerDeStats[trials];
      for (int t = 0; t < trials; t++) {
        final SerDeStats stats = statsArr[t];
        if (stats == null) {
          statsArr[t] = new SerDeStats();
        }
      }

      System.gc();
      for (int t = 0; t < trials; t++) {
        profile.doTrial(statsArr[t], nextU); //at this # of uniques
      }
      process(statsArr, trials, nextU, dataStr);
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

  private static void process(final SerDeStats[] statsArr, final int trials, final int uPerTrial,
      final StringBuilder dataStr) {
    double sumSerTime_nS = 0;
    double sumDeserTime_nS = 0;

    for (int t = 0; t < trials; t++) {
      sumSerTime_nS += statsArr[t].serializeTime_nS;
      sumDeserTime_nS += statsArr[t].deserializeTime_nS;
    }
    final double meanSerTime_nS = sumSerTime_nS / trials;
    final double meanDeserTime_ns = sumDeserTime_nS / trials;

    //OUTPUT
    dataStr.setLength(0);
    dataStr.append(uPerTrial).append(TAB);
    dataStr.append(trials).append(TAB);
    dataStr.append(meanSerTime_nS).append(TAB);
    dataStr.append(meanDeserTime_ns);
  }

  private static String getHeader() {
    final StringBuilder sb = new StringBuilder();
    sb.append("InU").append(TAB);
    sb.append("Trials").append(TAB);
    sb.append("Ser_nS").append(TAB);
    sb.append("DeSer_nS");
    return sb.toString();
  }

}
