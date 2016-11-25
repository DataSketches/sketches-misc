/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.demo;

import static com.yahoo.sketches.demo.Util.println;
import static java.nio.charset.StandardCharsets.UTF_8;

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
  public static long run(final String name, final String cmd) {
    final StringBuilder sbOut = new StringBuilder();
    final StringBuilder sbErr = new StringBuilder();
    String out = null;
    String err = null;
    final String[] envp = {"LC_ALL=C"}; //https://bugs.launchpad.net/ubuntu/+source/coreutils/+bug/846628
    Process p = null;
    try {
      p = Runtime.getRuntime().exec(cmd, envp);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
    final long testStartTime_mS = System.currentTimeMillis();

    try (
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream(), UTF_8));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream(), UTF_8));
    ) {
      // run the Unix cmd using the Runtime exec method:
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
    catch (final IOException e) {
      System.out.println("IOException: ");
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    if (p.isAlive()) {
      p.destroy();
    }
    final long testTime_mS = System.currentTimeMillis() - testStartTime_mS;
    println("Unix cmd: " + cmd);
    println(Util.getMinSecFromMilli(testTime_mS));
    if (sbOut.length() > 0) { println(sbOut.toString()); }
    if (sbErr.length() > 0) { println(sbErr.toString()); }
    return testTime_mS;
  }

}
