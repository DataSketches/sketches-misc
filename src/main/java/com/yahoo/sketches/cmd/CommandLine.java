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
  CommandLine(final boolean disablePrint, final String[] args) {
    this.disablePrint = disablePrint;
    this.parseType(args);
  }

  public static void main(final String[] args) {
    final CommandLine cl = new CommandLine();
    cl.parseType(args);
  }

  private void parseType(final String[] args) {
    if ((args == null) || (args.length == 0) || (args[0].isEmpty())) {
      help();
      return;
    }
    final String token1 = args[0].toLowerCase();
    switch (token1) {
      case "uniq": parseUniq(args); break;
      case "rank": parseRank(args); break;
      case "hist": parseHist(args); break;
      case "loghist": parseLogHist(args); break;
      case "freq": parseFreq(args); break;
      case "help": help(); break;
      default: {
        printlnErr("Unrecognized TYPE: " + token1);
        help();
      }
    }
  }

  private static int parseArgsCase(final String[] args) { //we already know type, args[0] is valid
    final int len = args.length;
    int ret = 0;
    switch (len) {
      case 1: ret = 1; break; //only type, assume default k, System.in
      case 2: {
        final String token2 = args[1]; //2nd arg could be help, k (numeric) or a fileName
        if (token2.equalsIgnoreCase("help")) { ret = 2; break; } //help
        if (!isNumeric(token2)) { ret = 3; break; } //2nd arg not numeric, must be a filename
        ret = 4; //2nd arg must be numeric, assume System.in
        break;
      }
      default: { //3 or more
        final String token2 = args[1]; //2nd arg could be help, k (numeric) or a fileName
        if (token2.equalsIgnoreCase("help")) { ret = 2; break; } //help
        if (!isNumeric(token2)) { ret = 3; break; } //2nd arg not numeric, must be a filename
        //2nd arg is numeric, 3rd arg must be filename
        ret = 5;
        break;
      }
    }
    return ret;
  }

  private void parseUniq(final String[] args) {
    final UpdateSketchBuilder bldr = Sketches.updateSketchBuilder();
    final UpdateSketch sketch;
    final int argsCase = parseArgsCase(args);
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

  private void doUniq(final BufferedReader br, final UpdateSketch sketch) {
    String itemStr = "";
    try {
      while ((itemStr = br.readLine()) != null) {
        sketch.update(itemStr);
      }
    } catch (final IOException e) {
      printlnErr("Read Error: Item: " + itemStr + ", " + br.toString());
      throw new RuntimeException(e);
    }
    println(sketch.toString());
  }

  private void parseRank(final String[] args) {
    final DoublesSketchBuilder bldr = new DoublesSketchBuilder();
    final DoublesSketch sketch;
    final int argsCase = parseArgsCase(args);
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

  private void doRank(final BufferedReader br, final DoublesSketch sketch) {
    String itemStr = "";
    try {
      while ((itemStr = br.readLine()) != null) {
        final double item = Double.parseDouble(itemStr);
        sketch.update(item);
      }
    } catch (final IOException | NumberFormatException e ) {
      printlnErr("Read Error: Item: " + itemStr + ", " + br.toString());
      throw new RuntimeException(e);
    }
    final int ranks = 101;
    final double[] valArr = sketch.getQuantiles(ranks);
    println("Rank" + TAB + "Value");
    for (int i = 0; i < ranks; i++) {
      final String r = String.format("%.2f",(double)i / ranks);
      println(r + TAB + valArr[i]);
    }
  }

  private void parseHist(final String[] args) {
    final DoublesSketchBuilder bldr = new DoublesSketchBuilder();
    final DoublesSketch sketch;
    final int argsCase = parseArgsCase(args);
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

  private void doHist(final BufferedReader br, final DoublesSketch sketch) {
    String itemStr = "";
    try {
      while ((itemStr = br.readLine()) != null) {
        final double item = Double.parseDouble(itemStr);
        sketch.update(item);
      }
    } catch (final IOException | NumberFormatException e ) {
      printlnErr("Read Error: Item: " + itemStr + ", " + br.toString());
      throw new RuntimeException(e);
    }
    final int splitPoints = 30;
    final long n = sketch.getN();
    final double[] splitsArr = getEvenSplits(sketch, splitPoints);
    final double[] histArr = sketch.getPMF(splitsArr);
    println("Value" + TAB + "Freq");
    //int histArrLen = histArr.length; //one larger than splitsArr
    final double min = sketch.getMinValue();
    String splitVal = String.format("%,f", min);
    String freqVal = String.format("%,d", (long)(histArr[0] * n));
    println(splitVal + TAB + freqVal);
    for (int i = 0; i < splitsArr.length; i++) {
      splitVal = String.format("%,f", splitsArr[i] * n);
      freqVal = String.format("%,d", (long)(histArr[i + 1] * n));
      println(splitVal + TAB + freqVal);
    }
  }

  private void parseLogHist(final String[] args) {
    final DoublesSketchBuilder bldr = new DoublesSketchBuilder();
    final DoublesSketch sketch;
    final int argsCase = parseArgsCase(args);
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

  private void doLogHist(final BufferedReader br, final DoublesSketch sketch) {
    String itemStr = "";
    try {
      while ((itemStr = br.readLine()) != null) {
        final double item = Double.parseDouble(itemStr);
        if (Double.isNaN(item) || (item <= 0.0)) { continue; }
        sketch.update(item);
      }
    } catch (final IOException | NumberFormatException e ) {
      printlnErr("Read Error: Item: " + itemStr + ", " + br.toString());
      throw new RuntimeException(e);
    }
    final int splitPoints = 30;
    final long n = sketch.getN();
    final double[] splitsArr = getLogSplits(sketch, splitPoints);
    final double[] histArr = sketch.getPMF(splitsArr);
    println("Value" + TAB + "Freq");
    //int histArrLen = histArr.length; //one larger than splitsArr
    final double min = sketch.getMinValue();
    String splitVal = String.format("%,f", min);
    String freqVal = String.format("%,d", (long)(histArr[0] * n));
    println(splitVal + TAB + freqVal);
    for (int i = 0; i < splitsArr.length; i++) {
      splitVal = String.format("%,f", splitsArr[i] * n);
      freqVal = String.format("%,d", (long)(histArr[i + 1] * n));
      println(splitVal + TAB + freqVal);
    }
  }

  private void parseFreq(final String[] args) {
    final ItemsSketch<String> sketch;
    final int defaultSize = 1 << 17; //128K
    final int argsCase = parseArgsCase(args);
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

  private void doFreq(final BufferedReader br, final ItemsSketch<String> sketch) {
    String itemStr = "";
    try {
      while ((itemStr = br.readLine()) != null) {
        sketch.update(itemStr);
      }
    } catch (final IOException e ) {
      printlnErr("Read Error: Item: " + itemStr + ", " + br.toString());
      throw new RuntimeException(e);
    }
    //NFP is a subset of NFN
    final ItemsSketch.Row<String>[] rowArr = sketch.getFrequentItems(ErrorType.NO_FALSE_POSITIVES);
    final int len = rowArr.length;
    println("Qualifying Rows: " + len);
    println(Row.getRowHeader());
    for (int i = 0; i < len; i++) {
      println((i + 1) + rowArr[i].toString());
    }
  }

  private static double[] getEvenSplits(final DoublesSketch sketch, final int splitPoints) {
    final double min = sketch.getMinValue();
    final double max = sketch.getMaxValue();
    return getSplits(min, max, splitPoints);
  }

  private static double[] getLogSplits(final DoublesSketch sketch, final int splitPoints) {
    final double min = sketch.getMinValue();
    final double max = sketch.getMaxValue();
    final double logMin = log10(min);
    final double logMax = log10(max);
    final double[] logArr = getSplits(logMin, logMax, splitPoints);
    final double[] expArr = new double[logArr.length];
    for (int i = 0; i < logArr.length; i++) {
      expArr[i] = pow(10.0, logArr[i]);
    }
    return expArr;
  }

  private static double[] getSplits(final double min, final double max, final int splitPoints) {
    final double range = max - min;
    final double delta = range / (splitPoints + 1);
    final double[] splits = new double[splitPoints];
    for (int i = 0; i < splitPoints; i++) {
      splits[i] = delta * (i + 1);
    }
    return splits;
  }

  private static boolean isNumeric(final String token) {
    for (char c : token.toCharArray()) {
        if (!Character.isDigit(c)) { return false; }
    }
    return true;
  }

  private static BufferedReader getBR(final String token) {
    BufferedReader br = null;
    try {
      if ((token == null) || (token.length() == 0)) {
        br = new BufferedReader(new InputStreamReader(System.in, UTF_8));
      } else {
        br = new BufferedReader(new InputStreamReader(new FileInputStream(token), UTF_8));
      }
    } catch (final FileNotFoundException e) {
      printlnErr("File Not Found: " + token);
      System.exit(1);
    }
    return br;
  }

  private void uniqHelp() {
    final StringBuilder sb = new StringBuilder();
    sb.append(BOLD + "UNIQ SYNOPSIS" + OFF).append(LS);
    sb.append("    sketch uniq help").append(LS);
    sb.append("    sketch uniq [SIZE] [FILE]").append(LS);
    println(sb.toString());
  }

  private void rankHelp() {
    final StringBuilder sb = new StringBuilder();
    sb.append(BOLD + "RANK SYNOPSIS" + OFF).append(LS);
    sb.append("    sketch rank help").append(LS);
    sb.append("    sketch rank [SIZE] [FILE]").append(LS);
    println(sb.toString());
  }

  private void histHelp() {
    final StringBuilder sb = new StringBuilder();
    sb.append(BOLD + "HIST SYNOPSIS" + OFF).append(LS);
    sb.append("    sketch hist help").append(LS);
    sb.append("    sketch hist [SIZE] [FILE]").append(LS);
    println(sb.toString());
  }

  private void logHistHelp() {
    final StringBuilder sb = new StringBuilder();
    sb.append(BOLD + "LOGHIST SYNOPSIS" + OFF).append(LS);
    sb.append("    sketch loghist help").append(LS);
    sb.append("    sketch loghist [SIZE] [FILE]").append(LS);
    println(sb.toString());
  }

  private void freqHelp() {
    final StringBuilder sb = new StringBuilder();
    sb.append(BOLD + "FREQ SYNOPSIS" + OFF).append(LS);
    sb.append("    sketch freq help").append(LS);
    sb.append("    sketch freq [SIZE] [FILE]").append(LS);
    println(sb.toString());
  }

  /**
   * Prints the help summaries.
   */
  public void help() {
    final StringBuilder sb = new StringBuilder();
    sb.append(BOLD + "NAME" + OFF).append(LS);
    sb.append("    sketch - sketch Uniques, Quantiles, Histograms, or Frequent Items.").append(LS);
    sb.append(BOLD + "SYNOPSIS" + OFF).append(LS);
    sb.append("    sketch (this help)").append(LS);
    sb.append("    sketch TYPE help").append(LS);
    sb.append("    sketch TYPE [SIZE] [FILE]").append(LS);
    sb.append(BOLD + "DESCRIPTION" + OFF).append(LS);
    sb.append("    Write a sketch(TYPE, SIZE) of FILE to standard output.").append(LS);
    sb.append("    TYPE is required.").append(LS);
    sb.append("    If SIZE is omitted, internal defaults are used.").append(LS);
    sb.append("    If FILE is omitted, Standard In is assumed.").append(LS);
    sb.append(BOLD + "TYPE DESCRIPTION" + OFF).append(LS);
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

  private static void printlnErr(final String s) {
    System.err.println(s);
  }

  private void println(final String s) {
    if (disablePrint) { return; }
    System.out. println(s);
  }
}
