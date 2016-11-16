/*
 * Copyright 2016, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.cmd;

import static com.yahoo.sketches.Util.LS;
import static com.yahoo.sketches.Util.TAB;
import static java.lang.Math.log10;
import static java.lang.Math.pow;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import com.yahoo.sketches.frequencies.ErrorType;
import com.yahoo.sketches.frequencies.ItemsSketch;
import com.yahoo.sketches.frequencies.LongsSketch.Row;
import com.yahoo.sketches.quantiles.DoublesSketch;
import com.yahoo.sketches.quantiles.DoublesSketchBuilder;
import com.yahoo.sketches.theta.Sketches;
import com.yahoo.sketches.theta.UpdateSketch;
import com.yahoo.sketches.theta.UpdateSketchBuilder;

//CHECKSTYLE.OFF: JavadocMethod
//CHECKSTYLE.OFF: WhitespaceAround
/**
 * Command line access to the basic sketch functions.  This is intentionally a very simple parser
 * with limited functionality that can be used for small experiments and for demos.
 * Although the sketching library can be used on a single machine, the more typical use case is on
 * large, highly distributed system architectures where a CLI is not of much use.
 */
public class CommandLine {
  private static final String BOLD = "\033[1m";
  private static final String OFF = "\033[0m";
  private boolean disablePrint = false;

  CommandLine() {}

  /**
   * Used for testing
   * @param disablePrint if true, disables normal System.out messages, but not System.err messages.
   * @param args the same args list as used for main.
   */
  CommandLine(boolean disablePrint, String[] args) {
    this.disablePrint = disablePrint;
    this.parseType(args);
  }

  public static void main(String[] args) {
    CommandLine cl = new CommandLine();
    cl.parseType(args);
  }

  private void parseType(String[] args) {
    if ((args == null) || (args.length == 0) || (args[0].isEmpty())) {
      help();
      return;
    }
    String token1 = args[0].toLowerCase();
    switch (token1) {
      case "uniq": parseUniq(args); break;
      case "rank": parseRank(args); break;
      case "hist": parseHist(args); break;
      case "loghist": parseLogHist(args); break;
      case "freq": parseFreq(args); break;
      case "help": help(); break;
      default: {
        printlnErr("Unrecognized TYPE: "+token1);
        help();
      }
    }
  }

  private static int parseArgsCase(String[] args) { //we already know type, args[0] is valid
    int len = args.length;
    int ret = 0;
    switch (len) {
      case 1: ret = 1; break; //only type, assume default k, System.in
      case 2: {
        String token2 = args[1]; //2nd arg could be help, k (numeric) or a fileName
        if (token2.equalsIgnoreCase("help")) { ret = 2; break; } //help
        if (!isNumeric(token2)) { ret = 3; break; } //2nd arg not numeric, must be a filename
        ret = 4; //2nd arg must be numeric, assume System.in
        break;
      }
      default: { //3 or more
        String token2 = args[1]; //2nd arg could be help, k (numeric) or a fileName
        if (token2.equalsIgnoreCase("help")) { ret = 2; break; } //help
        if (!isNumeric(token2)) { ret = 3; break; } //2nd arg not numeric, must be a filename
        //2nd arg is numeric, 3rd arg must be filename
        ret = 5;
        break;
      }
    }
    return ret;
  }

  private void parseUniq(String[] args) {
    UpdateSketchBuilder bldr = Sketches.updateSketchBuilder();
    UpdateSketch sketch;
    int argsCase = parseArgsCase(args);
    switch (argsCase) {
      case 1:
        doUniq(getBR(null), bldr.build()); break; //[default k], [System.in]
      case 2:
        uniqHelp(); break; //help
      case 3: //2nd arg not numeric, must be a filename
        doUniq(getBR(args[1]), bldr.build()); break; //[default k], file
      case 4: //2nd arg is numeric, no filename
        sketch = bldr.build(Integer.parseInt(args[1])); //args[1] is numeric = k
        doUniq(getBR(null), sketch); //user k, [System.in]
        break;
      case 5: //3 valid args
        sketch = bldr.build(Integer.parseInt(args[1])); //args[1] is numeric = k
        doUniq(getBR(args[2]), sketch);
    }
  }

