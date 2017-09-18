/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.performance.accuracy;

import com.yahoo.sketches.quantiles.DoublesSketchBuilder;
import com.yahoo.sketches.quantiles.UpdateDoublesSketch;

/**
 * @author Lee Rhodes
 *
 */
public class Quantiles {
  UpdateDoublesSketch qsk;
  double sumEst = 0;
  double sumRelErr = 0;
  double sumAbsErrSq = 0;
  int uniques;

  public Quantiles(int k, int uniques) {
    qsk = new DoublesSketchBuilder().setK(k).build(); //Quantiles
    this.uniques = uniques;
  }

  public void update(double est) {
    qsk.update(est);
    sumEst += est;
    sumRelErr += (est / uniques) - 1.0;
    double absErr = est - uniques;
    sumAbsErrSq += absErr * absErr;
  }

  public double[] getQuantiles(double[] fractions) {
    return qsk.getQuantiles(fractions);
  }
}
