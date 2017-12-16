/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.misc.performance2;

import static com.yahoo.sketches.Util.milliSecToString;
import static com.yahoo.sketches.Util.pwr2LawNext;
import static com.yahoo.sketches.misc.performance2.PerformanceUtil.FRACTIONS;
import static com.yahoo.sketches.misc.performance2.PerformanceUtil.FRACT_LEN;
import static com.yahoo.sketches.misc.performance2.PerformanceUtil.LS;
import static com.yahoo.sketches.misc.performance2.PerformanceUtil.TAB;
import static com.yahoo.sketches.misc.performance2.PerformanceUtil.buildAccuracyStatsArray;
import static com.yahoo.sketches.misc.performance2.PerformanceUtil.outputPMF;

/**
 * This measures count accuracy across a number of trials.
 * Several SketchTrials may leverage this same class.
 * @author Lee Rhodes
 */
public class CountAccuracyTrialsManager implements TrialsManager {
  private PerformanceJob perf;
  private SketchTrial trial;

  //Global counter that increments for every new input value.
  //This ensures that every trial is based on a different set of uniques
  private long vIn = 0;
  private Properties prop;
  private CountAccuracyStats[] qArr;

  /**
   *
   * @param perf a PerformanceJob
   */
  public CountAccuracyTrialsManager(final PerformanceJob perf) {
    this.perf = perf;
    prop = perf.getProperties();
    qArr = buildAccuracyStatsArray(prop);
    trial = perf.getSketchTrial();
    trial.configureSketch(prop);
  }

  @Override
  public void doTrials() {
    final int lgMinT = Integer.parseInt(prop.mustGet("Trials_lgMinT"));
    final int minT = 1 << lgMinT;
    final int lgMaxT = Integer.parseInt(prop.mustGet("Trials_lgMaxT"));
    final int maxT = 1 << lgMaxT;
    final boolean interData = Boolean.parseBoolean(prop.mustGet("Trials_interData"));
    final boolean postPMFs = Boolean.parseBoolean(prop.mustGet("Trials_postPMFs"));
    final int tPPO = Integer.parseInt(prop.mustGet("Trials_TPPO"));
    final int maxU = 1 << Integer.parseInt(prop.mustGet("Trials_lgMaxU"));

    //This will generate a table of data up for each intermediate Trials point
    int lastT = 0;
    while (lastT < maxT) {
      final int nextT = (lastT == 0) ? minT : pwr2LawNext(tPPO, lastT);
      final int delta = nextT - lastT;
      for (int i = 0; i < delta; i++) {
        vIn = trial.doAccuracyTrial(qArr, vIn);
      }
      lastT = nextT;
      final StringBuilder sb = new StringBuilder();
      if (nextT < maxT) { // intermediate
        if (interData) {
          perf.println(getHeader());
          process(prop, qArr, lastT, sb);
          perf.println(sb.toString());
        }
      } else { //done
        perf.println(getHeader());
        process(prop, qArr, lastT, sb);
        perf.println(sb.toString());
      }

      perf.println(prop.extractKvPairs());
      perf.println("Cum Trials             : " + lastT);
      perf.println("Cum Updates            : " + vIn);
      final long currentTime_mS = System.currentTimeMillis();
      final long cumTime_mS = currentTime_mS - perf.getStartTime();
      perf.println("Cum Trials Time        : " + milliSecToString(cumTime_mS));
      final double timePerTrial_mS = (cumTime_mS * 1.0) / lastT;
      final double avgUpdateTime_ns = (timePerTrial_mS * 1e6) / maxU;
      perf.println("Time Per Trial, mSec   : " + timePerTrial_mS);
      perf.println("Avg Update Time, nSec  : " + avgUpdateTime_ns);
      perf.println("Date Time              : "
          + perf.getReadableDateString(currentTime_mS));

      final long timeToComplete_mS = (long)(timePerTrial_mS * (maxT - lastT));
      perf.println("Est Time to Complete   : " + milliSecToString(timeToComplete_mS));
      perf.println("Est Time at Completion : "
          + perf.getReadableDateString(timeToComplete_mS + currentTime_mS));
      perf.println("");
      if (postPMFs) {
        for (int i = 0; i < qArr.length; i++) {
          outputPMF(perf, qArr[i]);
        }
      }
    }
  }

  private static void process(final Properties p, final CountAccuracyStats[] qArr, final int cumTrials,
      final StringBuilder sb) {
    final String getSizeStr = p.get("Trials_bytes");
    final boolean getSize = (getSizeStr == null) ? false : Boolean.parseBoolean(getSizeStr);

    final int points = qArr.length;
    sb.setLength(0);
    for (int pt = 0; pt < points; pt++) {
      final CountAccuracyStats q = qArr[pt];
      final int uniques = q.uniques;
      final double meanEst = q.sumEst / cumTrials;
      final double meanRelErr = q.sumRelErr / cumTrials;
      final double meanSqErr = q.sumSqErr / cumTrials;
      final double normMeanSqErr = meanSqErr / (1.0 * uniques * uniques);
      final double rmsRelErr = Math.sqrt(normMeanSqErr);
      q.rmsre = rmsRelErr;
      final int bytes = q.bytes;

      //OUTPUT
      //sb.setLength(0);
      sb.append(uniques).append(TAB);

      //Sketch meanEst, meanEstErr, norm RMS Err
      sb.append(meanEst).append(TAB);
      sb.append(meanRelErr).append(TAB);
      sb.append(rmsRelErr).append(TAB);

      //TRIALS
      sb.append(cumTrials).append(TAB);

      //Quantiles
      final double[] quants = qArr[pt].qsk.getQuantiles(FRACTIONS);
      for (int i = 0; i < FRACT_LEN; i++) {
        sb.append((quants[i] / uniques) - 1.0).append(TAB);
      }
      if (getSize) {
        sb.append(bytes).append(TAB);
        sb.append(rmsRelErr * Math.sqrt(bytes));
      } else {
        sb.append(0).append(TAB);
        sb.append(0);
      }
      sb.append(LS);
    }
  }

  private static String getHeader() {
    final StringBuilder sb = new StringBuilder();
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
