/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.performance.accuracy;

/**
 * A trial is one pass through all uniques, pausing to store the estimate into a
 * quantiles sketch at each point along the unique axis.
 *
 * @author Lee Rhodes
 */
public interface SketchAccuracyTrial {

  void configure(Properties prop, Quantiles[] qArr);

  long doTrial(long vInStart);

  Properties defaultProperties();
}
