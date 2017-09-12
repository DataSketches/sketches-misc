/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.performance;

import static com.yahoo.sketches.performance.PerformanceUtil.FRACTIONS;
import static com.yahoo.sketches.performance.PerformanceUtil.FRACT_LEN;
import static com.yahoo.sketches.performance.PerformanceUtil.TAB;

/**
 * @author Lee Rhodes
 */
public class ProcessQuantiles {

  public static void processCumTrials(AccuracyPerformance perf, int cumTrials) {
    Quantiles[] qArr = perf.getQuantilesArr();
    int points = qArr.length;
    StringBuilder sb = new StringBuilder();
    for (int p = 0; p < points; p++) {
      Quantiles q = qArr[p];
      int uniques = q.uniques;
      double meanEst = q.sumEst / cumTrials;
      double meanRelErr = q.sumRelErr / cumTrials;
      double meanAbsErrSq = q.sumAbsErrSq / cumTrials;
      double normMeanSqErr = meanAbsErrSq / (1.0*uniques * uniques);
      double nRMSerr = Math.sqrt(normMeanSqErr);

      //OUTPUT
      sb.setLength(0);
      sb.append(uniques).append(TAB);

      //Sketch meanEst, meanEstErr, norm RMS Err
      sb.append(meanEst).append(TAB);
      sb.append(meanRelErr).append(TAB);
      sb.append(nRMSerr).append(TAB);

      //TRIALS
      sb.append(cumTrials).append(TAB);

      //Quantiles
      double[] quants = qArr[p].getQuantiles(FRACTIONS);
      for (int i = 0; i < FRACT_LEN; i++) {
        sb.append((quants[i]/uniques) - 1.0).append(TAB);
      }

      perf.println(sb.toString());
    }
  }

  public static String getTableHeader() {
    StringBuilder sb = new StringBuilder();
    sb.append("InU").append(TAB);       //col 1
    //Estimates
    sb.append("MeanEst").append(TAB);    //col 2
    sb.append("MeanRelErr").append(TAB); //col 3
    sb.append("nRMSerr").append(TAB);    //col 4

    //Trials
    sb.append("Trials").append(TAB);     //col 5

    //Quantiles
    sb.append("Min").append(TAB);
    sb.append("Q(.0000317)").append(TAB);
    sb.append("Q(.00135)").append(TAB);
    sb.append("Q(.02275)").append(TAB);
    sb.append("Q(.15866)").append(TAB);
    sb.append("Q(.5)").append(TAB);
    sb.append("Q(.84134)").append(TAB);
    sb.append("Q(.97725)").append(TAB);
    sb.append("Q(.99865)").append(TAB);
    sb.append("Q(.9999683)").append(TAB);
    sb.append("Max");
    return sb.toString();
  }

}
