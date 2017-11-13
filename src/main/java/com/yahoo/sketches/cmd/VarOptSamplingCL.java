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
import com.yahoo.sketches.ArrayOfItemsSerDe;
import com.yahoo.sketches.sampling.SampleSubsetSummary;
import com.yahoo.sketches.sampling.VarOptItemsSamples;
import com.yahoo.sketches.sampling.VarOptItemsSketch;
import com.yahoo.sketches.sampling.VarOptItemsUnion;

import com.yahoo.memory.Memory;


public class VarOptSamplingCL extends CommandLine <VarOptItemsSketch<String> > {
  VarOptSamplingCL(){
    super();
    // input options
    options.addOption(OptionBuilder.withDescription("parameter k")
                                     .hasArg()
                                     .create("k"));
    // output options
    options.addOption(OptionBuilder.withDescription("query sketch samples (without counts)")
                                   .create("s"));

  }

  protected void showHelp(){
        HelpFormatter helpf = new HelpFormatter();
        helpf.setOptionComparator(null);
        helpf.printHelp( "ds vpsamp", options);  
  }


  protected void buildSketch(){
    final VarOptItemsSketch<String> sketch;
    if( cmd.hasOption("k")){
      sketch = VarOptItemsSketch.newInstance(Integer.parseInt(cmd.getOptionValue("k"))); // user defined k
    }else{  
      sketch = VarOptItemsSketch.newInstance(32); // default k is 32
    }
    sketches.add(sketch);
  }
  
  protected void updateSketch(BufferedReader br){
    String itemStr = "";
    VarOptItemsSketch<String>  sketch = sketches.get(sketches.size() - 1);
    try {
      while ((itemStr = br.readLine()) != null) {
        String[] tokens = itemStr.split("\\s+");
        if (tokens.length == 2) {
          sketch.update(tokens[1], Double.parseDouble(tokens[0]));
        }
      }
    } catch (final IOException | NumberFormatException e ) {
      printlnErr("Read Error: Item: " + itemStr + ", " + br.toString());
      throw new RuntimeException(e);
    }
  }

  protected VarOptItemsSketch<String>  deserializeSketch(byte[] bytes){
    // BufferReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes,UTF_8)));
    // String itemStr = "";
    // VarOptItemsSketch<String> sketch = VarOptItemsSketch.newInstance(Integer.parseInt(cmd.getOptionValue("k"))); // user defined k

    // try {
    //   while ((itemStr = br.readLine()) != null) {
    //     String[] tokens = itemStr.split("\\s+");
    //     if (tokens.length == 2) {
    //       sketch.update(tokens[1], Double.parseDouble(tokens[0]));
    //     }
    //   }
    // } catch (final IOException | NumberFormatException e ) {
    //   printlnErr("Read Error: Item: " + itemStr + ", " + br.toString());
    //   throw new RuntimeException(e);
    // } 
    return null; //VarOptItemsSketch.heapify(Memory.wrap(bytes), new ArrayOfItemsSerDe<String>());
  }

  protected byte[] serializeSketch(VarOptItemsSketch sketch){
      // String itemStr = "";
      // VarOptItemsSamples<String> samples = union.getResult().getSketchSamples();
      // for (VarOptItemsSamples<String>.WeightedSample ws : samples) {
      //   itemStr += ws.getItem() + "\t" + ws.getWeight() + "\n";
      // }
      // return itemStr.getBytes();
      return null; //
    // return sketch.toByteArray(new ArrayOfItemsSerDe<String>());
  }

  protected void mergeSketches(){
    //   int k = sketches.get(sketches.size()-1).getK();
    //   VarOptItemsUnion<String> union = VarOptItemsUnion.newInstance(k);
    //   for (VarOptItemsSketch  sketch: sketches) {
    //     union.update(sketch);
    //   }
    //   sketches.add(union.getResult());
  }

  protected void queryCurrentSketch(){
      VarOptItemsSketch<String>  sketch =  sketches.get(sketches.size()-1);
      VarOptItemsSamples<String> samples = sketch.getSketchSamples();
      
      if (cmd.hasOption("s")){
        for (VarOptItemsSamples<String>.WeightedSample ws : samples) {
          System.out.println(ws.getItem());
        }
        return;
      }

      for (VarOptItemsSamples<String>.WeightedSample ws : samples) {
        System.out.println(ws.getItem() + "\t" + ws.getWeight());
      }
  }
}
