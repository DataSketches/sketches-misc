/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.performance;

import static com.yahoo.sketches.Util.pwr2LawNext;

/**
 * @author Lee Rhodes
 */
public class SerDeTrialsManager extends SpeedTrialsManager {

  public SerDeTrialsManager(PerformanceJob perf) {
    super(perf);
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
      //Build stats arr
      SerDeStats[] statsArr = new SerDeStats[trials];
      for (int t = 0; t < trials; t++) {
        SerDeStats stats = statsArr[t];
        if (stats == null) {
          statsArr[t] = new SerDeStats();
        }
      }

      System.gc();
      for (int t = 0; t < trials; t++) {
        vIn = trial.doSerDeTrial(statsArr[t], nextU, vIn); //at this # of uniques
      }
      process(statsArr, trials, nextU, dataStr);
      perf.println(dataStr.toString());
    }
  }


  private static void process(SerDeStats[] statsArr, int trials, int uPerTrial, StringBuilder dataStr) {
    double sumSerTime_nS = 0;
    double sumDeserTime_nS = 0;

    for (int t = 0; t < trials; t++) {
      sumSerTime_nS += statsArr[t].serializeTime_nS;
      sumDeserTime_nS += statsArr[t].deserializeTime_nS;
    }
    double meanSerTime_nS = sumSerTime_nS / trials;
    double meanDeserTime_ns = sumDeserTime_nS / trials;

    //OUTPUT
    dataStr.setLength(0);
    dataStr.append(uPerTrial).append(TAB);
    dataStr.append(trials).append(TAB);
    dataStr.append(meanSerTime_nS).append(TAB);
    dataStr.append(meanDeserTime_ns);
  }

  private static String getHeader() {
    StringBuilder sb = new StringBuilder();
    sb.append("InU").append(TAB);
    sb.append("Trials").append(TAB);
    sb.append("Ser_nS").append(TAB);
    sb.append("DeSer_nS");
    return sb.toString();
  }

}
