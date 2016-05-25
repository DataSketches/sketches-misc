/*
 * Copyright 2016, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.cmd;

//import static org.testng.Assert.*;

import org.testng.annotations.Test;

import com.yahoo.sketches.demo.UnixCmd;

public class CommandLineTest {
  private static String miscDir = "/Users/lrhodes/dev/git/sketches-misc/target/classes/";
  private static String coreDir = "/Users/lrhodes/dev/git/sketches-core/target/classes/";
  private static String classPath = miscDir+":"+coreDir;
  private static String CmdLine = " com.yahoo.sketches.cmd.CommandLine ";
  private static String sketch = "java -cp " + classPath + CmdLine;
  
  @Test
  public void checkSketchUniq() {
    String cmd = sketch + " uniq " + "/Users/lrhodes/tenMillionNumbers.txt";
    UnixCmd.run("sketch uniq", cmd);
  }
  
  @Test
  public void checkSketchUniqHelp() {
    String cmd = sketch + " uniq help";
    UnixCmd.run("sketch uniq", cmd);
  }
  
  @Test
  public void checkHelp() {
    String cmd = sketch + " help";
    UnixCmd.run("\"sketch help\"", cmd);
  }
  
}
