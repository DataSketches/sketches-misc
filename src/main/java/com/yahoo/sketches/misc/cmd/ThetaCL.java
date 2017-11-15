package com.yahoo.sketches.misc.cmd;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;

import com.yahoo.memory.Memory;
import com.yahoo.sketches.theta.AnotB;
import com.yahoo.sketches.theta.Intersection;
import com.yahoo.sketches.theta.SetOperation;
import com.yahoo.sketches.theta.Sketch;
import com.yahoo.sketches.theta.Sketches;
import com.yahoo.sketches.theta.Union;
import com.yahoo.sketches.theta.UpdateSketch;
import com.yahoo.sketches.theta.UpdateSketchBuilder;


public class ThetaCL extends CommandLine<Sketch> {
   protected UpdateSketch updateSketch;

   ThetaCL() {
      super();
      // input options
      options.addOption(Option.builder("k")
          .desc("parameter k")
          .hasArg()
          .build());
      // sketch level operators
      options.addOption(Option.builder("i")
          .longOpt("merge-intersection")
          .desc("find intersection of sketches")
          .build());
      options.addOption(Option.builder("m")
          .longOpt("merge-set-minus")
          .desc("from first sketch subtract union of all others")
          .build());
      // output options
      options.addOption(Option.builder("u")
          .longOpt("upper-bound")
          .desc("upper bound on the estimate")
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
    helpf.printHelp( "ds theta", options);
  }


  @Override
  protected void buildSketch() {
    final UpdateSketchBuilder bldr = Sketches.updateSketchBuilder();
    if (cmd.hasOption("k")) {
      bldr.setNominalEntries(Integer.parseInt(cmd.getOptionValue("k")));  // user defined k
    }
    updateSketch = bldr.build();
  }

  @Override
  protected void updateSketch(final BufferedReader br) {
    if (sketches.size() > 0) {
      buildSketch();
    }
    String itemStr = "";
    try {
      while ((itemStr = br.readLine()) != null) {
        updateSketch.update(itemStr);
      }
    } catch (final IOException e) {
      printlnErr("Read Error: Item: " + itemStr + ", " + br.toString());
      throw new RuntimeException(e);
    }
    if (sketches.size() > 0) {
      final Union union = SetOperation.builder().buildUnion();
      union.update(sketches.get(sketches.size() - 1));
      union.update(updateSketch);
      sketches.add(union.getResult());
    } else {
      sketches.add(updateSketch.compact());
    }
  }

  @Override
  protected Sketch deserializeSketch(final byte[] bytes) {
    return Sketch.wrap(Memory.wrap(bytes));
  }

  @Override
  protected byte[] serializeSketch(final Sketch sketch) {
    return sketch.toByteArray();
  }


  @Override
  protected void mergeSketches() {

      if (cmd.hasOption("i")) {
        final Intersection intersection = SetOperation.builder().buildIntersection();
        for (Sketch sketch: sketches) {
          intersection.update(sketch);
        }
        sketches.add(intersection.getResult());
        return;
      }

      if (cmd.hasOption("m")) {
        final Union union = SetOperation.builder().buildUnion();
        for (int i = 1; i < sketches.size(); i++) {
          union.update(sketches.get(i));
        }

        final AnotB aNotB = Sketches.setOperationBuilder().buildANotB();
        aNotB.update(sketches.get(0),union.getResult());
        sketches.add(aNotB.getResult());
        return;
      }

      final Union union = SetOperation.builder().buildUnion();  //default merge is union
      for (Sketch sketch: sketches) {
        union.update(sketch);
      }
      sketches.add(union.getResult());
      return;

  }

  @Override
  protected void queryCurrentSketch() {
    if (sketches.size() > 0) {
      final Sketch sketch = sketches.get(sketches.size() - 1);
      if (cmd.hasOption("l")) {
          System.out.format("%f\n",sketch.getUpperBound(2));
      } else if (cmd.hasOption("u")) {
          System.out.format("%f\n",sketch.getLowerBound(2));
      } else {
          System.out.format("%f\n",sketch.getEstimate()); // default output
      }
    }
  }
}
