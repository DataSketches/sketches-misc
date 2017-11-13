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
import com.yahoo.sketches.ArrayOfLongsSerDe;
import com.yahoo.sketches.sampling.ReservoirItemsSketch;
import com.yahoo.sketches.sampling.ReservoirItemsUnion;

import com.yahoo.memory.Memory;


public class ReservoirSamplingCL extends CommandLine <ReservoirItemsSketch<Long> > {
  ReservoirSamplingCL(){
    super();
    // input options
    options.addOption(OptionBuilder.withDescription("parameter k")
                                     .hasArg()
                                     .create("k"));
  }
  
  protected void showHelp(){
        HelpFormatter helpf = new HelpFormatter();
        helpf.setOptionComparator(null);
        helpf.printHelp( "ds rsamp", options);  
  }


  protected void buildSketch(){
    final ReservoirItemsSketch<Long> sketch;
    if( cmd.hasOption("k")){
      sketch = ReservoirItemsSketch.newInstance(Integer.parseInt(cmd.getOptionValue("k"))); ;  // user defined k
    }else{  
      sketch = ReservoirItemsSketch.newInstance(32); // default k is 32
    }
    sketches.add(sketch);
  }
  
  protected void updateSketch(BufferedReader br){
    String itemStr = "";
    ReservoirItemsSketch<Long> sketch = sketches.get(sketches.size() - 1);
    try {
      while ((itemStr = br.readLine()) != null) {
        final long item = Long.parseLong(itemStr);
        sketch.update(item);
      }
    } catch (final IOException | NumberFormatException e ) {
      printlnErr("Read Error: Item: " + itemStr + ", " + br.toString());
      throw new RuntimeException(e);
    }
  }

  protected ReservoirItemsSketch<Long>  deserializeSketch(byte[] bytes){
    return ReservoirItemsSketch.heapify(Memory.wrap(bytes), new ArrayOfLongsSerDe());
  }

  protected byte[] serializeSketch(ReservoirItemsSketch<Long> sketch){
    return sketch.toByteArray(new ArrayOfLongsSerDe());
  }

  protected void mergeSketches(){
      int k = sketches.get(sketches.size()-1).getK();
      ReservoirItemsUnion<Long> union = ReservoirItemsUnion.newInstance(k);
      for (ReservoirItemsSketch<Long>  sketch: sketches) {
        union.update(sketch);
      }
      sketches.add(union.getResult());
  }

  protected void queryCurrentSketch(){
      ReservoirItemsSketch<Long>  sketch =  sketches.get(sketches.size()-1);
      Long[] samples = sketch.getSamples();
      for (int i = 0; i < samples.length; i++) {
          System.out.println(samples[i]);
      }
      return;
  }
}
