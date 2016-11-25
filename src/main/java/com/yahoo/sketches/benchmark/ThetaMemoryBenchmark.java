package com.yahoo.sketches.benchmark;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.yahoo.memory.Memory;
import com.yahoo.memory.NativeMemory;
import com.yahoo.sketches.theta.SetOperation;
import com.yahoo.sketches.theta.Sketch;
import com.yahoo.sketches.theta.Union;
import com.yahoo.sketches.theta.UpdateSketch;

//CHECKSTYLE.OFF: JavadocMethod
//CHECKSTYLE.OFF: WhitespaceAround
/**
 */
public class ThetaMemoryBenchmark implements SketchBenchmark {
  private final int nominalEntries;
  private final Random rand;
  private final byte[] bytes;

  private List<Memory> memories;

  public ThetaMemoryBenchmark(final int lgK) {
    this.nominalEntries = 1 << lgK;
    this.rand = new Random(lgK);
    this.bytes = new byte[Sketch.getMaxUpdateSketchBytes(nominalEntries) + 8];
  }

  @Override
  public void setup(final int numSketches, final List<Spec> specs)
  {
    memories = new ArrayList<>(numSketches);

    for (Spec spec : specs) {
      for (int i = 0; i < spec.getNumSketches(); ++i) {
        final UpdateSketch sketch = UpdateSketch.builder().build(nominalEntries);
        for (int j = 0; j < spec.getNumEntries(); ++j) {
          sketch.update(rand.nextLong());
        }
        memories.add(new NativeMemory(sketch.rebuild().compact(true, null).toByteArray()));
      }
    }
    Collections.shuffle(memories, rand);

    int numRetained = 0;
    int numEstimating = 0;
    for (Memory mem : memories) {
      final Sketch sketch = Sketch.wrap(mem);
      numRetained += sketch.getRetainedEntries(true);
      if (sketch.isEstimationMode()) {
        ++numEstimating;
      }
    }
    System.out.printf(
        "%,d entries, %,d/sketch, %,d estimating (%.2f%%)%n",
        numRetained, numRetained / memories.size(), numEstimating,
          (100 * numEstimating) / (double) memories.size()
    );
  }

  @Override
  public void runNTimes(final int n)
  {
    for (int i = 0; i < n; ++i) {
      final Union combined =
          SetOperation.builder().initMemory(new NativeMemory(bytes)).buildUnion(nominalEntries);
      for (Memory toUnion : memories) {
        combined.update(toUnion);
      }
    }
  }

  @Override
  public void reset()
  {
    memories = null;
  }

  @Override
  public String toString()
  {
    return String.format("Theta Memory Benchmark(nominalEntries=%s)", nominalEntries);
  }
}
