/*
 * Copyright 2016, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.cmd;

import org.testng.annotations.Test;

public class CommandLineTest {

  @SuppressWarnings("unused")
  @Test
  public void checkHelpDirect() {
    String[] args = new String[] {""};
    boolean disablePrint = true; //set this to false to see output on console.
    //CommandLine cl =
        new CommandLine(disablePrint, args);

    args = new String[] {"uniq","help"};
    //cl =
        new CommandLine(disablePrint, args);

    args = new String[] {"rank","help"};
    //cl =
        new CommandLine(disablePrint, args);

    args = new String[] {"hist","help"};
    //cl =
        new CommandLine(disablePrint, args);

    args = new String[] {"loghist","help"};
    //cl =
        new CommandLine(disablePrint, args);

    args = new String[] {"freq","help"};
    //cl =
        new CommandLine(disablePrint, args);
  }

}
