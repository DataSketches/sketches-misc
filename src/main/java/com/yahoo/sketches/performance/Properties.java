/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.performance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

/**
 * @author Lee Rhodes
 */
public class Properties {
  private HashMap<String, String> map;

  public Properties() {
    map = new HashMap<>();
  }

  /**
   * Put a key-value pair into this map, replacing the value if the key already exists.
   * @param key the given key
   * @param value the given value
   * @return the previous value or null, if the key did not exist.
   */
  public String put(String key, String value) {
    return map.put(key, value);
  }

  /**
   * Get the value associated with the given key.
   * Throws exception if key is null or empty
   * @param key the given key
   * @return the value associated with the given key
   */
  public String mustGet(String key) {
    String v = map.get(key);
    if ((v == null) || (v.isEmpty())) {
      throw new IllegalArgumentException("Key: " + key + " not found or empty.");
    }
    return v;
  }

  /**
   * Get the value associated with the given key.
   * If the key does not exist, this returns null.
   *
   * @param key the given key
   * @return the value associated with the given key. It may be empty.
   */
  public String get(String key) {
    return map.get(key);
  }

  /**
   * Merge the given properties into this one. Any duplicate keys will be replaced with the
   * latest value.
   * @param prop the given Properties.
   * @return this Properties
   */
  public Properties merge(Properties prop) {
    String kvPairs = prop.extractKvPairs();
    loadKvPairs(kvPairs);
    return this;
  }

  /**
   * Load the string containing key-value pairs into the map.
   * key-value pairs are split by ",".
   * Each key-value pair is split by "[:=]".
   * @param kvPairs the given string
   */
  public void loadKvPairs(String kvPairs) {
    String[] pairs = kvPairs.split(",");
    for (String pair : pairs) {
      String[] kv = pair.split("[:=]", 2);
      String k = kv[0].trim();
      String v = kv[1].trim();
      map.put(k, v);
    }
  }

  /**
   * Extract a sorted String representing all the KV pairs of this map.
   * @return a sorted String representing all the KV pairs of this map.
   */
  public String extractKvPairs() {
    ArrayList<String> list = new ArrayList<>();
    map.forEach((key, value) -> {
      String s = key + "=" + value + ",";
      list.add(s);
    });
    list.sort(Comparator.naturalOrder());
    Iterator<String> itr = list.iterator();
    StringBuilder sb = new StringBuilder();
    itr.forEachRemaining((s) -> {
      sb.append(s);
    });
    return sb.toString();
  }

}
