package org.apache.tools.ant.filters;
import java.io.Reader;
import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.types.Parameterizable;
public abstract class BaseParamFilterReader
    extends BaseFilterReader
    implements Parameterizable {
    private Parameter[] parameters;
    public BaseParamFilterReader() {
        super();
    }
    public BaseParamFilterReader(final Reader in) {
        super(in);
    }
    public final void setParameters(final Parameter[] parameters) {
        this.parameters = parameters;
        setInitialized(false);
    }
    protected final Parameter[] getParameters() {
        return parameters;
    }
}
