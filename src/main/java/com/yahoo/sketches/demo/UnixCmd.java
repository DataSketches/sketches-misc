/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.demo;

import static com.yahoo.sketches.demo.Util.println;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class UnixCmd {
  private static final String LS = System.getProperty("line.separator");

  /**
   * @param name the name of the command
   * @param cmd the actual command-line string
   * @return total test time in milliseconds
   */
  public static long run(String name, String cmd) {
    StringBuilder sbOut = new StringBuilder();
    StringBuilder sbErr = new StringBuilder();
    String out = null;
    String err = null;
    Process p = null;
    String[] envp = {"LC_ALL=C"}; //https://bugs.launchpad.net/ubuntu/+source/coreutils/+bug/846628
    long testStartTime_mS = System.currentTimeMillis();
    try {
      // run the Unix cmd using the Runtime exec method:
      p = Runtime.getRuntime().exec(cmd, envp);
      BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
      BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

      // read the output from the command
      boolean outFlag = true;
      while ((out = stdInput.readLine()) != null) {
        if (outFlag) {
          sbOut.append("Output from " + name + " command:").append(LS);
          outFlag = false;
        }
        sbOut.append(out).append(LS);
      }

      // read any errors from the attempted command
      boolean headerFlag = true;
      while ((err = stdError.readLine()) != null) {
        if (headerFlag) {
          sbErr.append("\nError from " + name + " command:").append(LS);
          headerFlag = false;
        }
        sbErr.append(err).append(LS);
      }
    }
    catch (IOException e) {
      System.out.println("Exception: ");
      e.printStackTrace();
      System.exit( -1);
    }
    if ((p != null) && (p.isAlive())) {
      p.destroy();
    }
    long testTime_mS = System.currentTimeMillis() - testStartTime_mS;
    println("Unix cmd: " + cmd);
    println(Util.getMinSecFromMilli(testTime_mS));
    if (sbOut.length() > 0) { println(sbOut.toString()); }
    if (sbErr.length() > 0) { println(sbErr.toString()); }
    return testTime_mS;
  }

}
