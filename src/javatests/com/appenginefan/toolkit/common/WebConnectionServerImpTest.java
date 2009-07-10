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

import java.util.Collections;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.easymock.IMocksControl;
import org.easymock.classextension.EasyMock;

import com.appenginefan.toolkit.common.data.ProtoSchema.ConnectionState;
import com.appenginefan.toolkit.common.data.ProtoSchema.Message;
import com.appenginefan.toolkit.persistence.MapBasedPersistence;
import com.appenginefan.toolkit.persistence.Persistence;
import com.google.common.collect.Lists;

import junit.framework.TestCase;

/**
 * Unit tests for the guts of a WebConnectionServer
 * 
 * @author Jens Scheffler
 */
public class WebConnectionServerImpTest extends TestCase {
  
  private static final String HANDLE = "23";
  private static final String SECRET = "secret";
  private static final Integer LAST_ACK = 17;
  private static final String META = 
    "23.17." + WebConnectionServerImpl.sign(SECRET, HANDLE, "1" + LAST_ACK);
  
  private IMocksControl control = EasyMock.createStrictControl();
  
  private Persistence<byte[]> store = control.createMock(Persistence.class);
  private WebConnectionServerImpl server = new WebConnectionServerImpl(store);
  private HttpServletRequest req = control.createMock(HttpServletRequest.class); 
  private HttpServletResponse res = control.createMock(HttpServletResponse.class);
  private Map<String, ServerEndpoint> cache = control.createMock(Map.class);
  private ServerEndpoint endpoint = control.createMock(ServerEndpoint.class);
  
  public void testSign() {
    
    // Calling the sign method twice will produce the same result
    String s1 = WebConnectionServerImpl.sign("secret1", "foo", "bar");
    assertNotNull(s1);
    assertFalse(s1.isEmpty());
    assertEquals(s1, WebConnectionServerImpl.sign("secret1", "foo", "bar"));
    
    // Using a different signature will prodce a different result
    String s2 = WebConnectionServerImpl.sign("secret2", "foo", "bar");
    assertNotNull(s2);
    assertFalse(s2.isEmpty());
    assertNotSame(s1, s2);
  }

  public void testCreateNewCache() {
    EasyMock.expect(req.getAttribute(WebConnectionServerImpl.CACHE_PARAM)).andReturn(null);
    req.setAttribute(
        EasyMock.eq(WebConnectionServerImpl.CACHE_PARAM),
        (Map<String, ServerEndpoint>) EasyMock.anyObject());
    control.replay();
    assertNotNull(server.getCache(req));
    control.verify();
  }
  
  public void testReuseExistingCache() {
    EasyMock.expect(req.getAttribute(WebConnectionServerImpl.CACHE_PARAM)).andReturn(cache);
    control.replay();
    assertEquals(cache, server.getCache(req));
    control.verify();
  }

  public void testFromHandleCached() {
    EasyMock.expect(req.getAttribute(WebConnectionServerImpl.CACHE_PARAM)).andReturn(cache);
    EasyMock.expect(cache.containsKey(HANDLE)).andReturn(true);
    EasyMock.expect(cache.get(HANDLE)).andReturn(endpoint);
    control.replay();
    assertEquals(endpoint, server.fromHandle(req, HANDLE));
    control.verify();
  }

  public void testFromHandleUnCached() {
    EasyMock.expect(req.getAttribute(WebConnectionServerImpl.CACHE_PARAM)).andReturn(cache);
    EasyMock.expect(cache.containsKey(HANDLE)).andReturn(false);
    EasyMock.expect(cache.put(EasyMock.eq(HANDLE), (ServerEndpoint) EasyMock.anyObject()))
    .andReturn(null);
    control.replay();
    assertNotNull(server.fromHandle(req, HANDLE));
    control.verify();
  }
  
  public void testLoadWithoutHandle() {
    control.replay();
    assertNull(server.loadServerEndpoint(req, null));
    assertNull(server.loadServerEndpoint(req, ""));
    assertNull(server.loadServerEndpoint(req, "randomString"));
    control.verify();
  }
  
