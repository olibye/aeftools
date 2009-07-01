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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ConcurrentModificationException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A client than can connect to a WebConnection.
 * WebConnection objects can be used in App Engine applications
 * to replace socket-based services. This class can be used as
 * basis of communicating with such an end point on the web.
 */
public class WebConnectionClient {
  
  private static final Logger LOG = Logger.getLogger(WebConnectionClient.class.getName());
  static final String META = "meta";
  
  /**
   * Represents the receiving end of the communication
   */
  public static interface Receiver {
    public void receive(String message);
  }
  
  /**
   * Abstraction of everything that depends on the runtime environment,
   * like system clock, threading, or network. Easy to replace with
   * mocks for unit tests.
   */
  public static interface Environment {
    
    /**
     * Performs an http request to the server
     * @param data the data to be transmitted
     * @return the response payload, or null if the connection failed.
     */
    public String fetch(String data);
    
    /**
     * Controls the execution of the client's run-method in an independent
     * thread. Similar to the Executor interface, just not for generic runnables,
     * and it would also work in Java 1.3
     */
    public void execute(WebConnectionClient client);
    
    /**
     * Holds the current thread for a certain amount of milliseconds
     */
    public void sleep(long millis) throws InterruptedException;
    
    /**
     * Gets the current time in milliseconds
     */
    public long currentTimeMillis();
    
  }
  
  private final Queue<String> outqueue = new ConcurrentLinkedQueue<String>();
  private final Environment env;
  private final int silencePeriodInMillis;
  private final int maxMessages;
  private boolean isInterrupted = false;
  private boolean isStarted = false;
  private Receiver receiver;
  
  /**
   * Constructor. Uses a simple environment implementation that opens the socket in a new
   * thread and uses URL.openStream() to connect to the WebSocket
   * @param url the URL to connect to
   * @param silencePeriodInMillis the time the thread should wait between each http call
   * @param maxMessages the maximum amount of messages that should be transported in one http request
   */
  public WebConnectionClient(
      final URL url,
      int silencePeriodInMillis,
      int maxMessages) {
    super();
    env = new Environment(){

      @Override
      public long currentTimeMillis() {
        return System.currentTimeMillis();
      }

      @Override
      public void execute(WebConnectionClient bus) {
        new Thread() {
          public void run() {
            try {
              WebConnectionClient.this.run();
            } catch (InterruptedException e) {
              return;
            }
          };
        }.start();
      }

      @Override
      public String fetch(String data) {
        BufferedReader in = null;
        try {
          //TODO: how to send data out?
          in = new BufferedReader(
              new InputStreamReader(url.openStream()));
          StringBuilder sb = new StringBuilder();
          for (String s = in.readLine(); s != null; s = in.readLine()) {
            sb.append(s);
            sb.append('\n');
          }
          return sb.toString();
        } catch (IOException e) {
          return null;
        } finally {
          try {
            if (in != null) {
              in.close();
            }
          } catch (IOException e) {
            return null;
          }
        }
      }

      @Override
      public void sleep(long millis)
          throws InterruptedException {
        Thread.sleep(millis);
      }
    };
    this.maxMessages = maxMessages;
    this.silencePeriodInMillis = silencePeriodInMillis;
  }


  /**
   * Constructor
   * @param env the Environment that this client runs in
   * @param silencePeriodInMillis the time the thread should wait between each http call
   * @param maxMessages the maximum amount of messages that should be transported in one http request
   */
  public WebConnectionClient(
      Environment env,
      int silencePeriodInMillis,
      int maxMessages) {
    super();
    this.env = env;
    this.maxMessages = maxMessages;
    this.silencePeriodInMillis = silencePeriodInMillis;
  }
  
  /**
   * Checks if the interrupted-flag is set (using synchronization on this object)
   */
  private synchronized void checkForInterruption() throws InterruptedException {
    if (isInterrupted) {
      throw new InterruptedException();
    }
  }
  
