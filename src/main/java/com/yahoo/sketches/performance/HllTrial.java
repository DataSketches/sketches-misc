/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.performance;

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

  //unique to sketch used in doTrial
  private boolean useComposite;
  private HllSketch sketch;

  @Override
  public void configureSketch(Properties prop) {
    String getSizeStr = prop.get("Trials_bytes");
    getSize = (getSizeStr == null) ? false : Boolean.parseBoolean(getSizeStr);

    int lgK = Integer.parseInt(prop.mustGet("lgK"));
    boolean direct = Boolean.parseBoolean(prop.mustGet("HLL_direct"));
    useComposite = Boolean.parseBoolean(prop.mustGet("HLL_useComposite"));

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
  public void setQuantilesArray(AccuracyStats[] qArr) {
    this.qArr = qArr;
    qArrLen = qArr.length;
  }

  @Override
  public long doAccuracyTrial(long vInStart) {
    long vIn = vInStart;
    sketch.reset(); //reuse the same sketch
    int lastUniques = 0;
    if (useComposite) {
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
    }
    else { //use HIP
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
    }
    return vIn;
  }

  @Override
  public long doSpeedTrial(SpeedStats stats, int uPerTrial, long vInStart) {
    long vIn = vInStart;
    sketch.reset(); // reuse the same sketch
    long startUpdateTime_nS = System.nanoTime();
    for (int u = uPerTrial; u-- > 0;) {
      sketch.update(++vIn);
    }
    long updateTime_nS = System.nanoTime() - startUpdateTime_nS;
    stats.update(uPerTrial, updateTime_nS);
    return vIn;
  }

}
