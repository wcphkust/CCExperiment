package org.apache.lucene.ant;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.lang.reflect.Constructor;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogMergePolicy;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DynamicConfigurator;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileResource;
public class IndexTask extends Task {
  protected Vector<ResourceCollection> rcs = new Vector<ResourceCollection>();
  private boolean overwrite = false;
  private File indexDir;
  private String handlerClassName =
    FileExtensionDocumentHandler.class.getName();
  private DocumentHandler handler;
  private String analyzerClassName =
    StandardAnalyzer.class.getName();
  private Analyzer analyzer;
  private int mergeFactor = 20;
  private HandlerConfig handlerConfig;
  private boolean useCompoundIndex = true;
  public IndexTask() {
  }
  public void setIndex(File indexDir) {
    this.indexDir = indexDir;
  }
  public void setMergeFactor(int mergeFactor) {
    this.mergeFactor = mergeFactor;
  }
  public void setOverwrite(boolean overwrite) {
    this.overwrite = overwrite;
  }
  public void setUseCompoundIndex(boolean useCompoundIndex) {
    this.useCompoundIndex = useCompoundIndex;
  }
  public void setDocumentHandler(String classname) {
    handlerClassName = classname;
  }
  public void setAnalyzer(AnalyzerType type) {
    analyzerClassName = type.getClassname();
  }
  public void setAnalyzerClassName(String classname) {
    analyzerClassName = classname;
  }
  public void addFileset(FileSet set) {
    add(set);
  }
    public void add(ResourceCollection res) {
        rcs.add(res);
    }
  public void addConfig(HandlerConfig config) throws BuildException {
    if (handlerConfig != null) {
      throw new BuildException("Only one config element allowed");
    }
    handlerConfig = config;
  }
  private static final Analyzer createAnalyzer(String className) throws Exception{
    final Class<? extends Analyzer> clazz = Class.forName(className).asSubclass(Analyzer.class);
    try {
      Constructor<? extends Analyzer> cnstr = clazz.getConstructor(Version.class);
      return cnstr.newInstance(Version.LUCENE_CURRENT);
    } catch (NoSuchMethodException nsme) {
      return clazz.newInstance();
    }
  }
  @Override
  public void execute() throws BuildException {
    try {
      handler = Class.forName(handlerClassName).asSubclass(DocumentHandler.class).newInstance();
      analyzer = IndexTask.createAnalyzer(analyzerClassName);
    } catch (Exception e) {
      throw new BuildException(e);
    }
    log("Document handler = " + handler.getClass(), Project.MSG_VERBOSE);
    log("Analyzer = " + analyzer.getClass(), Project.MSG_VERBOSE);
    if (handler instanceof ConfigurableDocumentHandler) {
      ((ConfigurableDocumentHandler) handler).configure(handlerConfig.getProperties());
    }
    try {
      indexDocs();
    } catch (IOException e) {
      throw new BuildException(e);
    }
  }
  private void indexDocs() throws IOException {
    Date start = new Date();
    boolean create = overwrite;
    if (indexDir.mkdirs() && !overwrite) {
      create = true;
    }
    FSDirectory dir = FSDirectory.open(indexDir);
    try {
      Searcher searcher = null;
      boolean checkLastModified = false;
      if (!create) {
        try {
          searcher = new IndexSearcher(dir, true);
          checkLastModified = true;
        } catch (IOException ioe) {
          log("IOException: " + ioe.getMessage());
        }
      }
      log("checkLastModified = " + checkLastModified, Project.MSG_VERBOSE);
      IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(
          Version.LUCENE_CURRENT, analyzer).setOpenMode(
          create ? OpenMode.CREATE : OpenMode.APPEND));
      LogMergePolicy lmp = (LogMergePolicy) writer.getMergePolicy();
      lmp.setUseCompoundFile(useCompoundIndex);
      lmp.setUseCompoundDocStore(useCompoundIndex);
      lmp.setMergeFactor(mergeFactor);
      int totalFiles = 0;
      int totalIndexed = 0;
      int totalIgnored = 0;
      try {
        for (int i = 0; i < rcs.size(); i++) {
          ResourceCollection rc = rcs.elementAt(i);
          if (rc.isFilesystemOnly()) {
            Iterator resources = rc.iterator();
            while (resources.hasNext()) {
              Resource r = (Resource) resources.next();
              if (!r.isExists() || !(r instanceof FileResource)) {
                continue;
              }
              totalFiles++;
              File file = ((FileResource) r).getFile();
              if (!file.exists() || !file.canRead()) {
                throw new BuildException("File \"" +
                                         file.getAbsolutePath()
                                         + "\" does not exist or is not readable.");
              }
              boolean indexIt = true;
              if (checkLastModified) {
                Term pathTerm =
                  new Term("path", file.getPath());
                TermQuery query =
                  new TermQuery(pathTerm);
                ScoreDoc[] hits = searcher.search(query, null, 1).scoreDocs;
                if (hits.length > 0) {
                  Document doc = searcher.doc(hits[0].doc);
                  String indexModified =
                    doc.get("modified").trim();
                  if (indexModified != null) {
                    long lastModified = 0;
                    try {
                      lastModified = DateTools.stringToTime(indexModified);
                    } catch (ParseException e) {
                    }
                    if (lastModified == file.lastModified()) {
                      indexIt = false;
                    }
                  }
                }
              }
              if (indexIt) {
                try {
                  log("Indexing " + file.getPath(),
                      Project.MSG_VERBOSE);
                  Document doc =
                    handler.getDocument(file);
                  if (doc == null) {
                    totalIgnored++;
                  } else {
                    doc.add(new Field("path", file.getPath(), Field.Store.YES, Field.Index.NOT_ANALYZED));
                    doc.add(new Field("modified", DateTools.timeToString(file.lastModified(), DateTools.Resolution.MILLISECOND), Field.Store.YES, Field.Index.NOT_ANALYZED));
                    writer.addDocument(doc);
                    totalIndexed++;
                  }
                } catch (DocumentHandlerException e) {
                  throw new BuildException(e);
                }
              }
            }
          }
        }
        writer.optimize();
      }
      finally {
        writer.close();
        if (searcher != null) {
          searcher.close();
        }
      }
      Date end = new Date();
      log(totalIndexed + " out of " + totalFiles + " indexed (" +
          totalIgnored + " ignored) in " + (end.getTime() - start.getTime()) +
          " milliseconds");
    } finally {
      dir.close();
    }
  }
  public static class HandlerConfig implements DynamicConfigurator {
    Properties props = new Properties();
    public void setDynamicAttribute(String attributeName, String value) throws BuildException {
      props.setProperty(attributeName, value);
    }
    public Object createDynamicElement(String elementName) throws BuildException {
      throw new BuildException("Sub elements not supported");
    }
    public Properties getProperties() {
      return props;
    }
  }
 public static class AnalyzerType extends EnumeratedAttribute {
    private static Map<String,String> analyzerLookup = new HashMap<String,String>();
    static {
      analyzerLookup.put("simple", SimpleAnalyzer.class.getName());
      analyzerLookup.put("standard", StandardAnalyzer.class.getName());
      analyzerLookup.put("stop", StopAnalyzer.class.getName());
      analyzerLookup.put("whitespace", WhitespaceAnalyzer.class.getName());
    }
    @Override
    public String[] getValues() {
      Set<String> keys = analyzerLookup.keySet();
      return keys.toArray(new String[0]);
    }
    public String getClassname() {
      return analyzerLookup.get(getValue());
    }
  }
}
