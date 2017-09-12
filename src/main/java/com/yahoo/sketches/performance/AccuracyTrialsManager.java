/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.performance;

import static com.yahoo.sketches.Util.milliSecToString;
import static com.yahoo.sketches.Util.pwr2LawNext;
import static com.yahoo.sketches.performance.PerformanceUtil.TAB;
import static com.yahoo.sketches.performance.PerformanceUtil.outputPMF;

/**
 * @author Lee Rhodes
 */
public class AccuracyTrialsManager {
  private AccuracyPerformance perf;
  private SketchAccuracyTrial trial;

  //Global counter that increments for every new input value.
  //This ensures that every trial is based on a different set of uniques
  private long vIn = 0;
  private Properties prop;
  private Quantiles[] qArr;

  public AccuracyTrialsManager(AccuracyPerformance perf) {
    this.perf = perf;
    prop = perf.getProperties();
    qArr = perf.getQuantilesArr();
    trial = perf.getSketchAccuracyTrial();
    trial.configure(prop, qArr);
  }

  public void doTrials() {
    int minNumT = 1 << Integer.parseInt(prop.mustGet("Trials_lgMinT"));
    int maxNumT = 1 << Integer.parseInt(prop.mustGet("Trials_lgMaxT"));
    boolean interData = Boolean.parseBoolean(prop.mustGet("Trials_interData"));
    boolean postPMFs = Boolean.parseBoolean(prop.mustGet("Trials_postPMFs"));
    int tPPO = Integer.parseInt(prop.mustGet("Trials_TPPO"));

    //This will generate a table of data up for each intermediate Trials point
    int lastNumT = 0;
    while (lastNumT < maxNumT) {
      long trialSetStartTime_mS = System.currentTimeMillis();
      int nextNumT = (lastNumT == 0) ? minNumT : pwr2LawNext(tPPO, lastNumT);
      int delta = nextNumT - lastNumT;
      for (int i = 0; i < delta; i++) {
        vIn = trial.doTrial(vIn);
      }
      lastNumT = nextNumT;
      long trialSetTime_mS = System.currentTimeMillis() - trialSetStartTime_mS;
      if (nextNumT < maxNumT) { // intermediate
        if (interData) {
          perf.println(ProcessQuantiles.getTableHeader());
          ProcessQuantiles.processCumTrials(perf, lastNumT);
        }
      } else { //done
        perf.println(ProcessQuantiles.getTableHeader());
        ProcessQuantiles.processCumTrials(perf, lastNumT);
      }
      perf.println(prop.extractKvPairs());
      perf.println("Cum Trials             : " + TAB + lastNumT);
      perf.println("Cumulative Updates     : " + TAB + vIn);
      perf.println("Incremental Trials Time: " + milliSecToString(trialSetTime_mS));
      perf.println("");
      if (postPMFs) {
        for (int i = 0; i < qArr.length; i++) {
          outputPMF(perf, qArr[i]);
        }
      }
    }
  }

}
