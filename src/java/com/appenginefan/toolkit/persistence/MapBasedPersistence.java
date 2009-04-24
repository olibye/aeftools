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

import java.util.List;
import java.util.SortedMap;
import java.util.Map.Entry;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * A simple, map-based store that holds data in memory.
 * Should only be used for unit tests, mostly for two
 * reasons. First of all, this persistence does not do any
 * defensive copies of its content, so nasty side effects
 * can occur. Second, since all is in memory, nothing is
 * really getting persisted beyond the lifetime of the
 * virtual machine.
 */
public class MapBasedPersistence<T> implements
    Persistence<T> {

  private SortedMap<String, T> store = Maps.newTreeMap();

  /**
   * Factory method
   */
  public static <T> MapBasedPersistence<T> newInstance() {
    return new MapBasedPersistence<T>();
  }

  @Override
  public T get(String key) {
    Preconditions.checkNotNull(key);
    return store.get(key);
  }

  @Override
  public T mutate(String key,
      Function<? super T, ? extends T> mutator) {
    Preconditions.checkNotNull(key);
    Preconditions.checkNotNull(mutator);
    T toSave = mutator.apply(get(key));
    if (toSave != null) {
      store.put(key, toSave);
    } else {
      store.remove(key);
    }
    return toSave;
  }

  @Override
  public List<Entry<String, T>> scan(String start,
      String end, int max) {
    List<Entry<String, T>> result = Lists.newArrayList();
    for (Entry<String, T> entry : store.subMap(start, end)
        .entrySet()) {
      if (result.size() >= max) {
        break;
      }
      result.add(entry);
    }
    return result;
  }

  @Override
  public List<Entry<String, T>> scanReverse(String start,
      String end, int max) {
    List<Entry<String, T>> sublist =
        Lists.newLinkedList(store.subMap(start, end)
            .entrySet());
    while (sublist.size() > max) {
      sublist.remove(0);
    }
    List<Entry<String, T>> result = Lists.newLinkedList();
    for (Entry<String, T> entry : sublist) {
      result.add(0, entry);
    }
    return result;
  }
}
