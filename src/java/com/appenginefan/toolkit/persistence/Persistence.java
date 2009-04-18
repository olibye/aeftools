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
import java.util.Map;

import com.google.common.base.Function;

/**
 * Represents a simple way of persisting a particular object
 * type.
 */
public interface Persistence<T> {

  /**
   * Modifies an entry in the store.
   * 
   * @param key
   *          the key to modify
   * @param mutator
   *          a function that applies a change to the data
   *          and stores the new result. If the data does
   *          not exist in the store yet, null will be
   *          passed in. If the data should be deleted from
   *          the store, the function will return null.
   * @return the data that was stored
   * @exception NullPointerException
   *              if either of the arguments is null
   * @exception StoreException
   *              if something went wrong while storing data
   */
  public T mutate(String key,
      Function<? super T, ? extends T> mutator);

  /**
   * Gets an entry from the store.
   * 
   * @param key
   *          the key to look up the data from
   * @return the data or null, if the store does not contain
   *         data
   * @exception NullPointerException
   *              if either of the arguments is null
   * @exception StoreException
   *              if something went wrong while loading data
   */
  public T get(String key);

  /**
   * Finds zero or more entries that are within a given
   * range
   * 
   * @param start
   *          a lower bound of the range of keys to look in
   *          (inclusive)
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
  public List<Map.Entry<String, T>> scan(String start,
      String end, int max);

}
