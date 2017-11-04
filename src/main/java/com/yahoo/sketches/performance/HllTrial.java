/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.performance;

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
  public void configureSketch(Properties prop) {
    String getSizeStr = prop.get("Trials_bytes");
    getSize = (getSizeStr == null) ? false : Boolean.parseBoolean(getSizeStr);

    int lgK = Integer.parseInt(prop.mustGet("LgK"));
    boolean direct = Boolean.parseBoolean(prop.mustGet("HLL_direct"));
    useComposite = Boolean.parseBoolean(prop.mustGet("HLL_useComposite"));
    compact = Boolean.parseBoolean(prop.mustGet("HLL_compact"));
    wrap = Boolean.parseBoolean(prop.mustGet("HLL_wrap"));
    String useCharArrStr = prop.get("Trials_charArr");
    useCharArr = (useCharArrStr == null) ? false: Boolean.parseBoolean(useCharArrStr);

    TgtHllType tgtHllType;
    String type = prop.mustGet("HLL_tgtHllType");
    if (type.equalsIgnoreCase("HLL4")) { tgtHllType = TgtHllType.HLL_4; }
    else if (type.equalsIgnoreCase("HLL6")) { tgtHllType = TgtHllType.HLL_6; }
    else { tgtHllType = TgtHllType.HLL_8; }

    if (direct) {
      int bytes = HllSketch.getMaxUpdatableSerializationBytes(lgK, tgtHllType);
      WritableMemory wmem = WritableMemory.allocate(bytes);
      sketch = new HllSketch(lgK, tgtHllType, wmem);
    } else {
      sketch = new HllSketch(lgK, tgtHllType);
    }
  }

  @Override
  public void setAccuracyStatsArray(AccuracyStats[] qArr) {
    this.qArr = qArr;
    qArrLen = qArr.length;
  }

  @Override
  public long doAccuracyTrial(long vInStart) {
    long vIn = vInStart;
    sketch.reset(); //reuse the same sketch
    int lastUniques = 0;
    int sw = (useCharArr ? 2 : 0) | (useComposite ? 1 : 0);
    switch(sw) {
      case 0: { //use longs; use HIP
        for (int i = 0; i < qArrLen; i++) {
          AccuracyStats q = qArr[i];
          int delta = q.uniques - lastUniques;
          for (int u = 0; u < delta; u++) {
            sketch.update(++vIn);
          }
          lastUniques += delta;
          double est = sketch.getEstimate();
          q.update(est);
          if (getSize) {
            q.bytes = sketch.toCompactByteArray().length;
          }
        }
        break;
      }
      case 1: { //use longs; use Composite
        for (int i = 0; i < qArrLen; i++) {
          AccuracyStats q = qArr[i];
          int delta = q.uniques - lastUniques;
          for (int u = 0; u < delta; u++) {
            sketch.update(++vIn);
          }
          lastUniques += delta;
          double est = sketch.getCompositeEstimate();
          q.update(est);
          if (getSize) {
            q.bytes = sketch.toCompactByteArray().length;
          }
        }
        break;
      }
      case 2: { //use char[]; use HIP
        for (int i = 0; i < qArrLen; i++) {
          AccuracyStats q = qArr[i];
          int delta = q.uniques - lastUniques;
          for (int u = 0; u < delta; u++) {
            String vstr = Long.toHexString(++vIn);
            sketch.update(vstr.toCharArray());
          }
          lastUniques += delta;
          double est = sketch.getEstimate();
          q.update(est);
          if (getSize) {
            q.bytes = sketch.toCompactByteArray().length;
          }
        }
        break;
      }
      case 3: { //use char[]; use Composite
        for (int i = 0; i < qArrLen; i++) {
          AccuracyStats q = qArr[i];
          int delta = q.uniques - lastUniques;
          for (int u = 0; u < delta; u++) {
            String vstr = Long.toHexString(++vIn);
            sketch.update(vstr.toCharArray());
          }
          lastUniques += delta;
          double est = sketch.getCompositeEstimate();
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
  public long doUpdateSpeedTrial(SpeedStats stats, int uPerTrial, long vInStart) {
    long vIn = vInStart;
    sketch.reset(); // reuse the same sketch
    long startUpdateTime_nS = System.nanoTime();
    for (int u = uPerTrial; u-- > 0;) {
      sketch.update(++vIn);
    }
    long updateTime_nS = System.nanoTime() - startUpdateTime_nS;
    stats.update(updateTime_nS);
    return vIn;
  }

  @Override
  public long doSerDeTrial(SerDeStats stats, int uPerTrial, long vInStart) {
    long vIn = vInStart;
    sketch.reset(); // reuse the same sketch

    for (int u = uPerTrial; u-- > 0;) {
      sketch.update(++vIn);
    }
    double est1 = sketch.getEstimate();

    byte[] byteArr;
    long startSerTime_nS, stopSerTime_nS;
    if (compact) {
      startSerTime_nS = System.nanoTime();
      byteArr = sketch.toCompactByteArray();
      stopSerTime_nS = System.nanoTime();
    } else {
      startSerTime_nS = System.nanoTime();
      byteArr = sketch.toUpdatableByteArray();
      stopSerTime_nS = System.nanoTime();
    }

    Memory mem = Memory.wrap(byteArr);
    HllSketch sketch2;
    long startDeserTime_nS, stopDeserTime_nS;
    if (wrap) {
      startDeserTime_nS = System.nanoTime();
      sketch2 = HllSketch.wrap(mem);
      stopDeserTime_nS = System.nanoTime();
    } else {
      startDeserTime_nS = System.nanoTime();
      sketch2 = HllSketch.heapify(mem);
      stopDeserTime_nS = System.nanoTime();
    }

    double est2 = sketch2.getEstimate();
    assert est1 == est2;

    long serTime_nS = stopSerTime_nS - startSerTime_nS;
    long deTime_nS = stopDeserTime_nS - startDeserTime_nS;
    stats.update(serTime_nS, deTime_nS);
    return vIn;
  }

}
