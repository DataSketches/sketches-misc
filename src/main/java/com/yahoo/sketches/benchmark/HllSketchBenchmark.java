package com.yahoo.sketches.benchmark;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.yahoo.sketches.hll.HllSketch;
import com.yahoo.sketches.hll.HllSketchBuilder;

/**
 */
public class HllSketchBenchmark implements SketchBenchmark {
  private final String name;
  private final Random rand;
  private final HllSketchBuilder inputBob;
  private final HllSketchBuilder unionBob;

  private List<HllSketch> sketches;

  HllSketchBenchmark(final String name, final Random rand, final HllSketchBuilder inputBob,
      final HllSketchBuilder unionBob)
  {
    this.name = name;
    this.rand = rand;
    this.inputBob = inputBob;
    this.unionBob = unionBob;
  }

  @Override
  public void setup(final int numSketches, final List<Spec> specs)
  {
    sketches = new ArrayList<>(numSketches);

    for (Spec spec : specs) {
      for (int i = 0; i < spec.getNumSketches(); ++i) {
        final HllSketch sketch = inputBob.build();
        for (int j = 0; j < spec.getNumEntries(); ++j) {
          sketch.update(new long[]{rand.nextLong()});
        }
        sketches.add(sketch.asCompact());
      }
    }
    Collections.shuffle(sketches);
  }

  @Override
  public void runNTimes(final int n)
  {
    for (int i = 0; i < n; ++i) {
      final HllSketch combined = unionBob.build();
      for (HllSketch toUnion : sketches) {
        combined.union(toUnion);
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
    return name;
  }
}
