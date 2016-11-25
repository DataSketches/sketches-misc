/*
 * Copyright 2016, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.performance;

import static java.lang.Math.log;
import static java.lang.Math.pow;

import com.yahoo.memory.Memory;
import com.yahoo.memory.NativeMemory;
import com.yahoo.sketches.hll.HllSketch;
import com.yahoo.sketches.hll.HllSketchBuilder;
import com.yahoo.sketches.theta.Sketch;
import com.yahoo.sketches.theta.UpdateSketch;
import com.yahoo.sketches.theta.UpdateSketchBuilder;

/**
 * Manages the execution of every trial.  One of these for the entire process.
 *
 * @author Lee Rhodes
 */
public class TrialManager {
  private static final double LN2 = log(2.0);
  private UpdateSketch udSketch_ = null;
  private HllSketchBuilder hllBuilder_ = null;
  private int lgK_;
  private double p_;
  //Global counter that increments for every new unique value.
  //Assures that all sketches are virtually independent.
  private long vIn_;
  private int lgBP_; //The break point
  private int lgMinTrials_;
  private int lgMaxTrials_;
  private int lgMaxU_;
  private int ppo_;
  private double slope_;
  private boolean rebuild_ = false;

  /**
   * Sets the theta UpdateSketch builder used to create the theta UpdateSketches.
   * @param udBldr the theta UpdateSketchBuilder
   * @param direct true if direct (off heap) mode is desired.  Instead of actual off heap memory
   * this will emulate that behavior by using an on-heap byte array accessed by the Memory package.
   * Performance-wise it is the same except for issues of garbage collection, which is not the
   * purpose of this test.
   * @param rebuild set true if rebuild is desired
   */
  public void setUpdateSketchBuilder(final UpdateSketchBuilder udBldr,  final boolean direct,
      final boolean rebuild) {
    lgK_ = udBldr.getLgNominalEntries();
    p_ = udBldr.getP();
    final int k = 1 << lgK_;
    lgBP_ = lgK_ + 1; //set the break point where the #trials starts to decrease.
    Memory mem = null;
    if (direct) {
      final int bytes = Sketch.getMaxUpdateSketchBytes(k);
      final byte[] memArr = new byte[bytes];
      mem = new NativeMemory(memArr);
      udBldr.initMemory(mem);
    }
    udSketch_ = udBldr.initMemory(mem).build(k);
    rebuild_ = rebuild;
  }

  /**
   * Sets the HLL builder used to create the HLL sketches.
   * @param hllBldr the HllSketchBuilder
   */
  public void setHllSketchBuilder(final HllSketchBuilder hllBldr) {
    lgK_ = hllBldr.getLogBuckets();
    p_ = 1.0;
    udSketch_ = null;
    hllBuilder_ = hllBldr;
  }

  /**
   * This sets the profile for how the number of trials vary with the number of uniques.
   * The number of trials is the maximum until the number of uniques exceeds k, whereby
   * the number of trials starts to decrease in a power-law fashion until the minimum
   * number of trials is reached at the maximum number of uniques to be tested.
   * @param lgMinTrials The minimum number of trials in a trial set specified as the
   * exponent of 2.  This will occur at the maximum uniques value.
   * @param lgMaxTrials The maximum number of trials in a trial set specified as the
   * exponent of 2.
   * @param lgMaxU The maximum number of uniques for this entire test specified as the
   * exponent of 2. The first trail set starts at uniques (u = 1).
   * @param ppo  The number of Points Per Octave along the unique value number line
   * that will be used for generating trial sets. Recommended values are one point per octave
   * to 16 points per octave.
   */
  public void setTrialsProfile(final int lgMinTrials, final int lgMaxTrials, final int lgMaxU,
      final int ppo) {
    lgMinTrials_ = lgMinTrials;
    lgMaxTrials_ = lgMaxTrials;
    lgMaxU_ = lgMaxU;
    ppo_ = ppo;
    slope_ = (double)(lgMaxTrials - lgMinTrials) / (lgBP_ - lgMaxU_);
  }

  /**
   * Create (or reset) a sketch and perform uPerTrial updates then update the given Stats.
   * @param stats The given Stats object
   * @param uPerTrial the number of updates for this trial.
   */
  public void doTrial(final Stats stats, final int uPerTrial) {
    if (udSketch_ != null) { //UpdateSketch
      udSketch_.reset(); //reuse the same sketch
      final long startUpdateTime_nS = System.nanoTime();
      for (int u = uPerTrial; u-- > 0; ) { udSketch_.update(vIn_++); }
      final long updateTime_nS = System.nanoTime() - startUpdateTime_nS;
      if (rebuild_) { udSketch_.rebuild(); } //Resizes down to k. Only useful with QuickSelectSketch
      stats.update(udSketch_, uPerTrial, updateTime_nS);
    }
    else { //HllSketch
      final HllSketch hllSketch = hllBuilder_.build();
      final long startUpdateTime_nS = System.nanoTime();
      for (int u = uPerTrial; u-- > 0; ) { hllSketch.update(new long[]{vIn_++}); }
      final long updateTime_nS = System.nanoTime() - startUpdateTime_nS;
      stats.update(hllSketch, uPerTrial, updateTime_nS);
    }
  }

  /**
   * Computes the number of trials for a given current number of uniques for a trial set.
   * @param curU the given current number of uniques for a trial set.
   * @return the number of trials for a given current number of uniques for a trial set.
   */
  public int getTrials(final int curU) {
    if ((lgMinTrials_ == lgMaxTrials_) || (curU <= (1 << lgBP_))) {
      return 1 << lgMaxTrials_;
    }
    final double lgCurU = log(curU) / LN2;
    final double lgTrials = slope_ * (lgCurU - lgBP_) + lgMaxTrials_;
    return (int) pow(2.0, lgTrials);
  }

  /**
   * Return the Log-base 2 of the configured nominal entries or k
   * @return the Log-base 2 of the configured nominal entries or k
   */
  public int getLgK() {
    return lgK_;
  }

  /**
   * Return the probability sampling rate, <i>p</i>.
   * @return the probability sampling rate, <i>p</i>.
   */
  public double getP() {
    return p_;
  }

  /**
   * Return the configured Points-Per-Octave.
   * @return the configured Points-Per-Octave.
   */
  public int getPPO() {
    return ppo_;
  }

  /**
   * Return true if sketch rebuild is requested to bring sketch size down to k, if necessary.
   * Only relevant for QuickSelectSketch.
   * @return true if sketch rebuild is requested to bring sketch size down to k, if necessary.
   */
  public boolean getRebuild() {
    return rebuild_;
  }

  /**
   * Returns the maximum generating index (gi) from the log_base2 of the maximum number of uniques
   * for the entire test run.
   * @return the maximum generating index (gi)
   */
  public int getMaximumGeneratingIndex() {
    return ppo_ * lgMaxU_;
  }

  @Override
  public String toString() {
    return "Trials Profile: LgMinTrials: " + lgMinTrials_ + ", LgMaxTrials: " + lgMaxTrials_
        + ", lgMaxU: " + lgMaxU_ + ", PPO: " + ppo_ + ", Rebuild: " + rebuild_;
  }

}
