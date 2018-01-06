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
import com.yahoo.sketches.Family;
import com.yahoo.sketches.ResizeFactor;
import com.yahoo.sketches.theta.Sketch;
import com.yahoo.sketches.theta.UpdateSketch;
import com.yahoo.sketches.theta.UpdateSketchBuilder;

/**
 * @author Lee Rhodes
 */
public class ThetaAccuracyProfile implements JobProfile {
  private static final char TAB = '\t';
  private Properties prop;
  private long vIn = 0;

  private UpdateSketch sketch;
  AccuracyStats[] qArr; //accuracy
  private boolean getSize = false; //accuracy
  private boolean rebuild; //Theta QS Sketch Accuracy

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
    final Family family = Family.stringToFamily(prop.mustGet("THETA_famName"));
    final float p = Float.parseFloat(prop.mustGet("THETA_p"));
    final ResizeFactor rf = ResizeFactor.getRF(Integer.parseInt(prop.mustGet("THETA_lgRF")));
    final boolean direct = Boolean.parseBoolean(prop.mustGet("THETA_direct"));
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
    //configure stats array
    qArr = buildAccuracyStatsArray(prop);
  }

  /**
   * An accuracy trial is one pass through all uniques, pausing to store the estimate into a
   * quantiles sketch at each point along the unique axis.
   */
  //@Override
  void doTrial() {
    final int qArrLen = qArr.length;
    sketch.reset(); //reuse the same sketch
    int lastUniques = 0;
    for (int i = 0; i < qArrLen; i++) {
      final AccuracyStats q = qArr[i];
      final double delta = q.trueValue - lastUniques;
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
  static void doTrials(final Job job, final ThetaAccuracyProfile profile) {
    final Properties prop = job.getProperties();
    final AccuracyStats[] qArr = profile.qArr;
    final int minT = 1 << Integer.parseInt(prop.mustGet("Trials_lgMinT"));
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
