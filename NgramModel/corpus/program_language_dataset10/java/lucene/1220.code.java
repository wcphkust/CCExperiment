package org.apache.lucene.queryParser.standard.processors;
import java.util.List;
import org.apache.lucene.queryParser.core.QueryNodeException;
import org.apache.lucene.queryParser.core.nodes.QueryNode;
import org.apache.lucene.queryParser.core.nodes.SlopQueryNode;
import org.apache.lucene.queryParser.core.nodes.TokenizedPhraseQueryNode;
import org.apache.lucene.queryParser.core.processors.QueryNodeProcessorImpl;
import org.apache.lucene.queryParser.standard.nodes.MultiPhraseQueryNode;
public class PhraseSlopQueryNodeProcessor extends QueryNodeProcessorImpl {
  public PhraseSlopQueryNodeProcessor() {
  }
  @Override
  protected QueryNode postProcessNode(QueryNode node) throws QueryNodeException {
    if (node instanceof SlopQueryNode) {
      SlopQueryNode phraseSlopNode = (SlopQueryNode) node;
      if (!(phraseSlopNode.getChild() instanceof TokenizedPhraseQueryNode)
          && !(phraseSlopNode.getChild() instanceof MultiPhraseQueryNode)) {
        return phraseSlopNode.getChild();
      }
    }
    return node;
  }
  @Override
  protected QueryNode preProcessNode(QueryNode node) throws QueryNodeException {
    return node;
  }
  @Override
  protected List<QueryNode> setChildrenOrder(List<QueryNode> children)
      throws QueryNodeException {
    return children;
  }
}
