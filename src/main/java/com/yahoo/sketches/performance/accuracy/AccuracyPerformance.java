/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.performance.accuracy;

import static com.yahoo.sketches.Util.milliSecToString;
import static com.yahoo.sketches.performance.accuracy.PerformanceUtil.LS;
import static com.yahoo.sketches.performance.accuracy.PerformanceUtil.buildQuantilesArray;
import static com.yahoo.sketches.performance.accuracy.PerformanceUtil.configureFile;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;

/**
 * @author Lee Rhodes
 */
public class AccuracyPerformance {
  private final Properties prop;
  private final Quantiles[] qArr;
  private PrintWriter out = null;
  private final SketchAccuracyTrial trial;
  private AccuracyTrialsManager trialsMgr;
  private SimpleDateFormat fileSDF;
  private SimpleDateFormat readSDF;
  private GregorianCalendar gc;
  private long startTime_mS;

  public AccuracyPerformance(Properties prop, SketchAccuracyTrial trial) {
    this.prop = prop;
    this.trial = trial;
    qArr = buildQuantilesArray(prop);
    configureDateFormats();
    configurePrintWriter();
    startJob();
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
    trialsMgr = new AccuracyTrialsManager(this);
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
   * This method drives the whole process.
   * See the main() method as an example of how to configure this.
   * @param prop the properties class
   */
  private void startJob() {
    startTime_mS = System.currentTimeMillis();
    println("JOBNUM=" + prop.mustGet("JOBNUM"));
    println(prop.extractKvPairs(LS));
    flush();
    //Run the full suite of trials for this job
    trialsMgr.doTrials();

    long testTime_mS = System.currentTimeMillis() - startTime_mS;
    println("Total Test Time        : " + milliSecToString(testTime_mS) + LS);
    if (out != null) {
      out.close();
    }
  }

  public Properties getProperties() {
    return prop;
  }

  public Quantiles[] getQuantilesArr() {
    return qArr;
  }

  public SketchAccuracyTrial getSketchAccuracyTrial() {
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
