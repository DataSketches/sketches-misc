/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.misc.demo;

/**
 * This demo computes a stream of values and feeds them first to
 * an exact sort-based method of computing the number of unique values
 * in the stream and then feeds a similar stream to two different types of
 * sketches from the library.
 *
 * <p>This demo becomes most significant in the case where the number of uniques in the
 * stream exceeds what the computer/JVM can hold in memory.
 *
 * <p>This demo utilizes the Unix/Linux/OS-X sort and wc commands for the brute force computation.
 * So this needs to be run on a linux or mac machine. A windows machine with a suitable unix
 * library installed should also work, but it has not been tested.
 *
 * <p>To configure this demo to run from the command line see the instructions
 * at <a href="http://datasketches.github.io">DataSketches.GitHub.io</a> under "Command Line".</p>
 */
public class ExactVsSketchDemo {

  /**
   * Runs the demo.
   *
   * @param args
   * <ul><li>arg[0]: (Optional) The stream length and can be expressed as a positive double value.
   * The default is 1E6.</li>
   * <li>arg[1] (Optional) The approximate fraction of the stream length that will be unique,
   * the remainder will be duplicates. The default is 0.5.</li>
   * </ul>
   */
  public static void main(final String[] args) {
    final int argsLen = args.length;
    long streamLen = (long)1E8;   //The default stream length
    double uFrac = .50;          //The default fraction that are unique
    if (argsLen == 1) {
      streamLen = (long)(Double.parseDouble(args[0]));
    } else if (argsLen > 1) {
      streamLen = (long)(Double.parseDouble(args[0]));
      uFrac = Double.parseDouble(args[1]);
    }

    final DemoImpl demo = new DemoImpl(streamLen, uFrac);

    demo.runDemo();
  }

}
