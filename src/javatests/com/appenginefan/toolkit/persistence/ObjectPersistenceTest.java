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
 * ObjectPersistence. The rest should be covered by the unit
 * testsof StringPersistence.
 */
public class ObjectPersistenceTest
    extends TestCase {

  private ObjectPersistence<Integer> persistence =
      new ObjectPersistence<Integer>(
          new MapBasedPersistence<byte[]>());

  public void testSerialization() {
    for (Integer i : new Integer[] { 3, 18,
        Integer.MAX_VALUE, Integer.MIN_VALUE, null }) {
      assertEquals(i, persistence.makeType(persistence
          .makeArray(i)));
    }
  }

}
