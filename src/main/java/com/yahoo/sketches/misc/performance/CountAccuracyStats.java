/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.misc.performance;

import com.yahoo.sketches.quantiles.DoublesSketchBuilder;
import com.yahoo.sketches.quantiles.UpdateDoublesSketch;

/**
 * Holds key metrics from a single accuracy trial
 *
 * @author Lee Rhodes
 */
public class CountAccuracyStats {
  public UpdateDoublesSketch qsk;
  public double sumEst = 0;
  public double sumRelErr = 0;
  public double sumSqErr = 0;
  public double rmsre = 0;
  public int uniques;
  public int bytes = 0;

  public CountAccuracyStats(final int k, final int uniques) {
    qsk = new DoublesSketchBuilder().setK(k).build(); //Quantiles
    this.uniques = uniques;
  }

  /**
   * Update
   *
   * @param est the value of the estimate for this trial
   * nanoSeconds.
   */
  public void update(final double est) {
    qsk.update(est);
    sumEst += est;
    sumRelErr += (est / uniques) - 1.0;
    final double error = est - uniques;
    sumSqErr += error * error;
  }
}
