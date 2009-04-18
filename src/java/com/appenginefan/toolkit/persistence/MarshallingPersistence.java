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
 * Wraps around a byte array based persistence for the
 * backend but uses a different type.
 */
public abstract class MarshallingPersistence<T> implements
    Persistence<T> {

  private final Persistence<byte[]> backend;

  protected abstract T makeType(byte[] nonNullValue);

  protected abstract byte[] makeArray(T nonNullValue);

  public MarshallingPersistence(Persistence<byte[]> backend) {
    Preconditions.checkNotNull(backend);
    this.backend = backend;
  }

  @Override
  public T get(String key) {
    byte[] asBytes = backend.get(key);
    if (asBytes == null) {
      return null;
    }
    return makeType(asBytes);
  }

  @Override
  public T mutate(String key,
      final Function<? super T, ? extends T> mutator) {
    byte[] asBytes =
        backend.mutate(key, new Function<byte[], byte[]>() {
          @Override
          public byte[] apply(byte[] arg0) {
            T asType =
                (arg0 == null) ? null : makeType(arg0);
            T mutated = mutator.apply(asType);
            if (mutated == null) {
              return null;
            }
            return makeArray(mutated);
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
    for (Entry<String, byte[]> entry : backend.scan(start,
        end, max)) {
      T value = null;
      if (entry.getKey() != null) {
        value = makeType(entry.getValue());
      }
      result
          .add(Maps.immutableEntry(entry.getKey(), value));
    }
    return result;
  }
}
