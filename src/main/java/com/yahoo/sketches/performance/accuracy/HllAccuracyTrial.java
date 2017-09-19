/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.performance.accuracy;

import com.yahoo.memory.WritableMemory;
import com.yahoo.sketches.hll.HllSketch;
import com.yahoo.sketches.hll.TgtHllType;

/**
 * @author Lee Rhodes
 */
public class HllAccuracyTrial implements SketchAccuracyTrial {
  private Quantiles[] qArr;
  private int qArrLen;
  private boolean getSize = false;

  //unique to sketch used in doTrial
  private boolean useComposite;
  private HllSketch sketch;

  @Override
  public void configure(Properties prop, Quantiles[] qArr) {
    this.qArr = qArr;
    qArrLen = qArr.length;

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
  public long doTrial(long vInStart) {
    long vIn = vInStart;
    sketch.reset(); //reuse the same sketch
    int lastUniques = 0;
    if (useComposite) {
      for (int i = 0; i < qArrLen; i++) {
        Quantiles q = qArr[i];
        int delta = q.uniques - lastUniques;
        for (int u = 0; u < delta; u++) {
          sketch.update(++vIn);
        }
        lastUniques += delta;
        double est = sketch.getCompositeEstimate();
        q.update(est);
        if (getSize) {
          q.updateBytes(sketch.toCompactByteArray().length);
        }
      }
    }
    else { //use HIP
      for (int i = 0; i < qArrLen; i++) {
        Quantiles q = qArr[i];
        int delta = q.uniques - lastUniques;
        for (int u = 0; u < delta; u++) {
          sketch.update(++vIn);
        }
        lastUniques += delta;
        double est = sketch.getEstimate();
        q.update(est);
        if (getSize) {
          q.updateBytes(sketch.toCompactByteArray().length);
        }
      }
    }
    return vIn;
  }

  @Override
  public Properties defaultProperties() {
    Properties p = new Properties();
    p.put("HLL_lgK", "14");
    p.put("HLL_direct", "false"); //only for Theta, HLL. See javadocs.
    p.put("HLL_tgtHllType", "HLL8");
    p.put("HLL_useComposite", "false");
    return p;
  }

}
