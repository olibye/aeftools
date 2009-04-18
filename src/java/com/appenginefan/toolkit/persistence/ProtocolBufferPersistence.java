package com.appenginefan.toolkit.persistence;

import com.google.common.base.Preconditions;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

/**
 * Persistence that uses googles protocol buffer framework
 * for efficient representation and serialization of data.
 * 
 * @param <T>
 *          the type of protocol message that this
 *          persistence is for
 */
public class ProtocolBufferPersistence<T extends Message>
    extends MarshallingPersistence<T> {

  private final T prototype;

  /**
   * Constructor
   * 
   * @param backend
   *          a backend that stores serialized protocol
   *          buffers
   * @param prototype
   *          a prototype instance of the generic type that
   *          this peristence object is for.
   */
  public ProtocolBufferPersistence(
      Persistence<byte[]> backend, T prototype) {
    super(backend);
    Preconditions.checkNotNull(prototype);
    this.prototype = prototype;
  }

  @Override
  protected byte[] makeArray(T nonNullValue) {
    return nonNullValue.toByteArray();
  }

  @SuppressWarnings("unchecked")
  @Override
  protected T makeType(byte[] nonNullValue) {
    try {
      return (T) prototype.newBuilderForType().mergeFrom(
          nonNullValue).build();
    } catch (InvalidProtocolBufferException e) {
      throw new StoreException(
          "ProtocolBuffer deserialization failed", e);
    }
  }

}
