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

import java.util.ConcurrentModificationException;

import com.appenginefan.toolkit.common.data.ProtoSchema.ConnectionState;
import com.appenginefan.toolkit.common.data.ProtoSchema.Message;
import com.appenginefan.toolkit.persistence.MapBasedPersistence;
import com.appenginefan.toolkit.persistence.Persistence;
import com.appenginefan.toolkit.persistence.ProtocolBufferPersistence;

import junit.framework.TestCase;

/**
 * Unit tests for the ServerEndpoint class
 */
public class ServerEndpointTest extends TestCase {
  
  private static final String KEY = "foo";
  private static final String VALUE = "bar";
  
  private Persistence<byte[]> binaryStore = new MapBasedPersistence<byte[]>();
  private Persistence<ConnectionState> protoStore = new ProtocolBufferPersistence<ConnectionState>(
      binaryStore, ConnectionState.getDefaultInstance());
  private ServerEndpoint bus = new ServerEndpoint(null, binaryStore) {
    @Override
    String getRandomKey() {
      return KEY;
    }
  };
  
  public void testOpen() {
    
    // Make sure that calling open once will populate the store
    assertNull(protoStore.get(KEY));
    bus.open();
    assertNotNull(protoStore.get(KEY));
    
    // Make sure that calling a second time will cause a problem
    try {
      bus.open();
      fail("Expected ConcurrentModificationException");
    } catch (ConcurrentModificationException expected) {
      // fall through
    }    
  }
  
  public void testCannotSetPropertyBeforeOpen() {
    try {
      bus.setProperty("foo", "bar");
      fail("Expected ConcurrentModificationException");
    } catch (ConcurrentModificationException expected) {
      // fall through
    }    
  }

  public void testCannotSendMessageBeforeOpen() {
    try {
      bus.send("23");
      fail("Expected ConcurrentModificationException");
    } catch (ConcurrentModificationException expected) {
      // fall through
    }    
  }
  
  public void testCloseBeforeOpen() {
    try {
      bus.close();
      fail("Expected ConcurrentModificationException");
    } catch (ConcurrentModificationException expected) {
      // fall through
    }    
  }

  public void testSaveBeforeOpen() {
    try {
      assertTrue(bus.save());
      fail("Expected ConcurrentModificationException");
    } catch (ConcurrentModificationException expected) {
      // fall through
    }    
  }
  
  public void testGetPropertyBeforeOpen() {
    try {
      bus.getProperty("foo", "bar");
      fail("Expected ConcurrentModificationException");
    } catch (ConcurrentModificationException expected) {
      // fall through
    }    
  }

  public void testClose() {
    
    // First, check that a "close" cleans up the datastore
    testOpen();
    assertNotNull(protoStore.get(KEY));
    bus.close();
    assertNull(protoStore.get(KEY));
    
    // Try to close a second time should fail
    try {
      bus.close();
      fail("Expected ConcurrentModificationException");
    } catch (ConcurrentModificationException expected) {
      // fall through
    }    
  }
  
  public void testGetNonExistentProperty() {
    testOpen();
    assertNull(bus.getProperty(KEY, null));
    assertEquals(bus.getProperty(KEY, VALUE), VALUE);
  }
  
  public void testGetUnsavedProperty() {
    testOpen();
    bus.setProperty(KEY, VALUE);
    assertEquals(VALUE, bus.getProperty(KEY, null));
    assertEquals(protoStore.get(KEY).getConnectionPropertiesCount(), 0);
  }

  public void testGetSavedProperty() {
    testOpen();
    bus.setProperty(KEY, VALUE);
    assertTrue(bus.save());
    assertEquals(VALUE, bus.getProperty(KEY, null));
    assertEquals(protoStore.get(KEY).getConnectionPropertiesCount(), 1);
    
    // Try to load directly from the store and re-populate the cache
    bus = new ServerEndpoint(KEY, binaryStore);
    assertEquals(VALUE, bus.getProperty(KEY, null));
  }
  
  public void testSendMessage() {
    testOpen();
    bus.send("23");
    assertEquals(protoStore.get(KEY).getMessageQueueCount(), 0);
    assertTrue(bus.save());
    assertEquals(protoStore.get(KEY).getMessageQueueCount(), 1);
    Message element = protoStore.get(KEY).getMessageQueue(0);
    assertEquals("23", element.getPayload());
    assertEquals(2, element.getAckToken());
  }

  public void testSendAckSend() {
    testSendMessage();
    bus.ack(2);
    bus.send("24");
    assertTrue(bus.save());
    assertEquals(protoStore.get(KEY).getMessageQueueCount(), 1);
    Message element = protoStore.get(KEY).getMessageQueue(0);
    assertEquals("24", element.getPayload());
    assertEquals(3, element.getAckToken());
  }
  
  public void testSaveAfterDelete() {
    testOpen();
    bus.send("23");
    bus.close();
    assertFalse(bus.save());
    assertEquals("23", bus.getUnsavedMessages().get(0));
    assertNotNull(bus.getSecretNoCheck());
    assertNotNull(bus.getHandleNoCheck());
  }
}
