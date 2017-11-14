/*
 * Copyright 2016, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.cmd;

import static com.yahoo.sketches.Util.TAB;
import static java.lang.Math.log10;
import static java.lang.Math.pow;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;

import com.yahoo.memory.Memory;
import com.yahoo.sketches.quantiles.DoublesSketch;
import com.yahoo.sketches.quantiles.DoublesUnion;
import com.yahoo.sketches.quantiles.UpdateDoublesSketch;

  public class QuantilesCL extends CommandLine<UpdateDoublesSketch> {

    QuantilesCL() {
      super();
      // input options
      options.addOption(Option.builder("k")
          .desc("parameter k")
          .hasArg()
          .build());
      // output options
      options.addOption(Option.builder("R")
          .longOpt("rank2value")
          .desc("query values with ranks DOUBLES")
          .hasArgs() //unlimited
          .argName("DOUBLES")
          .build());
      options.addOption(Option.builder("r")
          .longOpt("rank2value-file")
          .desc("query values with ranks from FILE")
          .hasArg()
          .argName("FILE")
          .build());
      options.addOption(Option.builder("V")
          .longOpt("value2rank")
          .desc("query ranks with values DOUBLES")
          .hasArgs() //unlimited
          .argName("DOUBLES")
          .build());
      options.addOption(Option.builder("v")
          .longOpt("value2rank-file")
          .desc("query ranks with values from FILE")
          .hasArg()
          .argName("FILE")
          .build());
      options.addOption(Option.builder("m")
          .longOpt("median")
          .desc("query median")
          .build());
      options.addOption(Option.builder("b")
          .longOpt("histogram-bars-number")
          .desc("number of bars in the histogram")
          .hasArg()
          .argName("INT")
          .build());
      options.addOption(Option.builder("h")
          .longOpt("query-histogram")
          .desc("query histogram")
          .build());
      options.addOption(Option.builder("lh")
          .longOpt("query-loghistogram")
          .desc("query log scale histogram")
          .build());
    }

  @Override
  protected void showHelp() {
        final HelpFormatter helpf = new HelpFormatter();
        helpf.setOptionComparator(null);
        helpf.printHelp( "ds quant", options);
  }

  @Override
  protected void buildSketch() {
    final UpdateDoublesSketch sketch;
    if (cmd.hasOption("k")) {
      sketch = DoublesSketch.builder()
                                  .setK(Integer.parseInt(cmd.getOptionValue("k")))
                                  .build();  // user defined k
    } else {
      sketch = DoublesSketch.builder().build(); // default k
    }
    sketches.add(sketch);
  }

  @Override
  protected void updateSketch(final BufferedReader br) {
    String itemStr = "";
    final UpdateDoublesSketch sketch = sketches.get(sketches.size() - 1);
    try {
      while ((itemStr = br.readLine()) != null) {
        final double item = Double.parseDouble(itemStr);
        sketch.update(item);
      }
    } catch (final IOException | NumberFormatException e ) {
      printlnErr("Read Error: Item: " + itemStr + ", " + br.toString());
      throw new RuntimeException(e);
    }
  }

  @Override
  protected UpdateDoublesSketch deserializeSketch(final byte[] bytes) {
    return UpdateDoublesSketch.heapify(Memory.wrap(bytes));  //still questionable
  }

  @Override
  protected byte[] serializeSketch(final UpdateDoublesSketch sketch) {
    return sketch.toByteArray();
  }

  @Override
  protected void mergeSketches() {
    final DoublesUnion union = DoublesUnion.builder().build(); // default k=128
      for (UpdateDoublesSketch sketch: sketches) {
        union.update(sketch);
      }
      sketches.add(union.getResult());
  }

  @Override
  protected void queryCurrentSketch() {
    if (sketches.size() > 0) {
      final UpdateDoublesSketch sketch = sketches.get(sketches.size() - 1);

      if (cmd.hasOption("h")) {
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

      if (cmd.hasOption("lh")) {
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

      if (cmd.hasOption("m")) {
          final String median = String.format("%.2f",sketch.getQuantile(0.5));
          println(median);
        return;
      }

      if (cmd.hasOption("R")) {
        final String[] items = cmd.getOptionValues("R");
        for (int i = 0; i < items.length; i++) {
          final String quant = String.format("%.2f",sketch.getQuantile(Double.parseDouble(items[i])));
          println(items[i] + TAB + quant);
        }
        return;
      }

      if (cmd.hasOption("r")) {
        final String[] items = queryFileReader(cmd.getOptionValue("r"));
        println("Rank" + TAB + "Value");
        for (String item: items) {
            final String quant = String.format("%.2f",sketch.getQuantile(Double.parseDouble(item)));
            println(item + TAB + quant);
        }
        return;
      }

      if (cmd.hasOption("V")) {
        final String[] items = cmd.getOptionValues("V");
        final double[] valuesArray = Arrays.stream(items).mapToDouble(Double::parseDouble).toArray();
        Arrays.sort(valuesArray);
        final double[] cdf =  sketch.getCDF(valuesArray);
        for (int i = 0; i < valuesArray.length ; i++) {
          println(String.format("%.2f",valuesArray[i]) + TAB + String.format("%.6f",cdf[i]));
        }
        return;
      }

      if (cmd.hasOption("v")) {
        final String[] items = queryFileReader(cmd.getOptionValue("v"));
        final double[] valuesArray = Arrays.stream(items).mapToDouble(Double::parseDouble).toArray();
        final double[] cdf =  sketch.getCDF(valuesArray);
        for (int i = 0; i < valuesArray.length ; i++) {
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
