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

import java.util.Arrays;

import com.google.common.base.Functions;

public class DatastorePersistenceTest
    extends ByteArrayBasedPersistenceTest {

  @Override
  protected void setUp() throws Exception {
    persistence = new DatastorePersistence(null, " foo ");
    supportsDefensiveCopy = true;
    super.setUp();
  }

  public void testNumericKey() throws Exception {
    persistence.mutate("13", Functions.constant("abc"
        .getBytes()));
    assertTrue(Arrays.equals("abc".getBytes(), persistence
        .get("13")));
  }

}
