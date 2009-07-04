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

import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A client-side subsitute for the common pattern of the blocking while(true) readline(); pattern
 */
public class ClientSocketSubstitute {
  
  private WebConnectionClient client;
  private BlockingQueue<String> incoming;
  
  /**
   * Constructor. Uses a simple environment implementation that opens the socket in a new
   * thread and uses URL.openStream() to connect to the WebSocket
   * @param url the URL to connect to
   * @param silencePeriodInMillis the time the thread should wait between each http call
   * @param maxMessages the maximum amount of messages that should be transported in one http request
   */
  public ClientSocketSubstitute(
      final URL url,
      int silencePeriodInMillis,
      int maxMessages) {
    client = new WebConnectionClient(url, silencePeriodInMillis, maxMessages);
    incoming = new LinkedBlockingQueue<String>();
    client.open(new WebConnectionClient.Receiver(){
      @Override
      public void receive(String message) {
        incoming.add(message);
      }});
  }
  
  /**
   * Gets the next line from the server. If nothing is received for 60 seconds, returns null.
   * @return
   */
  public String readLine() {
    try {
      return incoming.poll(60, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      return null;
    }
  }
  
  /**
   * Sends a snippet of data to the server.
   */
  public void println(String snippet) {
    client.send(snippet);
  }
  
  /**
   * Closes the fake socket
   */
  public void close() {
    client.close();
  }

}
