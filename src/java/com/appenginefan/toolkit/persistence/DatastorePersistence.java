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

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;

/**
 * A datastore-based persistence for byte arrays. Wrap other
 * persistences like StringPersistence or ObjectPersistence
 * around this class to persist arbitrary data types in the
 * store.
 */
public class DatastorePersistence extends
    PersistenceAdapter<Entity, byte[]> {

  private static final String PROPERTY = "blob";

  private static final String CREATED = "created_at";

  private static final String MODIFIED = "changed_at";

  /**
   * Constructor.
   *
   * @param serviceOrNull
   *          a DatastoreService to use. If left null, the
   *          constructor fetch its own service
   * @param partition
   *          determines what &quot;partition&quot; to store
   *          the data in. Different stores must use
   *          different partitions, or unspecified behavior
   *          will occur.
   */
  public DatastorePersistence(
      DatastoreService serviceOrNull, String partition) {
    super(new EntityBasedPersistence(serviceOrNull, partition));
  }

  /**
   * Constructor.
   *
   * @param partition
   *          determines what &quot;partition&quot; to store
   *          the data in. Different stores must use
   *          different partitions, or unspecified behavior
   *          will occur.
   */
  public DatastorePersistence(String partition) {
    super(new EntityBasedPersistence(partition));
  }

  @Override
  protected byte[] makeType(Entity entity) {
    if (entity == null || !entity.hasProperty(PROPERTY)) {
      return null;
    }
    return ((Blob) entity.getProperty(PROPERTY)).getBytes();
  }

  @Override
  protected Entity makeMarshalledType(Entity entity, byte[] data) {
    if (data == null) {
      return null;
    }
    if (entity.hasProperty(CREATED)) {
      entity.setProperty(CREATED, System.currentTimeMillis());
    }
    entity.setProperty(PROPERTY, new Blob(data));
    entity.setProperty(MODIFIED, System.currentTimeMillis());
    return entity;
  }
}
