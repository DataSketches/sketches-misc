package com.yahoo.sketches.sampling;

import java.util.List;
import java.util.Random;

//CHECKSTYLE.OFF: JavadocMethod
//CHECKSTYLE.OFF: WhitespaceAround
public class UnionBenchmark {
  //private static int TARGET_TOTAL_SKETCH_ITEMS = 1 << 24;
  // 64MB of data if ints, if not varying # of samples
  private static Random rand = new Random();
  private static final String LS = System.getProperty("line.separator");

  public static void main(String[] args) {
    int[] kSet = {100, 1000, 10000, 100000, 1000000};
    //int[] kSet = {100, 1000, 10000};
    //int[] numSketchesSet = {100, 1000, 10000, 100000};
    int[] numSketchesSet = {1000};

    double[] mean = new double[1];
    double[] stdev = new double[1];

    for (int k : kSet) {
      for (int numSketches : numSketchesSet) {
        if (k * numSketches > 1L << 28) {
          continue;
        } // limit to 2^30 bytes = 1GB of data

        //SamplingConfig sc = makeConfig(TARGET_TOTAL_SKETCH_ITEMS, kSet[k]);
        SamplingConfig sc = makeConfig(numSketches, k);
        //long[] times = new long[sc.getNumIters()];
        double[] times = new double[sc.getNumIters()];

        // generate sketches
        List<ReservoirItemsSketch<Integer>> sketchList = ReservoirEntropy.generateSketches(sc);

        // run union, save execute time
        for (int i = 0; i < sc.getNumIters(); ++i) {
          int startIdx = rand.nextInt(sc.getNumSketches()); // start on a random index
          //times[i] = unionSketchList(sketchList, startIdx, kSet[k]);
          times[i] = unionSketchList(sketchList, startIdx, k) / (1.0 * sc.getNumSketches());
        }

        updateStats(times, mean, stdev);
        System.out.printf("k = %-7d:\t%f +- %f\t(%d sketches)" + LS,
            k, mean[0], stdev[0], sc.getNumSketches());
      }
    }
  }

  static int countNumSamples(final SamplingConfig sc) {
    int[] kArr = sc.getKArray();
    int[] rangeArr = sc.getRangeSizeArray();
    int count = 0;

    for (int i = 0; i < kArr.length; ++i) {
      count += Math.min(kArr[i], rangeArr[i]);
    }

    return count;
  }

  static SamplingConfig makeConfig(final int tgtItems, final int k) {
    final int numIters = 1000;
    //int numSketches = (int) Math.round(0.5 + 1.0 * tgtItems / k);
    int numSketches = tgtItems;

    int[] kArray = new int[numSketches];
    int[] rangeArray = new int[numSketches];

    for (int i = 0; i < numSketches; ++i) {
      kArray[i] = k;
      rangeArray[i] = (int) Math.round(Math.exp(rand.nextGaussian()) * k) + 1;
    }

    return new SamplingConfig(numIters, numSketches, kArray, rangeArray);
  }

  static <T> long unionSketchList(final List<ReservoirItemsSketch<T>> sketches,
                                 final int stIdx,
                                 final int k) {
    ReservoirItemsUnion<T> riu = ReservoirItemsUnion.getInstance(k);
    int numSketches = sketches.size();

    long timeStartMs = System.currentTimeMillis();
    for (int i = 0; i < numSketches; ++i) {
      int sketchIdx = (stIdx + i) % numSketches;
      riu.update(sketches.get(sketchIdx));
    }
    long timeEndMs = System.currentTimeMillis();

    return timeEndMs - timeStartMs;
  }

  //static void updateStats(final long[] times,
  static void updateStats(final double[] times,
                          double[] meanArr,
                          double[] stdevArr) {

    int n = times.length;
    double timeSum = 0.0;
    double timeSqSum = 0.0;

    //for (long x : times) {
    for (double x : times) {
      timeSum += x;
      timeSqSum += x * x;
    }

    double mean = timeSum / n;
    double var = (timeSqSum / n) - (mean * mean);

    meanArr[0]  = mean;
    stdevArr[0] = Math.sqrt(var);
  }

}
