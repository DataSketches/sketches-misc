package com.yahoo.sketches.benchmark;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.yahoo.sketches.theta.CompactSketch;
import com.yahoo.sketches.theta.SetOperation;
import com.yahoo.sketches.theta.Sketch;
import com.yahoo.sketches.theta.Union;
import com.yahoo.sketches.theta.UpdateSketch;

/**
 */
public class ThetaBenchmark implements SketchBenchmark
{
  private final int nominalEntries;
  private final Random rand;

  private List<CompactSketch> sketches;

  public ThetaBenchmark(final int lgK) {
    this.nominalEntries = 1 << lgK;
    this.rand = new Random(lgK);
  }

  @Override
  public void setup(final int numSketches, final List<Spec> specs)
  {
    sketches = new ArrayList<>(numSketches);

    for (Spec spec : specs) {
      for (int i = 0; i < spec.getNumSketches(); ++i) {
        final UpdateSketch sketch = UpdateSketch.builder().build(nominalEntries);
        for (int j = 0; j < spec.getNumEntries(); ++j) {
          sketch.update(rand.nextLong());
        }

        sketches.add(sketch.rebuild().compact(true, null));
      }
    }
    Collections.shuffle(sketches, rand);

    int numRetained = 0;
    int numEstimating = 0;
    for (CompactSketch sketch : sketches) {
      numRetained += sketch.getRetainedEntries(true);
      if (sketch.isEstimationMode()) {
        ++numEstimating;
      }
    }
    System.out.printf(
        "%,d entries, %,d/sketch, %,d estimating (%.2f%%)%n",
        numRetained, numRetained / sketches.size(), numEstimating,
          (100 * numEstimating) / (double) sketches.size()
    );
  }

  @Override
  public void runNTimes(final int n)
  {
    for (int i = 0; i < n; ++i) {
      final Union combined = SetOperation.builder().buildUnion(nominalEntries);
      for (Object toUnion : sketches) {
        combined.update((Sketch) toUnion);
      }
    }
  }

  @Override
  public void reset()
  {
    sketches = null;
  }

  @Override
  public String toString()
  {
    return String.format("Theta OnHeap Benchmark(nominalEntries=%s)", nominalEntries);
  }
}
