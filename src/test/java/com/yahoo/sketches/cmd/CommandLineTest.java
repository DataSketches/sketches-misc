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
  
  @SuppressWarnings("unused")
  @Test
  public void checkHelpDirect() {
    String[] args = new String[] {""};
    boolean disablePrint = false;
    CommandLine cl = new CommandLine(disablePrint, args);
    
    args = new String[] {"uniq","help"};
    cl = new CommandLine(disablePrint, args);
    
    args = new String[] {"rank","help"};
    cl = new CommandLine(disablePrint, args);
    
    args = new String[] {"hist","help"};
    cl = new CommandLine(disablePrint, args);
    
    args = new String[] {"loghist","help"};
    cl = new CommandLine(disablePrint, args);
    
    args = new String[] {"freq","help"};
    cl = new CommandLine(disablePrint, args);
  }
  
  
  @Test
  public void checkHelpViaUnix() {
    String cmd = sketch + " help";
    UnixCmd.run("\" + cmd + \"", cmd);
  }
  
  @Test
  public void checkUniqHelpViaUnix() {
    String cmd = sketch + " uniq help";
    UnixCmd.run("sketch uniq", cmd);
  }
  
  @Test
  public void checkUniqFileViaUnix() {
    String cmd = sketch + " uniq " + "/Users/lrhodes/tenMillionNumbers.txt";
    UnixCmd.run("sketch uniq", cmd);
  }
  
}
