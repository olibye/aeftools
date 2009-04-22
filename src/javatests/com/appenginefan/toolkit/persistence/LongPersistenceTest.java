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

import junit.framework.TestCase;

/**
 * Tests the object serialization/deserialization methods of
 * LongPersistence. The rest should be covered by the unit
 * test sof StringPersistence.
 */
public class LongPersistenceTest
    extends TestCase {

  private LongPersistence persistence =
      new LongPersistence(new MapBasedPersistence<byte[]>());

  public void testSerialization() {
    for (Long l : new Long[] { 0L, 3L, 18L,
        (long) Integer.MAX_VALUE, (long) Integer.MIN_VALUE,
        Long.MAX_VALUE, Long.MIN_VALUE }) {
      assertEquals(l, persistence.makeType(persistence
          .makeArray(l)));
    }
  }
}
