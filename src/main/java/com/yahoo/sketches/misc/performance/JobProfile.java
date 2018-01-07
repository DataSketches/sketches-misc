/*
 * Copyright 2018, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.misc.performance;

import static java.lang.Math.log;

/**
 *
 * @author Lee Rhodes
 */
public interface JobProfile {
  static char TAB = '\t';
  static double LN2 = log(2.0);
  static String LS = System.getProperty("line.separator");

  void start(Job job);

}
