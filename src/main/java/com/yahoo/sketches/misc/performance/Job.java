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
 * This class runs a specific Job which defined by a JobProfile and a Properties
 * class that must already be loaded the properties key/value pairs required for this job.
 * All output to the file and to stdOut goes through this class.
 *
 * @author Lee Rhodes
 */
public class Job {
  private final Properties prop;
  //Output to File
  private PrintWriter out = null;
  //Date-Time
  private SimpleDateFormat compactSDF;  //used in the filename
  private SimpleDateFormat expandedSDF; //for human readability
  private GregorianCalendar gc;
  private long startTime_mS;
  private String profileName;

  /**
   * Construct and Run the Job
   * @param prop the given Properties
   * @param profile the given JobProfile
   */
  public Job(final Properties prop, final JobProfile profile) {
    startTime_mS = System.currentTimeMillis();
    this.prop = prop;
    profileName = profile.getClass().getSimpleName();

    configureDateFormats();
    configurePrintWriter();

    println("START JOB " + profileName );
    println(prop.extractKvPairs(LS));
    flush(); //flush print buffer

    profile.start(this);

    final long testTime_mS = System.currentTimeMillis() - startTime_mS;
    println("Total Job Time        : " + milliSecToString(testTime_mS));
    println("END JOB " + profileName +  LS + LS);
    flush();
    if (out != null) {
      out.close();
    }
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

    final String outputFileName = profileName + nowStr + ".txt";
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
   * Outputs a line to the configured PrintWriter and stdOut.
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
