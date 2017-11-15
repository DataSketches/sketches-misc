/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.misc.performance;

/**
 *
 * @author Lee Rhodes
 */
public interface SketchTrial {

  void configureSketch(Properties prop);

  void setAccuracyStatsArray(AccuracyStats[] qArr);

  /**
   * An accuracy trial is one pass through all uniques, pausing to store the estimate into a
   * quantiles sketch at each point along the unique axis.
   *
   * @param vInStart the starting global unique value for this trial.
   * @return the updated global unique value after this trial is finished.
   */
  long doAccuracyTrial(long vInStart);

  /**
   * A speed trial is filling a sketch from empty to uPerTrial.
   * @param stats where the results are stored
   * @param uPerTrial the number of uniques to be fed to the sketch
   * @param vInStart the starting value of the uniques.
   * @return the updated global unique value after this trial is finished.
   */
  long doUpdateSpeedTrial(SpeedStats stats, int uPerTrial, long vInStart);

  /**
   * A speed trial is filling a sketch from empty to uPerTrial, getting the estimate,
   * serializing it, deserializing it, and getting the estimate and checking against the
   * original estimate.
   * @param stats where the results are stored
   * @param uPerTrial the number of uniques to be fed to the sketch
   * @param vInStart the starting value of the uniques.
   * @return the updated global unique value after this trial is finished.
   */
  long doSerDeTrial(SerDeStats stats, int uPerTrial, long vInStart);
}
