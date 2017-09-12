/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.performance;

import com.clearspring.analytics.stream.cardinality.HyperLogLogPlus;

/**
 * @author Lee Rhodes
 */
public class HllppAccuracyTrial implements SketchAccuracyTrial {
  private Quantiles[] qArr;
  private int qArrLen;

  //unique to sketch used in doTrial
  private int p;
  private int sp;

  @Override
  public void configure(Properties prop, Quantiles[] qArr) {
    this.qArr = qArr;
    qArrLen = qArr.length;

    p = Integer.parseInt(prop.mustGet("hllpp_p"));
    sp = Integer.parseInt(prop.mustGet("hllpp_sp"));
  }

  @Override
  public long doTrial(long vInStart) {
    long vIn = vInStart;
    HyperLogLogPlus sketch = new HyperLogLogPlus(p, sp);
    int lastUniques = 0;
    for (int i = 0; i < qArrLen; i++) {
      Quantiles q = qArr[i];
      int delta = q.uniques - lastUniques;
      for (int u = 0; u < delta; u++) {
        sketch.offer(++vIn);
      }
      lastUniques += delta;
      double est = sketch.cardinality();
      q.update(est);
    }
    return vIn;
  }

  @Override
  public Properties defaultProperties() { //defaults
    Properties p = new Properties();
    p.put("hllpp_p", "14");
    p.put("hllpp_sp", "25"); // lgSK <= SP < 32
    return p;
  }

}
