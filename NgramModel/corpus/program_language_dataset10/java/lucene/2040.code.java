package org.apache.lucene.util;
public class TestVirtualMethod extends LuceneTestCase {
  private static final VirtualMethod<TestVirtualMethod> publicTestMethod =
    new VirtualMethod<TestVirtualMethod>(TestVirtualMethod.class, "publicTest", String.class);
  private static final VirtualMethod<TestVirtualMethod> protectedTestMethod =
    new VirtualMethod<TestVirtualMethod>(TestVirtualMethod.class, "protectedTest", int.class);
  public void publicTest(String test) {}
  protected void protectedTest(int test) {}
  static class TestClass1 extends TestVirtualMethod {
    @Override
    public void publicTest(String test) {}
    @Override
    protected void protectedTest(int test) {}
  }
  static class TestClass2 extends TestClass1 {
    @Override 
    public void protectedTest(int test) {}
  }
  static class TestClass3 extends TestClass2 {
    @Override
    public void publicTest(String test) {}
  }
  static class TestClass4 extends TestVirtualMethod {
  }
  static class TestClass5 extends TestClass4 {
  }
  public void testGeneral() {
    assertEquals(0, publicTestMethod.getImplementationDistance(this.getClass()));
    assertEquals(1, publicTestMethod.getImplementationDistance(TestClass1.class));
    assertEquals(1, publicTestMethod.getImplementationDistance(TestClass2.class));
    assertEquals(3, publicTestMethod.getImplementationDistance(TestClass3.class));
    assertFalse(publicTestMethod.isOverriddenAsOf(TestClass4.class));
    assertFalse(publicTestMethod.isOverriddenAsOf(TestClass5.class));
    assertEquals(0, protectedTestMethod.getImplementationDistance(this.getClass()));
    assertEquals(1, protectedTestMethod.getImplementationDistance(TestClass1.class));
    assertEquals(2, protectedTestMethod.getImplementationDistance(TestClass2.class));
    assertEquals(2, protectedTestMethod.getImplementationDistance(TestClass3.class));
    assertFalse(protectedTestMethod.isOverriddenAsOf(TestClass4.class));
    assertFalse(protectedTestMethod.isOverriddenAsOf(TestClass5.class));
    assertTrue(VirtualMethod.compareImplementationDistance(TestClass3.class, publicTestMethod, protectedTestMethod) > 0);
    assertEquals(0, VirtualMethod.compareImplementationDistance(TestClass5.class, publicTestMethod, protectedTestMethod));
  }
  @SuppressWarnings("unchecked")
  public void testExceptions() {
    try {
      publicTestMethod.getImplementationDistance((Class) LuceneTestCase.class);
      fail("LuceneTestCase is not a subclass and can never override publicTest(String)");
    } catch (IllegalArgumentException arg) {
    }
    try {
      new VirtualMethod<TestVirtualMethod>(TestVirtualMethod.class, "bogus");
      fail("Method bogus() does not exist, so IAE should be thrown");
    } catch (IllegalArgumentException arg) {
    }
    try {
      new VirtualMethod<TestClass2>(TestClass2.class, "publicTest", String.class);
      fail("Method publicTest(String) is not declared in TestClass2, so IAE should be thrown");
    } catch (IllegalArgumentException arg) {
    }
    try {
      new VirtualMethod<TestVirtualMethod>(TestVirtualMethod.class, "publicTest", String.class);
      fail("Violating singleton status succeeded");
    } catch (UnsupportedOperationException arg) {
    }
  }
}