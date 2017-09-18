/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.performance.accuracy;

import com.yahoo.memory.WritableMemory;
import com.yahoo.sketches.Family;
import com.yahoo.sketches.ResizeFactor;
import com.yahoo.sketches.theta.Sketch;
import com.yahoo.sketches.theta.UpdateSketch;
import com.yahoo.sketches.theta.UpdateSketchBuilder;

/**
 * @author Lee Rhodes
 */
public class ThetaAccuracyTrial implements SketchAccuracyTrial {
  private Quantiles[] qArr;
  private int qArrLen;

  //unique to sketch
  private int lgK;
  private Family family;
  private float p;
  private ResizeFactor rf;
  private boolean direct;
  private boolean rebuild;
  private UpdateSketch sketch;

  @Override
  public void configure(Properties prop, Quantiles[] qArr) {
    this.qArr = qArr;
    qArrLen = qArr.length;

    lgK = Integer.parseInt(prop.mustGet("THETA_lgK"));
    family = Family.stringToFamily(prop.mustGet("THETA_famName"));
    p = Float.parseFloat(prop.mustGet("THETA_p"));
    rf = ResizeFactor.getRF(Integer.parseInt(prop.mustGet("THETA_lgRF")));
    direct = Boolean.parseBoolean(prop.mustGet("THETA_direct"));
    rebuild = Boolean.parseBoolean(prop.mustGet("THETA_rebuild"));

    int k = 1 << lgK;
    UpdateSketchBuilder udBldr = UpdateSketch.builder()
        .setNominalEntries(k)
        .setFamily(family)
        .setP(p)
        .setResizeFactor(rf);
    if (direct) {
      int bytes = Sketch.getMaxUpdateSketchBytes(k);
      byte[] memArr = new byte[bytes];
      WritableMemory wmem = WritableMemory.wrap(memArr);
      sketch = udBldr.build(wmem);
    } else {
      sketch = udBldr.build();
    }
  }

  @Override
  public long doTrial(long vInStart) {
    long vIn = vInStart;
    sketch.reset(); //reuse the same sketch
    int lastUniques = 0;
    for (int i = 0; i < qArrLen; i++) {
      Quantiles q = qArr[i];
      int delta = q.uniques - lastUniques;
      for (int u = 0; u < delta; u++) {
        sketch.update(++vIn);
      }
      lastUniques += delta;
      if (rebuild) { sketch.rebuild(); } //Resizes down to k. Only useful with QSSketch
      q.update(sketch.getEstimate());
    }
    return vIn;
  }

  @Override
  public Properties defaultProperties() {
    Properties p = new Properties();
    p.put("THETA_lgK", "14");
    p.put("THETA_direct", "false"); //only for Theta, HLL
    p.put("THETA_famName", "alpha"); //for the builder
    p.put("THETA_lgRF", "0"); //ResizeFactor = X1
    p.put("THETA_p", "1.0");
    p.put("THETA_rebuild", "true");  //set true if rebuild is desired to reduce size down to k.
    return p;
  }


}
