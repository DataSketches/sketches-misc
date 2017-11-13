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

    options.addOption(OptionBuilder.withLongOpt("upper-bound")
                                     .withDescription("lower bound on the estimate")
                                     .create("l"));        
  }

  protected void showHelp(){
        HelpFormatter helpf = new HelpFormatter();
        helpf.setOptionComparator(null);
        helpf.printHelp( "ds hll", options);  
  }


  protected void buildSketch(){
    HllSketch sketch;
    if( cmd.hasOption("k")){
      sketch =  new HllSketch(Integer.parseInt(cmd.getOptionValue("k"))); // user defined lgK
    }else{  
      sketch =  new HllSketch(10); // default lgK is 10
    }
    sketches.add(sketch);
  }
  
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

  protected HllSketch deserializeSketch(byte[] bytes){
    return HllSketch.heapify(Memory.wrap(bytes));
  }

  protected byte[] serializeSketch(HllSketch sketch){
    return sketch.toCompactByteArray();
  }

  protected void mergeSketches(){
      int lgk = sketches.get(sketches.size() - 1).getLgConfigK();
      Union union = new Union(lgk);
      for (HllSketch sketch: sketches) {
        union.update(sketch);
      }
      sketches.add(union.getResult(TgtHllType.HLL_4));
  }

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
