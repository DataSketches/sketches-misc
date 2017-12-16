/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.misc.performance2;

import com.yahoo.memory.Memory;
import com.yahoo.memory.WritableMemory;
import com.yahoo.sketches.Family;
import com.yahoo.sketches.ResizeFactor;
import com.yahoo.sketches.theta.Sketch;
import com.yahoo.sketches.theta.UpdateSketch;
import com.yahoo.sketches.theta.UpdateSketchBuilder;

/**
 * @author Lee Rhodes
 */
public class ThetaTrial implements SketchTrial {
  private int qArrLen;
  private boolean getSize = false;

  //unique to sketch
  private int lgK;
  private Family family;
  private float p;
  private ResizeFactor rf;
  private boolean direct;
  private boolean rebuild;
  private UpdateSketch sketch;

  @Override
  public void configureSketch(final Properties prop) {
    final String getSizeStr = prop.get("Trials_bytes");
    getSize = (getSizeStr == null) ? false : Boolean.parseBoolean(getSizeStr);

    lgK = Integer.parseInt(prop.mustGet("LgK"));
    family = Family.stringToFamily(prop.mustGet("THETA_famName"));
    p = Float.parseFloat(prop.mustGet("THETA_p"));
    rf = ResizeFactor.getRF(Integer.parseInt(prop.mustGet("THETA_lgRF")));
    direct = Boolean.parseBoolean(prop.mustGet("THETA_direct"));
    rebuild = Boolean.parseBoolean(prop.mustGet("THETA_rebuild"));

    final int k = 1 << lgK;
    final UpdateSketchBuilder udBldr = UpdateSketch.builder()
        .setNominalEntries(k)
        .setFamily(family)
        .setP(p)
        .setResizeFactor(rf);
    if (direct) {
      final int bytes = Sketch.getMaxUpdateSketchBytes(k);
      final byte[] memArr = new byte[bytes];
      final WritableMemory wmem = WritableMemory.wrap(memArr);
      sketch = udBldr.build(wmem);
    } else {
      sketch = udBldr.build();
    }
  }

  @Override
  public TrialsManager getTrialsManager(final Properties prop, final PerformanceJob perf) {
    final String jobType = prop.mustGet("JobType");
    TrialsManager trialsMgr = null;
    if (jobType.equalsIgnoreCase("Accuracy")) {
      trialsMgr = new CountAccuracyTrialsManager(perf);
    }
    else if (jobType.equalsIgnoreCase("Speed")) {
      trialsMgr = new UpdateSpeedTrialsManager(perf);
    }
    else if (jobType.equalsIgnoreCase("SerDe")) {
      trialsMgr = new SerDeTrialsManager(perf);
    }
    else {
      throw new IllegalArgumentException("Unknown JobType");
    }
    return trialsMgr;
  }


  @Override
  public long doAccuracyTrial(final CountAccuracyStats[] qArr, final long vInStart) {
    long vIn = vInStart;
    sketch.reset(); //reuse the same sketch
    int lastUniques = 0;
    for (int i = 0; i < qArrLen; i++) {
      final CountAccuracyStats q = qArr[i];
      final int delta = q.uniques - lastUniques;
      for (int u = 0; u < delta; u++) {
        sketch.update(++vIn);
      }
      lastUniques += delta;
      if (rebuild) { sketch.rebuild(); } //Resizes down to k. Only useful with QSSketch
      q.update(sketch.getEstimate());
      if (getSize) {
        q.bytes = sketch.compact().toByteArray().length;
      }
    }
    return vIn;
  }

  @Override
  public long doUpdateSpeedTrial(final SpeedStats stats, final int uPerTrial, final long vInStart) {
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

    final long startSerTime_nS = System.nanoTime();
    final byte[] byteArr = sketch.compact().toByteArray();

    final long startDeSerTime_nS = System.nanoTime();

    final UpdateSketch sketch2 = UpdateSketch.heapify(Memory.wrap(byteArr));
    final long endDeTime_nS = System.nanoTime();

    final double est2 = sketch2.getEstimate();
    assert est1 == est2;

    final long serTime_nS = startDeSerTime_nS - startSerTime_nS;
    final long deSerTime_nS = endDeTime_nS - startDeSerTime_nS;
    stats.update(serTime_nS, deSerTime_nS);
    return vIn;
  }

}
