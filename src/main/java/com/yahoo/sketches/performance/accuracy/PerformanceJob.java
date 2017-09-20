/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.performance.accuracy;

import static com.yahoo.sketches.Util.milliSecToString;
import static com.yahoo.sketches.performance.PerformanceUtil.LS;
import static com.yahoo.sketches.performance.PerformanceUtil.buildAccuracyStatsArray;
import static com.yahoo.sketches.performance.PerformanceUtil.configureFile;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;

import com.yahoo.sketches.performance.AccuracyStats;
import com.yahoo.sketches.performance.Properties;
import com.yahoo.sketches.performance.SketchTrial;

/**
 * @author Lee Rhodes
 */
public class PerformanceJob {
  private final Properties prop;
  //Accuracy
  private AccuracyStats[] qArr;
  private AccuracyTrialsManager trialsMgr;
  //Speed

  //Sketch
  private final SketchTrial trial;
  //Output to File
  private PrintWriter out = null;
  //Date-Time
  private SimpleDateFormat fileSDF;
  private SimpleDateFormat readSDF;
  private GregorianCalendar gc;
  private long startTime_mS;

  public PerformanceJob(Properties prop, SketchTrial trial) {
    this.prop = prop;
    this.trial = trial;
    configureDateFormats();
    configurePrintWriter();
    String jobType = prop.mustGet("JobType");
    if (jobType.equalsIgnoreCase("accuracy")) {
      qArr = buildAccuracyStatsArray(prop);
      trialsMgr = new AccuracyTrialsManager(this);
      startAccuracyJob();
    } else { //assume speed for now

      startSpeedJob();
    }
  }

  private void configureDateFormats() {
    String fileSdfStr = prop.mustGet("FileNameDateFormat");
    fileSDF = new SimpleDateFormat(fileSdfStr);
    String readSdfStr = prop.mustGet("ReadableDateFormat");
    readSDF = new SimpleDateFormat(readSdfStr);
    int timeZoneOffset = Integer.parseInt(prop.mustGet("TimeZoneOffset"));
    String zoneStr = prop.mustGet("TimeZone");
    SimpleTimeZone stz = new SimpleTimeZone(timeZoneOffset, zoneStr);
    fileSDF.setTimeZone(stz);
    readSDF.setTimeZone(stz);
    gc = new GregorianCalendar(stz);
    gc.setFirstDayOfWeek(java.util.Calendar.SUNDAY); //Sun = 1, Sat = 7
  }

  private void configurePrintWriter() {
    //create file name
    gc.setTimeInMillis(System.currentTimeMillis());
    String nowStr = fileSDF.format(gc.getTime());
    String outputFileName = prop.mustGet("Sketch") + nowStr + ".txt";
    prop.put("OutputFileName", outputFileName);
    out = configureFile(outputFileName);
  }

  public String getReadableDateString(long timeMillisec) {
    gc.setTimeInMillis(timeMillisec);
    return readSDF.format(gc.getTime());
  }

  public long getStartTime() {
    return startTime_mS;
  }

  /**
   * This method drives the accuracy process.
   */
  private void startAccuracyJob() {
    startTime_mS = System.currentTimeMillis();
    println(prop.extractKvPairs(LS));

    //Run the full suite of trials for this job
    flush(); //flush print buffer
    trialsMgr.doTrials();

    long testTime_mS = System.currentTimeMillis() - startTime_mS;
    println("Total Test Time        : " + milliSecToString(testTime_mS) + LS);
    if (out != null) {
      out.close();
    }
  }

  /**
   * This method drives the speed process.
   */
  private void startSpeedJob() {
    startTime_mS = System.currentTimeMillis();

  //Run the full suite of trials for this job
    flush(); //flush print buffer
    //trialsMgr.doTrials();

    long testTime_mS = System.currentTimeMillis() - startTime_mS;
    println("Total Test Time        : " + milliSecToString(testTime_mS) + LS);
    if (out != null) {
      out.close();
    }
  }

  public Properties getProperties() {
    return prop;
  }

  public AccuracyStats[] getAccuracyStatsArr() {
    return qArr;
  }

  public SketchTrial getSketchTrial() {
    return trial;
  }

  //All output passes through here
  public void println(String s) {
    System.out.println(s);
    if (out != null) {
      out.println(s);
    }
  }

  public void flush() {
    if (out != null) {
      out.flush();
    }
  }

  @Override
  protected void finalize() throws Throwable {
    try {
      if (out != null) {
        out.close(); // close open files
      }
    } finally {
      super.finalize();
    }
  }

}
