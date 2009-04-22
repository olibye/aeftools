/*
 * Copyright (c) 2009 Jens Scheffler (appenginefan.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.appenginefan.toolkit.persistence;

import java.util.List;
import java.util.Map.Entry;

/**
 * A set of useful static utility functions on Persistence
 * objects
 */
public class Utilities {

  private Utilities() {
  }

  /**
   * Finds zero or more entries that start with a given
   * prefix
   * 
   * @param prefix
   *          a string that all keys found should start
   *          with.
   * @param max
   *          a maximum amount of elements to return. The
   *          implementation of the store may choose to
   *          return less (for example, if a store can only
   *          fetch 10 elements per query, then setting a
   *          max of 1000 will still only return 10), but
   *          never more.
   * @return a list of up to max key/value pairs, ordered by
   *         key
   */
  public static <T> List<Entry<String, T>> scanByPrefix(
      Persistence<T> persistence, String prefix, int max) {
    return persistence.scan(prefix, prefix
        + Character.MAX_VALUE, max);
  }

  /**
   * Finds zero or more entries that are within a given
   * range, with the start also being exclusive.
   * 
   * @param start
   *          a lower bound of the range of keys to look in
   *          (<b>exclusive</b>)
   * @param end
   *          an upper bound of the range of keys to look in
   *          (exclusive)
   * @param max
   *          a maximum amount of elements to return. The
   *          implementation of the store may choose to
   *          return less (for example, if a store can only
   *          fetch 10 elements per query, then setting a
   *          max of 1000 will still only return 10), but
   *          never more.
   * @return a list of up to max key/value pairs, ordered by
   *         key
   */
  public static <T> List<Entry<String, T>> scanExclusive(
      Persistence<T> persistence, String start, String end,
      int max) {
    return persistence.scan(start + Character.MIN_VALUE,
        end, max);
  }
}
