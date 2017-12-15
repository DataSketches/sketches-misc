/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.misc.performance2;

/**
 * @author Lee Rhodes
 */
@SuppressWarnings("unused")
public class QuantilesTrial implements SketchTrial {
  private AccuracyStats[] qArr;
  private int qArrLen;


  @Override
  public void configureSketch(final Properties prop) {
  }



  @Override
  public long doAccuracyTrial(final long vInStart) {

    return 0;
  }

  @Override
  public long doUpdateSpeedTrial(final SpeedStats stats, final int uPerTrial, final long vInStart) {

    return 0;
  }

  @Override
  public long doSerDeTrial(final SerDeStats stats, final int uPerTrial, final long vInStart) {

    return 0;
  }



  @Override
  public TrialsManager getTrialsManager(final Properties prop, final PerformanceJob perf) {
    // TODO Auto-generated method stub
    return null;
  }

}
