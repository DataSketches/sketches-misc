/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.performance;

import static com.yahoo.sketches.Util.milliSecToString;
import static com.yahoo.sketches.Util.pwr2LawNext;
import static com.yahoo.sketches.performance.PerformanceUtil.outputPMF;

/**
 * @author Lee Rhodes
 */
public class AccuracyTrialsManager implements TrialsManager {
  private PerformanceJob perf;
  private SketchTrial trial;

  //Global counter that increments for every new input value.
  //This ensures that every trial is based on a different set of uniques
  private long vIn = 0;
  private Properties prop;
  private AccuracyStats[] qArr;

  public AccuracyTrialsManager(PerformanceJob perf) {
    this.perf = perf;
    prop = perf.getProperties();
    qArr = perf.getAccuracyStatsArr();
    trial = perf.getSketchTrial();
    trial.configureSketch(prop);
    trial.setAccuracyStatsArray(qArr);
  }

  @Override
  public void doTrials() {
    int lgMinT = Integer.parseInt(prop.mustGet("Trials_lgMinT"));
    int minT = 1 << lgMinT;
    int lgMaxT = Integer.parseInt(prop.mustGet("Trials_lgMaxT"));
    int maxT = 1 << lgMaxT;
    boolean interData = Boolean.parseBoolean(prop.mustGet("Trials_interData"));
    boolean postPMFs = Boolean.parseBoolean(prop.mustGet("Trials_postPMFs"));
    int tPPO = Integer.parseInt(prop.mustGet("Trials_TPPO"));
    int maxU = 1 << Integer.parseInt(prop.mustGet("Trials_lgMaxU"));

    //This will generate a table of data up for each intermediate Trials point
    int lastT = 0;
    while (lastT < maxT) {
      int nextT = (lastT == 0) ? minT : pwr2LawNext(tPPO, lastT);
      int delta = nextT - lastT;
      for (int i = 0; i < delta; i++) {
        vIn = trial.doAccuracyTrial(vIn);
      }
      lastT = nextT;
      if (nextT < maxT) { // intermediate
        if (interData) {
          perf.println(ProcessAccuracyStats.getTableHeader());
          ProcessAccuracyStats.processCumTrials(perf, lastT);
        }
      } else { //done
        perf.println(ProcessAccuracyStats.getTableHeader());
        ProcessAccuracyStats.processCumTrials(perf, lastT);
      }

      perf.println(prop.extractKvPairs());
      perf.println("Cum Trials             : " + lastT);
      perf.println("Cum Updates            : " + vIn);
      long currentTime_mS = System.currentTimeMillis();
      long cumTime_mS = currentTime_mS - perf.getStartTime();
      perf.println("Cum Trials Time        : " + milliSecToString(cumTime_mS));
      double timePerTrial_mS = (cumTime_mS*1.0)/lastT;
      double avgUpdateTime_ns = (timePerTrial_mS*1e6) / maxU;
      perf.println("Time Per Trial, mSec   : " + timePerTrial_mS);
      perf.println("Avg Update Time, nSec  : " + avgUpdateTime_ns);
      perf.println("Date Time              : "
          + perf.getReadableDateString(currentTime_mS));

      long timeToComplete_mS = (long)(timePerTrial_mS * (maxT - lastT));
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

}
