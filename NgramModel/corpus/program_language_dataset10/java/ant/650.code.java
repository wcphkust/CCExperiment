package org.apache.tools.ant.types.resolver;
import org.apache.xml.resolver.Catalog;
import org.apache.xml.resolver.CatalogEntry;
import org.apache.xml.resolver.helpers.PublicId;
public class ApacheCatalog extends Catalog {
    private ApacheCatalogResolver resolver = null;
    protected Catalog newCatalog() {
        ApacheCatalog cat = (ApacheCatalog) super.newCatalog();
        cat.setResolver(resolver);
        return cat;
    }
    public void setResolver(ApacheCatalogResolver resolver) {
        this.resolver = resolver;
    }
    public void addEntry(CatalogEntry entry) {
        int type = entry.getEntryType();
        if (type == PUBLIC) {
            String publicid = PublicId.normalize(entry.getEntryArg(0));
            String systemid = normalizeURI(entry.getEntryArg(1));
            if (resolver == null) {
                catalogManager.debug
                    .message(1, "Internal Error: null ApacheCatalogResolver");
            } else {
                resolver.addPublicEntry(publicid, systemid, base);
            }
        } else if (type == URI) {
            String uri = normalizeURI(entry.getEntryArg(0));
            String altURI = normalizeURI(entry.getEntryArg(1));
            if (resolver == null) {
                catalogManager.debug
                    .message(1, "Internal Error: null ApacheCatalogResolver");
            } else {
                resolver.addURIEntry(uri, altURI, base);
            }
        }
        super.addEntry(entry);
    }
} 