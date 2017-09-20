/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.performance.speed;

import static java.lang.Math.log;
import static java.lang.Math.pow;

import com.yahoo.memory.WritableMemory;
import com.yahoo.sketches.hll.HllSketch;
import com.yahoo.sketches.hll.TgtHllType;
import com.yahoo.sketches.performance.SpeedStats;
import com.yahoo.sketches.theta.Sketch;
import com.yahoo.sketches.theta.UpdateSketch;
import com.yahoo.sketches.theta.UpdateSketchBuilder;

/**
 * Manages the execution of every trial. One of these for the entire process.
 *
 * @author Lee Rhodes
 */
public class SpeedTrialManager {
  private static final double LN2 = log(2.0);
  private UpdateSketch udSketch_ = null;
  private boolean rebuild_ = false;

  private HllSketch hllSketch_ = null;
  private int lgK_;
  private boolean useComposite = false;

  // Global counter that increments for every new unique value.
  // Assures that all sketches are independent.
  private long vIn_ = 1000;
  private int lgMinTrials_;
  private int lgMaxTrials_;
  private int lgMinU_;
  private int lgMaxU_;
  private int lgBpMinU_ = lgMinU_; // The first break point default
  private int lgBpMaxU_ = lgMaxU_; // The second break point default
  private int ppo_;
  private double slope_;
  private boolean direct_;

  public UpdateSketch getUpdateSketch() {
    return udSketch_;
  }

  public HllSketch getHllSketch() {
    return hllSketch_;
  }

  /**
   * Sets the theta UpdateSketch builder used to create the theta
   * UpdateSketches.
   *
   * @param udBldr the theta UpdateSketchBuilder
   * @param direct true if direct (off heap) mode is desired. Instead of actual
   * off heap memory this will emulate that behavior by using an on-heap byte
   * array accessed by the Memory package. Performance-wise it is the same
   * except for issues of garbage collection, which is not the purpose of this
   * test.
   * @param rebuild set true if rebuild is desired
   */
  public void setUpdateSketchBuilder(UpdateSketchBuilder udBldr, boolean direct, boolean rebuild) {
    lgK_ = udBldr.getLgNominalEntries();
    int k = 1 << lgK_;
    // lgBPU_ = lgK_ + 1; //set the break point where the #trials starts to
    // decrease.
    WritableMemory mem = null;
    if (direct) {
      int bytes = Sketch.getMaxUpdateSketchBytes(k);
      byte[] memArr = new byte[bytes];
      mem = WritableMemory.wrap(memArr);
      udSketch_ = udBldr.setNominalEntries(k).build(mem);
    } else {
      udSketch_ = udBldr.setNominalEntries(k).build();
    }
    rebuild_ = rebuild;
    hllSketch_ = null;
    direct_ = direct;
  }

  /**
   * Sets the HLL parameters used to create the HLL sketches.
   *
   * @param lgK the lgK for the HllSketch
   * @param tgtHllType one of HLL_4, HLL_6, HLL_8
   * @param useComposite use the composite estimator
   * @param direct off-heap
   */
  public void setHllSketchParam(int lgK, TgtHllType tgtHllType, boolean useComposite,
      boolean direct) {
    int bytes = HllSketch.getMaxUpdatableSerializationBytes(lgK, tgtHllType);
    WritableMemory wmem = WritableMemory.allocate(bytes);

    hllSketch_ = (direct) ? new HllSketch(lgK, tgtHllType, wmem) : new HllSketch(lgK, tgtHllType);
    lgK_ = lgK;
    udSketch_ = null;
    this.useComposite = useComposite;
    direct_ = direct;
  }

  /**
   * This sets the profile for how the number of trials vary with the number of
   * uniques. The number of trials is the maximum until the number of uniques
   * exceeds k, whereby the number of trials starts to decrease in a power-law
   * fashion until the minimum number of trials is reached at the maximum number
   * of uniques to be tested.
   *
   * @param lgMinTrials The minimum number of trials in a trial set specified as
   * the exponent of 2. This will occur at the maximum uniques value.
   * @param lgMaxTrials The maximum number of trials in a trial set specified as
   * the exponent of 2.
   * @param lgMinU The starting number of uniques per trial.
   * @param lgMaxU The maximum number of uniques for this entire test specified
   * as the exponent of 2. The first trail set starts at lgMinU.
   * @param lgBpMinU The first break point along the U axis. Default lgMinU.
   * @param lgBpMaxU the second break point along the U axis Default lgMaxU.
   * @param ppo The number of Points Per Octave along the unique value number
   * line that will be used for generating trial sets. Recommended values are
   * one point per octave to 16 points per octave.
   */
  public void setTrialsProfile(int lgMinTrials, int lgMaxTrials, int lgMinU, int lgMaxU,
      int lgBpMinU, int lgBpMaxU, int ppo) {
    lgMinTrials_ = lgMinTrials;
    lgMaxTrials_ = lgMaxTrials;
    lgMinU_ = lgMinU;
    lgMaxU_ = lgMaxU;
    lgBpMinU_ = lgBpMinU;
    lgBpMaxU_ = lgBpMaxU;
    ppo_ = ppo;
    slope_ = (double) (lgMaxTrials - lgMinTrials) / (lgBpMinU_ - lgBpMaxU_);
  }

