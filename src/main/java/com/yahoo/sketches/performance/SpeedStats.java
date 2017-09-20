/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.performance;

/**
 * Holds key metrics from a single trial
 *
 * @author Lee Rhodes
 */
public class SpeedStats {
  public int uPerTrial;
  public double updateTimePerU_nS;

  /**
   * Update this Stats
   *
   * @param uPerTrial the number of uniques fed to the sketch in this trial
   * @param updateTime_nS the update time required for all the updates in
   * nanoSeconds.
   */
  public void update(int uPerTrial, long updateTime_nS) {
    this.uPerTrial = uPerTrial;
    updateTimePerU_nS = (double) updateTime_nS / uPerTrial;
  }
}