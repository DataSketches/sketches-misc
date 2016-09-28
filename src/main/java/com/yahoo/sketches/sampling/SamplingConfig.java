package com.yahoo.sketches.sampling;

/**
 */
public class SamplingConfig {
    private final int numIters_;
    private final int numSketches_;
    private final int[] rangeSize_;
    private final int[] k_;

    public SamplingConfig(final int numIters, final int numSketches, final int k, final int rangeSize) {
        numIters_ = numIters;
        numSketches_ = numSketches;
        rangeSize_ = new int[1];
        rangeSize_[0] = rangeSize;
        k_ = new int[1];
        k_[0] = k;
    }

    public SamplingConfig(final int numIters, final int numSketches, final int[] k, final int[] rangeSize) {
        if (k.length != numSketches) {
            throw new IllegalArgumentException("Number of sketches to generate must equal length of array of k values");
        }
        if (rangeSize.length != numSketches) {
            throw new IllegalArgumentException("Number of sketches to generate must equal length of array of ranges");
        }

        numIters_ = numIters;
        numSketches_ = numSketches;
        rangeSize_ = rangeSize;
        k_ = k;
    }

    public int getNumIters() { return numIters_; }

    public int getNumSketches() { return numSketches_; }

    public int getRangeSize() { return rangeSize_[0]; }

    public int getRangeSize(final int i) { return rangeSize_[i]; }

    public int[] getRangesizeArray() { return rangeSize_; }

    public int getK() { return k_[0]; }

    public int getK(final int i) { return k_[i]; }

    public int[] getKArray() { return k_; }

    public boolean hasMultipleK() { return k_.length > 1; }

    public int getMaxK() {
        int max = k_[0];
        for (int val : k_) {
            max = (val > max ? val : max);
        }
        return max;
    }
}