  /**
   * Create (or reset) a sketch and perform uPerTrial updates then update the
   * given Stats.
   *
   * @param stats The given Stats object
   * @param uPerTrial the number of updates for this trial.
   */
  public void doTrial(SpeedStats stats, int uPerTrial) {
    long updateTime_nS;

    if (udSketch_ != null) { // UpdateSketch
      udSketch_.reset(); // reuse the same sketch
      long startUpdateTime_nS = System.nanoTime();
      for (int u = uPerTrial; u-- > 0;) {
        udSketch_.update(vIn_++);
      }
      updateTime_nS = System.nanoTime() - startUpdateTime_nS;

    } else { // HllSketch
      hllSketch_.reset(); // reuse the same sketch
      long startUpdateTime_nS = System.nanoTime();
      for (int u = uPerTrial; u-- > 0;) {
        hllSketch_.update(vIn_++);
      }
      updateTime_nS = System.nanoTime() - startUpdateTime_nS;
    }
    stats.update(uPerTrial, updateTime_nS);
  }

  /**
   * Computes the number of trials for a given current number of uniques for a
   * trial set.
   *
   * @param curU the given current number of uniques for a trial set.
   * @return the number of trials for a given current number of uniques for a
   * trial set.
   */
  public int getNumTrials(int curU) {
    if ((lgMinTrials_ == lgMaxTrials_) || (curU <= (1 << lgBpMinU_))) {
      return 1 << lgMaxTrials_;
    }
    if (curU >= (1 << lgBpMaxU_)) {
      return 1 << lgMinTrials_;
    }
    double lgCurU = log(curU) / LN2;
    double lgTrials = (slope_ * (lgCurU - lgBpMinU_)) + lgMaxTrials_;
    return (int) pow(2.0, lgTrials);
  }

  /**
   * Return the Log-base 2 of the configured nominal entries or k
   *
   * @return the Log-base 2 of the configured nominal entries or k
   */
  public int getLgK() {
    return lgK_;
  }

  /**
   * Return the configured Points-Per-Octave.
   *
   * @return the configured Points-Per-Octave.
   */
  public int getPPO() {
    return ppo_;
  }

  /**
   * Return true if sketch rebuild is requested to bring sketch size down to k,
   * if necessary. Only relevant for QuickSelectSketch.
   *
   * @return true if sketch rebuild is requested to bring sketch size down to k,
   * if necessary.
   */
  public boolean getRebuild() {
    return rebuild_;
  }

  /**
   * Returns the maximum generating index (gi) from the log_base2 of the maximum
   * number of uniques for the entire test run.
   *
   * @return the maximum generating index (gi)
   */
  public int getMaxGenIndex() {
    return ppo_ * lgMaxU_;
  }

  /**
   * Returns the minimum generating index (gi) from the log_base2 of the minimum
   * number of uniques for the test run.
   *
   * @return the minimum generating index (gi)
   */
  public int getMinGenIndex() {
    return ppo_ * lgMinU_;
  }

  @Override
  public String toString() {
    String skStr;
    if (udSketch_ != null) {
      skStr = "Theta, Rebuild = " + rebuild_ + ", Direct: " + direct_;
    } else {
      skStr = "HLL, Use Composite Estimator = " + useComposite + ", Direct: " + direct_;
    }

    String s1 = "Trials Profile: " + "LgMinTrials: " + lgMinTrials_ + ", LgMaxTrials: "
        + lgMaxTrials_ + ", lgMinU: " + lgMinU_ + ", lgMaxU: " + lgMaxU_ + ", lgBpMinU: "
        + lgBpMinU_ + ", lgBpMaxU: " + lgBpMaxU_ + ", PPO: " + ppo_;
    String s2 = "\nSketch Profile: " + skStr;
    return s1 + s2;
  }

}