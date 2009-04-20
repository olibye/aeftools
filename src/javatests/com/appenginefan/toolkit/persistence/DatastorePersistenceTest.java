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

package com.appenginefan.toolkit.persistence;

import java.io.File;
import java.util.Arrays;

import org.easymock.classextension.EasyMock;

import com.google.appengine.api.datastore.dev.LocalDatastoreService;
import com.google.appengine.tools.development.ApiProxyLocalImpl;
import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.ApiProxy.Environment;
import com.google.common.base.Functions;

public class DatastorePersistenceTest
    extends ByteArrayBasedPersistenceTest {

  @Override
  protected void setUp() throws Exception {
    Environment env =
        EasyMock.createNiceMock(Environment.class);
    EasyMock.expect(env.getAppId()).andReturn("Unit Tests")
        .anyTimes();
    EasyMock.expect(env.getVersionId()).andReturn("1.0")
        .anyTimes();
    EasyMock.replay(env);
    ApiProxy.setEnvironmentForCurrentThread(env);
    ApiProxyLocalImpl proxy =
        new ApiProxyLocalImpl(new File(".")) {
        };
    proxy.setProperty(
        LocalDatastoreService.NO_STORAGE_PROPERTY,
        Boolean.TRUE.toString());
    ApiProxy.setDelegate(proxy);
    persistence = new DatastorePersistence(null, " foo ");
    supportsDefensiveCopy = true;
    super.setUp();
  }
  
  public void testNumericKey() throws Exception {
    persistence.mutate("13", Functions.constant("abc".getBytes()));
    assertTrue(Arrays.equals("abc".getBytes(), persistence.get("13")));
  }

}
