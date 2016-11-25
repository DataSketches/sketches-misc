/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.hll;

import static com.yahoo.sketches.hll.ProcessIpStream.checkLen;
import static com.yahoo.sketches.hll.ProcessIpStream.printStats;
import static com.yahoo.sketches.hll.ProcessIpStream.printTaskTime;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.yahoo.sketches.Util;


/**
 * Processes an input stream of pairs of integers from Standard-In into the UniqueCountMap.
 * The input stream defines a distribution whereby each pair defines the number of keys with the
 * corresponding number of unique IDs. Each pair is of the form:
 *
 * <p><code>&lt;NumIDs&gt;&lt;TAB&gt;&lt;NumKeys&gt;&lt;line-separator&gt;.</code></p>
 *
 * <p>For each input pair, this model generates <i>NumIDs</i> unique identifiers for each of
 * <i>NumKeys</i> (also unique) and inputs them into the UniqueCountMap.</p>
 *
 * <p>The end of the stream is a null input line.</p>
 *
 * <p>At the end of the stream, UniqueCountMap.toString() is called and sent to Standard-Out.</p>
 *
 * <p>To run, create a jar of the test code for sketches-core.
 * A typical command line might be as follows:</p>
 *
 * <p><code>
 * cat NumIDsTabNumKeys.txt | java -cp sketches-misc.jar:sketches-core.jar:memory.jar \
 * com.yahoo.sketches.hll.ProcessDistributionStream
 * </code></p>
 */
public final class ProcessDistributionStream {
  private static final int IP_BYTES = 4;
  private static final int INIT_ENTRIES = 1000;

  private ProcessDistributionStream() {}

  /**
   * Main entry point.
   * @param args Not used.
   * @throws RuntimeException Generally an IOException.
   */
  public static void main(final String[] args) throws RuntimeException {
    final ProcessDistributionStream pds = new ProcessDistributionStream();
    pds.processDistributionModel();
  }

  private void processDistributionModel() {
    final StringBuilder sb = new StringBuilder();
    final long start_mS = System.currentTimeMillis();
    String line = "";
    long lineCount = 0;
    long updateCount = 0;
    int ipCount = 0;
    final byte[] ipBytes = new byte[IP_BYTES];
    final byte[] valBytes = new byte[Long.BYTES];

    final UniqueCountMap map = new UniqueCountMap(INIT_ENTRIES, IP_BYTES);
    long updateTime_nS = 0;
    try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in, UTF_8))) {

      while ((line = br.readLine()) != null) {
        final String[] tokens = line.split("\t");
        checkLen(tokens);
        lineCount++;
        final long numValues = Long.parseLong(tokens[0]); //Verify the token order!
        final long numIps = Long.parseLong(tokens[1]);

        for (long nips = 0; nips < numIps; nips++) {
          ipCount++;
          Util.intToBytes(ipCount, ipBytes);
          for (long vals = 0; vals < numValues; vals++) {
            final long start_nS = System.nanoTime();
            updateCount++; // never repeated for any ip
            map.update(ipBytes, Util.longToBytes(updateCount, valBytes));
            final long end_nS = System.nanoTime();
            updateTime_nS += end_nS - start_nS;
          }
        }
      }

      final String className = this.getClass().getSimpleName();
      printStats(sb, className, map, lineCount, ipCount, updateCount, updateTime_nS);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
    final long total_mS = System.currentTimeMillis() - start_mS;
    printTaskTime(sb, total_mS, updateCount);
  }

}