  private void doUniq(BufferedReader br, UpdateSketch sketch) {
    String itemStr = "";
    try {
      while ((itemStr = br.readLine()) != null) {
        sketch.update(itemStr);
      }
    } catch (IOException e) {
      printlnErr("Read Error: Item: "+itemStr +", "+br.toString());
      throw new RuntimeException(e);
    }
    println(sketch.toString());
  }

  private void parseRank(String[] args) {
    DoublesSketchBuilder bldr = new DoublesSketchBuilder();
    DoublesSketch sketch;
    int argsCase = parseArgsCase(args);
    switch (argsCase) {
      case 1:
        doRank(getBR(null), bldr.build()); break; //[default k], [System.in]
      case 2:
        rankHelp(); break; //help
      case 3: //2nd arg not numeric, must be a filename
        doRank(getBR(args[1]), bldr.build()); break; //[default k], file
      case 4: //2nd arg is numeric, no filename
        sketch = bldr.build(Integer.parseInt(args[1])); //args[1] is numeric = k
        doRank(getBR(null), sketch); //user k, [System.in]
        break;
      case 5: //3 valid args
        sketch = bldr.build(Integer.parseInt(args[1])); //args[1] is numeric = k
        doRank(getBR(args[2]), sketch);
    }
  }

  private void doRank(BufferedReader br, DoublesSketch sketch) {
    String itemStr = "";
    try {
      while ((itemStr = br.readLine()) != null) {
        double item = Double.parseDouble(itemStr);
        sketch.update(item);
      }
    } catch (IOException | NumberFormatException e ) {
      printlnErr("Read Error: Item: "+itemStr +", "+br.toString());
      throw new RuntimeException(e);
    }
    int ranks = 101;
    double[] valArr = sketch.getQuantiles(ranks);
    println("Rank"+TAB+ "Value");
    for (int i=0; i<ranks; i++) {
      String r = String.format("%.2f",(double)i/ranks);
      println(r + TAB + valArr[i]);
    }
  }

  private void parseHist(String[] args) {
    DoublesSketchBuilder bldr = new DoublesSketchBuilder();
    DoublesSketch sketch;
    int argsCase = parseArgsCase(args);
    switch (argsCase) {
      case 1:
        doHist(getBR(null), bldr.build()); break; //[default k], [System.in]
      case 2:
        histHelp(); break; //help
      case 3: //2nd arg not numeric, must be a filename
        doHist(getBR(args[1]), bldr.build()); break; //[default k], file
      case 4: //2nd arg is numeric, no filename
        sketch = bldr.build(Integer.parseInt(args[1])); //args[1] is numeric = k
        doHist(getBR(null), sketch); //user k, [System.in]
        break;
      case 5: //3 valid args
        sketch = bldr.build(Integer.parseInt(args[1])); //args[1] is numeric = k
        doHist(getBR(args[2]), sketch);
    }
  }

  private void doHist(BufferedReader br, DoublesSketch sketch) {
    String itemStr = "";
    try {
      while ((itemStr = br.readLine()) != null) {
        double item = Double.parseDouble(itemStr);
        sketch.update(item);
      }
    } catch (IOException | NumberFormatException e ) {
      printlnErr("Read Error: Item: "+itemStr +", "+br.toString());
      throw new RuntimeException(e);
    }
    int splitPoints = 30;
    long n = sketch.getN();
    double[] splitsArr = getEvenSplits(sketch, splitPoints);
    double[] histArr = sketch.getPMF(splitsArr);
    println("Value"+TAB+ "Freq");
    //int histArrLen = histArr.length; //one larger than splitsArr
    double min = sketch.getMinValue();
    String splitVal = String.format("%,f", min);
    String freqVal = String.format("%,d", (long)(histArr[0] * n));
    println(splitVal+TAB+freqVal);
    for (int i=0; i<splitsArr.length; i++) {
      splitVal = String.format("%,f", splitsArr[i] * n);
      freqVal = String.format("%,d", (long)(histArr[i+1] * n));
      println(splitVal+TAB+freqVal);
    }
  }

