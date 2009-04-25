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

import java.util.Random;
import java.util.Map.Entry;

import com.appenginefan.toolkit.persistence.DatastorePersistence;
import com.appenginefan.toolkit.persistence.LongPersistence;
import com.appenginefan.toolkit.persistence.Persistence;
import com.appenginefan.toolkit.persistence.Utilities;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.memcache.MemcacheService.SetPolicy;
import com.google.appengine.repackaged.com.google.common.base.Preconditions;
import com.google.common.base.Function;

/**
 * A class that utilizes a combination of memcache and the
 * datastore backend to implement a fast yet reliable
 * counter. Counts are increased transactionally in memcache
 * and occasionally persisted in the datastore. Sharding
 * techniques are used to minmize the chance of blockage in
 * the store. The frequency of writes can be somewhat
 * controlled, from a guaranteed write (useful for id
 * generators) to an occasional write (page counters). Fully
 * reliable counter objects (with a write-probability of 1)
 * are guaranteed to always be increasing, which is a useful
 * property for storing ordered entities in the store.
 * 
 * For more background, check out <a href="http://blog.appenginefan.com/2009/04/efficient-global-counters-revisited.html"
 * > this blog post</a>
 */
public class Counter {

  private static final String PARTITION = "common:counter";

  /**
   * Creates a counter that is not completely reliable (it
   * might loose counts if memcache is resetted)
   * 
   * @param key
   *          a key that is used to persist the counter
   *          shards in the datastore and memcache. must not
   *          contain any slashes
   * @param chanceToWrite
   *          the relative probability that changes to
   *          memcache get persisted. Must be between 0 and
   *          1. The higher the value, the more reliable the
   *          counter is.
   * @return a counter object
   */
  public static Counter createPageCounter(String key,
      double chanceToWrite) {
    return new Counter(new Random(),
        new DatastorePersistence(PARTITION),
        MemcacheServiceFactory.getMemcacheService(),
        chanceToWrite, key, 50);
  }

  /**
   * Creates a counter that does not loose count (in other
   * words, it writes to the data store all the time).
   * 
   * @param key
   *          a key that is used to persist the counter
   *          shards in the datastore and memcache. must not
   *          contain any slashes
   * @return a counter object
   */
  public static Counter createIdGenerator(String key) {
    return createPageCounter(key, 1.0);
  }

  private final Random random;

  private final Persistence<Long> persistence;

  private final MemcacheService memcache;

  private final double chanceToWrite;

  private final String prefix;

  private final String memcacheKey;

  private final int numShards;

  /**
   * Constructor
   * 
   * @param random
   *          a random number generator
   * @param persistence
   *          a persistence that can be used to write to the
   *          datastore
   * @param memcache
   *          a memcache service for quick access to shared
   *          transactional numbers
   * @param chanceToWrite
   *          a value between 0.0 and 1.0 (inclusive). Each
   *          time the counter gets increased, a random
   *          throw of the dice decides whether to write to
   *          the store. A chanceToWrite of 1.0 means that
   *          every change in the counter will be persisted;
   *          a chanceToWrite of 0.0 means that no change
   *          will be persisted
   * @param key
   *          a key that is used to persist the counter
   *          shards in the datastore and memcache. must not
   *          contain any slashes
   * @param numShards
   *          the number of shards that should be used to
   *          store the value. The more shards the less the
   *          chance of collision on writes, but the longer
   *          it will take to load shards from the store if
   *          memcache has been evicted
   */
  public Counter(Random random,
      Persistence<byte[]> persistence,
      MemcacheService memcache, double chanceToWrite,
      String key, int numShards) {
    super();
    Preconditions.checkNotNull(random);
    Preconditions.checkNotNull(memcache);
    Preconditions.checkNotNull(memcache);
    Preconditions.checkArgument(chanceToWrite >= 0.0
        && chanceToWrite <= 1.0,
        "chanceToWrite must be bewteen 0.0 and 1.0");
    Preconditions.checkArgument(key.indexOf('/') < 0,
        "key must not contain any slashes: " + key);
    Preconditions
        .checkArgument(numShards > 0 && numShards < 1000,
            "there must be at least one shard, but no more than 999");
    this.random = random;
    this.persistence = new LongPersistence(persistence);
    this.memcache = memcache;
    this.chanceToWrite = chanceToWrite;
    this.prefix = '/' + key + '/';
    this.memcacheKey = "aef/c/" + key;
    this.numShards = numShards;
  }

  /**
   * Makes sure that the memcache is populated. If the
   * memcache is prepopulated, or this process was
   * successful in updating memcache from the datastore,
   * return the known value. Otherwise, return null.
   */
  private Long populateMemcache() {
    Long result = (Long) memcache.get(memcacheKey);
    if (result == null) {
      long max = 0;
      for (Entry<String, Long> shard : Utilities
          .scanByPrefix(persistence, prefix, 1000)) {
        max = Math.max(max, shard.getValue());
      }
      boolean changed =
          memcache.put(memcacheKey, max, null,
              SetPolicy.ADD_ONLY_IF_NOT_PRESENT);
      if (changed) {
        result = max;
      }
    }
    return result;
  }

  /**
   * Gets the current value
   */
  public long get() {
    Long prepopulated = populateMemcache();
    return (prepopulated != null) ? prepopulated
        : (Long) memcache.get(memcacheKey);
  }

  /**
   * Increments the value by a positive delta
   * 
   * @param delta
   * @return the value that the counter was changed to
   */
  public long increment(long delta) {
    Preconditions.checkArgument(delta > 0,
        "delta must be a positive value");
    populateMemcache();
    final Long result =
        memcache.increment(memcacheKey, delta);
    if (random.nextDouble() <= chanceToWrite) {
      String shardKey = prefix + random.nextInt(numShards);
      persistence.mutate(shardKey,
          new Function<Long, Long>() {
            @Override
            public Long apply(Long oldValue) {
              if (oldValue == null) {
                return result;
              }
              return Math.max(oldValue, result);
            }
          });
    }
    return result;
  }

  /**
   * Returns true if the counter is writing to the store
   * each and every time, which makes it possible to use as
   * an id generator.
   * 
   * @return true if the chance-to-write is 1.0
   */
  public boolean isIdGenerator() {
    return chanceToWrite == 1.0;
  }
}
