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

import org.easymock.classextension.EasyMock;

import com.appenginefan.toolkit.persistence.DatastorePersistence;
import com.appenginefan.toolkit.persistence.LongPersistence;
import com.appenginefan.toolkit.persistence.MapBasedPersistence;
import com.appenginefan.toolkit.persistence.Persistence;
import com.appenginefan.toolkit.unittests.BaseTest;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.memcache.MemcacheService.SetPolicy;
import com.google.common.base.Functions;

/**
 * Unit tests for the Counter class
 */
public class CounterTest
    extends BaseTest {

  private static final String MEMCACHE_KEY = "aef/c/tst";

  private static final String STORE_PREFIX = "/tst/";

  private static final double CHANCE = 0.5;

  private static final int NUM_SHARDS = 10;

  private MemcacheService memcache;

  private Persistence<Long> datastore;

  private Random random;

  private Counter counter;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    random = EasyMock.createMock(Random.class);
    Persistence<byte[]> bytePersistence =
        new MapBasedPersistence<byte[]>();
    datastore = new LongPersistence(bytePersistence);
    memcache = EasyMock.createMock(MemcacheService.class);
    counter =
        new Counter(random, bytePersistence, memcache,
            CHANCE, "tst", NUM_SHARDS);
  }

  public void testEmptyStoreAndEmptyMemcacheWithSave() {

    // Get a value from the empty memcache. Set the memcache
    // to 0, since there is nothing in the store
    EasyMock.expect(memcache.get(MEMCACHE_KEY)).andReturn(
        null).once();
    EasyMock.expect(
        memcache.put(MEMCACHE_KEY, 0L, null,
            SetPolicy.ADD_ONLY_IF_NOT_PRESENT)).andReturn(
        true).once();

    // Before incrementing, check that the memcache is
    // populated
    EasyMock.expect(memcache.get(MEMCACHE_KEY)).andReturn(
        0L).once();
    EasyMock.expect(memcache.increment(MEMCACHE_KEY, 2))
        .andReturn(2L).once();

    // Test if we need to write (0.0 means always yes). If
    // so, pick "random shard" 3
    EasyMock.expect(random.nextDouble()).andReturn(0.0)
        .once();
    EasyMock.expect(random.nextInt(NUM_SHARDS))
        .andReturn(3).once();

    // Now, there should be a value in memcache, so let's
    // try to look it up.
    // We return a different value than what we stored,
    // which simulates a different
    // process having increased the counter
    EasyMock.expect(memcache.get(MEMCACHE_KEY)).andReturn(
        5L).once();

    // Now, let's replay the test and see if all
    // expectations are met
    EasyMock.replay(random, memcache);
    assertEquals(0L, counter.get());
    assertEquals(2L, counter.increment(2));
    assertEquals(5L, counter.get());
    assertEquals(2L, (long) datastore.get(STORE_PREFIX + 3));
    EasyMock.verify(random, memcache);
  }

  public void testPrepopulatedStoreAndEmptyMemcache() {
    datastore.mutate(STORE_PREFIX + 5, Functions
        .constant(10L));
    datastore.mutate(STORE_PREFIX + 7, Functions
        .constant(1L));

    // Get a value from the empty memcache. Set the memcache
    // to 10, since that is the max in the store
    EasyMock.expect(memcache.get(MEMCACHE_KEY)).andReturn(
        null).once();
    EasyMock.expect(
        memcache.put(MEMCACHE_KEY, 10L, null,
            SetPolicy.ADD_ONLY_IF_NOT_PRESENT)).andReturn(
        true).once();

    // Now, let's replay the test and see if all
    // expectations are met
    EasyMock.replay(random, memcache);
    assertEquals(10L, counter.get());
    EasyMock.verify(random, memcache);
  }

  public void testPrepopulatedStoreAndEmptyMemcacheUpdateFail() {
    datastore.mutate(STORE_PREFIX + 5, Functions
        .constant(10L));
    datastore.mutate(STORE_PREFIX + 7, Functions
        .constant(1L));

    // Get a value from the empty memcache. Set the memcache
    // to 10, since that is the max in the store.
    // We let the update fail by returning false
    EasyMock.expect(memcache.get(MEMCACHE_KEY)).andReturn(
        null).once();
    EasyMock.expect(
        memcache.put(MEMCACHE_KEY, 10L, null,
            SetPolicy.ADD_ONLY_IF_NOT_PRESENT)).andReturn(
        false).once();

    // Since the update failed, we expect the counter to do
    // an additional memcache check to get the latest and
    // greatest value
    EasyMock.expect(memcache.get(MEMCACHE_KEY)).andReturn(
        17L).once();

    // Now, let's replay the test and see if all
    // expectations are met
    EasyMock.replay(random, memcache);
    assertEquals(17L, counter.get());
    EasyMock.verify(random, memcache);
  }

  public void testIncrementCounterWithSave() {

    // Current counter is in memcache, so we can leave the
    // store empty for now
    EasyMock.expect(memcache.get(MEMCACHE_KEY)).andReturn(
        10L).once();

    // Perform an increment
    EasyMock.expect(memcache.increment(MEMCACHE_KEY, 2))
        .andReturn(4L).once();

    // Test if we need to write (0.5 barely means yes). If
    // so, pick "random shard" 8
    // we need one additional memcache.get, because
    // increment will check if the memcache is initialized
    EasyMock.expect(memcache.get(MEMCACHE_KEY)).andReturn(
        10L).once();
    EasyMock.expect(random.nextDouble()).andReturn(0.5)
        .once();
    EasyMock.expect(random.nextInt(NUM_SHARDS))
        .andReturn(8).once();

    // Now replay and check the datastore
    EasyMock.replay(random, memcache);
    assertEquals(10L, counter.get());
    assertEquals(4L, counter.increment(2));
    assertEquals((Long) 4L, datastore.get(STORE_PREFIX + 8));
    EasyMock.verify(random, memcache);
  }

  public void testIncrementCounterNoSave() {

    // Current counter is in memcache, so we can leave the
    // store empty for now
    EasyMock.expect(memcache.get(MEMCACHE_KEY)).andReturn(
        10L).once();

    // Perform an increment
    EasyMock.expect(memcache.increment(MEMCACHE_KEY, 2))
        .andReturn(4L).once();

    // Test if we need to write (0.6 means no).
    // We need one additional memcache.get, because
    // increment will check if the memcache is initialized.
    // We expect no call do random.nextDouble(), which means
    // that no shard is picked.
    EasyMock.expect(memcache.get(MEMCACHE_KEY)).andReturn(
        10L).once();
    EasyMock.expect(random.nextDouble()).andReturn(0.6)
        .once();

    // Now replay and check the datastore
    EasyMock.replay(random, memcache);
    assertEquals(10L, counter.get());
    assertEquals(4L, counter.increment(2));
    EasyMock.verify(random, memcache);
  }

  /**
   * Replicates one of the unit tests on the in-memory
   * backend.
   */
  public void testEmptyStoreAndEmptyMemcacheWithSaveForInMemoryAppEngine()
      throws Exception {

    // Set up in-memory App Engine
    DatastorePersistence bytePersistence =
        new DatastorePersistence("foo");
    datastore = new LongPersistence(bytePersistence);
    memcache = MemcacheServiceFactory.getMemcacheService();
    counter =
        new Counter(random, bytePersistence, memcache,
            CHANCE, "tst", NUM_SHARDS);

    // Run the test
    EasyMock.expect(random.nextDouble()).andReturn(0.0);
    EasyMock.expect(random.nextInt(NUM_SHARDS))
        .andReturn(3);
    EasyMock.replay(random);
    assertEquals(0L, counter.get());
    assertEquals(2L, counter.increment(2));
    assertEquals(2L, counter.get());
    assertEquals(2L, (long) datastore.get(STORE_PREFIX + 3));
    assertEquals(2L, memcache.get(MEMCACHE_KEY));
    EasyMock.verify(random);
  }
}
