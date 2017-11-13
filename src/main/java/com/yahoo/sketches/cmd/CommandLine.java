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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.ArrayList;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.CommandLineParser;

/**
 * Command line access to the basic sketch functions.  This is intentionally a very simple parser
 * with limited functionality that can be used for small experiments and for demos.
 * Although the sketching library can be used on a single machine, the more typical use case is on
 * large, highly distributed system architectures where a CLI is not of much use.
 */

public abstract class CommandLine <TSketch> {
  ArrayList<TSketch> sketches;
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

  CommandLine(){
    sketches = new ArrayList<TSketch>();
    options = new Options();
    
    options.addOption(OptionBuilder.withLongOpt("data-from-file")
                                   .withDescription("read data from FILE")
                                   .hasArg()
                                   .withArgName("FILE")
                                   .create("d"));

    options.addOption(OptionBuilder.withLongOpt("data-from-system-in")
                                   .withDescription("read data from system in")
                                   .create("D"));

    options.addOption(OptionBuilder.withLongOpt("sketch-input-file")
                                   .withDescription("read sketches from FILES")
                                   .hasArgs(Option.UNLIMITED_VALUES)
                                   .withArgName("FILES")
                                   .create("s"));
    
    options.addOption(OptionBuilder.withLongOpt("sketch-output-file")
                                   .withDescription("save sketch to FILE")
                                   .hasArg()
                                   .withArgName("FILE")
                                   .create("o"));

    options.addOption(OptionBuilder.withDescription("usage/help")
                                   .create("help"));

  }

  protected abstract void showHelp();
  protected abstract void buildSketch();
  protected abstract void updateSketch(BufferedReader br);
  protected abstract TSketch deserializeSketch(byte[] bytes);
  protected abstract byte[] serializeSketch(TSketch sketch);
  protected abstract void mergeSketches();
  protected abstract void queryCurrentSketch();
  

  protected void loadInputSketches(){
    
      try{
        String[] inputSketchesPathes = cmd.getOptionValues("s");
        for (int i=0; i<inputSketchesPathes.length; i++) {
          FileInputStream in = new FileInputStream(inputSketchesPathes[i]);
          byte[] bytes = new byte[in.available()];
          in.read(bytes);
          in.close();
          sketches.add(deserializeSketch(bytes));
        }
      }catch(Exception e) {      
        printlnErr("loadInputSketches Error: " + e.getMessage());
      }
  }

  protected void saveCurrentSketch(){
      try{
        FileOutputStream out = new FileOutputStream(cmd.getOptionValue("o"));
        out.write(serializeSketch(sketches.get(sketches.size() - 1)));
        out.close();
      }catch(Exception e) {      
        printlnErr("saveCurrentSketch Error: " + e.getMessage());
      }
  }

  protected void updateCurrentSketch(){
    try{  
      BufferedReader br = null;
      if(cmd.hasOption("D")){
        br = new BufferedReader(new InputStreamReader(System.in, UTF_8));
      }else if (cmd.hasOption("d")) {
        br = new BufferedReader(new InputStreamReader(new FileInputStream(cmd.getOptionValue("d")), UTF_8));
      }else {
        return;
      }
      String itemStr = "";
      updateSketch(br);
      updateFlag = true;   
    }catch(Exception e) {      
        printlnErr("updateCurrentSketch Error: " + e.getMessage());
    }
    
  }

  protected void runCommandLineUtil(final String[] args){
      updateFlag = false;
      parser = new DefaultParser();
      try{
          cmd = parser.parse(options, args);
          
          if(cmd.hasOption("help")){
            showHelp();
            return; 
          }
          
          if (cmd.hasOption("s")){
            loadInputSketches();
            updateFlag = true;
          }
          
          if (sketches.size() > 1)
            mergeSketches();
          else if (sketches.size() == 0)
            buildSketch();
          
          updateCurrentSketch();
          if (updateFlag){
            queryCurrentSketch();
            if (cmd.hasOption("o")) 
              saveCurrentSketch();  
          }else{
            showHelp();
          }
      }catch(Exception e) {      
              printlnErr("runCommandLineUtil Error: " + e.getMessage());
      }
  }
  
  public static void main(final String[] args) {
    if ((args == null) || (args.length == 0) || (args[0].isEmpty())) {
          help();
          return;
    }
    final String token1 = args[0].toLowerCase();
    switch (token1) {
        case "quant":     
          QuantilesCL qcl = new QuantilesCL();
          qcl.runCommandLineUtil(args);
          break;
        case "freq":     
          FrequenciesCL fcl = new FrequenciesCL();
          fcl.runCommandLineUtil(args);
          break;
        case "theta":     
          ThetaCL tcl = new ThetaCL();
          tcl.runCommandLineUtil(args);
          break;
        case "rsamp":     
          ReservoirSamplingCL rscl = new ReservoirSamplingCL();
          rscl.runCommandLineUtil(args);
          break;
        case "vpsamp":     
          VarOptSamplingCL vpscl = new VarOptSamplingCL();
          vpscl.runCommandLineUtil(args);
          break;
        case "hll":     
          HllCL hllcl = new HllCL();
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

  protected String[] queryFileReader(final String pathToFile){
    ArrayList<String> values = new ArrayList<String>();
    String itemStr = "";
    String[] valuesArray;
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(pathToFile)));
      while ((itemStr = in.readLine()) != null) {
        values.add(itemStr);
      }
      valuesArray = new String[values.size()];
      for (int i=0; i < valuesArray.length; i++){
          valuesArray[i] = values.get(i);
      }
    } catch (final IOException | NumberFormatException e ) {
      printlnErr("File Read Error: Item: " + itemStr );
      throw new RuntimeException(e);
    }
    return valuesArray;
  }

  public static void help() {
    final StringBuilder sb = new StringBuilder();
    sb.append(BOLD + "NAME" + OFF).append(LS);
    sb.append("    ds " + TAB + TAB + " sketches uniques, quantiles, pdf, and frequent items, ").append(LS);
    sb.append("       " + TAB + TAB + " implements reservoir and varopt sampling").append(LS);
    sb.append("       " + TAB + TAB + " for details refer to https://datasketches.github.io/ ").append(LS);
    sb.append(LS).append(BOLD + "SYNOPSIS" + OFF).append(LS);
    sb.append("    ds help").append(LS);
    sb.append("    ds ACTION").append(LS);
    sb.append("    ds ACTION -help").append(LS);
    sb.append(LS).append(BOLD + "ACTIONS" + OFF).append(LS);
    sb.append("    ds theta " + TAB + " theta sketch for estimating stream expression cardinalities").append(LS);
    sb.append("    ds quant " + TAB + " quantiles sketch for estimating distributions from a stream of values").append(LS);
    sb.append("    ds freq  " + TAB + " frequency sketch for finding the heavy hitter objects from a stream").append(LS);
    sb.append("    ds rsamp " + TAB + " reservoir sampling for uniform sampling of a stream into a fixed size space").append(LS);
    sb.append("    ds vpsamp" + TAB + " varopt sampling for uniform sampling from stream of weighted values").append(LS);
    sb.append("    ds hll   " + TAB + " Phillipe Flajolet’s HLL sketch for estimating the number of uniques from a stream of values").append(LS);
    println(sb.toString());
  }


}
