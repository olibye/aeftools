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
package com.appenginefan.toolkit.unittests;

import junit.framework.TestCase;

import static com.appenginefan.toolkit.unittests.TestInitializer.Option.GET_PERSISTENCE_MANAGER_FACTORY;
import static com.appenginefan.toolkit.unittests.TestInitializer.Option.INCLUDE_JDO;

/**
 * Unit tests for the test initializer.
 */
public class TestInitializerTest
    extends TestCase {

  /**
   * Getting JDO set up is tricky. Getting it set up more
   * than once is even trickier ;-). This test makes sure
   * that that works.
   */
  public void testMultipleJdoRuns() throws Exception {
    TestInitializer t1 =
        new TestInitializer(null,
            GET_PERSISTENCE_MANAGER_FACTORY, INCLUDE_JDO);
    t1.setUp();
    assertNotNull(t1.getPersistenceManagerFactory());
    t1.tearDown(true);

    TestInitializer t2 =
        new TestInitializer(null,
            GET_PERSISTENCE_MANAGER_FACTORY, INCLUDE_JDO);
    t2.setUp();
    assertNotNull(t2.getPersistenceManagerFactory());
    t2.tearDown(true);
  }

}
