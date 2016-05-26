/*
 * Copyright 2016, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.cmd;

//import static org.testng.Assert.*;

import org.testng.annotations.Test;

import com.yahoo.sketches.demo.UnixCmd;

public class CommandLineTest {
  private static String mver = "0.0.1";
  private static String cver = "0.5.2";
  private static String home = "/Users/lrhodes";
  private static String miscCP = home +
      "/.m2/repository/com/yahoo/datasketches/sketches-misc/"+mver+"/sketches-misc-"+mver+".jar";
  private static String coreCP = home +
      "/.m2/repository/com/yahoo/datasketches/sketches-core/"+cver+"/sketches-core-"+cver+".jar";
  private static String classPath = miscCP+":"+coreCP;
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
