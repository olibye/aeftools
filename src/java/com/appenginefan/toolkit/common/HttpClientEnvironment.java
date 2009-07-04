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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;

/**
 * Environment for http-based communication that uses an apache http client
 * 
 * @author Jens Scheffler
 *
 */
public class HttpClientEnvironment implements WebConnectionClient.Environment {
  
  private static final Logger LOG = Logger.getLogger(HttpClientEnvironment.class.getName());
  
  private final HttpClient client = new HttpClient();
  private final String url;
  
  public HttpClientEnvironment(String url) {
    this.url = url;
  }

  @Override
  public long currentTimeMillis() {
    return System.currentTimeMillis();
  }

  @Override
  public void execute(final WebConnectionClient client) {
    new Thread(new Runnable(){
      @Override
      public void run() {
        try {
          client.run();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }}).start();
  }

  @Override
  public void sleep(long millis)
      throws InterruptedException {
    if (millis > 0) {
      Thread.sleep(millis);
    }
  }

  @Override
  public String fetch(String data) {
    LOG.fine("sending " + data);
    PostMethod method = new PostMethod(url);
    method.setRequestBody(data);
    try {
      int returnCode = client.executeMethod(method);
      if(returnCode == HttpStatus.SC_OK) {
        String response = method.getResponseBodyAsString();
        LOG.fine("receiving " + response);
        return response;
      } else {
        LOG.log(Level.WARNING, "Communication failed, status code " + returnCode);
      }
    } catch (HttpException e) {
      LOG.log(Level.WARNING, "Communication failed ", e);
    } catch (IOException e) {
      LOG.log(Level.WARNING, "Communication failed ", e);
    }
    finally {
      method.releaseConnection();
    }
    return null;
  }

}
