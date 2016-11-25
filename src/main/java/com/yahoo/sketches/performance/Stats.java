/*
 * Copyright 2016, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.performance;

import com.yahoo.sketches.hll.HllSketch;
import com.yahoo.sketches.theta.UpdateSketch;

/**
 * Holds key metrics from a single trial
 *
 * @author Lee Rhodes
 */
public class Stats implements Comparable<Stats> {
  double estimate; //The estimate from the sketch
  double re = 0;   //Relative Error. Will sort by this
  double lb2est;   //LowerBound estimate at -2 StdDev
  double lb1est;   //LowerBound estimate at -1 StdDev
  double ub1est;   //UpperBound estimate at +1 StdDev
  double ub2est;   //UpperBound estimate at +2 StdDev
  double updateTimePerU_nS;

  /**
   * Update this Stats with a theta UpdateSketch
   * @param sketch the sketch to update with
   * @param uPerTrial the number of uniques fed to the sketch in this trial
   * @param updateTime_nS the update time requred for all the updates in nanoSeconds.
   */
  public void update(final UpdateSketch sketch, final int uPerTrial, final long updateTime_nS) {
    estimate = sketch.getEstimate();
    re = estimate / uPerTrial - 1.0;
    lb2est = sketch.getLowerBound(2);
    lb1est = sketch.getLowerBound(1);
    ub1est = sketch.getUpperBound(1);
    ub2est = sketch.getUpperBound(2);
    updateTimePerU_nS = (double)updateTime_nS / uPerTrial;
  }

  /**
   * Update this Stats with an HLL Sketch
   * @param sketch the sketch to update with
   * @param uPerTrial the number of uniques fed to the sketch in this trial
   * @param updateTime_nS the update time requred for all the updates in nanoSeconds.
   */
  public void update(final HllSketch sketch, final int uPerTrial, final long updateTime_nS) {
    estimate = sketch.getEstimate();
    re = estimate / uPerTrial - 1.0;
    lb2est = sketch.getLowerBound(2);
    lb1est = sketch.getLowerBound(1);
    ub1est = sketch.getUpperBound(1);
    ub2est = sketch.getUpperBound(2);
    updateTimePerU_nS = (double)updateTime_nS / uPerTrial;
  }

  @Override
  public int compareTo(final Stats that) {
    if (this.equals(that)) { return 0; }
    return Double.compare(this.re, that.re);
  }

  @Override
  public boolean equals(final Object that) {
    return this == that;
  }

  @Override
  public int hashCode() {
    return (int) ( estimate + re + lb2est + lb1est + ub1est + ub2est + updateTimePerU_nS );
  }
}

