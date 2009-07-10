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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.appenginefan.toolkit.common.data.ProtoSchema.Message;
import com.appenginefan.toolkit.persistence.Persistence;

/**
 * Concrete implementation of the inner guts of the WebConnectionServer
 */
class WebConnectionServerImpl implements WebConnectionServer.ServerGuts {
  
  static final String CACHE_PARAM = "WebConnectionServerImpl.cache";
  
  private static final Pattern META_PATTERN = 
      Pattern.compile("(\\d+)\\.(\\d+)\\.(.+)");
  
  static final String sign(String secret, String handle, String lastAckToken) {
    try {
      String signThis = secret + "--sign-this-with-SHA-1--" + handle;
      MessageDigest digest = MessageDigest.getInstance("SHA-1");
      digest.update(signThis.getBytes("iso-8859-1"), 0, signThis.length());
      return new BigInteger(digest.digest()).toString(Character.MAX_RADIX);
    } catch (NoSuchAlgorithmException e) {
      return "NOOP";
    } catch (UnsupportedEncodingException e) {
      return "NOENC";
    }
  }
  
  private final Persistence<byte[]> binaryStore;
  
  WebConnectionServerImpl(Persistence<byte[]> binaryStore) {
    this.binaryStore = binaryStore;
  }
  
  /**
   * @return a request-specific map that can be used to cache server endpoints
   */
  @SuppressWarnings("unchecked")
  Map<String, ServerEndpoint> getCache(HttpServletRequest request) {
    Map<String, ServerEndpoint> result = 
      (Map<String, ServerEndpoint>) request.getAttribute(CACHE_PARAM);
    if (result == null) {
      result = new HashMap<String, ServerEndpoint>();
      request.setAttribute(CACHE_PARAM, result);
    }
    return result;
  }

  @Override
  public ServerEndpoint fromHandle(HttpServletRequest request, String handle) {
    Map<String, ServerEndpoint> cache = getCache(request);
    if (cache.containsKey(handle)) {
      return cache.get(handle);
    }
    ServerEndpoint result = new ServerEndpoint(handle, binaryStore);
    cache.put(handle, result);
    return result;
  }

  @Override
  public final ServerEndpoint loadServerEndpoint(
      HttpServletRequest request, String meta) {
    
    // Null or malformed meta?
    if (meta == null) {
      return null;
    }
    Matcher matcher = META_PATTERN.matcher(meta);
    if (!matcher.matches()) {
      return null;
    }
    
    // Incorrectly signed meta?
    String handle = matcher.group(1);
    String lastAckToken = matcher.group(2);
    ServerEndpoint endpoint = (ServerEndpoint) fromHandle(request, handle);
    if (!sign(endpoint.getSecret(), handle, lastAckToken).equals(matcher.group(3))) {
      return null;
    }
    
    // Kick acknowledged messages out of the store
    endpoint.ack(Integer.parseInt(lastAckToken));

    // Done
    return endpoint;
  }

  @Override
  public ServerEndpoint newServerEndpoint(final HttpServletRequest request) {
    return new ServerEndpoint(null, binaryStore) {
      @Override
      public void open() {
        super.open();
        getCache(request).put(getHandle(), this);
      }
    };
  }

  @Override
  public String toHandle(ServerEndpoint endpoint) {
    return endpoint.getHandle();
  }

  @Override
  public void rollback(HttpServletRequest request) {
    request.setAttribute(CACHE_PARAM, null);
  }
  
  @Override
  public void commit(HttpServletRequest request) {
    for (ServerEndpoint endpoint : getCache(request).values()) {
      endpoint.save();
    }
    rollback(request);
  }

  @Override
  public void writeState(
      HttpServletRequest request,
      ServerEndpoint endpoint,
      HttpServletResponse response) throws IOException {
    
    // Persist the store to get to a consistent state
    final ServerEndpoint storeBus = (ServerEndpoint) endpoint;
    final boolean connectionIsAlive = storeBus.save();
    final PayloadBuilder payload = new PayloadBuilder();
    if (connectionIsAlive) {
      List<Message> messages = 
          storeBus.getLastKnownState().getMessageQueueList();
      
      // Create the meta
      final String handle = storeBus.getHandle();
      final String lastAckToken = 
        messages.isEmpty() 
          ? "0" 
          : String.valueOf(messages.get(messages.size() - 1).getAckToken());
      final String signature = sign(storeBus.getSecret(), handle, lastAckToken);
      final String meta = String.format("%s.%s.%s", handle, lastAckToken, signature);
      
      // Now, add all the outgoing messages
      payload.setProperty(WebConnectionClient.META, meta);
      for (Message message : messages) {
        payload.addPayload(message.getPayload());
      }
    } else {
      
      // Get the handle out there, knowing that this bus will never work again in the future
      final String handle = storeBus.getHandleNoCheck();
      final String signature = sign(storeBus.getSecretNoCheck(), handle, "0");
      final String meta = String.format("%s.0.%s", handle, signature);
      payload.setProperty(WebConnectionClient.META, meta);
      payload.setProperty(WebConnectionClient.CLOSED, "1");
      
      // There are no messages left in the store; we just send what has not been saved yet
      for (String message : storeBus.getUnsavedMessages()) {
        payload.addPayload(message);
      }
    }
    
    // Now, we can write the data to the outgoing stream
    response.getOutputStream().println(payload.toString());
  }

}
