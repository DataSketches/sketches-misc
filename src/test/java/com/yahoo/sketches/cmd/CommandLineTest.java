/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.cmd;

import org.testng.annotations.Test;

/**
 * @author Lee Rhodes
 */
public class CommandLineTest {

  @Test
  public void simpleTest1() {
    String[] line = new String[] {"theta", "help"};
    CommandLine.main(line);
  }


}
