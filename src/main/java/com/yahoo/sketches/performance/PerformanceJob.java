/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.performance;

import static com.yahoo.sketches.Util.milliSecToString;
import static com.yahoo.sketches.performance.PerformanceUtil.LS;
import static com.yahoo.sketches.performance.PerformanceUtil.configureFile;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;

/**
 * @author Lee Rhodes
 */
public class PerformanceJob {
  private final Properties prop;
  private TrialsManager trialsMgr;
  //Sketch
  private final SketchTrial trial;
  //Output to File
  private PrintWriter out = null;
  //Date-Time
  private SimpleDateFormat fileSDF;
  private SimpleDateFormat readSDF;
  private GregorianCalendar gc;
  private long startTime_mS;

  /**
   * Run the Performance Job
   * @param prop the given Properties
   * @param trial the given SketchTrial
   */
  public PerformanceJob(final Properties prop, final SketchTrial trial) {
    startTime_mS = System.currentTimeMillis();
    this.prop = prop;
    this.trial = trial;
    configureDateFormats();
    configurePrintWriter();
    final String jobType = prop.mustGet("JobType");
    println("START " + jobType + " JOB");
    println(prop.extractKvPairs(LS));
    flush(); //flush print buffer

    //### Choose the type of performance to run
    if (jobType.equalsIgnoreCase("Accuracy")) {
      trialsMgr = new AccuracyTrialsManager(this);
    }
    else if (jobType.equalsIgnoreCase("Speed")) {
      trialsMgr = new SpeedTrialsManager(this);
    }
    else if (jobType.equalsIgnoreCase("SerDe")) {
      trialsMgr = new SerDeTrialsManager(this);
    }

    trialsMgr.doTrials();

    final long testTime_mS = System.currentTimeMillis() - startTime_mS;
    println("Total Job Time        : " + milliSecToString(testTime_mS));
    println("END " + jobType + " JOB" + LS + LS);
    flush();
    if (out != null) {
      out.close();
    }
  }

  private void configureDateFormats() {
    final String fileSdfStr = prop.mustGet("FileNameDateFormat");
    fileSDF = new SimpleDateFormat(fileSdfStr);
    final String readSdfStr = prop.mustGet("ReadableDateFormat");
    readSDF = new SimpleDateFormat(readSdfStr);
    final int timeZoneOffset = Integer.parseInt(prop.mustGet("TimeZoneOffset"));
    final String zoneStr = prop.mustGet("TimeZone");
    final SimpleTimeZone stz = new SimpleTimeZone(timeZoneOffset, zoneStr);
    fileSDF.setTimeZone(stz);
    readSDF.setTimeZone(stz);
    gc = new GregorianCalendar(stz);
    gc.setFirstDayOfWeek(java.util.Calendar.SUNDAY); //Sun = 1, Sat = 7
  }

  private void configurePrintWriter() {
    //create file name
    gc.setTimeInMillis(System.currentTimeMillis());
    final String nowStr = fileSDF.format(gc.getTime());
    final String outputFileName = prop.mustGet("Sketch") + nowStr + ".txt";
    prop.put("OutputFileName", outputFileName);
    out = configureFile(outputFileName);
  }

  public String getReadableDateString(final long timeMillisec) {
    gc.setTimeInMillis(timeMillisec);
    return readSDF.format(gc.getTime());
  }

  public long getStartTime() {
    return startTime_mS;
  }

  public Properties getProperties() {
    return prop;
  }

  public SketchTrial getSketchTrial() {
    return trial;
  }

  /**
   *
   * @param s The String to print
   */
  public void println(final String s) {
    System.out.println(s);
    if (out != null) {
      out.println(s);
    }
  }

  /**
   * Flush any buffered output
   */
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
