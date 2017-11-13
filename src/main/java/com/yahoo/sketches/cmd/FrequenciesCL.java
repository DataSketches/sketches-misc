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
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;

import com.yahoo.sketches.ArrayOfStringsSerDe;
import com.yahoo.sketches.frequencies.ErrorType;
import com.yahoo.sketches.frequencies.ItemsSketch;

import com.yahoo.memory.Memory;


  public class FrequenciesCL extends CommandLine <ItemsSketch<String> > {
    FrequenciesCL(){
      super();
      // input options
      options.addOption(OptionBuilder.withDescription("parameter k")
                                     .hasArg()
                                     .create("k"));
     
      // output options
      options.addOption(OptionBuilder.withLongOpt("topk-ids")
                                     .withDescription("query identities for top k frequent items")
                                     .create("i"));
      
      
      options.addOption(OptionBuilder.withLongOpt("id2freq")
                                     .withDescription("query frequencies for items with given ID")
                                     .hasArgs(Option.UNLIMITED_VALUES)
                                     .withArgName("ID")
                                     .create("F"));
      
      options.addOption(OptionBuilder.withLongOpt("id2freq-file")
                                     .withDescription("query frequencies for items with ids from FILE")
                                     .hasArg()
                                     .withArgName("FILE")
                                     .create("f"));         
  
    }
  
  protected void showHelp(){
        HelpFormatter helpf = new HelpFormatter();
        helpf.setOptionComparator(null);
        helpf.printHelp( "ds freq", options);
  }


  protected void buildSketch(){
    final ItemsSketch<String> sketch;
    final int defaultSize = 1 << 17; //128K
    if( cmd.hasOption("k")){
            sketch = new ItemsSketch<String>(Integer.parseInt(cmd.getOptionValue("k")));  // user defined k
    }else{
            sketch = new ItemsSketch<String>(defaultSize);                                           // default k
    }
    sketches.add(sketch);
  }
  
  protected void updateSketch(BufferedReader br){
    final ItemsSketch<String> sketch = sketches.get(sketches.size() - 1);
    String itemStr = "";
    try {
      while ((itemStr = br.readLine()) != null) {
        sketch.update(itemStr);
      }
    } catch (final IOException e ) {
      printlnErr("Read Error: Item: " + itemStr + ", " + br.toString());
      throw new RuntimeException(e);
    }
  };

  protected ItemsSketch<String> deserializeSketch(byte[] bytes){
    return ItemsSketch.getInstance(Memory.wrap(bytes), new ArrayOfStringsSerDe());
  };

  protected byte[] serializeSketch(ItemsSketch<String> sketch){
    return sketch.toByteArray(new ArrayOfStringsSerDe());
  }

  protected void mergeSketches(){
      ItemsSketch<String> union = sketches.get(sketches.size() - 1);
      for (int i = 0;i < sketches.size() - 1; i++) {
        union.merge(sketches.get(i));
      }
  }

  protected void queryCurrentSketch(){
      ItemsSketch<String> sketch = sketches.get(sketches.size() - 1);  

      if (cmd.hasOption("t")){      
        final ItemsSketch.Row<String>[] rowArr = sketch.getFrequentItems(ErrorType.NO_FALSE_POSITIVES);
        for (int i = 0; i < rowArr.length; i++) {
          println(rowArr[i].getItem());
        }
        return;
      }

      if (cmd.hasOption("F")){      
        final ItemsSketch.Row<String>[] rowArr = sketch.getFrequentItems(ErrorType.NO_FALSE_POSITIVES);
        String[] items = cmd.getOptionValues("F");
        for (int i=0; i<items.length; i++) {
          long freq = 0;
          for (int j = 0; j < rowArr.length; j++) {
            if (rowArr[j].getItem().equals(items[i]))
              freq = rowArr[j].getEstimate();
          } 
          println(items[i] + TAB + freq); 
        }
        return;
      }

      if (cmd.hasOption("f")){      
        final ItemsSketch.Row<String>[] rowArr = sketch.getFrequentItems(ErrorType.NO_FALSE_POSITIVES);
        String[] items = queryFileReader(cmd.getOptionValue("f"));
        for (int i=0; i<items.length; i++) {
          long freq = 0;
          for (int j = 0; j < rowArr.length; j++) {
            if (rowArr[j].getItem().equals(items[i]))
              freq = rowArr[j].getEstimate();
          } 
          println(items[i] + TAB + freq); 
        }
        return;
      } 
      
      //default output just topK items with freqs
      final ItemsSketch.Row<String>[] rowArr = sketch.getFrequentItems(ErrorType.NO_FALSE_POSITIVES);
      for (int i = 0; i < rowArr.length; i++) {
        println(rowArr[i].getItem() + TAB + rowArr[i].getEstimate());
      }
      return;
    
  }


}
