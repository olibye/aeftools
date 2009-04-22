package com.appenginefan.toolkit.persistence;

/**
 * Storage specialized in persisting long values. Wraps
 * around a byte array based persistence for the backend.
 */
public class LongPersistence
    extends MarshallingPersistence<Long> {

  public LongPersistence(Persistence<byte[]> backend) {
    super(backend);
  }

  @Override
  protected byte[] makeArray(Long nonNullValue) {
    final long asPrimitiveValue = nonNullValue.longValue();
    byte[] result = new byte[8];
    for (int i = 0; i < 8; i++) {
      result[i] = (byte) (asPrimitiveValue >>> (i * 8));
    }
    return result;
  }

  @Override
  protected Long makeType(byte[] nonNullValue) {
    if (nonNullValue.length != 8) {
      throw new AssertionError(
          "Invalid byte array length: "
              + nonNullValue.length);
    }
    long result = 0;
    for (int i = 7; i >= 0; i--) {
      result <<= 8;
      result = result ^ ((long) nonNullValue[i] & 0xFF);
    }
    return result;
  }

}
