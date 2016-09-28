package com.yahoo.sketches.sampling;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.yahoo.sketches.Util;

/**
 */
public class ReservoirEntropy {
    public static void main(String[] args) {
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
        SamplingConfig sc = new SamplingConfig(numIter, 1, k, valueRange);
        Map histogram = runExperiment(sc);

        System.out.println("sketch entropy result:");
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
        SamplingConfig sc = new SamplingConfig(numIter, numSketches, k, k);
        Map histogram = runExperiment(sc);

        System.out.println("union entropy result:");
        System.out.println(printStats(histogram, sc));
    }

    static void mismatchedKEntropy() {
        final int numIter = 100000;
        final int numSketches = 2;
        final int[] k = {128, 1024};
        final int[] valueRange = {1024, 8192};
        SamplingConfig sc = new SamplingConfig(numIter, numSketches, k, valueRange);
        Map histogram = runExperiment(sc);

        System.out.println("mismatched k entropy result:");
        System.out.println(printStats(histogram, sc));
    }

    static Map runExperiment(SamplingConfig sc) {
        java.util.TreeMap<Integer, Integer> hist = new java.util.TreeMap<>();

        for (int i = 0; i < sc.getNumIters(); ++i) {
            List<ReservoirItemsSketch<Integer>> sketchList = generateSketches(sc);

            Integer[] out = unionNSketches(sketchList, i, sc);

            for (int key : out) {
                if (hist.containsKey(key)) {
                    int count = hist.get(key);
                    hist.put(key, ++count);
                } else {
                    hist.put(key, 1);
                }
            }
        }

        return hist;
    }

    // Creates a list of sketches with non-overlapping value ranges.
    static List<ReservoirItemsSketch<Integer>> generateSketches(final SamplingConfig sc) {
        List<ReservoirItemsSketch<Integer>> sketchList = new ArrayList<>(sc.getNumSketches());

        int idx = 0;
        for (int i = 0; i < sc.getNumSketches(); ++i) {
            int k = sc.hasMultipleK() ? sc.getKArray()[i] : sc.getK();
            int rangeMax = sc.getRangeSize(sc.hasMultipleK() ? i : 0);

            ReservoirItemsSketch<Integer> ris = ReservoirItemsSketch.getInstance(k);
            for (int j = 0; j < rangeMax; ++j) {
                ris.update(idx++);
            }

            sketchList.add(ris);
        }

        return sketchList;
    }

    static <T> String printStats(final Map<T,Integer> histogram, final SamplingConfig sc) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        long outputCount = 0;
        for (Map.Entry<T,Integer> e : histogram.entrySet()) {
            if (e.getValue() < min) { min = e.getValue(); }
            if (e.getValue() > max) { max = e.getValue(); }
            outputCount += e.getValue();
        }

        return "H      = " + computeEntropy(outputCount, histogram) + Util.LS +
                "Theo H = " + Math.log(countPossibleValues(sc)) / Math.log(2.0) + Util.LS +
                "min    = " + min + Util.LS +
                "max    = " + max + Util.LS;
    }

    static <T> double computeEntropy(final long denom, final Map<T, Integer> data) {
        double H = 0.0;
        final double scaleFactor = 1.0 / denom;
        final double INV_LN_2 = 1.0 / Math.log(2.0);

        for (int count : data.values()) {
            double p = count * scaleFactor;
            H -= p * Math.log(p) * INV_LN_2;
        }

        return H;
    }

    /**
     * If multiple values of k, uses the max to instantiate the union
     * @param sketches List of sketches to use
     * @param stIdx Starting index in the list, to allow round-robin ordering (still deterministic, but not 100% fixed)
     * @param sc SamplingConfig object to use
     * @param <T> Type of item in the sketches
     * @return Array of samples selected by the union
     */
    static <T> T[] unionNSketches(final List<ReservoirItemsSketch<T>> sketches,
                                  final int stIdx,
                                  final SamplingConfig sc) {
        ReservoirItemsUnion<T> riu = ReservoirItemsUnion.getInstance(sc.getMaxK());

        for (int i = 0; i < sketches.size(); ++i) {
            int sketchIdx = (stIdx + i) % sc.getNumSketches();
            riu.update(sketches.get(sketchIdx));
        }

        return riu.getResult().getSamples();
    }

    // Sum of all ranges
    static int countPossibleValues(final SamplingConfig sc) {
        if (sc.getRangesizeArray().length > 1) {
            int total = 0;
            for (int val : sc.getRangesizeArray()) {
                total += val;
            }
            return total;
        }

        return sc.getRangeSize() * sc.getNumSketches();
    }

    static Integer[] simpleUnion(final int k) {
        ReservoirItemsSketch<Integer> rls1 = ReservoirItemsSketch.getInstance(k);
        ReservoirItemsSketch<Integer> rls2 = ReservoirItemsSketch.getInstance(k);

        for (int i = 0; i < 10 * k; ++i) {
            rls1.update(i);
            rls2.update(k * k + i);
        }

        ReservoirItemsUnion<Integer> rlu = ReservoirItemsUnion.getInstance(k);
        rlu.update(rls1);
        rlu.update(rls2);

        return rlu.getResult().getSamples();
    }

}
