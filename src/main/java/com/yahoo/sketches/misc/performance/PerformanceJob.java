/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.misc.performance;

import static com.yahoo.sketches.Util.milliSecToString;
import static com.yahoo.sketches.misc.Files.openPrintWriter;
import static com.yahoo.sketches.misc.performance.PerformanceUtil.LS;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;

/**
 * This class runs a specific Performance Job which defined by a SketchTrial and a Properties
 * class that must already be loaded the properties key/value pairs required for this job.
 * This job will obtain a TrialsManager from the specified SketchTrial.  This TrialsManager
 * manages how the actual performance testing is to be done.
 *
 * <p>All specific configurations about the sketch, for the TrialsManager, and to configure the
 * date-time formatting is contained in the Properties class.
 *
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
  private SimpleDateFormat compactSDF;  //used in the filename
  private SimpleDateFormat expandedSDF; //for human readability
  private GregorianCalendar gc;
  private long startTime_mS;

  /**
   * Construct and Run the Performance Job
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

    trialsMgr = trial.getTrialsManager(prop, this);

    trialsMgr.doTrials();

    final long testTime_mS = System.currentTimeMillis() - startTime_mS;
    println("Total Job Time        : " + milliSecToString(testTime_mS));
    println("END " + jobType + " JOB" + LS + LS);
    flush();
    if (out != null) {
      out.close();
    }
  }

  /**
   * Factory method for constructing this class
   * @param prop the given Properties
   * @param trial the given SketchTrial
   * @return a new PerformanceJob
   */
  public static PerformanceJob runPerformanceJob(final Properties prop, final SketchTrial trial) {
    return new PerformanceJob(prop, trial);
  }

  /**
   * Called from constructor to configure Date Formats
   */
  private void configureDateFormats() {
    final String fileSdfStr = prop.mustGet("FileNameDateFormat");
    compactSDF = new SimpleDateFormat(fileSdfStr);
    final String readSdfStr = prop.mustGet("ReadableDateFormat");
    expandedSDF = new SimpleDateFormat(readSdfStr);
    final int timeZoneOffset = Integer.parseInt(prop.mustGet("TimeZoneOffset"));
    final String zoneStr = prop.mustGet("TimeZone");
    final SimpleTimeZone stz = new SimpleTimeZone(timeZoneOffset, zoneStr);
    compactSDF.setTimeZone(stz);
    expandedSDF.setTimeZone(stz);
    gc = new GregorianCalendar(stz);
    gc.setFirstDayOfWeek(java.util.Calendar.SUNDAY); //Sun = 1, Sat = 7
  }

  /**
   * Called from constructor to configure the Print Writer
   */
  private void configurePrintWriter() {
    //create file name
    gc.setTimeInMillis(System.currentTimeMillis());
    final String nowStr = compactSDF.format(gc.getTime());
    final String outputFileName = prop.mustGet("SketchType") + nowStr + ".txt";
    prop.put("OutputFileName", outputFileName);
    out = openPrintWriter(outputFileName);
  }

  /**
   * Gets a human-readable date string given the time in milliseconds.
   * @param timeMillisec the given time generated from System.currentTimeMillis().
   * @return the date string
   */
  public String getReadableDateString(final long timeMillisec) {
    gc.setTimeInMillis(timeMillisec);
    return expandedSDF.format(gc.getTime());
  }

  /**
   * Gets the start time in milliseconds
   * @return the start time in milliseconds
   */
  public long getStartTime() {
    return startTime_mS;
  }

  /**
   * Gets the Properties class.
   * @return the Properties class.
   */
  public Properties getProperties() {
    return prop;
  }

  /**
   * Gets the configured SketchTrial
   * @return the configured SketchTrial
   */
  public SketchTrial getSketchTrial() {
    return trial;
  }

  /**
   * Outputs a line to the configured PrintWriter.
   * @param s The String to print
   */
  public void println(final String s) {
    System.out.println(s);
    if (out != null) {
      out.println(s);
    }
  }

  /**
   * Flush any buffered output to the configured PrintWriter.
   */
  public void flush() {
    if (out != null) {
      out.flush();
    }
  }

  /**
   * The JVM may call this method to closes the PrintWriter resource.
   */
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
