/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.performance;

import static com.yahoo.sketches.Util.pwr2LawNext;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;

import com.yahoo.sketches.quantiles.DoublesSketch;

/**
 * @author Lee Rhodes
 */
public class PerformanceUtil {
  public static final char TAB = '\t';
  public static final String LS = System.getProperty("line.separator");
  //Quantile fractions computed from the standard normal cumulative distribution.
  public static final double M4SD = 0.0000316712418331; //minus 4 StdDev //0
  public static final double M3SD = 0.0013498980316301; //minus 3 StdDev //1
  public static final double M2SD = 0.0227501319481792; //minus 2 StdDev //2
  public static final double M1SD = 0.1586552539314570; //minus 1 StdDev //3
  public static final double MED  = 0.5; //median                        //4
  public static final double P1SD = 0.8413447460685430; //plus  1 StdDev //5
  public static final double P2SD = 0.9772498680518210; //plus  2 StdDev //6
  public static final double P3SD = 0.9986501019683700; //plus  3 StdDev //7
  public static final double P4SD = 0.9999683287581670; //plus  4 StdDev //8
  public static final double[] FRACTIONS =
    {0.0, M4SD, M3SD, M2SD, M1SD, MED, P1SD, P2SD, P3SD, P4SD, 1.0};
  public static final int FRACT_LEN = FRACTIONS.length;

  public static final AccuracyStats[] buildAccuracyStatsArray(Properties prop) {
    int lgMinU = Integer.parseInt(prop.mustGet("Trials_lgMinU"));
    int lgMaxU = Integer.parseInt(prop.mustGet("Trials_lgMaxU"));
    int uPPO = Integer.parseInt(prop.mustGet("Trials_UPPO"));
    int lgQK = Integer.parseInt(prop.mustGet("Trials_lgQK"));

    int qLen = countPoints(lgMinU, lgMaxU, uPPO);
    AccuracyStats[] qArr = new AccuracyStats[qLen];
    int p = 1 << lgMinU;
    for (int i = 0; i < qLen; i++) {
      qArr[i] = new AccuracyStats(1 << lgQK, p);
      p = pwr2LawNext(uPPO, p);
    }
    return qArr;
  }

   private static final int countPoints(int lgStart, int lgEnd, int ppo) {
     int p = 1 << lgStart;
     int end = 1 << lgEnd;
     int count = 0;
     while (p <= end) {
       p = pwr2LawNext(ppo, p);
       count++;
     }
     return count;
   }

   public static final PrintWriter configureFile(String fileName) {
     File file = null;
     PrintWriter pw = null;
     if ((fileName != null) && !fileName.isEmpty()) {
       file = new File(fileName);
       if (file.isFile()) {
         file.delete(); //remove old file if it exists
       } else {
         try {
           file.createNewFile();
         } catch (Exception e) {
           throw new RuntimeException("Cannot create file: " + fileName + LS + e);
         }
       }
       BufferedWriter bw;
       try {
         FileOutputStream fos = new FileOutputStream(file, true);
         OutputStreamWriter osw = new OutputStreamWriter(fos, Charset.defaultCharset());
         bw = new BufferedWriter(osw, 8192);
       } catch (IOException e) {
         // never opened, so don't close it.
         throw new RuntimeException("Could not create: " + file.getPath() + LS + e);
       }
       pw = new PrintWriter(bw);
     }
     return pw;
   }

   public static void outputPMF(PerformanceJob perf, AccuracyStats q) {
     DoublesSketch qSk = q.qsk;
     double[] splitPoints = qSk.getQuantiles(FRACTIONS); //1:1
     double[] reducedSp = reduceSplitPoints(splitPoints);
     double[] pmfArr = qSk.getPMF(reducedSp); //pmfArr is one larger
     long trials = qSk.getN();

     //output Histogram
     String hdr = String.format("%10s%4s%12s", "Trials", "    ", "Est");
     String fmt = "%10d%4s%12.2f";
     perf.println("Histogram At " + q.uniques);
     perf.println(hdr);
     for (int i = 0; i < reducedSp.length; i++) {
       int hits = (int)(pmfArr[i+1] * trials);
       double est = reducedSp[i];
       String line = String.format(fmt, hits, " >= ", est);
       perf.println(line);
     }
     perf.println("");
   }

   public static double[] reduceSplitPoints(double[] splitPoints) {
     int num = 1;
     double lastV = splitPoints[0];
     for (int i = 0; i < splitPoints.length; i++) {
       double v = splitPoints[i];
       if (v <= lastV) { continue; }
       num++;
       lastV = v;
     }
     lastV = splitPoints[0];
     int idx = 0;
     double[] sp = new double[num];
     sp[0] = lastV;
     for (int i = 0; i < splitPoints.length; i++) {
       double v = splitPoints[i];
       if (v <= lastV) { continue; }
       sp[++idx] = v;
       lastV = v;
     }
     return sp;
   }

}
