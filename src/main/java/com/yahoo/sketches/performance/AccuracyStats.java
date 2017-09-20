/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.performance;

import com.yahoo.sketches.quantiles.DoublesSketchBuilder;
import com.yahoo.sketches.quantiles.UpdateDoublesSketch;

/**
 * @author Lee Rhodes
 *
 */
public class AccuracyStats {
  public UpdateDoublesSketch qsk;
  public double sumEst = 0;
  public double sumRelErr = 0;
  public double sumSqErr = 0;
  public double rmsre = 0;
  public int uniques;
  public int bytes = 0;

  public AccuracyStats(int k, int uniques) {
    qsk = new DoublesSketchBuilder().setK(k).build(); //Quantiles
    this.uniques = uniques;
  }

  public void update(double est) {
    qsk.update(est);
    sumEst += est;
    sumRelErr += (est / uniques) - 1.0;
    double error = est - uniques;
    sumSqErr += error * error;
  }
}
