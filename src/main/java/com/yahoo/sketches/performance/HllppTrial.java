/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.performance;

import java.io.IOException;

import com.clearspring.analytics.stream.cardinality.HyperLogLogPlus;

/**
 * @author Lee Rhodes
 */
public class HllppTrial implements SketchTrial {
  private AccuracyStats[] qArr;
  private int qArrLen;
  private boolean getSize = false;

  //unique to sketch used in doTrial
  private int p;
  private int sp;

  @Override
  public void configureSketch(Properties prop) {
    String getSizeStr = prop.get("Trials_bytes");
    getSize = (getSizeStr == null) ? false : Boolean.parseBoolean(getSizeStr);

    p = Integer.parseInt(prop.mustGet("lgK"));
    sp = Integer.parseInt(prop.mustGet("HLLP_sp"));
  }

  @Override
  public void setQuantilesArray(AccuracyStats[] qArr) {
    this.qArr = qArr;
    qArrLen = qArr.length;
  }

  @Override
  public long doAccuracyTrial(long vInStart) {
    long vIn = vInStart;
    HyperLogLogPlus sketch = new HyperLogLogPlus(p, sp);
    int lastUniques = 0;
    for (int i = 0; i < qArrLen; i++) {
      AccuracyStats q = qArr[i];
      int delta = q.uniques - lastUniques;
      for (int u = 0; u < delta; u++) {
        sketch.offer(++vIn);
      }
      lastUniques += delta;
      double est = sketch.cardinality();
      q.update(est);
      if (getSize) {
        try {
          q.bytes = sketch.getBytes().length;
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return vIn;
  }

  @Override
  public long doSpeedTrial(SpeedStats stats, int uPerTrial, long vInStart) {
    long vIn = vInStart;
    HyperLogLogPlus sketch = new HyperLogLogPlus(p, sp);
    long startUpdateTime_nS = System.nanoTime();
    for (int u = uPerTrial; u-- > 0;) {
      sketch.offer(++vIn);
    }
    long updateTime_nS = System.nanoTime() - startUpdateTime_nS;
    stats.update(uPerTrial, updateTime_nS);
    return vIn;
  }

}
