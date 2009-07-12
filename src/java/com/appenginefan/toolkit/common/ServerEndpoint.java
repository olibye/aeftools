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
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import com.appenginefan.toolkit.common.data.ProtoSchema.ConnectionState;
import com.appenginefan.toolkit.common.data.ProtoSchema.Message;
import com.appenginefan.toolkit.common.data.ProtoSchema.Property;
import com.appenginefan.toolkit.persistence.Persistence;
import com.appenginefan.toolkit.persistence.ProtocolBufferPersistence;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * A single endpoint for a WebConnectionClient.
 *
 */
public class ServerEndpoint {
  
  private static final Logger LOG = Logger.getLogger(ServerEndpoint.class.getName());
  private final Persistence<ConnectionState> store;
  
  private String key;
  private Properties cachedProperties;
  private List<String> unsavedMessages = Lists.newArrayList();
  private String cachedSecret;
  private int highestAckedMessage = -1;
  private boolean isModified;
  private ConnectionState lastKnownState = null;
  
  String getRandomKey() {
    return String.valueOf(Math.random()).substring(2);
  }
  
  ServerEndpoint(String key, Persistence<byte[]> binaryStore) {
    Preconditions.checkNotNull(binaryStore);
    this.key = key;
    store = new ProtocolBufferPersistence<ConnectionState>(
        binaryStore, ConnectionState.getDefaultInstance());
  }
  
  /**
   * Make sure that the bus is open and not deleted from the store
   */
  private void checkOpen() {
    if (key == null) {
      throw new ConcurrentModificationException("bus is not open");
    }
    if (cachedProperties == null) {
      lastKnownState = store.get(key);
      if (lastKnownState == null) {
        throw new ConcurrentModificationException("bus has already been closed");
      }
      isModified = false;
      cachedSecret = lastKnownState.getRandomSecret();
      cachedProperties = new Properties();
      for (Property p : lastKnownState.getConnectionPropertiesList()) {
        cachedProperties.setProperty(p.getKey(), p.getValue());
      }
      if (lastKnownState.hasHighestAckedMessage()) {
      highestAckedMessage = Math.max(
          highestAckedMessage, lastKnownState.getHighestAckedMessage());
      }
    }
  }

  public void close() {
    checkOpen();
    store.mutate(key, Functions.constant((ConnectionState) null));
    isModified = false;
    cachedProperties = null;
  }

  public String getProperty(String key, String defaultValue) {
    checkOpen();
    return cachedProperties.getProperty(key, defaultValue);
  }

  public void open() {
    
    // Already open?
    if (key != null) {
      throw new ConcurrentModificationException("Cannot open twice");
    }
    
    // Try to create a new entry in the store
    isModified = false;
    while(key == null) {
      
      // Create random key and try to insert
      key = getRandomKey();
      store.mutate(key, new Function<ConnectionState, ConnectionState>(){
        @Override
        public ConnectionState apply(ConnectionState oldState) {
          
          // If oldState exists, that means there was a key collision
          if (oldState != null) {
            key = null;
            return oldState;
          }
          
          // No collision, so store a new empty object
          cachedSecret = String.valueOf(Math.random());
          return lastKnownState = ConnectionState
                                  .newBuilder()
                                  .setRandomSecret(cachedSecret)
                                  .build();
        }});
    }
  }

  public void send(String message) {
    checkOpen();
    isModified = true;
    unsavedMessages.add(message);
  }

  public void setProperty(String key, String valueOrNull) {
    checkOpen();
    isModified = true;
    if (valueOrNull != null) {
      cachedProperties.setProperty(key, valueOrNull);
    } else {
      cachedProperties.remove(valueOrNull);
    }
  }

  boolean save() {
    if (key == null) {
      throw new ConcurrentModificationException("bus is not open");
    }
    if (cachedProperties == null) {
      return false;
    }
    if (!isModified) {
      return true;
    }
    ConnectionState result = store.mutate(key, new Function<ConnectionState, ConnectionState>(){
      @Override
      public ConnectionState apply(ConnectionState oldState) {
        
        // If oldState exists, that means there was a deletion in-between
        if (oldState == null) {
          key = null;
          return oldState;
        }
        
        // Build a new object from the old one
        ConnectionState.Builder builder = ConnectionState.newBuilder(oldState);
        
        // Overwrite all connection properties
        builder.clearConnectionProperties();
        for (Object prop : cachedProperties.keySet()) {
          builder.addConnectionProperties(
              Property.newBuilder()
              .setKey((String) prop)
              .setValue(cachedProperties.getProperty((String) prop))
              .build());
        }
        
        // Remember the highest acked message
        if (builder.hasHighestAckedMessage()) {
          highestAckedMessage = Math.max(
              highestAckedMessage, builder.getHighestAckedMessage());
        }
        builder.setHighestAckedMessage(highestAckedMessage);
        
        // Remove any acked message
        int maxId = Math.max(highestAckedMessage, 1);
        builder.clearMessageQueue();
        final int oldMessageCount = oldState.getMessageQueueCount();
        for (int i = 0; i < oldMessageCount; i++) {
          Message messageInQueue = oldState.getMessageQueue(i);
          maxId = Math.max(maxId, messageInQueue.getAckToken());
          if (messageInQueue.getAckToken() > highestAckedMessage) {
            builder.addMessageQueue(messageInQueue);
          }
        }
        
        // Attach any cached messages
        if (!unsavedMessages.isEmpty()) {
          maxId++;
          for (String payload : unsavedMessages) {
            builder.addMessageQueue(
                Message.newBuilder()
                .setAckToken(maxId)
                .setPayload(payload)
                .build());
          }
        }
        
        // Done
        return lastKnownState = builder.build();
      }});
    
    // If there were any unsaved messages and we arrived at this point,
    // they should be persisted
    if (unsavedMessages.size() > 0) {
      unsavedMessages.clear();
      LOG.finer("Current state of endpoint " + getHandleNoCheck() + ": " + result);
    }
    return true;
  }
  
  public String getHandle() {
    checkOpen();
    return key;
  }
  
  String getHandleNoCheck() {
    return key;
  }
  
  String getSecret() {
    checkOpen();
    return cachedSecret;
  }
  
  String getSecretNoCheck() {
    return cachedSecret;
  }
  
  void ack(int lastAckTocken) {
    checkOpen();
    isModified = true;
    highestAckedMessage = Math.max(highestAckedMessage, lastAckTocken);
  }
  
  ConnectionState getLastKnownState() {
    return lastKnownState;
  }
  
  List<String> getUnsavedMessages() {
    return unsavedMessages;
  }
}