  /**
   * Sleeps for a while.
   */
  private void sleep(long lastComm) throws InterruptedException {
    long diff = silencePeriodInMillis - (env.currentTimeMillis() - lastComm);
    if (diff > 0) {
      env.sleep(diff);
    }
  }
  
  /**
   * Signals a running executing thread to cease its work.
   */
  public synchronized void close() {
    isInterrupted = true;
  }
  
  /**
   * Starts a thread (through the executor) that connects to the server on a regular base
   */
  public synchronized void open(Receiver receiver) {
    if (isStarted) {
      throw new ConcurrentModificationException("Cannot call open more than once!");
    }
    isStarted = true;
    this.receiver = receiver;
    env.execute(this);
  }

  /**
   * Enqueues an object for transmission.
   */
  public void send(String message) {
    outqueue.add(message);
  }

  /**
   * For unit tests, retrieve the queue that internally stores the XML snippets.
   */
  Queue<String> getQueue() {
    return outqueue;
  }

  /**
   * Performs the communication loop. Visible for unit tests and the "Environment" implementation.
   */
  void run() throws InterruptedException {
    
    // Some tool variables we will need throughout this method
    final PayloadBuilder payload = new PayloadBuilder();   // constructs JSON messages
    String lastMeta = null;  // The last "meta"-tag that was sent from the server
    long lastCommunication = 0;  // the last time we tried to contact the server
    
    // Initial connection: an empty payload is sent. All we expect the response to be
    // at this point is parseable xml with a meta-element that we can begin conversation with
    while(lastMeta == null) {
      checkForInterruption();
      lastCommunication = env.currentTimeMillis();
      String hello = env.fetch(payload.toString());
      try {
        if (hello != null) {
          JSONObject parsed = new JSONObject(hello);
          lastMeta = parsed.getString(META);
        }
      } catch (JSONException parseFailed) {
        LOG.severe("Could not parse http response" + hello);
      }
      if (lastMeta == null) {
        sleep(lastCommunication);
      }
    }
    payload.reset();
    payload.setProperty(META, lastMeta);
    
    // Payload exchange
    while (true) {
      
      // First, check if we should terminate, or at least sleep a little while
      checkForInterruption();
      sleep(lastCommunication);
      
      // Fill the payload
      while (payload.size() < maxMessages && !outqueue.isEmpty()) {
        payload.addPayload(outqueue.poll());       
      }
      
      // Submit the data as a request and evaluate the response
      lastCommunication = env.currentTimeMillis();
      final String response = env.fetch(payload.toString());
      if (response != null) {
        
        // Parse the xml element and extract the last meta. If there is
        // no meta, something went wrong, and we should try again later
        JSONArray responseMessages = null;
        try {
          JSONObject parsed = new JSONObject(response);
          final String newMeta = parsed.getString(META);
          if (lastMeta == null) {
            continue;
          } else {
            lastMeta = newMeta;
          }
          responseMessages = parsed.getJSONArray(PayloadBuilder.TAG);
        } catch (JSONException parseFailed) {
          LOG.severe("Could not parse http response" + response);
        }
        
        // Iterate through the children and submit them to the parser. If a message fails,
        // we log the issue but continue
        for (int i = 0; i < responseMessages.length(); i++) {
          checkForInterruption();
          try {
            String message = responseMessages.getString(i);
            receiver.receive(message);
          } catch (JSONException e) {
            LOG.log(Level.WARNING, "JSON exception for index " + i, e);
          } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Runtime Exception for index " + i, e);            
          } catch (Error e) {
            LOG.log(Level.WARNING, "Runtime Error for index " + i, e);
          }
        }
        
        // Now that we successfully transmitted the messages, let's reset the buffer to make space
        // for new messages from the queue
        payload.reset();
        payload.setProperty(META, lastMeta);
      }
    }
  }

}
