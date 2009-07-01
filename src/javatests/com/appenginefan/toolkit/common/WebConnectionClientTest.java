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

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Queue;

import org.easymock.EasyMock;

import junit.framework.TestCase;

/**
 * Unit tests for the WebSocketClient
 * 
 * @author Jens Scheffler
 *
 */
public class WebConnectionClientTest extends TestCase {
  
  private static final int SILENCE = 5000;
  private static final int MAX = 3;
  
  static final String p(String meta, String... messages) {
    PayloadBuilder result = new PayloadBuilder().setProperty(WebConnectionClient.META, meta);
    for (String message : messages) {
      result.addPayload(message);
    }
    return result.toString();
  }
  
  static final String EMPTY = new PayloadBuilder().toString();
  static final String META_A = p("a");
  static final String META_B = p("b");
  static final String META_C = p("c");
  static final String META_D = p("d");
  
  // mock object, auto-created by EasyMock
  private final WebConnectionClient.Environment environment = 
      EasyMock.createStrictMock(WebConnectionClient.Environment.class);
  private final WebConnectionClient.Receiver receiver = 
      EasyMock.createStrictMock(WebConnectionClient.Receiver.class);
  
  
  private final WebConnectionClient client = 
    new WebConnectionClient(environment, SILENCE, MAX);
  private final Queue<String> queue = client.getQueue();
  
  @Override
  protected void setUp() throws Exception {
    assertNotNull(queue);
    assertEquals(0, queue.size());
  }
  
  /**
   * Tests that sending messages puts xml snippets into the local queue.
   */
  public void testSend() {
    String message1 = "23";
    String message2 = "24";
    client.send(message1);
    assertEquals(1, queue.size());
    assertEquals(message1, queue.peek());
    
    client.send(message2);
    assertEquals(2, queue.size());
    assertEquals(message1, queue.poll());
    assertEquals(message2, queue.poll());
  }
  
  public void testCannotOpenTwice() throws Exception {

    // Call "open" once. run() will not be executed,
    // since our environment is smart enough to intercept that
    environment.execute(client);
    EasyMock.replay(environment, receiver);
    client.open(receiver);
    EasyMock.verify(environment, receiver);
    
    // Call "open" a second time, which should fail
    try {
      client.open(receiver);
      fail("second open() should have caused an exception");
    } catch (ConcurrentModificationException expected) {
      // fall through
    }    
  }
  
  public void testCloseWillTerminateRun() throws Exception {
    client.close();
    EasyMock.replay(environment, receiver);
    try {
      client.run();
      fail("Expected an InterruptedException");
    } catch (InterruptedException expected) {
      // fall through
    }
    EasyMock.verify(environment, receiver);
  }

  public void testSimpleLoop() throws Exception {
    
    // Record a very simple sequence for the environment:
    // The first http-send will fail, which will cause the
    // thread to sleep. We will jump out of the loop by
    // throwing an InterruptedException
    EasyMock.expect(environment.currentTimeMillis()).andReturn(0L);
    EasyMock.expect(environment.fetch("{\"payload\":[]}")).andReturn(null);
    EasyMock.expect(environment.currentTimeMillis()).andReturn((long) (SILENCE - 1));
    environment.sleep(1L);
    EasyMock.expectLastCall().andThrow(new InterruptedException("end of test"));
    
    // Now, let's try this out
    EasyMock.replay(environment, receiver);
    try {
      client.run();
      fail("Expected an InterruptedException");
    } catch (InterruptedException expected) {
      // fall through
    }
    EasyMock.verify(environment, receiver);
  }
  
  public void testEstablishConnectionInSecondAttempt() throws Exception {

    // First communication fails, so we sleep for a while
    EasyMock.expect(environment.currentTimeMillis()).andReturn(0L);
    EasyMock.expect(environment.fetch(EMPTY)).andReturn(null);
    EasyMock.expect(environment.currentTimeMillis()).andReturn((long) (SILENCE - 1));
    environment.sleep(1L);
    
    // Second communication attempt succeeds -- the meta-value is "a"
    // We then sleep for a while again
    EasyMock.expect(environment.currentTimeMillis()).andReturn(0L);
    EasyMock.expect(environment.fetch(EMPTY)).andReturn(META_A);
    EasyMock.expect(environment.currentTimeMillis()).andReturn((long) (SILENCE - 2));
    environment.sleep(2L);
    
    // Nothing is in the queue, so the next communication attempt will still be empty.
    // It will contain a meta-tag though
    EasyMock.expect(environment.currentTimeMillis()).andReturn(0L);
    EasyMock.expect(environment.fetch(META_A)).andReturn(META_B);
    EasyMock.expect(environment.currentTimeMillis()).andReturn((long) (SILENCE - 3));
    environment.sleep(3L);
    
    // That's it for now, let's interrupt the thread and try it out
    EasyMock.expectLastCall().andThrow(new InterruptedException("end of test"));
    EasyMock.replay(environment, receiver);
    try {
      client.run();
      fail("Expected an InterruptedException");
    } catch (InterruptedException expected) {
      // fall through
    }
    EasyMock.verify(environment, receiver);
  }
  
