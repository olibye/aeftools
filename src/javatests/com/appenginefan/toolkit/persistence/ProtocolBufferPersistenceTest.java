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

import com.appenginefan.toolkit.persistence.TestData.Person;

import junit.framework.TestCase;

/**
 * Tests the object serialization/deserialization methods of
 * ProtocolBufferPersistence. The rest should be covered by
 * the unit testsof StringPersistence.
 */
public class ProtocolBufferPersistenceTest
    extends TestCase {

  private ProtocolBufferPersistence<TestData.Person> persistence =
      new ProtocolBufferPersistence<TestData.Person>(
          new MapBasedPersistence<byte[]>(),
          TestData.Person.newBuilder().buildPartial());

  public void testSerialization() {
    Person original =
        TestData.Person.newBuilder().setEmail(
            "foo@example.com").setName("John Doe").build();
    Person cloned =
        persistence.makeType(persistence
            .makeArray(original));
    assertTrue(original != cloned);
    assertEquals(cloned, original);
  }

}
