/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.performance;

import org.testng.annotations.Test;

/**
 * @author Lee Rhodes
 */
public class AccuracyPerformanceRun {

  static Properties trialsProfile() {
    Properties prop = new Properties();
    //Trials Profile
    prop.put("Trials_lgQK", "13"); //size of quantiles sketch

    prop.put("Trials_lgMinT", "10"); //prints intermediate results starting w/ this #T
    prop.put("Trials_lgMaxT", "14"); //The max trials
    prop.put("Trials_TPPO", "1"); //how often intermediate results are printed
    prop.put("Trials_interData", "true");
    prop.put("Trials_postPMFs", "false");

    //Uniques
    prop.put("Trials_lgMinU", "0"); //The starting # of uniques that is printed at the end.
    prop.put("Trials_lgMaxU", "23"); //How high the # uniques go
    prop.put("Trials_UPPO", "16"); // The horizontal x-resolution of trials points
    return prop;
  }

  static Properties hllppProp() {
    Properties p = new Properties();
    p.put("hllpp_p", "19");
    p.put("hllpp_sp", "25"); // lgSK <= SP < 32
    p.put("FileName", "AccuracyDataHllpp.txt");
    return p;
  }

  static Properties hllProp() {
    Properties p = new Properties();
    p.put("hll_lgK", "14");
    p.put("hll_direct", "false"); //only for Theta, HLL. See javadocs.
    p.put("hll_tgtHllType", "HLL8");
    p.put("hll_useComposite", "false");
    p.put("FileName", "AccuracyDataHll.txt");
    return p;
  }

  static Properties thetaProp() {
    Properties p = new Properties();
    p.put("theta_lgK", "14");
    p.put("theta_direct", "false"); //only for Theta, HLL
    p.put("theta_famName", "alpha"); //for the builder
    p.put("theta_lgRF", "0"); //ResizeFactor = X1
    p.put("theta_p", "1.0");
    p.put("theta_rebuild", "true");  //set true if rebuild is desired to reduce size down to k.
    p.put("FileName", "AccuracyDataTheta.txt");
    return p;
  }

  @SuppressWarnings("unused")
  @Test
  public void runHllpp() {
    SketchAccuracyTrial trial = new HllppAccuracyTrial(); //CHOOSE THE SKETCH TRIAL
    Properties prop = trialsProfile().merge(hllppProp()); //create properties
    new AccuracyPerformance(prop, trial);
  }

  @SuppressWarnings("unused")
  @Test
  public void runHll() {
    SketchAccuracyTrial trial = new HllAccuracyTrial(); //CHOOSE THE SKETCH TRIAL
    Properties prop = trialsProfile().merge(hllProp()); //create properties
    new AccuracyPerformance(prop, trial);
  }

  @SuppressWarnings("unused")
  @Test
  public void runTheta() {
    SketchAccuracyTrial trial = new ThetaAccuracyTrial(); //CHOOSE THE SKETCH TRIAL
    Properties prop = trialsProfile().merge(thetaProp()); //create properties
    new AccuracyPerformance(prop, trial);
  }

  public static void main(String[] args) {
    AccuracyPerformanceRun perfRun = new AccuracyPerformanceRun();
    perfRun.runHllpp();
  }

}
