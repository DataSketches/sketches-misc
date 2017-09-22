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
public class ProcessAccuracyStats {

  public static void processCumTrials(PerformanceJob perf, int cumTrials) {
    AccuracyStats[] qArr = perf.getAccuracyStatsArr();

    Properties p = perf.getProperties();
    String getSizeStr = p.get("Trials_bytes");
    boolean getSize = (getSizeStr == null) ? false : Boolean.parseBoolean(getSizeStr);

    int points = qArr.length;
    StringBuilder sb = new StringBuilder();
    for (int pt = 0; pt < points; pt++) {
      AccuracyStats q = qArr[pt];
      int uniques = q.uniques;
      double meanEst = q.sumEst / cumTrials;
      double meanRelErr = q.sumRelErr / cumTrials;
      double meanSqErr = q.sumSqErr / cumTrials;
      double normMeanSqErr = meanSqErr / (1.0*uniques * uniques);
      double rmsRelErr = Math.sqrt(normMeanSqErr);
      q.rmsre = rmsRelErr;
      int bytes = q.bytes;

      //OUTPUT
      sb.setLength(0);
      sb.append(uniques).append(TAB);

      //Sketch meanEst, meanEstErr, norm RMS Err
      sb.append(meanEst).append(TAB);
      sb.append(meanRelErr).append(TAB);
      sb.append(rmsRelErr).append(TAB);

      //TRIALS
      sb.append(cumTrials).append(TAB);

      //Quantiles
      double[] quants = qArr[pt].qsk.getQuantiles(FRACTIONS);
      for (int i = 0; i < FRACT_LEN; i++) {
        sb.append((quants[i]/uniques) - 1.0).append(TAB);
      }
      if (getSize) {
        sb.append(bytes).append(TAB);
        sb.append(rmsRelErr * Math.sqrt(bytes));
      } else {
        sb.append(0);
        sb.append(0);
      }
      perf.println(sb.toString());
    }
  }

  public static String getTableHeader() {
    StringBuilder sb = new StringBuilder();
    sb.append("InU").append(TAB);        //col 1
    //Estimates
    sb.append("MeanEst").append(TAB);    //col 2
    sb.append("MeanRelErr").append(TAB); //col 3
    sb.append("RMS_RE").append(TAB);     //col 4

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
    sb.append("Max").append(TAB);
    sb.append("Bytes").append(TAB);
    sb.append("ReMerit");
    return sb.toString();
  }

}
