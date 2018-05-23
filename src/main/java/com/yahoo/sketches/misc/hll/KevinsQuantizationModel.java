/*
 * Copyright 2018, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.misc.hll;

import static java.lang.Math.log;

import org.testng.annotations.Test;

/**
 * @author Lee Rhodes
 */
public final class KevinsQuantizationModel {
  static final double twopi = 2 * Math.PI;

  static double nemesGamma(final double z) {
    double tmp = (12.0 * z) - (1.0 / (10.0 * z));
    double term1 = 0.5 * (log(twopi) - log(z));
    double term2 = z * (log(z + (1.0 / tmp)) - 1.0);
    return term1 + term2;
  }

  static double nemesLogNatNbang(double n) {
    return nemesGamma(1.0 + n);
  }

  static double[] makeBruteForceLogNatNbangTable(int len) {
    double[] arr = new double[len];
    double tot = 0.0;
    for (int i = 2; i < len; i++) {
      tot += log(i);
      arr[i] = tot;
    }
    return arr;
  }

  static final int blnbt_len = 80;
  static final double blnbt_lenf = blnbt_len;

  static final double[] bruteforce_lognat_n_bang_table = makeBruteForceLogNatNbangTable(blnbt_len);

  static double logNatNbang(double n) {
    assert n >= 0;
    if (n < blnbt_lenf) {
      int i = (int) n;
      return bruteforce_lognat_n_bang_table[i];
    } else {
      assert n < 1e15;
      return nemesLogNatNbang(n);
    }
  }

  /**
   * The probability that no collision occurs in a bitmap sketch:
   *
   * <p>Prob(C = n) = k! / (k^n * (k-n)!)
   * @param k size of bit-map sketch
   * @param n input number of uniques
   * @return probability
   */
  static double probCequalsN(double k, double n) { //caller multiplies by 3 if desired
    final double term1 = logNatNbang((k));
    final double term2 = n * log(k);
    final double term3 = logNatNbang(k - n);
    return Math.exp(term1 - (term2 + term3));
  }

  static double[] oldThresholds = { 0.5, 0.158655319158602648, 0.0227502618904135701,
      0.00134981268617317962 };

  static double[] thresholds = {
      0.00134981268617317962, 0.0227502618904135701, 0.158655319158602648, 0.5,
      0.841344680841397352, 0.977249738109586374, 0.99865018731382682 };


  static void crossesAt(int kappa, int k, int kFactor) {
    int n = 0;
    boolean cont = true;
    while (cont) {
      n++;
      double prob = 1.0 - probCequalsN(1.0 * kFactor * k, n);
      if (prob >= thresholds[kappa + 3]) {
        String s = String.format("%d\t%d\t%.8f\t%.8f", kappa, n, prob, -1.0/n);
        println(s);
        cont = false;
      }
    }
  }

  @Test
  public static void runTest() {
    int lgK = 26;
    int k = 1 << lgK;
    int kFactor = 3;
    println("LgK = " + lgK);
    println("K factor = " + kFactor);
    println("Kappa\tn\tNormRank\tError");
    for (int kappa = -3; kappa <= 3; kappa++) {
      crossesAt(kappa, k, kFactor);
    }
  }

  @Test
  public static void runTest2() { //models the Druid HLL sketch
    int lgK = 11;
    int k = 1 << lgK;
    int kFactor = 1;
    println("LgK = " + lgK);
    println("K factor = " + kFactor);
    println("Kappa\tn\tNormRank\tError");
    for (int kappa = -3; kappa <= 3; kappa++) {
      crossesAt(kappa, k, kFactor);
    }
  }

  static void println(String s) { System.out.println(s); }

}

