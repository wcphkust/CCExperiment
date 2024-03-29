package org.apache.solr.common.params;
import junit.framework.TestCase;
public class ModifiableSolrParamsTest extends TestCase
{
  @Override
  public void setUp()
  {
    modifiable = new ModifiableSolrParams();
  }
  @Override
  public void tearDown()
  {
    modifiable.clear();
  }
  public void testAdd()
  {
    String key = "key";
    String[] values = new String[1];
    values[0] = null;
    modifiable.add(key, values);
    String[] result = modifiable.getParams(key);
    assertEquals("params", values, result);
  }
  public void testAddNormal()
  {
    String key = "key";
    String[] helloWorld = new String[] { "Hello", "World" };
    String[] universe = new String[] { "Universe" };
    String[] helloWorldUniverse = new String[] { "Hello", "World", "Universe" };
    modifiable.add(key, helloWorld);
    assertEquals("checking Hello World: ", helloWorld, modifiable.getParams(key));
    modifiable.add(key, universe);
    String[] result = modifiable.getParams(key);
    compareArrays("checking Hello World Universe ", helloWorldUniverse, result);
  }
  public void testAddNull()
  {
    String key = "key";
    String[] helloWorld = new String[] { "Hello", "World" };
    String[] universe = new String[] { null };
    String[] helloWorldUniverse = new String[] { "Hello", "World", null };
    modifiable.add(key, helloWorld);
    assertEquals("checking Hello World: ", helloWorld, modifiable.getParams(key));
    modifiable.add(key, universe);
    String[] result = modifiable.getParams(key);
    compareArrays("checking Hello World Universe ", helloWorldUniverse, result);
  }
  public void testOldZeroLength()
  {
    String key = "key";
    String[] helloWorld = new String[] {};
    String[] universe = new String[] { "Universe" };
    String[] helloWorldUniverse = new String[] { "Universe" };
    modifiable.add(key, helloWorld);
    assertEquals("checking Hello World: ", helloWorld, modifiable.getParams(key));
    modifiable.add(key, universe);
    String[] result = modifiable.getParams(key);
    compareArrays("checking Hello World Universe ", helloWorldUniverse, result);
  }
  public void testAddPseudoNull()
  {
    String key = "key";
    String[] helloWorld = new String[] { "Hello", "World" };
    String[] universe = new String[] { "Universe", null };
    String[] helloWorldUniverse = new String[] { "Hello", "World", "Universe", null };
    modifiable.add(key, helloWorld);
    assertEquals("checking Hello World: ", helloWorld, modifiable.getParams(key));
    modifiable.add(key, universe);
    String[] result = modifiable.getParams(key);
    compareArrays("checking Hello World Universe ", helloWorldUniverse, result);
  }
  private void compareArrays(String prefix,
                             String[] expected,
                             String[] actual)
  {
    assertEquals(prefix + "length: ", expected.length, actual.length);
    for (int i = 0; i < expected.length; ++i)
    {
      assertEquals(prefix + " index  " + i, expected[i], actual[i]);
    }
  }
  private ModifiableSolrParams modifiable;
}
