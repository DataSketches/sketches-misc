package com.yahoo.sketches.sampling;

import datafu.pig.sampling.ReservoirSample;

import org.apache.pig.EvalFunc;
import org.apache.pig.data.BagFactory;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;

import com.yahoo.sketches.pig.sampling.ReservoirSampling;

import java.io.IOException;

/**
 * A class to compare the Reservoir Sampling pig UDF to DataFu's reservoir sampling.
 *
 * @author Jon Malkin
 */
public class DatafuCompare {
  private final SamplingConfig sc;
  private Tuple inputData;

  private DatafuCompare(SamplingConfig config) throws IOException {
    this.sc = config;
    inputData = createData(sc);
  }

  public static void main(String[] args) throws IOException {
    if (args.length < 3) {
      System.err.println("Usage: DatafuCompare <model> <log2(k)> <log2(maxValue)>");
      System.exit(0);
    }

    final int numIters = 10000;

    final String model = args[0];
    final int k = 1 << Integer.parseInt(args[1]);
    final int valueRange = 1 << Integer.parseInt(args[2]);

    final SamplingConfig sc = new SamplingConfig(numIters, 1, k, valueRange);

    final DatafuCompare dc = new DatafuCompare(sc);

    EvalFunc udf;
    if (model.equalsIgnoreCase("datafu")) {
      udf = new ReservoirSample(Integer.toString(sc.getK()));
    } else if (model.toLowerCase().startsWith("sketch")) {
      udf = new ReservoirSampling(Integer.toString(sc.getK()));
    } else {
      throw new IllegalArgumentException("Model must be one of {datafu, sketch}, found: "  + model);
    }

    dc.runTrial(udf); // priming run
    dc.doGC();
    System.out.print("(" + model + ", " + numIters + ", " + k + ", " + valueRange + "):\t");
    System.out.println(dc.runTrial(udf));
  }

  private void doGC() {
    for (int i = 0; i < 10; ++i) {
      System.gc();
    }
  }

  private String runTrial(final EvalFunc udf) throws IOException {
    //inputData = createData(sc);

    final long[] trialTimes = new long[sc.getNumIters()];

    for (int i = 0; i < sc.getNumIters(); ++i) {
      final long startTime_ns = System.nanoTime();
      udf.exec(inputData);
      final long endTime_ns = System.nanoTime();

      trialTimes[i] = endTime_ns - startTime_ns;
      //System.err.println(i + ": " + trialTimes[i] / 1e6);
    }

    double mean = UpdateBenchmark.computeMean(trialTimes);
    double stdev = UpdateBenchmark.computeStDev(trialTimes, mean);

    return mean / 1e6 + " +- " + stdev / 1e6 + " ms";
  }

  private static Tuple createData(final SamplingConfig sc) throws IOException {
    final DataBag inputBag = BagFactory.getInstance().newDefaultBag();

    for (int n = 0; n < sc.getRangeSize(); ++n) {
      final Tuple t = TupleFactory.getInstance().newTuple(1);
      t.set(0, n);
      inputBag.add(t);
    }

    return TupleFactory.getInstance().newTuple(inputBag);
  }
}
