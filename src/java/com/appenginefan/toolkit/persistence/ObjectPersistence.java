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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * A persistence that uses serialization to put objects into
 * a store. Use with caution!
 */
public class ObjectPersistence<T extends Serializable>
    extends MarshallingPersistence<T> {

  public ObjectPersistence(Persistence<byte[]> backend) {
    super(backend);
  }

  @Override
  protected byte[] makeArray(T nonNullValue) {
    try {
      ByteArrayOutputStream buffer =
          new ByteArrayOutputStream();
      ObjectOutputStream out =
          new ObjectOutputStream(buffer);
      out.writeObject(nonNullValue);
      out.close();
      return buffer.toByteArray();
    } catch (IOException e) {
      throw new StoreException(
          "Object serialization failed", e);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  protected T makeType(byte[] nonNullValue) {
    try {
      return (T) new ObjectInputStream(
          new ByteArrayInputStream(nonNullValue))
          .readObject();
    } catch (ClassCastException e) {
      throw new StoreException(
          "Object deserialization failed", e);
    } catch (IOException e) {
      throw new StoreException(
          "Object deserialization failed", e);
    } catch (ClassNotFoundException e) {
      throw new StoreException(
          "Object deserialization failed", e);
    }
  }

}
