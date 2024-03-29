package org.apache.lucene.benchmark.byTask.tasks;
import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.benchmark.byTask.PerfRunData;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexReader;
public class CommitIndexTask extends PerfTask {
  Map<String,String> commitUserData;
  public CommitIndexTask(PerfRunData runData) {
    super(runData);
  }
  @Override
  public boolean supportsParams() {
    return true;
  }
  @Override
  public void setParams(String params) {
    commitUserData = new HashMap<String,String>();
    commitUserData.put(OpenReaderTask.USER_DATA, params);
  }
  @Override
  public int doLogic() throws Exception {
    IndexWriter iw = getRunData().getIndexWriter();
    if (iw != null) {
      iw.commit(commitUserData);
    } else {
      IndexReader r = getRunData().getIndexReader();
      if (r != null) {
        r.commit(commitUserData);
        r.decRef();
      } else {
        throw new IllegalStateException("neither IndexWriter nor IndexReader is currently open");
      }
    }
    return 1;
  }
}
