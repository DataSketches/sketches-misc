/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.performance;

import static com.yahoo.sketches.Util.milliSecToString;
import static com.yahoo.sketches.performance.PerformanceUtil.LS;
import static com.yahoo.sketches.performance.PerformanceUtil.buildQuantilesArray;
import static com.yahoo.sketches.performance.PerformanceUtil.configureFile;

import java.io.PrintWriter;

/**
 * @author Lee Rhodes
 */
public class AccuracyPerformance {
  private final Properties prop;
  private final Quantiles[] qArr;
  private PrintWriter out = null;
  private final SketchAccuracyTrial trial;
  private final AccuracyTrialsManager trialsMgr;

  public AccuracyPerformance(Properties prop, SketchAccuracyTrial trial) {
    this.prop = prop;
    this.trial = trial;
    qArr = buildQuantilesArray(prop);
    trialsMgr = new AccuracyTrialsManager(this);
    String fileName = this.prop.get("FileName");
    if ((fileName != null) && (!fileName.isEmpty())) {
      out = configureFile(fileName);
    }
    start();
  }

  /**
   * This method drives the whole process.
   * See the main() method as an example of how to configure this.
   * @param prop the properties class
   */
  private void start() {
    long testStartTime_mS = System.currentTimeMillis();

    //Run the full suite of trials
    trialsMgr.doTrials();

    long testTime_mS = System.currentTimeMillis() - testStartTime_mS;
    println("Total Test Time        : " + milliSecToString(testTime_mS) + LS);
    if (out != null) {
      out.close();
    }
  }

  public Properties getProperties() {
    return prop;
  }

  public Quantiles[] getQuantilesArr() {
    return qArr;
  }

  public SketchAccuracyTrial getSketchAccuracyTrial() {
    return trial;
  }

  //All output passes through here
  public void println(String s) {
    System.out.println(s);
    if (out != null) {
      out.println(s);
    }
  }

  @Override
  protected void finalize() throws Throwable {
    try {
      if (out != null) {
        out.close(); // close open files
      }
    } finally {
      super.finalize();
    }
  }

}
