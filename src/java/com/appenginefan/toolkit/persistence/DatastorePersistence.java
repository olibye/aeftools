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

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map.Entry;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.DatastoreFailureException;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * A datastore-based persistence for byte arrays. Wrap other
 * persistences like StringPersistence or ObjectPersistence
 * around this class to persist arbitrary data types in the
 * store.
 */
public class DatastorePersistence implements
    Persistence<byte[]> {

  private static final String PREFIX = "aef:";

  private static final String PROPERTY = "blob";

  private static final int NUM_RETRIES = 10;

  private final DatastoreService service;

  private final String kind;

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
    Preconditions.checkNotNull(partition);
    if (serviceOrNull == null) {
      serviceOrNull =
          DatastoreServiceFactory.getDatastoreService();
    }
    this.service = serviceOrNull;
    this.kind = PREFIX + partition;
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
    this(null, partition);
  }

  @Override
  public byte[] get(String key) {
    Preconditions.checkNotNull(key);
    try {
      Entity entity =
          service.get(KeyFactory.createKey(kind, key));
      return ((Blob) entity.getProperty(PROPERTY))
          .getBytes();
    } catch (EntityNotFoundException e) {
      return null;
    }
  }

  @Override
  public byte[] mutate(String key,
      Function<? super byte[], ? extends byte[]> mutator) {
    Preconditions.checkNotNull(key);
    Preconditions.checkNotNull(mutator);
    Key dbKey = KeyFactory.createKey(kind, key);
    Exception lastException = null;
    for (int i = 0; i < NUM_RETRIES; i++) {
      Transaction t = service.beginTransaction();
      boolean success = false;
      Entity entity;
      byte[] data;
      try {
        entity = service.get(t, dbKey);
        data =
            ((Blob) entity.getProperty(PROPERTY))
                .getBytes();
      } catch (EntityNotFoundException e) {
        data = null;
        entity = new Entity(kind, key);
      }
      data = mutator.apply(data);
      try {
        if (data != null) {
          entity.setProperty(PROPERTY, new Blob(data));
          service.put(t, entity);
        } else {
          service.delete(t, dbKey);
        }
        t.commit();
        success = true;
      } catch (ConcurrentModificationException e) {
        success = false;
        lastException = e;
      } catch (DatastoreFailureException e) {
        success = false;
        lastException = e;
      } finally {
        if (!success) {
          t.rollback();
        } else {
          return data;
        }
      }
    }
    throw new StoreException(
        "Could not store data for key " + key,
        lastException);
  }

  @Override
  public List<Entry<String, byte[]>> scan(String start,
      String end, int max) {
    Preconditions.checkNotNull(start);
    Preconditions.checkNotNull(end);
    Preconditions.checkArgument(max > -1);
    if (max == 0) {
      return Lists.newArrayList();
    }
    Query query = new Query(kind);
    query.addFilter("__key__",
        FilterOperator.GREATER_THAN_OR_EQUAL, KeyFactory
            .createKey(kind, start));
    query.addFilter("__key__", FilterOperator.LESS_THAN,
        KeyFactory.createKey(kind, end));
    query.addSort("__key__");
    PreparedQuery preparedQuery = service.prepare(query);
    List<Entry<String, byte[]>> result =
        Lists.newArrayList();
    for (Entity entity : preparedQuery
        .asIterable(FetchOptions.Builder.withLimit(max))) {
      result.add(Maps.immutableEntry(entity.getKey()
          .getName(), ((Blob) entity.getProperty(PROPERTY))
          .getBytes()));
    }
    return result;
  }

}
