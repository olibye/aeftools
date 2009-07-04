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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.appenginefan.toolkit.persistence.Persistence;
import com.google.common.base.Preconditions;

/**
 * Manages the peristence of server-side endpoints for a WebConnectionClient,
 * and the execution of incoming requests.
 *  
 * @author Jens Scheffler
 *
 */
public class WebConnectionServer {
  
  private static final Logger LOG = Logger.getLogger(WebConnectionServer.class.getName());
  
  /**
   * Represents the receiving end of a communication
   */
  public static interface Receiver {
    
    public void onEmptyPayload(
        WebConnectionServer server, 
        ServerEndpoint endpoint, 
        HttpServletRequest req);
    
    public void receive(
        WebConnectionServer server, 
        ServerEndpoint endpoint, 
        String message,
        HttpServletRequest req);
  }
  
  /**
   * Represents the inner works of a server. An instance of this interface is 
   * contained within a WebConnectionServer, which uses its different methods during
   * dispatch.
   */
  public static interface ServerGuts {
    
    /**
     * Assumed a given request belongs to an existing ServerEndpoint, loads
     * that object. It is assumed that the ServerEndpoint is in its connected 
     * state. Unlike newServerEndpoint(), an existing ServerEndpoint object may have
     * additional state in a database. This method is expected to load
     * that data, as well.
     * @param request the request that triggers the lookup
     * @param meta the meta-tag from the http request
     * @return a ServerEndpoint object if existent, or null if nothing is found
     */
    abstract ServerEndpoint loadServerEndpoint(HttpServletRequest request, String meta);
    
    /**
     * For the given HttpServletRequest, create a new server endpoint.
     * This is the equivalent of a newly opened socket connection.
     * @param request the request that triggers the creation
     * @return a non-null ServerEndpoint object that is not "open" yet
     */
    abstract ServerEndpoint newServerEndpoint(HttpServletRequest request);
    
    /**
     * Populate the cookies in a servlet response with whatever data is needed
     * to support the loadServerEndpoint() method in future requests. Also, write the
     * body of the response.
     * @param bus the ServerEndpoint to transmit
     * @param response the response to populate
     */
    abstract void writeState(
        HttpServletRequest request, 
        ServerEndpoint bus, 
        HttpServletResponse response) throws IOException;
    
    /**
     * Converts a ServerEndpoint to a unique handle that can be used to persist
     * inner state in a data store
     * @param bus a non-null ServerEndpoint
     * @return the unique id of the bus
     */
    abstract String toHandle(ServerEndpoint bus);
    
    /**
     * Loads a ServerEndpoint object from a given handle. The ServerEndpoint object is
     * usually less "complete" than an object constructed from a request, since it
     * does not contain the state stored in the http client, but it is sufficient
     * to perform certain manipulattions, like enqueuing outgoing messages.
     * @param handle the handle that should be used for constructing the object
     * @return a ServerEndpoint object, or null if the handle is invalid
     */
    abstract ServerEndpoint fromHandle(HttpServletRequest request, String handle);
    
    /**
     * Commits any changes made to the store in this thread that have not been written yet.
     */
    abstract void commit(HttpServletRequest request);
    
    /**
     * Commits any changes made to the store in this thread that have not been written yet.
     */
    abstract void rollback(HttpServletRequest request);
  }
  
  private final ServerGuts server;

  /**
   * Constructor
   * @param server the server guts being wrapped. Must not be null.
   */
  public WebConnectionServer(ServerGuts server) {
    Preconditions.checkNotNull(server);
    this.server = server;
  }
  
  /**
   * Factory-method; creates a server based on a binary store
   * @param persistence the storage algorithm for binary data
   * @return a fully configured server
   */
  public static WebConnectionServer fromPeristence(Persistence<byte[]> persistence) {
    return new WebConnectionServer(new WebConnectionServerImpl(persistence));
  }
  
  /**
   * Dispatches an incoming request. This method can be called from a servlet
   * that wraps this object.
   * @throws IOException
   * @return true if the incoming body was valid and the message has been processed
   */
  public final boolean dispatch(
      Receiver receiver,
      HttpServletRequest req, 
      HttpServletResponse resp) throws IOException {
    
    // First, read the content of the request into a string
    BufferedReader reader = req.getReader();
    String body = null;
    try {
      StringBuilder sb = new StringBuilder();
      for (int c = reader.read(); c >= 0; c = reader.read()) {
        sb.append((char) c);
      }
      body = sb.toString();
    } finally {
      reader.close();
    }
    
    // Can we parse the body?
    String meta = null;
    List<String> messages = null;
    try {
      
      // Is it valid JSON?
      JSONObject payload = null;
      payload = new JSONObject(body);
      
      // Does it have a meta tag and an array of messages?
      if (payload.has(WebConnectionClient.META)) {
        meta = payload.getString(WebConnectionClient.META);
      }
      JSONArray array = payload.getJSONArray(PayloadBuilder.TAG);
      
      // Extract all the messages out of the array
      messages = new ArrayList<String>();
      for (int i = 0; i < array.length(); i++) {
        messages.add(array.getString(i));
      }
    } catch (JSONException e) {
      return false;
    }
    
    boolean success = false;
    
    try {
      // New or existing connection?
      ServerEndpoint bus = null;
      boolean newBus = false;
      if (meta != null) {
        bus = server.loadServerEndpoint(req, meta);
      }
      if (bus == null) {
        bus = server.newServerEndpoint(req);
        newBus = true;
      }
      if (newBus) {
        bus.open();
      }
      
      // Process the payload
      if (!newBus) {
        if (messages.isEmpty()) {
          try {
            receiver.onEmptyPayload(this, bus, req);
          } catch (Throwable t) {
            LOG.log(Level.WARNING, "Error while processing empty message", t);
          }
        } else {
          for (String message : messages) {
            try {
              receiver.receive(this, bus, message, req);
            } catch (Throwable t) {
              LOG.log(Level.WARNING, "Error while processing: " + message, t);
            }
          }
        }
      }
      
      // Now, put some data into the response and return
      server.writeState(req, bus, resp);
      success = true;
      
    } finally {
      
      // Commit all changes to the store, or roll them back
      if (success) {
        server.commit(req);
      } else {
        server.rollback(req);
      }
    }
    
    // Done
    return true;
  }

  /**
   * Loads a ServerEndpoint object from a given handle. The ServerEndpoint object is
   * usually less "complete" than an object constructed from a request, since it
   * does not contain the state stored in the http client, but it is sufficient
   * to perform certain manipulations, like enqueuing outgoing messages.
   * @param handle the handle that should be used for constructing the object
   * @return a ServerEndpoint object, or null if the handle is invalid
   */
  public ServerEndpoint fromHandle(HttpServletRequest request, String handle) {
    return server.fromHandle(request, handle);
  }
  
}
