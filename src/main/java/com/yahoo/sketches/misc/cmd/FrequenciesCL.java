package com.yahoo.sketches.misc.cmd;

import static com.yahoo.sketches.Util.TAB;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;

import com.yahoo.memory.Memory;
import com.yahoo.sketches.ArrayOfStringsSerDe;
import com.yahoo.sketches.frequencies.ErrorType;
import com.yahoo.sketches.frequencies.ItemsSketch;


  public class FrequenciesCL extends CommandLine<ItemsSketch<String>> {

    FrequenciesCL() {
      super();
      // input options
      options.addOption(Option.builder("k")
          .desc("parameter k")
          .hasArg()
          .build());

      // output options
      options.addOption(Option.builder("i")
          .longOpt("topk-ids")
          .desc("query identities for top k frequent items")
          .build());
      options.addOption(Option.builder("F")
          .longOpt("id2freq")
          .desc("query frequencies for items with given ID")
          .hasArgs() //unlimited
          .argName("ID")
          .build());
      options.addOption(Option.builder("f")
          .longOpt("id2freq-file")
          .desc("query frequencies for items with ids from FILE")
          .hasArg()
          .argName("FILE")
          .build());
    }

  @Override
  protected void showHelp() {
        final HelpFormatter helpf = new HelpFormatter();
        helpf.setOptionComparator(null);
        helpf.printHelp( "ds freq", options);
  }


  @Override
  protected void buildSketch() {
    final ItemsSketch<String> sketch;
    final int defaultSize = 1 << 17; //128K
    if (cmd.hasOption("k")) { //user defined k
            sketch = new ItemsSketch<>(Integer.parseInt(cmd.getOptionValue("k")));
    } else { //default k
            sketch = new ItemsSketch<>(defaultSize);
    }
    sketches.add(sketch);
  }

  @Override
  protected void updateSketch(final BufferedReader br) {
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
  }

  @Override
  protected ItemsSketch<String> deserializeSketch(final byte[] bytes) {
    return ItemsSketch.getInstance(Memory.wrap(bytes), new ArrayOfStringsSerDe());
  }

  @Override
  protected byte[] serializeSketch(final ItemsSketch<String> sketch) {
    return sketch.toByteArray(new ArrayOfStringsSerDe());
  }

  @Override
  protected void mergeSketches() {
      final ItemsSketch<String> union = sketches.get(sketches.size() - 1);
      for (int i = 0;i < (sketches.size() - 1); i++) {
        union.merge(sketches.get(i));
      }
  }

  @Override
  protected void queryCurrentSketch() {
    final ItemsSketch<String> sketch = sketches.get(sketches.size() - 1);

      if (cmd.hasOption("t")) {
        final ItemsSketch.Row<String>[] rowArr =
            sketch.getFrequentItems(ErrorType.NO_FALSE_POSITIVES);
        for (int i = 0; i < rowArr.length; i++) {
          println(rowArr[i].getItem());
        }
        return;
      }

      if (cmd.hasOption("F")) {
        final ItemsSketch.Row<String>[] rowArr =
            sketch.getFrequentItems(ErrorType.NO_FALSE_POSITIVES);
        final String[] items = cmd.getOptionValues("F");
        for (int i = 0; i < items.length; i++) {
          long freq = 0;
          for (int j = 0; j < rowArr.length; j++) {
            if (rowArr[j].getItem().equals(items[i])) {
              freq = rowArr[j].getEstimate();
            }
          }
          println(items[i] + TAB + freq);
        }
        return;
      }

      if (cmd.hasOption("f")) {
        final ItemsSketch.Row<String>[] rowArr =
            sketch.getFrequentItems(ErrorType.NO_FALSE_POSITIVES);
        final String[] items = queryFileReader(cmd.getOptionValue("f"));
        for (int i = 0; i < items.length; i++) {
          long freq = 0;
          for (int j = 0; j < rowArr.length; j++) {
            if (rowArr[j].getItem().equals(items[i])) {
              freq = rowArr[j].getEstimate();
            }
          }
          println(items[i] + TAB + freq);
        }
        return;
      }

      //default output just topK items with frequencies
      final ItemsSketch.Row<String>[] rowArr =
          sketch.getFrequentItems(ErrorType.NO_FALSE_POSITIVES);
      for (int i = 0; i < rowArr.length; i++) {
        println(rowArr[i].getItem() + TAB + rowArr[i].getEstimate());
      }
      return;

  }

}
