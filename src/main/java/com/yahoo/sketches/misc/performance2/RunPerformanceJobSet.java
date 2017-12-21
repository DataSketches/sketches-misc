/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.misc.performance2;

import static com.yahoo.sketches.misc.Files.isFileValid;

import org.testng.annotations.Test;

import com.yahoo.sketches.misc.Files;

/**
 * This class parses an input string command file, which contains specific configuration
 * information for the SketchTrial, the TrailsManager and settings for the date and time.
 * As an example:
 *
 * <pre>
 * # Job
 * JobType=Accuracy
 *
 * # Trials Profile
 * Trials_lgMinT=8  #prints intermediate results starting w/ this lgMinT
 * Trials_lgMaxT=16 #The max trials
 * Trials_TPPO=1    #how often intermediate results are printed
 *
 * Trials_lgQK=13   #size of quantiles sketch
 * Trials_interData=true
 * Trials_postPMFs=false
 * Trials_bytes=false
 *
 * #Uniques Profile
 * Trials_lgMinU=0  #The starting # of uniques that is printed at the end.
 * Trials_lgMaxU=20 #How high the # uniques go
 * Trials_UPPO=16   #The horizontal x-resolution of trials points
 * Trials_charArr=true
 *
 * # Date-Time Profile //interpreted by this class
 * TimeZone=PST
 * TimeZoneOffset=-28800000 # offset in milliseconds
 * FileNameDateFormat=yyyyMMdd'_'HHmmssz
 * ReadableDateFormat=yyyy/MM/dd HH:mm:ss z
 *
 * #Sketch Profile
 * SketchTrial=com.yahoo.sketches.misc.performance2.HllTrial //interpreted here
 * LgK=21
 * HLL_direct=false #only for Theta, HLL. See javadocs.
 * HLL_tgtHllType=HLL8
 * HLL_useComposite=false
 * HLL_compact=true
 * HLL_wrap=false
 * </pre>
 *
 * <p>Multiple jobs, such as the above can be included in the same command file, each
 * separated by a semicolon ";".
 *
 * @author Lee Rhodes
 */
public class RunPerformanceJobSet {
  static final char TAB = '\t';
  static final String LS = System.getProperty("line.separator");

  void parseJobs(final String cmdFileName) {
    if (!isFileValid(cmdFileName)) {
      throw new IllegalArgumentException("File not valid.");
    }

    final String cmdStr = Files.fileToString(cmdFileName); //includes line feeds
    final String[] jobs = cmdStr.split(";");
    for (int i = 0; i < jobs.length; i++) {
      prepareJobProperties(jobs[i], i + 1);
    }
  }

  void prepareJobProperties(final String job, final int jobNum) {
    final Properties p = new Properties();
    p.put("JOBNUM", Integer.toString(jobNum));
    final String[] lines = job.split(LS);
    for (int i = 0; i < lines.length; i++) {
      final String line = lines[i].trim();
      final int commentIdx = line.indexOf('#');
      final String tmp;
      if (commentIdx >= 0) { //comment
        tmp = line.substring(0, commentIdx).trim();
      } else {
        tmp = line;
      }
      if (tmp.length() < 3) { continue; }
      final String[] kv = tmp.split("=", 2);
      if (kv.length < 2) {
        throw new IllegalArgumentException("Missing valid key-value separator: " + tmp);
      }
      p.put(kv[0].trim(), kv[1].trim());
    }
    setDateFormats(p);
    chooseSketchTrialAndRunJob(p);
  }

  private static void setDateFormats(final Properties p) {
    String fileFormat = p.get("FileNameDateFormat");
    if (fileFormat == null) {
      fileFormat = "yyyyMMdd'_'HHmmssz";
      p.put("FileNameDateFormat", "yyyyMMdd'_'HHmmssz");
    }
    String readFormat = p.get("ReadableDateFormat");
    if (readFormat == null) {
      readFormat = "yyyy/MM/dd HH:mm:ss z";
      p.put("ReadableDateFormat", readFormat);
    }
    String timeZone = p.get("TimeZone");
    if (timeZone == null) {
      timeZone = "UTC";
      p.put("TimeZone", timeZone);
    }
    String tzOffsetStr = p.get("TimeZoneOffset");
    if (tzOffsetStr == null) {
      tzOffsetStr = "0";
      p.put("TimeZoneOffset", tzOffsetStr);
    }
  }

  @SuppressWarnings("unused")
  private static void chooseSketchTrialAndRunJob(final Properties prop) {
    final String sketchStr = prop.mustGet("SketchTrial");
    final SketchTrial sketchTrial;
    try {
      sketchTrial = (SketchTrial) Class.forName(sketchStr).newInstance();
    } catch (final Exception e) {
      throw new RuntimeException("Cannot instantiate " + sketchStr + "\n" + e);
    }
    new PerformanceJob(prop, sketchTrial);
  }

  @Test
  public void runJobs() {
    final String fileName = "src/test/resources/HllSerDeJob.txt";
    RunPerformanceJobSet.main(new String[] {fileName});
  }

  /**
   * Run from the command line
   * @param args Argument zero is the command file
   */
  public static void main(final String[] args) {
    final RunPerformanceJobSet perfRun = new RunPerformanceJobSet();
    perfRun.parseJobs(args[0]);
  }
}
