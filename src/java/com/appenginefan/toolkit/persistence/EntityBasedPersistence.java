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

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map.Entry;

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
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * A datastore-based persistence that operates on entities.
 */
public class EntityBasedPersistence implements
    Persistence<Entity> {

  private static final String PREFIX = "aef:";

  private static final int NUM_RETRIES = 10;

  private final DatastoreService service;

  private final String kind;

  private static String escape(String key) {
    Preconditions.checkNotNull(key);
    return ":" + key;
  }

  private static String unescape(String key) {
    Preconditions.checkNotNull(key);
    Preconditions.checkArgument(key.startsWith(":"),
        "key should start with :");
    return key.substring(1);
  }

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
  public EntityBasedPersistence(
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
  public EntityBasedPersistence(String partition) {
    this(null, partition);
  }

  @Override
  public Entity get(String key) {
    Preconditions.checkNotNull(key);
    try {
      return
          service.get(KeyFactory.createKey(kind,
              escape(key)));
    } catch (EntityNotFoundException e) {
      return null;
    }
  }

  /**
   * One specialty compared to the other stores is that the
   * incoming entity is never null. The user of this persistence
   * needs therefore to somehow determine if the entity is persistent
   * or not, e.g. through the lack of a particular attribute.
   * It also means that the return value of the function is slightly
   * less meaningful, since the entity is manipulated in place.
   * Exception: returning null will still delete the entity.
   */
  @Override
  public Entity mutate(String key,
      Function<? super Entity, ? extends Entity> mutator) {
    Preconditions.checkNotNull(key);
    Preconditions.checkNotNull(mutator);
    Key dbKey = KeyFactory.createKey(kind, escape(key));
    Exception lastException = null;
    for (int i = 0; i < NUM_RETRIES; i++) {
      Transaction t = service.beginTransaction();
      boolean success = false;
      Entity entity;
      try {
        entity = service.get(t, dbKey);
      } catch (EntityNotFoundException e) {
        entity = new Entity(kind, escape(key));
      }
      Entity data = mutator.apply(entity);
      try {
        if (data != null) {
          service.put(t, data);
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

  public List<Entry<String, Entity>> scan(String start,
      String end, int max, SortDirection direction, boolean keysOnly) {
    Preconditions.checkNotNull(start);
    Preconditions.checkNotNull(end);
    Preconditions.checkArgument(max > -1);
    if (max == 0) {
      return Lists.newArrayList();
    }
    Query query = new Query(kind);
    query.addFilter("__key__",
        FilterOperator.GREATER_THAN_OR_EQUAL, KeyFactory
            .createKey(kind, escape(start)));
    query.addFilter("__key__", FilterOperator.LESS_THAN,
        KeyFactory.createKey(kind, escape(end)));
    query.addSort("__key__", direction);
    if (keysOnly) {
      query.setKeysOnly();
    }
    PreparedQuery preparedQuery = service.prepare(query);
    List<Entry<String, Entity>> result =
        Lists.newArrayList();
    for (Entity entity : preparedQuery
        .asIterable(FetchOptions.Builder.withLimit(max))) {
      if (keysOnly) {
        result.add(Maps.immutableEntry(unescape(entity
            .getKey().getName()), (Entity) null));
      } else {
        result.add(Maps.immutableEntry(unescape(entity
            .getKey().getName()), entity));
      }
    }
    return result;
  }

  @Override
  public List<Entry<String, Entity>> scan(String start,
      String end, int max) {
    return scan(start, end, max, SortDirection.ASCENDING, false);
  }

  @Override
  public List<Entry<String, Entity>> scanReverse(
      String start, String end, int max) {
    return scan(start, end, max, SortDirection.DESCENDING, false);
  }

  @Override
  public List<String> keyScan(String start, String end,
      int max) {
    List<Entry<String, Entity>> toUnwrap =
        scan(start, end, max, SortDirection.ASCENDING, true);
    List<String> result = new ArrayList<String>(toUnwrap.size());
    for (Entry<String, Entity> e : toUnwrap) {
      result.add(e.getKey());
    }
    return result;
  }

  @Override
  public List<String> keyScanReverse(String start,
      String end, int max) {
    List<Entry<String, Entity>> toUnwrap =
      scan(start, end, max, SortDirection.DESCENDING, true);
  List<String> result = new ArrayList<String>(toUnwrap.size());
  for (Entry<String, Entity> e : toUnwrap) {
    result.add(e.getKey());
  }
  return result;
  }

}
