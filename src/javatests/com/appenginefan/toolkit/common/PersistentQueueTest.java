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

import org.easymock.classextension.EasyMock;

import junit.framework.TestCase;

import com.appenginefan.toolkit.persistence.MapBasedPersistence;
import com.appenginefan.toolkit.persistence.Persistence;

/**
 * Unit tests for PersistentQueue
 */
public class PersistentQueueTest
    extends TestCase {

  private Counter counter;

  private Persistence<String> store;

  private PersistentQueue<String> queue;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    counter = EasyMock.createMock(Counter.class);
    store = new MapBasedPersistence<String>();
    EasyMock.expect(counter.isIdGenerator())
        .andReturn(true).anyTimes();
    EasyMock.replay(counter);
    queue =
        new PersistentQueue<String>(counter, store, "foo");
    EasyMock.reset(counter);
  }

  public void testRegularUsecase() {
    EasyMock.expect(counter.increment(1)).andReturn(3L);
    EasyMock.expect(counter.increment(1)).andReturn(4L);
    EasyMock.expect(counter.increment(1)).andReturn(
        Long.MAX_VALUE);
    EasyMock.expect(counter.increment(1)).andReturn(
        (long) Integer.MAX_VALUE);
    EasyMock.replay(counter);
    assertTrue(queue.offer("foo"));
    assertTrue(queue.offer("bar"));
    assertTrue(queue.offer("last entry"));
    assertTrue(queue.offer("foobar"));
    assertEquals("foo", store.get("/foo/00000000003"));
    assertEquals("bar", store.get("/foo/00000000004"));
    for (String entry : new String[] { "foo", "bar",
        "foobar", "last entry", null, null }) {
      assertEquals(entry, queue.peek());
      assertEquals(entry, queue.peek());
      assertEquals(entry, queue.poll());
      if (entry != null) {
        assertNotSame(entry, queue.peek());
      }
    }
    EasyMock.verify(counter);
  }

}
