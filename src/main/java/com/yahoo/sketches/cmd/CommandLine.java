/*
 * Copyright 2016, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.cmd;

import static com.yahoo.sketches.Util.LS;
import static com.yahoo.sketches.Util.TAB;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Command line access to the basic sketch functions.  This is intentionally a very simple parser
 * with limited functionality that can be used for small experiments and for demos.
 * Although the sketching library can be used on a single machine, the more typical use case is on
 * large, highly distributed system architectures where a CLI is not of much use.
 * @param <T> Sketch Type
 */

public abstract class CommandLine<T> {
  ArrayList<T> sketches;
  boolean updateFlag;
  String[] inputSketchesPathes;
  String dataInputFile;
  String outputSketchPath;
  private static final String BOLD = "\033[1m";
  private static final String OFF = "\033[0m";
  Options options;
  HelpFormatter help;
  CommandLineParser parser;
  org.apache.commons.cli.CommandLine cmd;

  CommandLine() {
    sketches = new ArrayList<>();
    options = new Options();
    options.addOption(Option.builder("d")
        .longOpt("data-from-file")
        .desc("read data from FILE")
        .hasArg()
        .argName("FILE")
        .build());
    options.addOption(Option.builder("D")
        .longOpt("data-from-system-in")
        .desc("read data from system in")
        .build());
    options.addOption(Option.builder("s")
        .longOpt("sketch-input-files")
        .desc("read sketches from FILES")
        .hasArgs() //unlimited
        .argName("FILES")
        .build());
    options.addOption(Option.builder("o")
        .longOpt("sketch-output-file")
        .desc("save sketch to FILE")
        .hasArg()
        .argName("FILE")
        .build());
    options.addOption(Option.builder("help")
        .desc("usage/help")
        .build());
  }

  protected abstract void showHelp();

  protected abstract void buildSketch();

  protected abstract void updateSketch(BufferedReader br);

  protected abstract T deserializeSketch(byte[] bytes);

  protected abstract byte[] serializeSketch(T sketch);

  protected abstract void mergeSketches();

  protected abstract void queryCurrentSketch();


  protected void loadInputSketches() {
      try {
        final String[] inputSketchesPathes = cmd.getOptionValues("s");
        for (int i = 0; i < inputSketchesPathes.length; i++) {
          try (FileInputStream in = new FileInputStream(inputSketchesPathes[i])) {
            final byte[] bytes = new byte[in.available()];
            in.read(bytes);
            sketches.add(deserializeSketch(bytes));
          }
        }
      } catch (final IOException e) {
        printlnErr("loadInputSketches Error: " + e.getMessage());
      }
  }

  protected void saveCurrentSketch() {
      try (FileOutputStream out = new FileOutputStream(cmd.getOptionValue("o"))) {
        out.write(serializeSketch(sketches.get(sketches.size() - 1)));
      } catch (final IOException e) {
        printlnErr("saveCurrentSketch Error: " + e.getMessage());
      }
  }

