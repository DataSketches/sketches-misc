package com.yahoo.sketches.sampling;

import java.util.ArrayList;
import java.util.List;

import com.yahoo.sketches.Util;

public class ReservoirEntropy {

  /**
   *
   * @param args not used
   */
  public static void main(final String[] args) {
    //largeSketchEntropy();
    sketchEntropy();
    unionEntropy();
    mismatchedKEntropy();
  }

  /**
   * Computes entropy of distribution over a single sketch
   */
  static void sketchEntropy() {
    final int numIter = 100000;
    final int k = 64;
    final int valueRange = k * k;
    final SamplingConfig sc = new SamplingConfig(numIter, 1, k, valueRange);
    final int[] histogram = runExperiment(sc);

    System.out.println("sketch entropy result:");
    System.out.println(printStats(histogram, sc));
  }

  /**
   * Computes entropy of distribution over a single large sketch
   */
  static void largeSketchEntropy() {
    final int numIter = 20000;
    final int k = 1 << 20;
    final int valueRange = k << 3;
    final SamplingConfig sc = new SamplingConfig(numIter, 1, k, valueRange);
    final int[] histogram = runExperiment(sc);

    System.out.println("large sketch entropy result:");
    System.out.println(printStats(histogram, sc));
  }

  /**
   * Computes entropy of distribution over the union of multiple sketches.
   * If we have a valid random sample per-sketch from sketchEntropy(), this will run faster by
   * using sketches with exactly k input values as the union input.
   */
  static void unionEntropy() {
    final int numIter = 100000;
    final int numSketches = 13;
    final int k = 100;
    final SamplingConfig sc = new SamplingConfig(numIter, numSketches, k, k);
    final int[] histogram = runExperiment(sc);

    System.out.println("union entropy result:");
    System.out.println(printStats(histogram, sc));
  }

  static void mismatchedKEntropy() {
    final int numIter = 100000;
    final int numSketches = 2;
    final int[] k = {128, 1024};
    final int[] valueRange = {8192, 1024};
    final SamplingConfig sc = new SamplingConfig(numIter, numSketches, k, valueRange);
    final int[] histogram = runExperiment(sc);

    System.out.println("mismatched k entropy result:");
    System.out.println(printStats(histogram, sc));
  }

  static int[] runExperiment(final SamplingConfig sc) {
    final int[] hist = new int[sc.getCumulativeRange()];

    for (int i = 0; i < sc.getNumIters(); ++i) {
      //if (i > 0 && i % 100 == 0) { System.err.println("Iter " + i); }
      final List<ReservoirItemsSketch<Integer>> sketchList = generateSketches(sc);

      final Integer[] out = unionSketchList(sketchList, i, sc);

      for (int key : out) {
        ++hist[key];
      }
    }

    return hist;
  }

  // Creates a list of sketches with non-overlapping value ranges.
  static List<ReservoirItemsSketch<Integer>> generateSketches(final SamplingConfig sc) {
    final List<ReservoirItemsSketch<Integer>> sketchList = new ArrayList<>(sc.getNumSketches());

    int idx = 0;
    for (int i = 0; i < sc.getNumSketches(); ++i) {
      final int k = sc.hasMultipleK() ? sc.getKArray()[i] : sc.getK();
      final int rangeMax = sc.getRangeSize(sc.hasMultipleK() ? i : 0);

      final ReservoirItemsSketch<Integer> ris = ReservoirItemsSketch.newInstance(k);
      for (int j = 0; j < rangeMax; ++j) {
        ris.update(idx++);
      }

      sketchList.add(ris);
    }

    return sketchList;
  }

  static String printStats(final int[] histogram, final SamplingConfig sc) {
    int min = Integer.MAX_VALUE;
    int max = Integer.MIN_VALUE;
    long outputCount = 0;

    for (int i = 0; i < histogram.length; ++i) {
      final int val = histogram[i];
      //System.out.printf("[%d]: %d\n", i, histogram[i]);
      if (val < min) {
        min = val;
      }
      if (val > max) {
        max = val;
      }
      outputCount += val;
    }

    return "H      = " + computeEntropy(outputCount, histogram) + Util.LS
            + "Theo H = " + (Math.log(countPossibleValues(sc)) / Math.log(2.0)) + Util.LS
            + "min    = " + min + Util.LS
            + "max    = " + max + Util.LS;
  }

  static double computeEntropy(final long denom, final int[] data) {
    double H = 0.0;
    final double scaleFactor = 1.0 / denom;
    final double INV_LN_2 = 1.0 / Math.log(2.0);

    for (int count : data) {
      final double p = count * scaleFactor;
      H -= p * Math.log(p) * INV_LN_2;
    }

    return H;
  }

  /**
   * If multiple values of k, uses the max to instantiate the union
   *
   * @param sketches List of sketches to use
   * @param stIdx    Starting index in the list, to allow round-robin ordering (still
   *                 deterministic, but not 100% fixed)
   * @param sc       SamplingConfig object to use
   * @param <T>      Type of item in the sketches
   * @return Array of samples selected by the union
   */
  static <T> T[] unionSketchList(final List<ReservoirItemsSketch<T>> sketches,
                                 final int stIdx,
                                 final SamplingConfig sc) {
    final ReservoirItemsUnion<T> riu = ReservoirItemsUnion.newInstance(sc.getMaxK());

    for (int i = 0; i < sketches.size(); ++i) {
      final int sketchIdx = (stIdx + i) % sc.getNumSketches();
      riu.update(sketches.get(sketchIdx));
    }

    return riu.getResult().getSamples();
  }

  // Sum of all ranges
  static int countPossibleValues(final SamplingConfig sc) {
    if (sc.getRangeSizeArray().length > 1) {
      int total = 0;
      for (int val : sc.getRangeSizeArray()) {
        total += val;
      }
      return total;
    }

    return sc.getRangeSize() * sc.getNumSketches();
  }

  static Integer[] simpleUnion(final int k) {
    final ReservoirItemsSketch<Integer> rls1 = ReservoirItemsSketch.newInstance(k);
    final ReservoirItemsSketch<Integer> rls2 = ReservoirItemsSketch.newInstance(k);

    for (int i = 0; i < (10 * k); ++i) {
      rls1.update(i);
      rls2.update((k * k) + i);
    }

    final ReservoirItemsUnion<Integer> rlu = ReservoirItemsUnion.newInstance(k);
    rlu.update(rls1);
    rlu.update(rls2);

    return rlu.getResult().getSamples();
  }

}
