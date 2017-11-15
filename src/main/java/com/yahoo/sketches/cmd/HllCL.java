package com.yahoo.sketches.cmd;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;

import com.yahoo.memory.Memory;
import com.yahoo.sketches.hll.HllSketch;
import com.yahoo.sketches.hll.TgtHllType;
import com.yahoo.sketches.hll.Union;

public class HllCL extends CommandLine<HllSketch> {

  HllCL() {
    super();
    // input options
    options.addOption(Option.builder("k")
        .desc("parameter lgK")
        .hasArg()
        .build());
    // output options
    options.addOption(Option.builder("u")
        .desc("upper bound on the estimate")
        .longOpt("upper-bound")
        .build());
    options.addOption(Option.builder("l")
        .longOpt("lower-bound")
        .desc("lower bound on the estimate")
        .build());
  }

  @Override
  protected void showHelp() {
        final HelpFormatter helpf = new HelpFormatter();
        helpf.setOptionComparator(null);
        helpf.printHelp( "ds hll", options);
  }

  @Override
  protected void buildSketch() {
    final HllSketch sketch;
    if (cmd.hasOption("k")) {
      sketch =  new HllSketch(Integer.parseInt(cmd.getOptionValue("k"))); // user defined lgK
    } else {
      sketch =  new HllSketch(10); //default lgK is 10
    }
    sketches.add(sketch);
  }

  @Override
  protected void updateSketch(final BufferedReader br) {
    String itemStr = "";
    final HllSketch sketch = sketches.get(sketches.size() - 1);
    try {
      while ((itemStr = br.readLine()) != null) {
        sketch.update(itemStr);
      }
    } catch (final IOException e) {
      printlnErr("Read Error: Item: " + itemStr + ", " + br.toString());
      throw new RuntimeException(e);
    }
  }

  @Override
  protected HllSketch deserializeSketch(final byte[] bytes) {
    return HllSketch.heapify(Memory.wrap(bytes));
  }

  @Override
  protected byte[] serializeSketch(final HllSketch sketch) {
    return sketch.toCompactByteArray();
  }

  @Override
  protected void mergeSketches() {
    final int lgk = sketches.get(sketches.size() - 1).getLgConfigK();
    final Union union = new Union(lgk);
    for (HllSketch sketch: sketches) {
      union.update(sketch);
    }
    sketches.add(union.getResult(TgtHllType.HLL_4));
  }

  @Override
  protected void queryCurrentSketch() {
    final HllSketch sketch =  sketches.get(sketches.size() - 1);
    if (cmd.hasOption("u")) {
      System.out.println(sketch.getLowerBound(2));
      return;
    }
    if (cmd.hasOption("l")) {
      System.out.println(sketch.getUpperBound(2));
      return;
    }

    //default output is estimated number of uniques
    System.out.println(sketch.getEstimate());
    return;
  }

}
