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

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.appenginefan.toolkit.unittests.BaseTest;
import com.google.common.base.Function;
import com.google.common.base.Functions;

/**
 * A template for testing persistences that can digest byte
 * arrays. This class uses a MapBasedPersistence, but this
 * can be changed by simply overwriting the setUp method
 */
public class ByteArrayBasedPersistenceTest
    extends BaseTest {

  /**
   * Should be set up in the setUp method
   */
  protected Persistence<byte[]> persistence;

  /**
   * Set this to true if the persistence implementation
   * supports defensive copying
   */
  protected boolean supportsDefensiveCopy;

  /**
   * will set up a MapBasedPersistence if persistence is
   * null when called.
   */
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    if (persistence == null) {
      persistence = new MapBasedPersistence<byte[]>();
      supportsDefensiveCopy = false;
    }
  }

  public void testBasicSetAndGet() {
    persistence.mutate("A", Functions.constant("A"
        .getBytes()));
    assertTrue(Arrays.equals(persistence.get("A"), "A"
        .getBytes()));
  }

  public void testOverwrite() {
    persistence.mutate("A", Functions.constant("A"
        .getBytes()));
    assertTrue(Arrays.equals("B".getBytes(), persistence
        .mutate("A", Functions.constant("B".getBytes()))));
    assertTrue(Arrays.equals(persistence.get("A"), "B"
        .getBytes()));
  }

  public void testGetUnknownKey() {
    assertNull(persistence.get("A"));
  }

  public void testDelete() {
    persistence.mutate("A", Functions.constant("A"
        .getBytes()));
    assertNull(persistence.mutate("A", Functions
        .constant((byte[]) null)));
    assertNull(persistence.get("A"));
  }

  public void testFunctionInput() {
    persistence.mutate("A", Functions.constant("B"
        .getBytes()));
    persistence.mutate("A", new Function<byte[], byte[]>() {
      @Override
      public byte[] apply(byte[] fromPersistence) {
        assertTrue(Arrays.equals(fromPersistence, "B"
            .getBytes()));
        return "C".getBytes();
      }
    });
    assertTrue(Arrays.equals(persistence.get("A"), "C"
        .getBytes()));
  }

  public void testDefensiveCopy() {
    byte[] input = "B".getBytes();
    persistence.mutate("A", Functions.constant(input));
    input[0]++;
    if (supportsDefensiveCopy) {
      assertTrue(Arrays.equals(persistence.get("A"), "B"
          .getBytes()));
    } else {
      assertTrue(Arrays.equals(persistence.get("A"), input));
    }
  }

  public void testScan() {
    persistence.mutate("A1", Functions.constant("A1"
        .getBytes()));
    persistence.mutate("A2", Functions.constant("A2"
        .getBytes()));
    persistence.mutate("A3", Functions.constant("A3"
        .getBytes()));
    persistence.mutate("A2", Functions
        .constant((byte[]) null));
    List<Entry<String, byte[]>> scanResult =
        persistence.scan("A", "B", 10);
    assertEquals(2, scanResult.size());
    assertEquals("A1", scanResult.get(0).getKey());
    assertEquals("A3", scanResult.get(1).getKey());
    assertTrue(Arrays.equals("A1".getBytes(), scanResult
        .get(0).getValue()));
    assertTrue(Arrays.equals("A3".getBytes(), scanResult
        .get(1).getValue()));
    scanResult = persistence.scan("A1", "A3", 10);
    assertEquals(1, scanResult.size());
    assertEquals("A1", scanResult.get(0).getKey());
    scanResult = persistence.scan("A1", "A4", 1);
    assertEquals(1, scanResult.size());
    assertEquals("A1", scanResult.get(0).getKey());
    scanResult = persistence.scan("B", "Z", 10);
    assertEquals(0, scanResult.size());
    scanResult = persistence.scan("A1", "A4", 0);
    assertEquals(0, scanResult.size());
  }

  public void testScanReverse() {
    persistence.mutate("A1", Functions.constant("A1"
        .getBytes()));
    persistence.mutate("A2", Functions.constant("A2"
        .getBytes()));
    persistence.mutate("A3", Functions.constant("A3"
        .getBytes()));
    persistence.mutate("A2", Functions
        .constant((byte[]) null));
    List<Entry<String, byte[]>> scanResult =
        persistence.scanReverse("A", "B", 10);
    assertEquals(2, scanResult.size());
    assertEquals("A3", scanResult.get(0).getKey());
    assertEquals("A1", scanResult.get(1).getKey());
    assertTrue(Arrays.equals("A3".getBytes(), scanResult
        .get(0).getValue()));
    assertTrue(Arrays.equals("A1".getBytes(), scanResult
        .get(1).getValue()));
    scanResult = persistence.scanReverse("A1", "A3", 10);
    assertEquals(1, scanResult.size());
    assertEquals("A1", scanResult.get(0).getKey());
    scanResult = persistence.scanReverse("A0", "A4", 1);
    assertEquals(1, scanResult.size());
    assertEquals("A3", scanResult.get(0).getKey());
    scanResult = persistence.scanReverse("B", "Z", 10);
    assertEquals(0, scanResult.size());
    scanResult = persistence.scanReverse("A0", "A4", 0);
    assertEquals(0, scanResult.size());
  }
}
