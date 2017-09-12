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
public class HllAccuracyTrial implements SketchAccuracyTrial {
  private Quantiles[] qArr;
  private int qArrLen;

  //unique to sketch used in doTrial
  private boolean useComposite;
  private HllSketch sketch;

  @Override
  public void configure(Properties prop, Quantiles[] qArr) {
    this.qArr = qArr;
    qArrLen = qArr.length;

    int lgK = Integer.parseInt(prop.mustGet("hll_lgK"));
    boolean direct = Boolean.parseBoolean(prop.mustGet("hll_direct"));
    useComposite = Boolean.parseBoolean(prop.mustGet("hll_useComposite"));

    TgtHllType tgtHllType;
    String type = prop.mustGet("hll_tgtHllType");
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
      }
    }
    return vIn;
  }

  @Override
  public Properties defaultProperties() {
    Properties p = new Properties();
    p.put("hll_lgK", "14");
    p.put("hll_direct", "false"); //only for Theta, HLL. See javadocs.
    p.put("hll_tgtHllType", "HLL8");
    p.put("hll_useComposite", "false");
    return p;
  }

}
