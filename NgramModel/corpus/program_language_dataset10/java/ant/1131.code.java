package org.apache.tools.ant.types.selectors;
import java.io.File;
import org.apache.tools.ant.types.selectors.modifiedselector.Algorithm;
public class MockAlgorithm implements Algorithm {
    public boolean isValid() {
        return true;
    }
    public String getValue(File file) {
        return "TEST";
    }
    public String toString() {
        return "MockAlgorithm@" + hashCode();
    }
}
