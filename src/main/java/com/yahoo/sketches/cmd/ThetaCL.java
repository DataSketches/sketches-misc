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

import com.yahoo.sketches.theta.Sketch;
import com.yahoo.sketches.theta.Sketches;
import com.yahoo.sketches.theta.CompactSketch;
import com.yahoo.sketches.theta.UpdateSketch;
import com.yahoo.sketches.theta.Union;
import com.yahoo.sketches.theta.Intersection;
import com.yahoo.sketches.theta.SetOperation;
import com.yahoo.sketches.theta.UpdateSketchBuilder;
import com.yahoo.sketches.theta.AnotB;


import com.yahoo.memory.Memory;

  
public class ThetaCL extends CommandLine <Sketch> {
   protected UpdateSketch updateSketch;
   ThetaCL(){
      super();
      // input options
      options.addOption(OptionBuilder.withDescription("parameter k")
                                     .hasArg()
                                     .create("k"));
      
      // sketch level operators
      options.addOption(OptionBuilder.withLongOpt("merge-intersection")
                                     .withDescription("find intersection of sketches")
                                     .create("i")); 

      options.addOption(OptionBuilder.withLongOpt("merge-set-minus")
                                     .withDescription("from first sketch subtract union of all others")
                                     .create("m"));        

      

      // output options
      options.addOption(OptionBuilder.withLongOpt("upper-bound")
                                     .withDescription("upper bound on the estimate")
                                     .create("u"));        

      options.addOption(OptionBuilder.withLongOpt("lower-bound")
                                     .withDescription("lower bound on the estimate")
                                     .create("l"));        
   }

  protected void showHelp(){
        HelpFormatter helpf = new HelpFormatter();
        helpf.setOptionComparator(null);
        helpf.printHelp( "ds theta", options);  
  }

  
  protected void buildSketch(){
    final UpdateSketchBuilder bldr = Sketches.updateSketchBuilder();
    if( cmd.hasOption("k")){
      bldr.setNominalEntries(Integer.parseInt(cmd.getOptionValue("k")));  // user defined k
    }
    updateSketch = bldr.build();                                           
  }
  
  protected void updateSketch(BufferedReader br){
    if (sketches.size() > 0){
      buildSketch();
    }
    String itemStr = "";
    try {
      while ((itemStr = br.readLine()) != null) {
        updateSketch.update(itemStr);
      }
    }catch (final IOException e) {
      printlnErr("Read Error: Item: " + itemStr + ", " + br.toString());
      throw new RuntimeException(e);
    }
    if (sketches.size() > 0){
      Union union = SetOperation.builder().buildUnion();
      union.update(sketches.get(sketches.size()-1));
      union.update(updateSketch);
      sketches.add(union.getResult());   
    }else{
      sketches.add(updateSketch.compact());
    }
  };

  protected Sketch deserializeSketch(byte[] bytes){
    return Sketch.wrap(Memory.wrap(bytes));
  };

  protected byte[] serializeSketch(Sketch sketch){
    return sketch.toByteArray();
  }



  protected void mergeSketches(){
      
      if (cmd.hasOption("i")){
        Intersection intersection = SetOperation.builder().buildIntersection();
        for (Sketch sketch: sketches) {
          intersection.update(sketch);
        }
        sketches.add(intersection.getResult());
        return;
      }

      if (cmd.hasOption("m")){
        Union union = SetOperation.builder().buildUnion();
        for (int i = 1; i < sketches.size(); i++){
          union.update(sketches.get(i));
        }

        AnotB aNotB = Sketches.setOperationBuilder().buildANotB();
        aNotB.update(sketches.get(0),union.getResult());
        sketches.add(aNotB.getResult());
        return;
      }

      Union union = SetOperation.builder().buildUnion();  //default merge is union
      for (Sketch sketch: sketches) {
        union.update(sketch);
      }
      sketches.add(union.getResult()); 
      return; 

  }

  protected void queryCurrentSketch(){
    if (sketches.size() > 0) {
      Sketch sketch = sketches.get(sketches.size() - 1);
      if (cmd.hasOption("l")){
          System.out.format("%f\n",sketch.getUpperBound(2));
      }else if (cmd.hasOption("u")){
          System.out.format("%f\n",sketch.getLowerBound(2));
      }else{
          System.out.format("%f\n",sketch.getEstimate()); // default output
      }  
    }
  }
}
