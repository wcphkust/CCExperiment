package org.apache.tools.ant.util;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;
import org.apache.tools.ant.types.Mapper;
public abstract class ContainerMapper implements FileNameMapper {
    private List mappers = new ArrayList();
    public void addConfiguredMapper(Mapper mapper) {
        add(mapper.getImplementation());
    }
    public void addConfigured(FileNameMapper fileNameMapper) {
        add(fileNameMapper);
    }
    public synchronized void add(FileNameMapper fileNameMapper) {
        if (this == fileNameMapper
            || (fileNameMapper instanceof ContainerMapper
            && ((ContainerMapper) fileNameMapper).contains(this))) {
            throw new IllegalArgumentException(
                "Circular mapper containment condition detected");
        } else {
            mappers.add(fileNameMapper);
        }
    }
    protected synchronized boolean contains(FileNameMapper fileNameMapper) {
        boolean foundit = false;
        for (Iterator iter = mappers.iterator(); iter.hasNext() && !foundit;) {
            FileNameMapper next = (FileNameMapper) (iter.next());
            foundit = (next == fileNameMapper
                || (next instanceof ContainerMapper
                && ((ContainerMapper) next).contains(fileNameMapper)));
        }
        return foundit;
    }
    public synchronized List getMappers() {
        return Collections.unmodifiableList(mappers);
    }
    public void setFrom(String ignore) {
    }
    public void setTo(String ignore) {
    }
}