  public void testLoadWithWrongSignature() {
    EasyMock.expect(req.getAttribute(WebConnectionServerImpl.CACHE_PARAM)).andReturn(cache);
    EasyMock.expect(cache.containsKey(HANDLE)).andReturn(true);
    EasyMock.expect(cache.get(HANDLE)).andReturn(endpoint);
    EasyMock.expect(endpoint.getSecret()).andReturn(SECRET);
    control.replay();
    assertNull(server.loadServerEndpoint(req, META + "123"));
    control.verify();
  }
  
  public void testLoadWithCorrectMeta() {
    EasyMock.expect(req.getAttribute(WebConnectionServerImpl.CACHE_PARAM)).andReturn(cache);
    EasyMock.expect(cache.containsKey(HANDLE)).andReturn(true);
    EasyMock.expect(cache.get(HANDLE)).andReturn(endpoint);
    EasyMock.expect(endpoint.getSecret()).andReturn(SECRET);
    endpoint.ack(LAST_ACK);
    control.replay();
    assertEquals(endpoint, server.loadServerEndpoint(req, META));
    control.verify();
  }
  
  public void testNewEndpointWillCacheUponOpen() {
    store = new MapBasedPersistence<byte[]>();
    server = new WebConnectionServerImpl(store);
    EasyMock.expect(req.getAttribute(WebConnectionServerImpl.CACHE_PARAM)).andReturn(cache);
    EasyMock.expect(cache.put(
        (String) EasyMock.anyObject(), 
        (ServerEndpoint) EasyMock.anyObject()))
    .andReturn(endpoint);
    control.replay();
    endpoint = server.newServerEndpoint(req);
    endpoint.open();
    control.verify();
  }
  
  public void testToHandle() {
    EasyMock.expect(endpoint.getHandle()).andReturn(HANDLE);
    control.replay();
    assertEquals(HANDLE, server.toHandle(endpoint));
    control.verify();
  }
  
  public void testRollbackEmptiesCache() {
    req.setAttribute(WebConnectionServerImpl.CACHE_PARAM, null);
    control.replay();
    server.rollback(req);
    control.verify();
  }
  
  public void testCommitSavesEndpoints() {
    EasyMock.expect(
        req.getAttribute(WebConnectionServerImpl.CACHE_PARAM)).andReturn(cache);
    EasyMock.expect(cache.values()).andReturn(Collections.singleton(endpoint));
    EasyMock.expect(endpoint.save()).andReturn(true);
    req.setAttribute(WebConnectionServerImpl.CACHE_PARAM, null);
    control.replay();
    server.commit(req);
    control.verify();
  }
  
  public void testWriteStateExistingBus() throws Exception {
    ServletOutputStream out = control.createMock(ServletOutputStream.class);
    EasyMock.expect(endpoint.save()).andReturn(true);
    EasyMock.expect(endpoint.getLastKnownState()).andReturn(
        ConnectionState
        .newBuilder()
        .addMessageQueue(
            Message
            .newBuilder()
            .setPayload("foo")
            .setAckToken(LAST_ACK)
            .build())
        .setRandomSecret("secret")
        .build());
    EasyMock.expect(endpoint.getHandle()).andReturn(HANDLE);
    EasyMock.expect(endpoint.getSecret()).andReturn(SECRET);
    EasyMock.expect(res.getOutputStream()).andReturn(out);
    out.println(
        new PayloadBuilder()
        .addPayload("foo")
        .setProperty(WebConnectionClient.META, META)
        .toString());
    control.replay();
    server.writeState(req, endpoint, res);
    control.verify();
  }

  public void testWriteStateRecentlyDeletedBus() throws Exception {
    ServletOutputStream out = control.createMock(ServletOutputStream.class);
    EasyMock.expect(endpoint.save()).andReturn(false);
    EasyMock.expect(endpoint.getHandleNoCheck()).andReturn(HANDLE);
    EasyMock.expect(endpoint.getSecretNoCheck()).andReturn(SECRET);
    EasyMock.expect(endpoint.getUnsavedMessages()).andReturn(Lists.newArrayList("foo"));
    EasyMock.expect(res.getOutputStream()).andReturn(out);
    out.println(
        new PayloadBuilder()
        .addPayload("foo")
        .setProperty(WebConnectionClient.META, "23.0." +
            WebConnectionServerImpl.sign(SECRET, HANDLE, "0"))
        .setProperty(WebConnectionClient.CLOSED, "1")
        .toString());
    control.replay();
    server.writeState(req, endpoint, res);
    control.verify();
  }
}