  private void parseLogHist(String[] args) {
    DoublesSketchBuilder bldr = new DoublesSketchBuilder();
    DoublesSketch sketch;
    int argsCase = parseArgsCase(args);
    switch (argsCase) {
      case 1:
        doLogHist(getBR(null), bldr.build()); break; //[default k], [System.in]
      case 2:
        logHistHelp(); break; //help
      case 3: //2nd arg not numeric, must be a filename
        doLogHist(getBR(args[1]), bldr.build()); break; //[default k], file
      case 4: //2nd arg is numeric, no filename
        sketch = bldr.build(Integer.parseInt(args[1])); //args[1] is numeric = k
        doLogHist(getBR(null), sketch); //user k, [System.in]
        break;
      case 5: //3 valid args
        sketch = bldr.build(Integer.parseInt(args[1])); //args[1] is numeric = k
        doLogHist(getBR(args[2]), sketch);
    }
  }

  private void doLogHist(BufferedReader br, DoublesSketch sketch) {
    String itemStr = "";
    try {
      while ((itemStr = br.readLine()) != null) {
        double item = Double.parseDouble(itemStr);
        if (Double.isNaN(item) || (item <= 0.0)) { continue; }
        sketch.update(item);
      }
    } catch (IOException | NumberFormatException e ) {
      printlnErr("Read Error: Item: "+itemStr +", "+br.toString());
      throw new RuntimeException(e);
    }
    int splitPoints = 30;
    long n = sketch.getN();
    double[] splitsArr = getLogSplits(sketch, splitPoints);
    double[] histArr = sketch.getPMF(splitsArr);
    println("Value"+TAB+ "Freq");
    //int histArrLen = histArr.length; //one larger than splitsArr
    double min = sketch.getMinValue();
    String splitVal = String.format("%,f", min);
    String freqVal = String.format("%,d", (long)(histArr[0] * n));
    println(splitVal+TAB+freqVal);
    for (int i=0; i<splitsArr.length; i++) {
      splitVal = String.format("%,f", splitsArr[i] * n);
      freqVal = String.format("%,d", (long)(histArr[i+1] * n));
      println(splitVal+TAB+freqVal);
    }
  }

  private void parseFreq(String[] args) {
    ItemsSketch<String> sketch;
    int defaultSize = 1 << 17; //128K
    int argsCase = parseArgsCase(args);
    switch (argsCase) {
      case 1:
        sketch = new ItemsSketch<String>(defaultSize);
        doFreq(getBR(null), sketch); break; //[default k], [System.in]
      case 2:
        freqHelp(); break; //help
      case 3: //2nd arg not numeric, must be a filename
        sketch = new ItemsSketch<String>(defaultSize);
        doFreq(getBR(args[1]), sketch); break; //[default k], file
      case 4: //2nd arg is numeric, no filename
        sketch = new ItemsSketch<String>(Integer.parseInt(args[1])); //args[1] is numeric = k
        doFreq(getBR(null), sketch); //user k, [System.in]
        break;
      case 5: //3 valid args
        sketch = new ItemsSketch<String>(Integer.parseInt(args[1])); //args[1] is numeric = k
        doFreq(getBR(args[2]), sketch);
    }
  }

  private void doFreq(BufferedReader br, ItemsSketch<String> sketch) {
    String itemStr = "";
    try {
      while ((itemStr = br.readLine()) != null) {
        sketch.update(itemStr);
      }
    } catch (IOException e ) {
      printlnErr("Read Error: Item: "+itemStr +", "+br.toString());
      throw new RuntimeException(e);
    }
    //NFP is a subset of NFN
    ItemsSketch.Row<String>[] rowArr = sketch.getFrequentItems(ErrorType.NO_FALSE_POSITIVES);
    int len = rowArr.length;
    println("Qualifying Rows: "+len);
    println(Row.getRowHeader());
    for (int i=0; i<len; i++) {
      println((i+1) + rowArr[i].toString());
    }
  }

  private static double[] getEvenSplits(DoublesSketch sketch, int splitPoints) {
    double min = sketch.getMinValue();
    double max = sketch.getMaxValue();
    return getSplits(min, max, splitPoints);
  }

  private static double[] getLogSplits(DoublesSketch sketch, int splitPoints) {
    double min = sketch.getMinValue();
    double max = sketch.getMaxValue();
    double logMin = log10(min);
    double logMax = log10(max);
    double[] logArr = getSplits(logMin, logMax, splitPoints);
    double[] expArr = new double[logArr.length];
    for (int i= 0; i<logArr.length; i++) {
      expArr[i] = pow(10.0, logArr[i]);
    }
    return expArr;
  }

  private static double[] getSplits(double min, double max, int splitPoints) {
    double range = max - min;
    double delta = range/(splitPoints + 1);
    double[] splits = new double[splitPoints];
    for (int i = 0; i < splitPoints; i++) {
      splits[i] = delta * (i+1);
    }
    return splits;
  }