  public void testSendUntilQueueIsEmpty() throws Exception {
    
    // Push a few (MAX + 1) messages onto the bus
    for (int i = 0; i <= MAX; i++) {
      client.send("" + (i + 1));
      
    }
    
    // First, a connection is established 
    EasyMock.expect(environment.currentTimeMillis()).andReturn(0L);
    EasyMock.expect(environment.fetch(EMPTY)).andReturn(META_A);
    EasyMock.expect(environment.currentTimeMillis()).andReturn((long) (SILENCE + 1));
    
    // Next, the first three messages get delivered
    EasyMock.expect(environment.currentTimeMillis()).andReturn(0L);
    EasyMock.expect(environment.fetch(p("a", "1", "2", "3"))).andReturn(META_B);
    EasyMock.expect(environment.currentTimeMillis()).andReturn((long) (SILENCE + 1));
    
    // Then, the fourth message gets delivered
    EasyMock.expect(environment.currentTimeMillis()).andReturn(0L);
    EasyMock.expect(environment.fetch(p("b", "4"))).andReturn(META_C);
    EasyMock.expect(environment.currentTimeMillis()).andReturn((long) (SILENCE + 1));
    
    // Now, the queue should be empty. 
    // An empty payload gets delivered, just to poll for new messages
    // from the server.
    EasyMock.expect(environment.currentTimeMillis()).andReturn(0L);
    EasyMock.expect(environment.fetch(META_C)).andReturn(META_D);
    EasyMock.expect(environment.currentTimeMillis()).andReturn((long) (SILENCE - 1));
    environment.sleep(1L);

    // Let's try it out
    EasyMock.expectLastCall().andThrow(new InterruptedException("end of test"));
    EasyMock.replay(environment, receiver);
    try {
      client.run();
      fail("Expected an InterruptedException");
    } catch (InterruptedException expected) {
      // fall through
    }
    EasyMock.verify(environment, receiver);
  }
  
  public void testResendIfCommunicationFailed() throws Exception {
    
    // Push a message onto the bus
    client.send("13");
    
    // First, a connection is established 
    EasyMock.expect(environment.currentTimeMillis()).andReturn(0L);
    EasyMock.expect(environment.fetch("{\"payload\":[]}")).andReturn(META_A);
    EasyMock.expect(environment.currentTimeMillis()).andReturn((long) (SILENCE + 1));
    
    // Next, the message gets delivered, but something goes wrong
    EasyMock.expect(environment.currentTimeMillis()).andReturn(0L);
    EasyMock.expect(environment.fetch(p("a", "13"))).andReturn(null);
    EasyMock.expect(environment.currentTimeMillis()).andReturn((long) (SILENCE + 1));
    
    // Since communication failed, the thread should try to re-deliver
    EasyMock.expect(environment.currentTimeMillis()).andReturn(0L);
    EasyMock.expect(environment.fetch(p("a", "13"))).andReturn(META_B);
    EasyMock.expect(environment.currentTimeMillis()).andReturn((long) (SILENCE + 1));
    
    // Now, the queue should be empty. 
    // An empty payload gets delivered, just to poll for new messages
    // from the server.
    EasyMock.expect(environment.currentTimeMillis()).andReturn(0L);
    EasyMock.expect(environment.fetch(META_B)).andReturn(META_C);
    EasyMock.expect(environment.currentTimeMillis()).andReturn((long) (SILENCE - 1));
    environment.sleep(1L);

    // Let's try it out
    EasyMock.expectLastCall().andThrow(new InterruptedException("end of test"));
    EasyMock.replay(environment, receiver);
    try {
      client.run();
      fail("Expected an InterruptedException");
    } catch (InterruptedException expected) {
      // fall through
    }
    EasyMock.verify(environment, receiver);
  }
  
  public void testSendResponseToParser() throws Exception {
    
    // Store incoming elements in a local list, then end the loop
    final List<String> incoming = new ArrayList<String>();
    final WebConnectionClient.Receiver bufferingParser = new WebConnectionClient.Receiver(){
      @Override
      public void receive(String message) {
        incoming.add(message);
        client.close();
      }};
      
    // Establish communication
    environment.execute(client);
    EasyMock.expect(environment.currentTimeMillis()).andReturn(0L);
    EasyMock.expect(environment.fetch(EMPTY)).andReturn(META_A);
    EasyMock.expect(environment.currentTimeMillis()).andReturn((long) (SILENCE - 1));
    environment.sleep(1L);
    
    // After sleeping for a while, another request should go out.
    // Let's respond with something.
    EasyMock.expect(environment.currentTimeMillis()).andReturn(0L);
    EasyMock.expect(environment.fetch(META_A)).andReturn(p("b", "foo"));
    
    // That's it for now, let's try it out
    EasyMock.replay(environment);
    try {
      client.open(bufferingParser);
      client.run();
      fail("Expected an InterruptedException");
    } catch (InterruptedException expected) {
      // fall through
    }
    EasyMock.verify(environment);
    
    // Let's look at the incoming queue
    assertEquals(1, incoming.size());
    assertEquals("foo", incoming.get(0));
  }
}
