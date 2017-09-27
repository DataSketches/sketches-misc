/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.performance;

import org.testng.annotations.Test;

import com.yahoo.sketches.Files;

/**
 * @author Lee Rhodes
 */
public class RunPerformanceJobs {
  static final char TAB = '\t';
  static final String LS = System.getProperty("line.separator");

  void parseJobs(String cmdFile) {
    char[] chArr = Files.fileToCharArray(cmdFile); //includes line feeds
    String cmdStr = new String(chArr);
    String[] jobs = cmdStr.split(";");
    for (int i = 0; i < jobs.length; i++) {
      prepareJobProperties(jobs[i], i + 1);
    }
  }

  void prepareJobProperties(String job, int jobNum) {
    Properties p = new Properties();
    p.put("JOBNUM", Integer.toString(jobNum));
    String[] lines = job.split(LS);
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i].trim();
      int commentIdx = line.indexOf('#');
      String tmp;
      if (commentIdx >= 0) { //comment
        tmp = line.substring(0, commentIdx).trim();
      } else {
        tmp = line;
      }
      if (tmp.length() < 3) { continue; }
      String[] kv = tmp.split("=", 2);
      if (kv.length < 2) {
        throw new IllegalArgumentException("Missing valid key-value separator: " + tmp);
      }
      p.put(kv[0].trim(), kv[1].trim());
    }
    setDateFormats(p);
    chooseSketchAndRunJob(p);
  }

  void setDateFormats(Properties p) {
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

  void chooseSketchAndRunJob(Properties p) {
    String sketch = p.mustGet("Sketch");
    SketchTrial sketchTrial;
    if (sketch.equals("HLL")) {
      sketchTrial = new HllTrial();
    } else if (sketch.equals("HLLP")) {
      sketchTrial = new HllppTrial();
    } else if (sketch.equals("THETA")) {
      sketchTrial = new ThetaTrial();
    } else {
      throw new IllegalArgumentException("Sketch type not found");
    }
    @SuppressWarnings("unused")
    PerformanceJob ap = new PerformanceJob(p, sketchTrial);
  }

  @Test
  public void runFromTest() {
    parseJobs("src/test/resources/HllpAccuracyJob.txt");
  }

  public static void main(String[] args) {
    String cmdFile = args[0];
    if ((cmdFile == null) || cmdFile.isEmpty()) {
      throw new IllegalArgumentException("No command file.");
    }
    RunPerformanceJobs perfRun = new RunPerformanceJobs();
    perfRun.parseJobs(cmdFile);
  }
}
