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
import java.util.Map.Entry;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Creates a persistence for a particular type by mapping
 * to another type.
 */
public abstract class PersistenceAdapter<S, T> implements
    Persistence<T> {

  private final Persistence<S> backend;

  protected abstract T makeType(S valueOrNull);

  protected abstract S makeMarshalledType(S original, T valueOrNull);

  public PersistenceAdapter(Persistence<S> backend) {
    Preconditions.checkNotNull(backend);
    this.backend = backend;
  }

  @Override
  public T get(String key) {
    S asBytes = backend.get(key);
    if (asBytes == null) {
      return null;
    }
    return makeType(asBytes);
  }

  @Override
  public T mutate(String key,
      final Function<? super T, ? extends T> mutator) {
    S asBytes =
        backend.mutate(key, new Function<S, S>() {
          @Override
          public S apply(S arg0) {
            T asType =
                (arg0 == null) ? null : makeType(arg0);
            T mutated = mutator.apply(asType);
            if (mutated == null) {
              return null;
            }
            return makeMarshalledType(arg0, mutated);
          }
        });
    if (asBytes == null) {
      return null;
    }
    return makeType(asBytes);
  }

  @Override
  public List<Entry<String, T>> scan(String start,
      String end, int max) {
    List<Entry<String, T>> result = Lists.newArrayList();
    for (Entry<String, S> entry : backend.scan(start,
        end, max)) {
      T value = null;
      if (entry.getValue() != null) {
        value = makeType(entry.getValue());
      }
      result
          .add(Maps.immutableEntry(entry.getKey(), value));
    }
    return result;
  }

  @Override
  public List<Entry<String, T>> scanReverse(String start,
      String end, int max) {
    List<Entry<String, T>> result = Lists.newArrayList();
    for (Entry<String, S> entry : backend.scanReverse(
        start, end, max)) {
      T value = null;
      if (entry.getValue() != null) {
        value = makeType(entry.getValue());
      }
      result
          .add(Maps.immutableEntry(entry.getKey(), value));
    }
    return result;
  }

  @Override
  public List<String> keyScan(String start, String end,
      int max) {
    return backend.keyScan(start, end, max);
  }

  @Override
  public List<String> keyScanReverse(String start,
      String end, int max) {
    return backend.keyScanReverse(start, end, max);
  }
}