  private static boolean isNumeric(String token) {
    for (char c : token.toCharArray()) {
        if (!Character.isDigit(c)) { return false; }
    }
    return true;
  }

  private static BufferedReader getBR(String token) {
    BufferedReader br = null;
    try {
      if ((token == null) || (token.length() == 0)) {
        br = new BufferedReader(new InputStreamReader(System.in, UTF_8));
      } else {
        br = new BufferedReader(new InputStreamReader(new FileInputStream(token), UTF_8));
      }
    } catch (FileNotFoundException e) {
      printlnErr("File Not Found: "+token);
      System.exit(1);
    }
    return br;
  }

  private void uniqHelp() {
    StringBuilder sb = new StringBuilder();
    sb.append(BOLD+"UNIQ SYNOPSIS"+OFF).append(LS);
    sb.append("    sketch uniq help").append(LS);
    sb.append("    sketch uniq [SIZE] [FILE]").append(LS);
    println(sb.toString());
  }

  private void rankHelp() {
    StringBuilder sb = new StringBuilder();
    sb.append(BOLD+"RANK SYNOPSIS"+OFF).append(LS);
    sb.append("    sketch rank help").append(LS);
    sb.append("    sketch rank [SIZE] [FILE]").append(LS);
    println(sb.toString());
  }

  private void histHelp() {
    StringBuilder sb = new StringBuilder();
    sb.append(BOLD+"HIST SYNOPSIS"+OFF).append(LS);
    sb.append("    sketch hist help").append(LS);
    sb.append("    sketch hist [SIZE] [FILE]").append(LS);
    println(sb.toString());
  }

  private void logHistHelp() {
    StringBuilder sb = new StringBuilder();
    sb.append(BOLD+"LOGHIST SYNOPSIS"+OFF).append(LS);
    sb.append("    sketch loghist help").append(LS);
    sb.append("    sketch loghist [SIZE] [FILE]").append(LS);
    println(sb.toString());
  }

  private void freqHelp() {
    StringBuilder sb = new StringBuilder();
    sb.append(BOLD+"FREQ SYNOPSIS"+OFF).append(LS);
    sb.append("    sketch freq help").append(LS);
    sb.append("    sketch freq [SIZE] [FILE]").append(LS);
    println(sb.toString());
  }

  /**
   * Prints the help summaries.
   */
  public void help() {
    StringBuilder sb = new StringBuilder();
    sb.append(BOLD+"NAME"+OFF).append(LS);
    sb.append("    sketch - sketch Uniques, Quantiles, Histograms, or Frequent Items.").append(LS);
    sb.append(BOLD+"SYNOPSIS"+OFF).append(LS);
    sb.append("    sketch (this help)").append(LS);
    sb.append("    sketch TYPE help").append(LS);
    sb.append("    sketch TYPE [SIZE] [FILE]").append(LS);
    sb.append(BOLD+"DESCRIPTION"+OFF).append(LS);
    sb.append("    Write a sketch(TYPE, SIZE) of FILE to standard output.").append(LS);
    sb.append("    TYPE is required.").append(LS);
    sb.append("    If SIZE is omitted, internal defaults are used.").append(LS);
    sb.append("    If FILE is omitted, Standard In is assumed.").append(LS);
    sb.append(BOLD+"TYPE DESCRIPTION"+OFF).append(LS);
    sb.append("    sketch uniq    : Sketch the unique string items of a stream.").append(LS);
    sb.append("    sketch rank    : Sketch the rank-value distribution of a numeric value stream.")
      .append(LS);
    sb.append("    sketch hist    : "
       + "Sketch the linear-axis value-frequency distribution of numeric value stream.").append(LS);
    sb.append("    sketch loghist : "
       + "Sketch the log-axis value-frequency distribution of numeric value stream.").append(LS);
    sb.append("    sketch freq    : "
       + "Sketch the Heavy Hitters of a string item stream.").append(LS);
    println(sb.toString());
    uniqHelp();
    rankHelp();
    histHelp();
    logHistHelp();
    freqHelp();
  }

  private static void printlnErr(String s) {
    System.err.println(s);
  }

  private void println(String s) {
    if (disablePrint) { return; }
    System.out. println(s);
  }
}