  protected void updateCurrentSketch() {
    try {
      if (cmd.hasOption("D")) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
            System.in, UTF_8))) {
          updateSketch(br);
          updateFlag = true;
        }
      }
      else if (cmd.hasOption("d")) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
            new FileInputStream(cmd.getOptionValue("d")), UTF_8))) {
          updateSketch(br);
          updateFlag = true;
        }
      }
      else { return; }
    } catch (final IOException e) {
      printlnErr("updateCurrentSketch Error: " + e.getMessage());
    }
  }

  protected void runCommandLineUtil(final String[] args) {
      updateFlag = false;
      parser = new DefaultParser();
      try {
          cmd = parser.parse(options, args);

          if (cmd.hasOption("help")) {
            showHelp();
            return;
          }

          if (cmd.hasOption("s")) {
            loadInputSketches();
            updateFlag = true;
          }

          if (sketches.size() > 1) {
            mergeSketches();
          } else if (sketches.size() == 0) {
            buildSketch();
          }

          updateCurrentSketch();
          if (updateFlag) {
            queryCurrentSketch();
            if (cmd.hasOption("o")) {
              saveCurrentSketch();
            }
          } else {
            showHelp();
          }
      } catch (final Exception e) {
              printlnErr("runCommandLineUtil Error: " + e.getMessage());
      }
  }

  /**
   * Entry point
   * @param args array of tokens
   */
  public static void main(final String[] args) {
    if ((args == null) || (args.length == 0) || (args[0].isEmpty())) {
          help();
          return;
    }
    final String token1 = args[0].toLowerCase();
    switch (token1) {
        case "quant":
          final QuantilesCL qcl = new QuantilesCL();
          qcl.runCommandLineUtil(args);
          break;
        case "freq":
          final FrequenciesCL fcl = new FrequenciesCL();
          fcl.runCommandLineUtil(args);
          break;
        case "theta":
          final ThetaCL tcl = new ThetaCL();
          tcl.runCommandLineUtil(args);
          break;
        case "rsamp":
          final ReservoirSamplingCL rscl = new ReservoirSamplingCL();
          rscl.runCommandLineUtil(args);
          break;
        case "vpsamp":
          final VarOptSamplingCL vpscl = new VarOptSamplingCL();
          vpscl.runCommandLineUtil(args);
          break;
        case "hll":
          final HllCL hllcl = new HllCL();
          hllcl.runCommandLineUtil(args);
          break;
        case "help":
          help();
          break;
        case "-help":
          help();
          break;
        default: {
            printlnErr("Unrecognized TYPE: " + token1);
            help();
        }
    }
    return;
  }

  protected static void printlnErr(final String s) {
    System.err.println(s);
  }

  protected static void println(final String s) {
    System.out.println(s);
  }

  protected String[] queryFileReader(final String pathToFile) {
    final ArrayList<String> values = new ArrayList<>();
    String itemStr = "";
    final String[] valuesArray;
    try (BufferedReader in =
        new BufferedReader(new InputStreamReader(new FileInputStream(pathToFile)))) {
      while ((itemStr = in.readLine()) != null) {
        if (itemStr.isEmpty()) { continue; }
        values.add(itemStr);
      }
      valuesArray = new String[values.size()];
      for (int i = 0; i < valuesArray.length; i++) {
          valuesArray[i] = values.get(i);
      }
    }
    catch (final IOException  e ) {
      printlnErr("File Read Error: Item: " + itemStr );
      throw new RuntimeException(e);
    }
    return valuesArray;
  }

  /**
   * Help function for level 0 tokens
   */
  public static void help() {
    final StringBuilder sb = new StringBuilder();
    sb.append(BOLD + "NAME" + OFF).append(LS);
    sb.append("    ds " + TAB + TAB
        + " sketches uniques, quantiles, pdf, and frequent items, ").append(LS);
    sb.append("       " + TAB + TAB + " implements reservoir and varopt sampling").append(LS);
    sb.append("       " + TAB + TAB
        + " for details refer to https://datasketches.github.io/ ").append(LS);
    sb.append(LS).append(BOLD + "SYNOPSIS" + OFF).append(LS);
    sb.append("    ds help").append(LS);
    sb.append("    ds ACTION").append(LS);
    sb.append("    ds ACTION -help").append(LS);
    sb.append(LS).append(BOLD + "ACTIONS" + OFF).append(LS);
    sb.append("    ds theta " + TAB
        + " theta sketch for estimating stream expression cardinalities").append(LS);
    sb.append("    ds quant " + TAB
        + " quantiles sketch for estimating distributions from a stream of values").append(LS);
    sb.append("    ds freq  " + TAB
        + " frequency sketch for finding the heavy hitter objects from a stream").append(LS);
    sb.append("    ds rsamp " + TAB
        + " reservoir sampling for uniform sampling of a stream into a fixed size space").append(LS);
    sb.append("    ds vpsamp" + TAB
        + " varopt sampling for uniform sampling from stream of weighted values").append(LS);
    sb.append("    ds hll   " + TAB
        + " Phillipe Flajolet’s HLL sketch for estimating the number of uniques"
        + " from a stream of values").append(LS);
    println(sb.toString());
  }

}
