package com.yahoo.sketches.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.yahoo.sketches.hll.HllSketch;
import com.yahoo.sketches.hll.HllSketchBuilder;
import com.yahoo.sketches.hll.Preamble;

/**
 */
public class BenchmarkMain {

  /**
   *
   * @param args not used
   */
  public static void main(final String[] args) {
    final int lgK = 12;

    final List<SketchBenchmark> benchmarks = new ArrayList<SketchBenchmark>() {
      private static final long serialVersionUID = 1L;

      {
        this.add(new ThetaMemoryBenchmark(lgK));
        this.add(new ThetaBenchmark(lgK));

        final HllSketchBuilder sparseBob = HllSketch.builder().setPreamble(Preamble.fromLogK(lgK));
        final HllSketchBuilder denseBob = sparseBob.copy().setDenseMode(true);
        this.add(new HllSketchBenchmark("HLL Sketch", new Random(lgK), sparseBob, denseBob));
        this.add(
            new HllSketchBenchmark(
                "HLL Non-Compressed to Compressed",
                new Random(lgK), sparseBob, denseBob.copy().setCompressedDense(true)
            )
        );
        this.add(
            new HllSketchBenchmark(
                "HLL Compressed to Non-Compressed",
                new Random(lgK), sparseBob.copy().setCompressedDense(true), denseBob
            )
        );
        this.add(
            new HllSketchBenchmark(
                "HLL All Compressed",
                new Random(lgK), denseBob.copy().setCompressedDense(true),
                denseBob.copy().setCompressedDense(true)
            )
        );
      }
    };

    runBenchmarks(benchmarks, 20, 100, powerLawDistribution);
  }

  private static void runBenchmarks(
      final List<SketchBenchmark> benchmarks,
      final int increment,
      final int numTimes,
      final List<SketchBenchmark.Spec> distribution
  )
  {
    int numSketches = 0;
    for (SketchBenchmark.Spec spec : distribution) {
      numSketches += spec.getNumSketches();
    }

    for (SketchBenchmark benchmark : benchmarks) {
      System.out.printf("Starting benchmark[%s]%n", benchmark);
      long start = System.currentTimeMillis();
      benchmark.setup(numSketches, powerLawDistribution);
      System.out.printf(
          "benchmark[%s] setup done in %,d millis.%n", benchmark, System.currentTimeMillis() - start);
      start = System.currentTimeMillis();
      benchmark.runNTimes(increment);
      System.out.printf("benchmark[%s] priming[%s] done in %,d millis.%n", benchmark, increment,
          System.currentTimeMillis() - start);
      doGC();


      for (int i = 0; i < numTimes; i += increment) {
        start = System.currentTimeMillis();
        benchmark.runNTimes(increment);
        final long time = System.currentTimeMillis() - start;
        System.out.printf(
            "Benchmark[%s], %,d runs => %,d millis (%,d ms/run), %,d/sec%n",
            benchmark,
            i + increment,
            time,
            (int) (time / (double) increment),
            (int) ((1000 / (time / (double) increment)) * numSketches)
        );
        doGC();
      }
      System.out.printf("Done with benchmark[%s]%n", benchmark);
    }
  }

  private static void doGC()
  {
    for (int i = 0; i < 10; ++i) {
      System.gc();
    }
  }


  @SuppressWarnings("serial")
  static final List<SketchBenchmark.Spec> powerLawDistribution =
      new ArrayList<SketchBenchmark.Spec>() { {
    this.add(new SketchBenchmark.Spec(0, 44129));
    this.add(new SketchBenchmark.Spec(1, 431561));
    this.add(new SketchBenchmark.Spec(2, 129063));
    this.add(new SketchBenchmark.Spec(3, 64821));
    this.add(new SketchBenchmark.Spec(4, 67522));
    this.add(new SketchBenchmark.Spec(6, 20291));
    this.add(new SketchBenchmark.Spec(7, 15767));
    this.add(new SketchBenchmark.Spec(8, 22975));
    this.add(new SketchBenchmark.Spec(11, 22441));
    this.add(new SketchBenchmark.Spec(14, 14531));
    this.add(new SketchBenchmark.Spec(17, 13472));
    this.add(new SketchBenchmark.Spec(22, 13253));
    this.add(new SketchBenchmark.Spec(28, 9002));
    this.add(new SketchBenchmark.Spec(35, 8406));
    this.add(new SketchBenchmark.Spec(45, 7618));
    this.add(new SketchBenchmark.Spec(57, 6349));
    this.add(new SketchBenchmark.Spec(71, 5194));
    this.add(new SketchBenchmark.Spec(89, 4524));
    this.add(new SketchBenchmark.Spec(112, 4032));
    this.add(new SketchBenchmark.Spec(141, 3397));
    this.add(new SketchBenchmark.Spec(178, 2935));
    this.add(new SketchBenchmark.Spec(224, 2516));
    this.add(new SketchBenchmark.Spec(282, 2118));
    this.add(new SketchBenchmark.Spec(355, 1825));
    this.add(new SketchBenchmark.Spec(447, 1527));
    this.add(new SketchBenchmark.Spec(561, 1269));
    this.add(new SketchBenchmark.Spec(709, 1088));
    this.add(new SketchBenchmark.Spec(890, 900));
    this.add(new SketchBenchmark.Spec(1118, 767));
    this.add(new SketchBenchmark.Spec(1410, 654));
    this.add(new SketchBenchmark.Spec(1776, 550));
    this.add(new SketchBenchmark.Spec(2246, 469));
    this.add(new SketchBenchmark.Spec(2813, 353));
    this.add(new SketchBenchmark.Spec(3552, 325));
    this.add(new SketchBenchmark.Spec(4472, 252));
    this.add(new SketchBenchmark.Spec(5639, 249));
    this.add(new SketchBenchmark.Spec(7022, 187));
    this.add(new SketchBenchmark.Spec(8952, 150));
    this.add(new SketchBenchmark.Spec(11270, 138));
    this.add(new SketchBenchmark.Spec(14198, 106));
    this.add(new SketchBenchmark.Spec(17544, 74));
    this.add(new SketchBenchmark.Spec(22145, 81));
    this.add(new SketchBenchmark.Spec(27848, 50));
    this.add(new SketchBenchmark.Spec(35319, 58));
    this.add(new SketchBenchmark.Spec(44267, 33));
    this.add(new SketchBenchmark.Spec(55292, 22));
    this.add(new SketchBenchmark.Spec(72264, 10));
    this.add(new SketchBenchmark.Spec(88903, 13));
    this.add(new SketchBenchmark.Spec(111538, 12));
    this.add(new SketchBenchmark.Spec(136481, 11));
    this.add(new SketchBenchmark.Spec(178605, 6));
    this.add(new SketchBenchmark.Spec(215707, 5));
    this.add(new SketchBenchmark.Spec(273075, 5));
    this.add(new SketchBenchmark.Spec(362878, 5));
    this.add(new SketchBenchmark.Spec(546015, 1));
    this.add(new SketchBenchmark.Spec(1106004, 2));
    this.add(new SketchBenchmark.Spec(1766259, 2));
  } };

}
