package org.apache.solr.response;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.DocList;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.search.DocIterator;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
public abstract class BaseResponseWriter {
  private static final Logger LOG = LoggerFactory
      .getLogger(BaseResponseWriter.class);
  private static final String SCORE_FIELD = "score";
  public void write(SingleResponseWriter responseWriter,
      SolrQueryRequest request, SolrQueryResponse response) throws IOException {
    responseWriter.start();
    NamedList nl = response.getValues();
    for (int i = 0; i < nl.size(); i++) {
      String name = nl.getName(i);
      Object val = nl.getVal(i);
      if ("responseHeader".equals(name)) {
        Boolean omitHeader = request.getParams().getBool(CommonParams.OMIT_HEADER);
        if (omitHeader == null || !omitHeader) responseWriter.writeResponseHeader((NamedList) val);
      } else if (val instanceof SolrDocumentList) {
        SolrDocumentList list = (SolrDocumentList) val;
        DocListInfo info = new DocListInfo((int)list.getNumFound(), list.size(), (int)list.getStart(), list.getMaxScore());
        if (responseWriter.isStreamingDocs()) {
          responseWriter.startDocumentList(name,info);
          for (SolrDocument solrDocument : list)
            responseWriter.writeDoc(solrDocument);
          responseWriter.endDocumentList();
        } else {
          responseWriter.writeAllDocs(info, list);
        }
      } else if (val instanceof DocList) {
        DocList docList = (DocList) val;
        int sz = docList.size();
        IdxInfo idxInfo = new IdxInfo(request.getSchema(), request
            .getSearcher(), response.getReturnFields());
        DocListInfo info = new DocListInfo(docList.matches(), docList.size(),docList.offset(),
            docList.maxScore());
        DocIterator iterator = docList.iterator();
        if (responseWriter.isStreamingDocs()) {
          responseWriter.startDocumentList(name,info);
          for (int j = 0; j < sz; j++) {
            SolrDocument sdoc = getDoc(iterator.nextDoc(), idxInfo);
            if (idxInfo.includeScore && docList.hasScores()) {
              sdoc.addField(SCORE_FIELD, iterator.score());
            }
            responseWriter.writeDoc(sdoc);
          }
          responseWriter.end();
        } else {
          ArrayList<SolrDocument> list = new ArrayList<SolrDocument>(docList
              .size());
          for (int j = 0; j < sz; j++) {
            SolrDocument sdoc = getDoc(iterator.nextDoc(), idxInfo);
            if (idxInfo.includeScore && docList.hasScores()) {
              sdoc.addField(SCORE_FIELD, iterator.score());
            }
          }
          responseWriter.writeAllDocs(info, list);
        }
      } else {
        responseWriter.writeOther(name, val);
      }
    }
    responseWriter.end();
  }
  public void init(NamedList args){}
  private static class IdxInfo {
    IndexSchema schema;
    SolrIndexSearcher searcher;
    Set<String> returnFields;
    boolean includeScore;
    private IdxInfo(IndexSchema schema, SolrIndexSearcher searcher,
        Set<String> returnFields) {
      this.schema = schema;
      this.searcher = searcher;
      this.includeScore = returnFields != null
              && returnFields.contains(SCORE_FIELD);
      if (returnFields != null) {
        if (returnFields.size() == 0 || (returnFields.size() == 1 && includeScore) || returnFields.contains("*")) {
          returnFields = null;  
        }
      }
      this.returnFields = returnFields;
    }
  }
  private static SolrDocument getDoc(int id, IdxInfo info) throws IOException {
    Document doc = info.searcher.doc(id);
    SolrDocument solrDoc = new SolrDocument();
    for (Fieldable f : (List<Fieldable>) doc.getFields()) {
      String fieldName = f.name();
      if (info.returnFields != null && !info.returnFields.contains(fieldName))
        continue;
      SchemaField sf = info.schema.getFieldOrNull(fieldName);
      FieldType ft = null;
      if (sf != null) ft = sf.getType();
      Object val = null;
      if (ft == null) { 
        if (f.isBinary())
          val = f.getBinaryValue();
        else
          val = f.stringValue();
      } else {
        try {
          if (BinaryResponseWriter.KNOWN_TYPES.contains(ft.getClass())) {
            val = ft.toObject(f);
          } else {
            val = ft.toExternal(f);
          }
        } catch (Exception e) {
          LOG.warn("Error reading a field from document : " + solrDoc, e);
          continue;
        }
      }
      if (sf != null && sf.multiValued() && !solrDoc.containsKey(fieldName)) {
        ArrayList l = new ArrayList();
        l.add(val);
        solrDoc.addField(fieldName, l);
      } else {
        solrDoc.addField(fieldName, val);
      }
    }
    return solrDoc;
  }
  public static class DocListInfo {
    public final int numFound;
    public final int start ;
    public Float maxScore = null;
    public final int size;
    public DocListInfo(int numFound, int sz,int start, Float maxScore) {
      this.numFound = numFound;
      size = sz;
      this.start = start;
      this.maxScore = maxScore;
    }
  }
  public static abstract class SingleResponseWriter {
    public void start() throws IOException { }
    public void startDocumentList(String name, DocListInfo info) throws IOException { }
    public void writeDoc(SolrDocument solrDocument) throws IOException { }
    public void endDocumentList() throws IOException { } 
    public void writeResponseHeader(NamedList responseHeader) throws IOException { }
    public void end() throws IOException { }
    public void writeOther(String name, Object other) throws IOException { }
    public boolean isStreamingDocs() { return true; }
    public void writeAllDocs(DocListInfo info, List<SolrDocument> allDocs) throws IOException { }
  }
}
