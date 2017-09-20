/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.performance.speed;

import com.yahoo.sketches.performance.SpeedStats;

/**
 * Processes the statistics collected from an array of Stats objects from a
 * trial set and creates an output row
 *
 * @author Lee Rhodes
 */
public class ProcessSpeedStats {
  private static final char TAB = '\t';

  /**
   * Process the Stats[] array and place the output row into the dataStr.
   *
   * @param statsArr the input Stats array
   * @param uPerTrial the number of uniques per trial for this trial set.
   * @param dataStr The StringBuilder object that is reused for each row of output
   */
  public static void process(SpeedStats[] statsArr, int uPerTrial, StringBuilder dataStr) {
    int trials = statsArr.length;
    double sumUpdateTimePerU_nS = 0;

    for (int i = 0; i < trials; i++) {
      sumUpdateTimePerU_nS += statsArr[i].updateTimePerU_nS;
    }
    double meanUpdateTimePerU_nS = sumUpdateTimePerU_nS / trials;

    // OUTPUT
    dataStr.setLength(0);
    dataStr.append(uPerTrial).append(TAB);
    dataStr.append(trials).append(TAB);
    dataStr.append(meanUpdateTimePerU_nS);
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

  static void println(String s) {
    System.out.println(s);
  }
}
