package com.yahoo.sketches.sampling;

//CHECKSTYLE.OFF: JavadocMethod
//CHECKSTYLE.OFF: WhitespaceAround
public class UpdateBenchmark {
  private static final String LS = System.lineSeparator();
  private static final int ACCEPT_BREAKPOINT = 2;

  public static void main(String[] args) {
    if (args.length < 2) {
      System.err.println("Usage: UpdateBenchmark [numIters] [log2(k)]");
    }
    //final int numIters = 1000;
    final int numIters = Integer.parseInt(args[0]);
    final int lgk = Integer.parseInt(args[1]);
    final int k = 1 << lgk;
    println("Running " + numIters + " iterations with k=" + k + ":");
    measureReservoirLongsSketch(numIters, k);
    println(measureReservoirLongsSketch(numIters, k));

    measureReservoirItemsSketch(numIters, k);
    println(measureReservoirItemsSketch(numIters, k));
  }

  static String measureReservoirLongsSketch(final int numIters, final int k) {
    final int n = k << 4;

    long[] primingResults = new long[numIters];
    long[] pAcceptHighResults = new long[numIters];
    long[] pAcceptLowResults = new long[numIters];
    long[] totalResults = new long[numIters];

    ReservoirLongsSketch rls = ReservoirLongsSketch.getInstance(k);

    final int samplingBreakIdx = ACCEPT_BREAKPOINT * k; // p(accept) == 0.5, so measure update
    // before/after here
    long inputValue;

    for (int iter = 0; iter < numIters; iter++) {
      // initial reservoir fill
      long startUpdateTime_ns = System.nanoTime();
      for (inputValue = 0; inputValue < k; ) { rls.update(inputValue++); }
      long primingTime_ns = System.nanoTime();

      // p(accept) > 0.5
      for (; inputValue < samplingBreakIdx; ) { rls.update(inputValue++); }
      long pAcceptHighTime_ns = System.nanoTime();

      // p(accept) < 0.5
      for (; inputValue < n; ) { rls.update(inputValue++); }
      long pAcceptLowTime_ns = System.nanoTime();

      primingResults[iter] = primingTime_ns - startUpdateTime_ns;
      pAcceptHighResults[iter] = pAcceptHighTime_ns - primingTime_ns;
      pAcceptLowResults[iter] = pAcceptLowTime_ns - pAcceptHighTime_ns;
      totalResults[iter] = pAcceptLowTime_ns - startUpdateTime_ns;
    }

    return getStatsString(rls.getClass().getSimpleName(), k, n,
            primingResults, pAcceptHighResults, pAcceptLowResults, totalResults);
  }

  static String measureReservoirItemsSketch(final int numIters, final int k) {
    final int n = k << 4;

    long[] primingResults = new long[numIters];
    long[] pAcceptHighResults = new long[numIters];
    long[] pAcceptLowResults = new long[numIters];
    long[] totalResults = new long[numIters];

    ReservoirItemsSketch<Long> ris = ReservoirItemsSketch.getInstance(k);

    final int samplingBreakIdx = ACCEPT_BREAKPOINT * k; // p(accept) == 0.5, so measure update
    // before/after here
    long inputValue;

    for (int iter = 0; iter < numIters; iter++) {
      // initial reservoir fill
      long startUpdateTime_ns = System.nanoTime();
      for (inputValue = 0; inputValue < k; ) { ris.update(inputValue++); }
      long primingTime_ns = System.nanoTime();

      // p(accept) > 0.5
      for (; inputValue < samplingBreakIdx; ) { ris.update(inputValue++); }
      long pAcceptHighTime_ns = System.nanoTime();

      // p(accept) < 0.5
      for (; inputValue < n; ) { ris.update(inputValue++); }
      long pAcceptLowTime_ns = System.nanoTime();

      primingResults[iter] = primingTime_ns - startUpdateTime_ns;
      pAcceptHighResults[iter] = pAcceptHighTime_ns - primingTime_ns;
      pAcceptLowResults[iter] = pAcceptLowTime_ns - pAcceptHighTime_ns;
      totalResults[iter] = pAcceptLowTime_ns - startUpdateTime_ns;
    }

    return getStatsString(ris.getClass().getSimpleName(), k, n,
            primingResults, pAcceptHighResults, pAcceptLowResults, totalResults);
  }


  static String getStatsString(final String simpleName,
                               final int k,
                               final int n,
                               final long[] priming,
                               final long[] pAcceptHigh,
                               final long[] pAcceptLow,
                               final long[] total) {
    double meanPriming = computeMean(priming);
    double meanAcceptHigh = computeMean(pAcceptHigh);
    double meanAcceptLow = computeMean(pAcceptLow);
    double meanTotal = computeMean(total);

    StringBuilder sb = new StringBuilder();
    sb.append("### Results for ").append(simpleName).append(LS);
    sb.append("  iterations: ").append(priming.length).append(LS);
    sb.append("  k         : ").append(k).append(LS);
    sb.append("  n         : ").append(n).append(LS);
    sb.append("  priming update (ns)       : ").append(meanPriming).append(LS);
    sb.append("  high p(accept) update (ns): ").append(meanAcceptHigh).append(LS);
    sb.append("  low p(accept) update (ns) : ").append(meanAcceptLow).append(LS);
    sb.append("  total update (ns)         : ").append(meanTotal).append(LS);
    sb.append("  priming per update (ns)   : ").append(meanPriming / k).append(LS);
    sb.append("  high p per update (ns)    : ")
      .append(meanAcceptHigh / ((ACCEPT_BREAKPOINT - 1) * k)).append(LS);
    sb.append("  low p per update (ns)     : ")
      .append(meanAcceptLow / (n - ACCEPT_BREAKPOINT * k)).append(LS);
    sb.append("  total per update (ns)     : ").append(meanTotal / n).append(LS);
    return sb.toString();
  }

  // being inefficient since squaring ns times seems likely to overflow
  static double computeMean(long[] data) {
    long sum = 0;
    for (long item : data) {
      sum += item;
    }
    return (double) sum / data.length;
  }

  static double computeStDev(long[] data, double mean) {
    double diff = 0;
    for (long item : data) {
      diff += Math.pow(item - mean, 2);
    }
    return Math.sqrt(diff / data.length);
  }

  static void println(String msg) {
    System.out.println(msg);
  }
}
