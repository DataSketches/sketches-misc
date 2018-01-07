/*
 * Copyright 2018, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.misc.performance;

import static com.yahoo.sketches.misc.Files.isFileValid;

import org.testng.annotations.Test;

import com.yahoo.sketches.misc.Files;

/**
 * This class parses an input string command file, which contains specific configuration
 * information for the JobProfile, and then loads and runs the JobProfile.
 *
 * <p>A single JobProfile can actually contain multiple sub-jobs, each
 * separated by a semicolon ";".
 *
 * @author Lee Rhodes
 */
public class RunJobSet {
  private static final String LS = System.getProperty("line.separator");

  /**
   * Parse the command file for jobs.  Jobs are separated by a semicolon ";".
   * Each job is assigned a Job number starting with one.
   * @param cmdFileName the name of the command file containing the job(s) to be run.
   */
  private static void parseJobs(final String cmdFileName) {
    if (!isFileValid(cmdFileName)) {
      throw new IllegalArgumentException("File not valid.");
    }
    final String cmdStr = Files.fileToString(cmdFileName); //includes line feeds
    final String[] jobs = cmdStr.split(";");
    for (int i = 0; i < jobs.length; i++) {
      final Properties prop = parseJobProperties(jobs[i], i + 1);
      setDateFormats(prop);
      final JobProfile profile = createJobProfile(prop);
      @SuppressWarnings("unused")
      final Job pj = new Job(prop, profile);
    }
  }

  /**
   * Each job is assigned a new Properties class, which is simply a hash map of string
   * key value pairs. The pairs are separated by System.getProperty("line.separator").
   * The key is separated from the value by "=". Comments start with "#" and continue to the
   * end of the line.
   * @param job the job name
   * @param jobNum the job number
   */
  private static Properties parseJobProperties(final String job, final int jobNum) {
    final Properties prop = new Properties();
    prop.put("JOBNUM", Integer.toString(jobNum));
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
      prop.put(kv[0].trim(), kv[1].trim());
    }
    return prop;
  }

  /**
   * Set two date formats into the Properties file.
   * The default date format for the output file name is "yyyyMMdd'_'HHmmssz".
   * This can be overridden using the key "FileNameDateFormat".
   *
   * <p>The default date format for the reports is "yyyy/MM/dd HH:mm:ss z".
   * This can be overridden using the key "ReadableDateFormat".
   *
   * <p>The default time-zone is GMT, with an TimeZoneOffset of zero.
   * This can be overridden using the key "TimeZone" for the 3 letter abreviation of the time
   * zone name, and the key "TimeZoneOffset" to specify the offset in milliseconds.
   * @param prop Properties
   */
  private static void setDateFormats(final Properties prop) {
    String fileFormat = prop.get("FileNameDateFormat");
    if (fileFormat == null) {
      fileFormat = "yyyyMMdd'_'HHmmssz";
      prop.put("FileNameDateFormat", "yyyyMMdd'_'HHmmssz");
    }
    String readFormat = prop.get("ReadableDateFormat");
    if (readFormat == null) {
      readFormat = "yyyy/MM/dd HH:mm:ss z";
      prop.put("ReadableDateFormat", readFormat);
    }
    String timeZone = prop.get("TimeZone");
    if (timeZone == null) {
      timeZone = "UTC";
      prop.put("TimeZone", timeZone);
    }
    String tzOffsetStr = prop.get("TimeZoneOffset");
    if (tzOffsetStr == null) {
      tzOffsetStr = "0";
      prop.put("TimeZoneOffset", tzOffsetStr);
    }
  }

  private static JobProfile createJobProfile(final Properties prop) {
    final String sketchStr = prop.mustGet("JobProfile");
    final JobProfile profile;
    try {
      profile = (JobProfile) Class.forName(sketchStr).newInstance();
    } catch (final Exception e) {
      throw new RuntimeException("Cannot instantiate " + sketchStr + "\n" + e);
    }
    return profile;
  }

  @Test
  public void runJobs() {
    final String fileName = "src/main/resources/uniquecount/ThetaSerDeJob.txt";
    RunJobSet.main(new String[] {fileName});
  }

  /**
   * Run from the command line
   * @param args Argument zero is the command file
   */
  public static void main(final String[] args) {
    parseJobs(args[0]);
  }
}
