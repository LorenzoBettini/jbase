package jbase.example.client.maven;

import jbasescript.JbasescriptExample;
import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("all")
public class FooTest {
  @Test
  public void testNull() {
    Assert.assertEquals("passed: null", JbasescriptExample.testMe(null));
  }
  
  @Test
  public void testNotNull() {
    Assert.assertEquals("passed: foo", JbasescriptExample.testMe("foo"));
  }
}
