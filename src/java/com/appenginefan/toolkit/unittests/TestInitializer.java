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

import java.util.Properties;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;

import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.apphosting.api.ApiProxy;

/**
 * Initializes an App Engine environment for unit tests
 */
public class TestInitializer {

  private final ApiProxy.Environment environment;
  private final LocalServiceTestHelper helper =
	        new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private PersistenceManagerFactory pmf;

  /**
   * Constructor
   * 
   * @param environmentOrNull
   *          the environment that should be used for the
   *          test (null to use the default)
   */
  public TestInitializer(
      ApiProxy.Environment environmentOrNull) {
    this.environment =
        (environmentOrNull == null) ? new TestEnvironment()
            : environmentOrNull;
  }

  /** Constructor with default parameters */
  public TestInitializer() {
    this(null);
  }

  /**
   * Sets up a unit test with the options stored in this
   * object. This method should only be called once per
   * object.
   */
  public void setUp() throws Exception {
	  helper.setUp();
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
    Transaction txn =
        DatastoreServiceFactory.getDatastoreService()
            .getCurrentTransaction(null);
    try {
      if (txn != null) {
        try {
          txn.rollback();
        } finally {
          if (testWasSuccessful) {
            throw new IllegalStateException(
                "Datastore service still has an active txn.  Please "
                    + "rollback or commit all txns before test completes.");
          }
        }
      }
    } finally {
      ApiProxy.clearEnvironmentForCurrentThread();
    }

  }

  /**
   * Gets the PersistenceManagerFactory that was created by
   * this Initializer.
   */
  public PersistenceManagerFactory getPersistenceManagerFactory() {
    if (pmf == null) {
      Properties newProperties = new Properties();
      newProperties
          .put(
              "javax.jdo.PersistenceManagerFactoryClass",
              "org.datanucleus.store.appengine.jdo.DatastoreJDOPersistenceManagerFactory");
      newProperties.put("javax.jdo.option.ConnectionURL",
          "appengine");
      newProperties.put(
          "javax.jdo.option.NontransactionalRead", "true");
      newProperties.put(
          "javax.jdo.option.NontransactionalWrite", "true");
      newProperties.put("javax.jdo.option.RetainValues",
          "true");
      newProperties.put(
          "datanucleus.appengine.autoCreateDatastoreTxns",
          "true");
      newProperties.put(
          "datanucleus.appengine.autoCreateDatastoreTxns",
          "true");
      pmf =
          JDOHelper
              .getPersistenceManagerFactory(newProperties);
    }
    return pmf;
  }

}
