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

import java.io.File;
import java.util.Properties;
import java.util.Set;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;

import org.datanucleus.store.appengine.DatastoreTestHelper;

import com.google.appengine.api.datastore.dev.LocalDatastoreService;
import com.google.appengine.tools.development.ApiProxyLocalImpl;
import com.google.apphosting.api.ApiProxy;
import com.google.common.collect.Sets;

/**
 * Initializes an App Engine environment for unit tests
 */
public class TestInitializer {

  /**
   * A couple of options that determine how the {@link TestInitializer}
   * behaves. 
   */
  public static enum Option {

    /** Initialize JDO-specific parts of the test. */
    INCLUDE_JDO,

    /**
     * If JDO is used, also store a local
     * PersistenceManagerFactory. Not necessary if an
     * application has its own jdoconfig.xml and wants to
     * take care of getting its own persistencemanager
     * factory.
     */
    GET_PERSISTENCE_MANAGER_FACTORY;
  }

  private final ApiProxy.Environment environment;

  private final Set<Option> options;

  private final DatastoreTestHelper jdoHelper;

  private PersistenceManagerFactory pmf;

  private Properties originalProperties;

  /**
   * Constructor
   * 
   * @param environmentOrNull
   *          the environment that should be used for the
   *          test
   * @param options
   *          options useful to customize the behavior
   *          during the setup process (like whether or not
   *          to initialize jdo)
   */
  public TestInitializer(
      ApiProxy.Environment environmentOrNull,
      Option... options) {
    this.options = Sets.newHashSet(options);
    this.jdoHelper = new DatastoreTestHelper();
    this.environment =
        (environmentOrNull == null) ? new TestEnvironment()
            : environmentOrNull;
  }

  /**
   * Sets up a unit test with the options stored in this
   * object. This method should only be called once per
   * object.
   */
  public void setUp() throws Exception {

    // Start with the JDO setup
    if (options.contains(Option.INCLUDE_JDO)) {

      // First, let's make a backup of the system
      // properties,
      originalProperties = System.getProperties();

      // Now, let's go through the regular setup
      jdoHelper.setUp();

      // Should we build our own persistence manager
      // factory?
      if (options
          .contains(Option.GET_PERSISTENCE_MANAGER_FACTORY)) {
        Properties newProperties = new Properties();
        newProperties.putAll(originalProperties);
        newProperties
            .put(
                "javax.jdo.PersistenceManagerFactoryClass",
                "org.datanucleus.store.appengine.jdo.DatastoreJDOPersistenceManagerFactory");
        newProperties.put("javax.jdo.option.ConnectionURL",
            "appengine");
        newProperties
            .put("javax.jdo.option.NontransactionalRead",
                "true");
        newProperties.put(
            "javax.jdo.option.NontransactionalWrite",
            "true");
        newProperties.put("javax.jdo.option.RetainValues",
            "true");
        newProperties
            .put(
                "datanucleus.appengine.autoCreateDatastoreTxns",
                "true");
        newProperties
            .put(
                "datanucleus.appengine.autoCreateDatastoreTxns",
                "true");
        pmf =
            JDOHelper
                .getPersistenceManagerFactory(newProperties);
      }
    } else {

      // Alternative setup process if jdoHelper.setUp() is
      // not used
      ApiProxyLocalImpl proxy =
          new ApiProxyLocalImpl(new File(".")) {
          };
      proxy.setProperty(
          LocalDatastoreService.NO_STORAGE_PROPERTY,
          Boolean.TRUE.toString());
      ApiProxy.setDelegate(proxy);
    }

    // Replace the ApiProxy-environment with our own,
    // which will be more customizable eventually
    ApiProxy.setEnvironmentForCurrentThread(environment);
  }

  /**
   * Cleans up after a unit test. This method should only be
   * called once per object.
   * 
   * @param testWasSuccessful
   *          tells the object to do additional consistency
   *          checks to make sure that the test (which was
   *          successful so far) did not leave any resources
   *          in an inconsistent state
   */
  public void tearDown(boolean testWasSuccessful)
      throws Exception {
    if (options.contains(Option.INCLUDE_JDO)) {
      jdoHelper.tearDown(testWasSuccessful);
    }
  }

  /**
   * Gets the PersistenceManagerFactory that was created by
   * this Initializer.
   */
  public PersistenceManagerFactory getPersistenceManagerFactory() {
    return pmf;
  }

}
