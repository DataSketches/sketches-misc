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
import java.util.Arrays; 

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.CommandLineParser;

import com.yahoo.sketches.quantiles.DoublesSketch;
import com.yahoo.sketches.quantiles.DoublesSketchBuilder;
import com.yahoo.sketches.quantiles.DoublesUnion;
import com.yahoo.sketches.quantiles.UpdateDoublesSketch;


import com.yahoo.memory.Memory;


  public class QuantilesCL extends CommandLine <UpdateDoublesSketch> {
    QuantilesCL(){
      super();
      // input options
      options.addOption(OptionBuilder.withDescription("parameter k")
                                     .hasArg()
                                     .create("k"));
      
      // output options
      options.addOption(OptionBuilder.withLongOpt("rank2value")
                                     .withDescription("query values with ranks DOUBLES")
                                     .hasArgs(Option.UNLIMITED_VALUES)
                                     .withArgName("DOUBLES")
                                     .create("R"));
      
      options.addOption(OptionBuilder.withLongOpt("rank2value-file")
                                     .withDescription("query values with ranks from FILE")
                                     .hasArg()
                                     .withArgName("FILE")
                                     .create("r"));
      
      options.addOption(OptionBuilder.withLongOpt("value2rank")
                                     .withDescription("query ranks with values DOUBLES")
                                     .hasArgs(Option.UNLIMITED_VALUES)
                                     .withArgName("DOUBLES")
                                     .create("V"));

      options.addOption(OptionBuilder.withLongOpt("value2rank-file")
                                     .withDescription("query ranks with values from FILE")
                                     .hasArg()
                                     .withArgName("FILE")
                                     .create("v"));

      options.addOption(OptionBuilder.withLongOpt("median")
                                     .withDescription("query median")
                                     .create("m"));
      
      options.addOption(OptionBuilder.withLongOpt("histogram-bars-number")
                                     .withDescription("number of bars in the histogram")
                                     .hasArg()
                                     .withArgName("INT")
                                     .create("b"));

      options.addOption(OptionBuilder.withLongOpt("query-histogram")
                                     .withDescription("query histogram")
                                     .create("h"));

      options.addOption(OptionBuilder.withLongOpt("query-loghistogram")
                                     .withDescription("query log scale histogram")
                                     .create("lh"));
    }

  protected void showHelp(){
        HelpFormatter helpf = new HelpFormatter();
        helpf.setOptionComparator(null);

        helpf.printHelp( "ds quant", options);  
  }


  protected void buildSketch(){
    UpdateDoublesSketch sketch;
    if(cmd.hasOption("k")){
      sketch = UpdateDoublesSketch.builder()
                                  .setK(Integer.parseInt(cmd.getOptionValue("k")))
                                  .build();  // user defined k
    }else{
      sketch = UpdateDoublesSketch.builder().build();                                           // default k
    }
    sketches.add(sketch);
  }
  
  protected void updateSketch(BufferedReader br){
    String itemStr = "";
    UpdateDoublesSketch sketch = sketches.get(sketches.size() - 1);
    try {
      while ((itemStr = br.readLine()) != null) {
        final double item = Double.parseDouble(itemStr);
        sketch.update(item);
      }
    } catch (final IOException | NumberFormatException e ) {
      printlnErr("Read Error: Item: " + itemStr + ", " + br.toString());
      throw new RuntimeException(e);
    }
  };

  protected UpdateDoublesSketch deserializeSketch(byte[] bytes){
    return UpdateDoublesSketch.heapify(Memory.wrap(bytes));  //still questionable
  };

  protected byte[] serializeSketch(UpdateDoublesSketch sketch){
    return sketch.toByteArray();
  }

  protected void mergeSketches(){
      DoublesUnion union = DoublesUnion.builder().build(); // default k=128
      for (UpdateDoublesSketch sketch: sketches) {
        union.update(sketch);
      }
      sketches.add(union.getResult());
  }

  protected void queryCurrentSketch(){
    if (sketches.size()>0) {
      UpdateDoublesSketch sketch = sketches.get(sketches.size() -1);
  

      if (cmd.hasOption("h")){
        int splitPoints = 30; 
        if (cmd.hasOption("b")) {
          splitPoints = Integer.parseInt(cmd.getOptionValue("b")); 
        }
        final long n = sketch.getN();
        final double[] splitsArr = getEvenSplits(sketch, splitPoints);
        final double[] histArr = sketch.getPMF(splitsArr);
        println("Value" + TAB + "Freq");
        final double min = sketch.getMinValue();
        String splitVal = String.format("%,f", min);
        String freqVal = String.format("%,d", (long)(histArr[0] * n));
        println(splitVal + TAB + freqVal);
        for (int i = 0; i < splitsArr.length; i++) {
          splitVal = String.format("%,f", splitsArr[i]  );
          freqVal = String.format("%,d", (long)(histArr[i + 1] * n));
          println(splitVal + TAB + freqVal);
        }
        return;
      }

      if (cmd.hasOption("lh")){
        int splitPoints = 30;
        if (cmd.hasOption("b")) {
          splitPoints = Integer.parseInt(cmd.getOptionValue("b")); 
        }
        final long n = sketch.getN();
        final double[] splitsArr = getLogSplits(sketch, splitPoints);
        final double[] histArr = sketch.getPMF(splitsArr);
        println("Value" + TAB + "Freq");
        final double min = sketch.getMinValue();
        String splitVal = String.format("%,f", min);
        String freqVal = String.format("%,d", (long)(histArr[0] * n));
        println(splitVal + TAB + freqVal);
        for (int i = 0; i < splitsArr.length; i++) {
          splitVal = String.format("%,f", splitsArr[i] );
          freqVal = String.format("%,d", (long)(histArr[i + 1] * n));
          println(splitVal + TAB + freqVal);
        }
        return;
      }
      
      if (cmd.hasOption("m")){       
          final String median = String.format("%.2f",sketch.getQuantile(0.5));
          println(median);
        return;
      }

      if (cmd.hasOption("R")){      
        String[] items = cmd.getOptionValues("R");
        for (int i=0; i<items.length; i++) {
          final String quant = String.format("%.2f",sketch.getQuantile(Double.parseDouble(items[i])));
          println(items[i] + TAB + quant);
        }
        return;
      }





      if (cmd.hasOption("r")){
        String[] items = queryFileReader(cmd.getOptionValue("r"));
        println("Rank" + TAB + "Value");      
        for (String item: items){
            final String quant = String.format("%.2f",sketch.getQuantile(Double.parseDouble(item)));
            println(item + TAB + quant);
        }
        return;
      }


      if (cmd.hasOption("V")){      
        String[] items = cmd.getOptionValues("V");
        double[] valuesArray = Arrays.stream(items).mapToDouble(Double::parseDouble).toArray(); 
        Arrays.sort(valuesArray);
        double[] cdf =  sketch.getCDF(valuesArray);
        for (int i=0; i < valuesArray.length ; i++) {
          println(String.format("%.2f",valuesArray[i]) + TAB + String.format("%.6f",cdf[i]));
        }
        return;
      }


      if (cmd.hasOption("v")){
        String[] items = queryFileReader(cmd.getOptionValue("v"));
        double[] valuesArray = Arrays.stream(items).mapToDouble(Double::parseDouble).toArray();
        double[] cdf =  sketch.getCDF(valuesArray);
        for (int i=0; i < valuesArray.length ; i++) {
          println(String.format("%.2f",valuesArray[i]) + TAB + String.format("%.6f",cdf[i]));
        }  
        return;
      }

      //default output - all percentiles
      final int ranks = 101;
      final double[] valArr = sketch.getQuantiles(ranks);
      println("Rank" + TAB + "Value");
      for (int i = 0; i < ranks; i++) {
        final String r = String.format("%.2f",(double)i / ranks);
        println(r + TAB + valArr[i]);
      }
      return; 
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

}
