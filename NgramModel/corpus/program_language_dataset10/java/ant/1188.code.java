package org.example.junit;
import junit.framework.TestCase;
public class Timeout extends TestCase {
    public void testTimeout() throws InterruptedException {
        Thread.sleep(5000);
    }
    public void tearDown() {
        System.out.println("tearDown called on Timeout");
    }
}
