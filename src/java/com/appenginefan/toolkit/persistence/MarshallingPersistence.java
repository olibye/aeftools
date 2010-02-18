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

/**
 * Wraps around a byte array based persistence for the
 * backend but uses a different type.
 */
public abstract class MarshallingPersistence<T> extends
    PersistenceAdapter<byte[], T> {

  protected abstract byte[] makeArray(T nonNullValue);

  protected final byte[] makeMarshalledType(byte[] original, T nonNullValue) {
    return makeArray(nonNullValue);
  };

  public MarshallingPersistence(Persistence<byte[]> backend) {
    super(backend);
  }
}
