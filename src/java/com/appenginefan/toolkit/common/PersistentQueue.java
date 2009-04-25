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
package com.appenginefan.toolkit.common;

import java.util.List;
import java.util.Map.Entry;

import com.appenginefan.toolkit.persistence.Persistence;
import com.appenginefan.toolkit.persistence.StoreException;
import com.appenginefan.toolkit.persistence.Utilities;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Preconditions;

/**
 * An <i>experimental!!</i> class that can be used to
 * persist objects in the data store in an ordered fashion.
 * This class does not implement the
 * <code>java.util.Queue</code> interface, since certain
 * aspects (like size) are inefficient to implement.
 * However, it tries to use the same method names and
 * semantics as the core Queue interface, thus making it an
 * easy class to use.
 * 
 * If you run into any problems, please let me know!
 */
public class PersistentQueue<T> {

  private final Counter idGenerator;

  private final Persistence<T> store;

  private final String prefix;

  /**
   * Creates a persistent queue, using the given backend and
   * a given name
   * 
   * @param name
   *          the name to be used for persisting elements
   *          (must not contain slashes)
   * @return a queue object
   */
  public static <T> PersistentQueue<T> createQueue(
      String name, Persistence<T> persistence) {
    return new PersistentQueue<T>(Counter
        .createIdGenerator(name), persistence, name);
  }

  /**
   * Constructor
   * 
   * @param idGenerator
   *          a counter (must be idgenerator-capable) that
   *          is used to maintain an order in the queue
   * @param store
   *          a place to persist queue elements
   * @param name
   *          a key that is used to persist the elements in
   *          the datastore. Must not contain any slashes.
   */
  public PersistentQueue(Counter idGenerator,
      Persistence<T> store, String name) {
    super();
    Preconditions.checkNotNull(idGenerator);
    Preconditions.checkNotNull(store);
    Preconditions.checkArgument(name.indexOf('/') < 0,
        "key must not contain any slashes: " + name);
    Preconditions.checkArgument(
        idGenerator.isIdGenerator(),
        "counter must be an id generator");
    this.idGenerator = idGenerator;
    this.store = store;
    this.prefix = '/' + name + '/';
  }

  /**
   * Helper: fetches the head of the queue
   * 
   * @param delete
   *          if set to true, delete the head from the store
   * @return the head object or null if it was empty
   */
  private T fetch(boolean delete) {
    List<Entry<String, T>> firstElement =
        Utilities.scanByPrefix(store, prefix, 1);
    if (firstElement.isEmpty()) {
      return null;
    }
    if (delete) {
      store.mutate(firstElement.get(0).getKey(), Functions
          .constant((T) null));
    }
    return firstElement.get(0).getValue();
  }

  /**
   * Helper: using the prefix, encodes a number that it will
   * be string-sorted correctly
   * 
   * @param number
   *          the number to encode
   * @return the encoded number
   */
  private String encode(long number) {
    if (number < 0) {
      throw new AssertionError("negative number");
    }
    StringBuilder sb = new StringBuilder();
    while (number > 0) {
      long current = number % 74;
      number /= 74;
      sb.append((char) ('0' + current));
    }
    while (sb.length() < 11) {
      sb.append('0');
    }
    return sb.reverse().insert(0, prefix).toString();
  }

  /**
   * Inserts the specified element into this queue, if
   * possible.
   * 
   * @param element
   *          the element to insert. null is not allowed.
   * @return true if it was possible to add the element to
   *         this queue, else false
   * @exception NullPointerException
   *              if the element was null
   */
  public boolean offer(final T element) {
    Preconditions.checkNotNull(element);
    try {
      store.mutate(encode(idGenerator.increment(1)),
          new Function<T, T>() {
            @Override
            public T apply(T arg0) {
              if (arg0 != null) {
                throw new StoreException();
              }
              return element;
            }
          });
    } catch (StoreException e) {
      return false;
    }
    return true;
  }

  /**
   * Retrieves and removes the head of this queue, or null
   * if this queue is empty.
   * 
   * @return the head of this queue, or null if this queue
   *         is empty.
   */
  public T poll() {
    return fetch(true);
  }

  /**
   * Retrieves, but does not remove, the head of this queue,
   * returning null if this queue is empty.
   * 
   * Returns: the head of this queue, or null if this queue
   * is empty.
   */

  public T peek() {
    return fetch(false);
  }

}
