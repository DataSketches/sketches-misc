/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.misc.performance;

import com.yahoo.memory.Memory;
import com.yahoo.memory.WritableMemory;
import com.yahoo.sketches.hll.HllSketch;
import com.yahoo.sketches.hll.TgtHllType;

/**
 * @author Lee Rhodes
 */
public class HllTrial implements SketchTrial {
  private AccuracyStats[] qArr;
  private int qArrLen;
  private boolean getSize = false;

  //unique to sketch, used in doTrial
  private boolean compact;
  private boolean wrap;
  private boolean useComposite;
  private boolean useCharArr;
  private HllSketch sketch;

  @Override
  public void configureSketch(final Properties prop) {
    final String getSizeStr = prop.get("Trials_bytes");
    getSize = (getSizeStr == null) ? false : Boolean.parseBoolean(getSizeStr);

    final int lgK = Integer.parseInt(prop.mustGet("LgK"));
    final boolean direct = Boolean.parseBoolean(prop.mustGet("HLL_direct"));
    useComposite = Boolean.parseBoolean(prop.mustGet("HLL_useComposite"));
    compact = Boolean.parseBoolean(prop.mustGet("HLL_compact"));
    wrap = Boolean.parseBoolean(prop.mustGet("HLL_wrap"));
    final String useCharArrStr = prop.get("Trials_charArr");
    useCharArr = (useCharArrStr == null) ? false : Boolean.parseBoolean(useCharArrStr);

    final TgtHllType tgtHllType;
    final String type = prop.mustGet("HLL_tgtHllType");
    if (type.equalsIgnoreCase("HLL4")) { tgtHllType = TgtHllType.HLL_4; }
    else if (type.equalsIgnoreCase("HLL6")) { tgtHllType = TgtHllType.HLL_6; }
    else { tgtHllType = TgtHllType.HLL_8; }

    if (direct) {
      final int bytes = HllSketch.getMaxUpdatableSerializationBytes(lgK, tgtHllType);
      final WritableMemory wmem = WritableMemory.allocate(bytes);
      sketch = new HllSketch(lgK, tgtHllType, wmem);
    } else {
      sketch = new HllSketch(lgK, tgtHllType);
    }
  }

  @Override
  public void setAccuracyStatsArray(final AccuracyStats[] qArr) {
    this.qArr = qArr;
    qArrLen = qArr.length;
  }

  @Override
  public long doAccuracyTrial(final long vInStart) {
    long vIn = vInStart;
    sketch.reset(); //reuse the same sketch
    int lastUniques = 0;
    final int sw = (useCharArr ? 2 : 0) | (useComposite ? 1 : 0);
    switch (sw) {
      case 0: { //use longs; use HIP
        for (int i = 0; i < qArrLen; i++) {
          final AccuracyStats q = qArr[i];
          final int delta = q.uniques - lastUniques;
          for (int u = 0; u < delta; u++) {
            sketch.update(++vIn);
          }
          lastUniques += delta;
          final double est = sketch.getEstimate();
          q.update(est);
          if (getSize) {
            q.bytes = sketch.toCompactByteArray().length;
          }
        }
        break;
      }
      case 1: { //use longs; use Composite
        for (int i = 0; i < qArrLen; i++) {
          final AccuracyStats q = qArr[i];
          final int delta = q.uniques - lastUniques;
          for (int u = 0; u < delta; u++) {
            sketch.update(++vIn);
          }
          lastUniques += delta;
          final double est = sketch.getCompositeEstimate();
          q.update(est);
          if (getSize) {
            q.bytes = sketch.toCompactByteArray().length;
          }
        }
        break;
      }
      case 2: { //use char[]; use HIP
        for (int i = 0; i < qArrLen; i++) {
          final AccuracyStats q = qArr[i];
          final int delta = q.uniques - lastUniques;
          for (int u = 0; u < delta; u++) {
            final String vstr = Long.toHexString(++vIn);
            sketch.update(vstr.toCharArray());
          }
          lastUniques += delta;
          final double est = sketch.getEstimate();
          q.update(est);
          if (getSize) {
            q.bytes = sketch.toCompactByteArray().length;
          }
        }
        break;
      }
      case 3: { //use char[]; use Composite
        for (int i = 0; i < qArrLen; i++) {
          final AccuracyStats q = qArr[i];
          final int delta = q.uniques - lastUniques;
          for (int u = 0; u < delta; u++) {
            final String vstr = Long.toHexString(++vIn);
            sketch.update(vstr.toCharArray());
          }
          lastUniques += delta;
          final double est = sketch.getCompositeEstimate();
          q.update(est);
          if (getSize) {
            q.bytes = sketch.toCompactByteArray().length;
          }
        }
        break;
      }
    }
    return vIn;
  }

  @Override
  public long doUpdateSpeedTrial(final SpeedStats stats, final int uPerTrial,
      final long vInStart) {
    long vIn = vInStart;
    sketch.reset(); // reuse the same sketch
    final long startUpdateTime_nS = System.nanoTime();
    for (int u = uPerTrial; u-- > 0;) {
      sketch.update(++vIn);
    }
    final long updateTime_nS = System.nanoTime() - startUpdateTime_nS;
    stats.update(updateTime_nS);
    return vIn;
  }

  @Override
  public long doSerDeTrial(final SerDeStats stats, final int uPerTrial, final long vInStart) {
    long vIn = vInStart;
    sketch.reset(); // reuse the same sketch

    for (int u = uPerTrial; u-- > 0;) {
      sketch.update(++vIn);
    }
    final double est1 = sketch.getEstimate();

    final byte[] byteArr;
    final long startSerTime_nS, stopSerTime_nS;
    if (compact) {
      startSerTime_nS = System.nanoTime();
      byteArr = sketch.toCompactByteArray();
      stopSerTime_nS = System.nanoTime();
    } else {
      startSerTime_nS = System.nanoTime();
      byteArr = sketch.toUpdatableByteArray();
      stopSerTime_nS = System.nanoTime();
    }

    final Memory mem = Memory.wrap(byteArr);
    final HllSketch sketch2;
    final long startDeserTime_nS, stopDeserTime_nS;
    if (wrap) {
      startDeserTime_nS = System.nanoTime();
      sketch2 = HllSketch.wrap(mem);
      stopDeserTime_nS = System.nanoTime();
    } else {
      startDeserTime_nS = System.nanoTime();
      sketch2 = HllSketch.heapify(mem);
      stopDeserTime_nS = System.nanoTime();
    }

    final double est2 = sketch2.getEstimate();
    assert est1 == est2;

    final long serTime_nS = stopSerTime_nS - startSerTime_nS;
    final long deTime_nS = stopDeserTime_nS - startDeserTime_nS;
    stats.update(serTime_nS, deTime_nS);
    return vIn;
  }

}
