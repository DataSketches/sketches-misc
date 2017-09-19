/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.performance.accuracy;

import java.io.IOException;

import com.clearspring.analytics.stream.cardinality.HyperLogLogPlus;

/**
 * @author Lee Rhodes
 */
public class HllppAccuracyTrial implements SketchAccuracyTrial {
  private Quantiles[] qArr;
  private int qArrLen;
  private boolean getSize = false;

  //unique to sketch used in doTrial
  private int p;
  private int sp;

  @Override
  public void configure(Properties prop, Quantiles[] qArr) {
    this.qArr = qArr;
    qArrLen = qArr.length;

    String getSizeStr = prop.get("Trials_bytes");
    getSize = (getSizeStr == null) ? false : Boolean.parseBoolean(getSizeStr);

    p = Integer.parseInt(prop.mustGet("lgK"));
    sp = Integer.parseInt(prop.mustGet("HLLP_sp"));
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
      if (getSize) {
        try {
          q.updateBytes(sketch.getBytes().length);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return vIn;
  }

  @Override
  public Properties defaultProperties() { //defaults
    Properties p = new Properties();
    p.put("HLLP_p", "14");
    p.put("HLLP_sp", "25"); // lgSK <= SP < 32
    return p;
  }

}
