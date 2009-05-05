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

import com.appenginefan.toolkit.unittests.BaseTest;
import com.google.appengine.repackaged.com.google.common.collect.Maps;
import com.google.common.base.Functions;
import com.google.common.collect.Lists;

/**
 * Unit tests for the utility functions
 */
public class UtilitiesTest
    extends BaseTest {

  private Persistence<String> persistence;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    persistence =
        new StringPersistence(new DatastorePersistence(
            null, " foo "));
    persistence.mutate("k1", Functions.constant("v1"));
    persistence.mutate("k2", Functions.constant("v2"));
  }

  @SuppressWarnings("unchecked")
  public void testScanByPrefix() {
    assertEquals(Lists.newArrayList(), Utilities
        .scanByPrefix(persistence, "f", 100));
    assertEquals(Lists.newArrayList(), Utilities
        .scanByPrefix(persistence, "k1b", 100));
    assertEquals(Lists.newArrayList(Maps.immutableEntry(
        "k1", "v1"), Maps.immutableEntry("k2", "v2")),
        Utilities.scanByPrefix(persistence, "k", 100));
    assertEquals(Lists.newArrayList(Maps.immutableEntry(
        "k1", "v1")), Utilities.scanByPrefix(persistence,
        "k1", 100));
    assertEquals(Lists.newArrayList(Maps.immutableEntry(
        "k1", "v1")), Utilities.scanByPrefix(persistence,
        "k", 1));
  }

  @SuppressWarnings("unchecked")
  public void testScanExclusive() {
    assertEquals(Lists.newArrayList(), Utilities
        .scanExclusive(persistence, "k1", "k2", 100));
    assertEquals(Lists.newArrayList(Maps.immutableEntry(
        "k1", "v1"), Maps.immutableEntry("k2", "v2")),
        Utilities.scanExclusive(persistence, "k", "l", 100));
    assertEquals(Lists.newArrayList(Maps.immutableEntry(
        "k1", "v1")), Utilities.scanExclusive(persistence,
        "k", "k2", 100));
  }

  @SuppressWarnings("unchecked")
  public void testScanReverseByPrefix() {
    assertEquals(Lists.newArrayList(), Utilities
        .scanReverseByPrefix(persistence, "f", 100));
    assertEquals(Lists.newArrayList(), Utilities
        .scanReverseByPrefix(persistence, "k1b", 100));
    assertEquals(Lists.newArrayList(Maps.immutableEntry(
        "k2", "v2"), Maps.immutableEntry("k1", "v1")),
        Utilities
            .scanReverseByPrefix(persistence, "k", 100));
    assertEquals(Lists.newArrayList(Maps.immutableEntry(
        "k1", "v1")), Utilities.scanReverseByPrefix(
        persistence, "k1", 100));
    assertEquals(Lists.newArrayList(Maps.immutableEntry(
        "k2", "v2")), Utilities.scanReverseByPrefix(
        persistence, "k", 1));
  }

  @SuppressWarnings("unchecked")
  public void testScanReverseExclusive() {
    assertEquals(Lists.newArrayList(), Utilities
        .scanReverseExclusive(persistence, "k1", "k2", 100));
    assertEquals(Lists.newArrayList(Maps.immutableEntry(
        "k2", "v2"), Maps.immutableEntry("k1", "v1")),
        Utilities.scanReverseExclusive(persistence, "k",
            "l", 100));
    assertEquals(Lists.newArrayList(Maps.immutableEntry(
        "k1", "v1")), Utilities.scanReverseExclusive(
        persistence, "k", "k2", 100));
  }
}
