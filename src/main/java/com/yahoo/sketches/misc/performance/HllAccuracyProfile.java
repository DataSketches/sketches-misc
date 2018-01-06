/*
 * Copyright 2018, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.misc.performance;

import static com.yahoo.sketches.Util.milliSecToString;
import static com.yahoo.sketches.Util.pwr2LawNext;
import static com.yahoo.sketches.misc.performance.PerformanceUtil.FRACTIONS;
import static com.yahoo.sketches.misc.performance.PerformanceUtil.FRACT_LEN;
import static com.yahoo.sketches.misc.performance.PerformanceUtil.LS;
import static com.yahoo.sketches.misc.performance.PerformanceUtil.buildAccuracyStatsArray;
import static com.yahoo.sketches.misc.performance.PerformanceUtil.outputPMF;

import com.yahoo.memory.WritableMemory;
import com.yahoo.sketches.hll.HllSketch;
import com.yahoo.sketches.hll.TgtHllType;

public class HllAccuracyProfile implements JobProfile {
  private static final char TAB = '\t';
  private Properties prop;
  private long vIn = 0;

  private HllSketch sketch;

  private boolean getSize = false; //accuracy
  private boolean useComposite; //accuracy, HLL
  private boolean useCharArr; //accuracy ?? or speed HLL, Theta?

  private AccuracyStats[] qArr; //accuracy

  @Override
  public void start(final Job job) {
    prop = job.getProperties();
    configure();
    doTrials(job, this);
  }

  void configure() {
    //Configure Sketch
    final String getSizeStr = prop.get("Trials_bytes");
    getSize = (getSizeStr == null) ? false : Boolean.parseBoolean(getSizeStr);

    final int lgK = Integer.parseInt(prop.mustGet("LgK"));
    final boolean direct = Boolean.parseBoolean(prop.mustGet("HLL_direct"));
    useComposite = Boolean.parseBoolean(prop.mustGet("HLL_useComposite"));
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
    //configure stats array
    qArr = buildAccuracyStatsArray(prop);
  }

  void doTrial() {
    final int qArrLen = qArr.length;
    sketch.reset(); //reuse the same sketch
    int lastUniques = 0;
    final int sw = (useCharArr ? 2 : 0) | (useComposite ? 1 : 0);
    switch (sw) {
      case 0: { //use longs; use HIP
        for (int i = 0; i < qArrLen; i++) {
          final AccuracyStats q = qArr[i];
          final double delta = q.trueValue - lastUniques;
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
          final double delta = q.trueValue - lastUniques;
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
          final double delta = q.trueValue - lastUniques;
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
          final double delta = q.trueValue - lastUniques;
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
  }

  /**
   * Manages multiple trials for measuring accuracy.
   *
   * <p>An accuracy trial is run along the count axis (X-axis) first.  The "points" along the X-axis
   * where accuracy data is collected is controled by the data loaded into the CountAccuracyStats
   * array. A single trial consists of a single sketch being updated with the Trials_lgMaxU unique
   * values, stopping at the configured x-axis points along the way where the accuracy is recorded
   * into the corresponding stats array. Each stats array retains the distribtion of
   * the accuracies measured for all the trials at that x-axis point.
   *
   * <p>Because accuracy trials take a long time, this profile will output intermediate
   * accuracy results starting after Trials_lgMinT trials and then again and trial intervals
   * determined by Trials_TPPO until Trials_lgMaxT.  This allows you to stop the testing at
   * any intermediate trials point if you feel you have sufficient trials for the accuracy you
   * need.
   *
   * @param job the given job
   * @param profile the given profile
   */
  static void doTrials(final Job job, final HllAccuracyProfile profile) {
    final Properties prop = job.getProperties();
    final AccuracyStats[] qArr = profile.qArr;
    final int lgMinT = Integer.parseInt(prop.mustGet("Trials_lgMinT"));
    final int minT = 1 << lgMinT;
    final int lgMaxT = Integer.parseInt(prop.mustGet("Trials_lgMaxT"));
    final int maxT = 1 << lgMaxT;
    final boolean interData = Boolean.parseBoolean(prop.mustGet("Trials_interData"));
    final boolean postPMFs = Boolean.parseBoolean(prop.mustGet("Trials_postPMFs"));
    final int tPPO = Integer.parseInt(prop.mustGet("Trials_TPPO"));
    final int maxU = 1 << Integer.parseInt(prop.mustGet("Trials_lgMaxU"));

    //This will generate a table of data up for each intermediate Trials point
    int lastT = 0;
    while (lastT < maxT) {
      final int nextT = (lastT == 0) ? minT : pwr2LawNext(tPPO, lastT);
      final int delta = nextT - lastT;
      for (int i = 0; i < delta; i++) {
        profile.doTrial();
      }
      lastT = nextT;
      final StringBuilder sb = new StringBuilder();
      if (nextT < maxT) { // intermediate
        if (interData) {
          job.println(getHeader());
          process(prop, qArr, lastT, sb);
          job.println(sb.toString());
        }
      } else { //done
        job.println(getHeader());
        process(prop, qArr, lastT, sb);
        job.println(sb.toString());
      }

      job.println(prop.extractKvPairs());
      job.println("Cum Trials             : " + lastT);
      job.println("Cum Updates            : " + profile.vIn);
      final long currentTime_mS = System.currentTimeMillis();
      final long cumTime_mS = currentTime_mS - job.getStartTime();
      job.println("Cum Trials Time        : " + milliSecToString(cumTime_mS));
      final double timePerTrial_mS = (cumTime_mS * 1.0) / lastT;
      final double avgUpdateTime_ns = (timePerTrial_mS * 1e6) / maxU;
      job.println("Time Per Trial, mSec   : " + timePerTrial_mS);
      job.println("Avg Update Time, nSec  : " + avgUpdateTime_ns);
      job.println("Date Time              : "
          + job.getReadableDateString(currentTime_mS));

      final long timeToComplete_mS = (long)(timePerTrial_mS * (maxT - lastT));
      job.println("Est Time to Complete   : " + milliSecToString(timeToComplete_mS));
      job.println("Est Time at Completion : "
          + job.getReadableDateString(timeToComplete_mS + currentTime_mS));
      job.println("");
      if (postPMFs) {
        for (int i = 0; i < qArr.length; i++) {
          outputPMF(job, qArr[i]);
        }
      }
    }
  }


  private static void process(final Properties prop, final AccuracyStats[] qArr,
      final int cumTrials, final StringBuilder sb) {
    final String getSizeStr = prop.get("Trials_bytes");
    final boolean getSize = (getSizeStr == null) ? false : Boolean.parseBoolean(getSizeStr);

    final int points = qArr.length;
    sb.setLength(0);
    for (int pt = 0; pt < points; pt++) {
      final AccuracyStats q = qArr[pt];
      final double uniques = q.trueValue;
      final double meanEst = q.sumEst / cumTrials;
      final double meanRelErr = q.sumRelErr / cumTrials;
      final double meanSqErr = q.sumSqErr / cumTrials;
      final double normMeanSqErr = meanSqErr / (1.0 * uniques * uniques);
      final double rmsRelErr = Math.sqrt(normMeanSqErr);
      q.rmsre = rmsRelErr;
      final int bytes = q.bytes;

      //OUTPUT
      //sb.setLength(0);
      sb.append(uniques).append(TAB);

      //Sketch meanEst, meanEstErr, norm RMS Err
      sb.append(meanEst).append(TAB);
      sb.append(meanRelErr).append(TAB);
      sb.append(rmsRelErr).append(TAB);

      //TRIALS
      sb.append(cumTrials).append(TAB);

      //Quantiles
      final double[] quants = qArr[pt].qsk.getQuantiles(FRACTIONS);
      for (int i = 0; i < FRACT_LEN; i++) {
        sb.append((quants[i] / uniques) - 1.0).append(TAB);
      }
      if (getSize) {
        sb.append(bytes).append(TAB);
        sb.append(rmsRelErr * Math.sqrt(bytes));
      } else {
        sb.append(0).append(TAB);
        sb.append(0);
      }
      sb.append(LS);
    }
  }

  private static String getHeader() {
    final StringBuilder sb = new StringBuilder();
    sb.append("InU").append(TAB);        //col 1
    //Estimates
    sb.append("MeanEst").append(TAB);    //col 2
    sb.append("MeanRelErr").append(TAB); //col 3
    sb.append("RMS_RE").append(TAB);     //col 4

    //Trials
    sb.append("Trials").append(TAB);     //col 5

    //Quantiles
    sb.append("Min").append(TAB);
    sb.append("Q(.0000317)").append(TAB);
    sb.append("Q(.00135)").append(TAB);
    sb.append("Q(.02275)").append(TAB);
    sb.append("Q(.15866)").append(TAB);
    sb.append("Q(.5)").append(TAB);
    sb.append("Q(.84134)").append(TAB);
    sb.append("Q(.97725)").append(TAB);
    sb.append("Q(.99865)").append(TAB);
    sb.append("Q(.9999683)").append(TAB);
    sb.append("Max").append(TAB);
    sb.append("Bytes").append(TAB);
    sb.append("ReMerit");
    return sb.toString();
  }


}
