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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utility class, used to concatenate a set of messages (given as strings)
 * into a single json object that can be parsed back on the receiving side.
 * The final result can be retrieved by using the toString() method.
 * This object is not threadsafe!
 * 
 * The returned object is a json map containing an array of messages
 * named "payload", plus an additional arbitrary amount of key/value pairs.
 * 
 */
class PayloadBuilder {
  
  /**
   * Name of the message list (payload)
   */
  public static final String TAG = "payload";
  
  private JSONObject object = new JSONObject();
  private JSONArray payload = new JSONArray();;
  private int size;
  
  /**
   * Constructor
   */
  public PayloadBuilder() {
    reset();
  }
  
  /**
   * Resets the builder, so that it can be reused
   */
  public PayloadBuilder reset() {
    object = new JSONObject();
    payload = new JSONArray();;
    size = 0;
    return this;
  }

  /**
   * Adds a string message to the payload
   */
  public PayloadBuilder addPayload(String message) {
    payload.put(message);
    size++;
    return this;
  }
  
  public PayloadBuilder setProperty(String key, String value) {
    if (key.equals(TAG)) {
      throw new IllegalArgumentException("Illegal tag: " + key);
    }
    try {
      object.put(key, value);
    } catch (JSONException e) {
      throw new AssertionError("Cannot add as string to json object");
    }
    return this;
  }
  
  /**
   * Returns the amount of elements the builder currently contains.
   */
  public int size() {
    return size;
  }

  /**
   * Builds the result and returns it as a string
   */
  public String toString() {
    try {
      object.put(TAG, payload);
      return object.toString();
    } catch (JSONException e) {
      throw new AssertionError("Cannot add json array to json object");
    }
  }
}
