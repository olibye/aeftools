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

import static com.appenginefan.toolkit.unittests.TestInitializer.Option.GET_PERSISTENCE_MANAGER_FACTORY;
import static com.appenginefan.toolkit.unittests.TestInitializer.Option.INCLUDE_JDO;

import java.util.Date;

import javax.jdo.PersistenceManager;

/**
 * A simple example that shows how building a JDO-based test
 * can work.
 */
public class SimpleJDOTest
    extends BaseTest {

  public SimpleJDOTest() {
    super(GET_PERSISTENCE_MANAGER_FACTORY, INCLUDE_JDO);
  }

  public void testSetAndGet() {

    // Create a new employee and write the object to the
    // store
    final Date date = new Date();
    EmployeeData employee =
        new EmployeeData("John", "Doe", date);
    PersistenceManager manager = newPersistenceManager();
    final Long id =
        manager.makePersistent(employee).getId();
    manager.close();

    // From a different persistence manager, look up the
    // object
    manager = newPersistenceManager();
    employee =
        manager.getObjectById(EmployeeData.class, id);
    assertEquals(employee.getFirstName(), "John");
    assertEquals(employee.getLastName(), "Doe");
    manager.close();
  }

}
