/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the 
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.demo;

//import static com.yahoo.sketches.demo.Util.nextLong;
//import static com.yahoo.sketches.hash.MurmurHash3.hash;

import java.util.Random;

public class Util {
  private static Random rand = new Random();
  
  public static String getMinSecFromNano(long nanoSec) {
    long totSec = nanoSec/1000000000L;
    long min = totSec/60; //whole min
    long sec = totSec%60; //remainder whole sec
    long ns  = nanoSec - totSec * 1000000000L; //remainder ns
    long ms  = ns / 1000000L; //remainder whole ms 
    String t = String.format("Time Min:Sec.mSec = %d:%02d.%03d", min, sec, ms);
    return t;
  }
  
  public static String getMinSecFromMilli(long milliSec) {
    long totSec = milliSec/1000L;
    long min = totSec/60; //whole min
    long sec = totSec%60; //remainder whole sec
    long ms  = milliSec - totSec * 1000L; //remainder whole ms 
    String t = String.format("Time Min:Sec.mSec = %d:%02d.%03d", min, sec, ms);
    return t;
  }
  
  /**
   * Extracted from java.util.Random.
   * The form of nextLong used by LongStream Spliterators.  If origin is greater than bound, 
   * acts as unbounded form of nextLong, else as bounded form.
   *
   * @param origin the least value, unless greater than bound
   * @param bound the upper bound (exclusive), must not equal origin
   * @return a pseudorandom value
   */
  public static final long nextLong(long origin, long bound) {
      long r = rand.nextLong();
      if (origin < bound) {
          long n = bound - origin, m = n - 1;
          if ((n & m) == 0L)  // power of two
              r = (r & m) + origin;
          else if (n > 0L) {  // reject over-represented candidates
              for (long u = r >>> 1;            // ensure nonnegative
                   u + m - (r = u % n) < 0L;    // rejection check
                   u = rand.nextLong() >>> 1) // retry
              r += origin;
          }
          else {              // range not representable as long
              while (r < origin || r >= bound)
                  r = rand.nextLong();
          }
      }
      return r;
  }
  
  public static void println(String s) { System.out.println(s); }
  
}
