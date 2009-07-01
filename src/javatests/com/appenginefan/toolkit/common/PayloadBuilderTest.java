package com.appenginefan.toolkit.common;

import junit.framework.TestCase;

/**
 * Unit tests for the PayloadBuilder
 * 
 * @author Jens Scheffler
 *
 */
public class PayloadBuilderTest extends TestCase {
  
  private PayloadBuilder p = new PayloadBuilder();
  
  public void testEmptyBuilder() {
    assertEquals("{\"payload\":[]}", p.toString());
    assertEquals(0, p.size());
  }
  
  public void testAddPayload() {
    assertEquals(
        "{\"payload\":[\"a\",\"B\"]}",
        p.addPayload("a").addPayload("B").toString());
    assertEquals(2, p.size());
  }
  
  public void testSetProperty() {
    assertEquals(
        "{\"b\":\"C\",\"payload\":[\"a\"]}",
        p.addPayload("a").setProperty("b", "C").toString());
    assertEquals(1, p.size());
  }
  
  public void testReset() {
    assertEquals(
        "{\"payload\":[]}", 
        p.addPayload("a").setProperty("b", "C").reset().toString());
    assertEquals(0, p.size());
  }
}
