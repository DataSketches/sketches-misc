package com.yahoo.sketches.cmd;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;

import com.yahoo.memory.Memory;
import com.yahoo.sketches.hll.HllSketch;
import com.yahoo.sketches.hll.TgtHllType;
import com.yahoo.sketches.hll.Union;

public class HllCL extends CommandLine <HllSketch> {
  HllCL(){
    super();
    // input options
    options.addOption(OptionBuilder.withDescription("parameter lgK")
                                     .hasArg()
                                     .create("k"));

    // output options
    options.addOption(OptionBuilder.withLongOpt("upper-bound")
                                     .withDescription("upper bound on the estimate")
                                     .create("u"));

    options.addOption(OptionBuilder.withLongOpt("lower-bound")
                                     .withDescription("lower bound on the estimate")
                                     .create("l"));
  }

  @Override
  protected void showHelp(){
        HelpFormatter helpf = new HelpFormatter();
        helpf.setOptionComparator(null);
        helpf.printHelp( "ds hll", options);
  }


  @Override
  protected void buildSketch(){
    HllSketch sketch;
    if( cmd.hasOption("k")){
      sketch =  new HllSketch(Integer.parseInt(cmd.getOptionValue("k"))); // user defined lgK
    }else{
      sketch =  new HllSketch(10); // default lgK is 10
    }
    sketches.add(sketch);
  }

  @Override
  protected void updateSketch(BufferedReader br){
    String itemStr = "";
    HllSketch sketch = sketches.get(sketches.size() - 1);
    try {
      while ((itemStr = br.readLine()) != null) {
        final int item = Integer.parseInt(itemStr);
        sketch.update(item);
      }
    } catch (final IOException | NumberFormatException e ) {
      printlnErr("Read Error: Item: " + itemStr + ", " + br.toString());
      throw new RuntimeException(e);
    }
  }

  @Override
  protected HllSketch deserializeSketch(byte[] bytes){
    return HllSketch.heapify(Memory.wrap(bytes));
  }

  @Override
  protected byte[] serializeSketch(HllSketch sketch){
    return sketch.toCompactByteArray();
  }

  @Override
  protected void mergeSketches(){
      int lgk = sketches.get(sketches.size() - 1).getLgConfigK();
      Union union = new Union(lgk);
      for (HllSketch sketch: sketches) {
        union.update(sketch);
      }
      sketches.add(union.getResult(TgtHllType.HLL_4));
  }

  @Override
  protected void queryCurrentSketch(){
      HllSketch sketch =  sketches.get(sketches.size()-1);
      if (cmd.hasOption("u")){
        System.out.println(sketch.getLowerBound(2));
        return;
      }
      if (cmd.hasOption("l")){
        System.out.println(sketch.getUpperBound(2));
        return;
      }

      //default output is estimated number of uniques
      System.out.println(sketch.getEstimate());
      return;

  }


}
