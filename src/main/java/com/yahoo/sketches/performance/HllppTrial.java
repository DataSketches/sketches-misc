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
  public void configureSketch(final Properties prop) {
    final String getSizeStr = prop.get("Trials_bytes");
    getSize = (getSizeStr == null) ? false : Boolean.parseBoolean(getSizeStr);

    p = Integer.parseInt(prop.mustGet("LgK"));
    sp = Integer.parseInt(prop.mustGet("HLLP_sp"));
  }

  @Override
  public void setAccuracyStatsArray(final AccuracyStats[] qArr) {
    this.qArr = qArr;
    qArrLen = qArr.length;
  }

  @Override
  public long doAccuracyTrial(final long vInStart) {
    long vIn = vInStart;
    final HyperLogLogPlus sketch = new HyperLogLogPlus(p, sp);
    int lastUniques = 0;
    for (int i = 0; i < qArrLen; i++) {
      final AccuracyStats q = qArr[i];
      final int delta = q.uniques - lastUniques;
      for (int u = 0; u < delta; u++) {
        sketch.offer(++vIn);
      }
      lastUniques += delta;
      final double est = sketch.cardinality();
      q.update(est);
      if (getSize) {
        try {
          q.bytes = sketch.getBytes().length;
        } catch (final IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return vIn;
  }

  @Override
  public long doUpdateSpeedTrial(final SpeedStats stats, final int uPerTrial,
      final long vInStart) {
    long vIn = vInStart;
    final HyperLogLogPlus sketch = new HyperLogLogPlus(p, sp);
    final long startUpdateTime_nS = System.nanoTime();
    for (int u = uPerTrial; u-- > 0;) {
      sketch.offer(++vIn);
    }
    final long updateTime_nS = System.nanoTime() - startUpdateTime_nS;
    stats.update(updateTime_nS);
    return vIn;
  }

  @Override
  public long doSerDeTrial(final SerDeStats stats, final int uPerTrial, final long vInStart) {
    long vIn = vInStart;
    final HyperLogLogPlus sketch = new HyperLogLogPlus(p, sp);
    HyperLogLogPlus sketch2 = null;
    for (int u = uPerTrial; u-- > 0;) {
      sketch.offer(++vIn);
    }
    final double est1 = sketch.cardinality();

    final long startSerTime_nS = System.nanoTime();
    byte[] byteArr = null;
    long startDeTime_nS = 0;

    try {
      byteArr = sketch.getBytes();

      startDeTime_nS = System.nanoTime();

      sketch2 = HyperLogLogPlus.Builder.build(byteArr);
    } catch (final IOException e) { throw new RuntimeException(e); }

    final long endDeTime_nS = System.nanoTime();
    final double est2 = sketch2.cardinality();
    assert est1 == est2;

    final long serTime_nS = startDeTime_nS - startSerTime_nS;
    final long deTime_nS = endDeTime_nS - startDeTime_nS;
    stats.update(serTime_nS, deTime_nS);
    return vIn;
  }

}
