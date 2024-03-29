package org.apache.lucene.queryParser.core.nodes;
import org.apache.lucene.messages.MessageImpl;
import org.apache.lucene.queryParser.core.QueryNodeError;
import org.apache.lucene.queryParser.core.QueryNodeException;
import org.apache.lucene.queryParser.core.QueryNodeParseException;
import org.apache.lucene.queryParser.core.messages.QueryParserMessages;
import org.apache.lucene.queryParser.core.parser.EscapeQuerySyntax;
public class PhraseSlopQueryNode extends QueryNodeImpl implements FieldableNode {
  private static final long serialVersionUID = 0L;
  private int value = 0;
  public PhraseSlopQueryNode(QueryNode query, int value)
      throws QueryNodeException {
    if (query == null) {
      throw new QueryNodeError(new MessageImpl(
          QueryParserMessages.NODE_ACTION_NOT_SUPPORTED, "query", "null"));
    }
    this.value = value;
    setLeaf(false);
    allocate();
    add(query);
  }
  public QueryNode getChild() {
    return getChildren().get(0);
  }
  public int getValue() {
    return this.value;
  }
  private CharSequence getValueString() {
    Float f = Float.valueOf(this.value);
    if (f == f.longValue())
      return "" + f.longValue();
    else
      return "" + f;
  }
  @Override
  public String toString() {
    return "<phraseslop value='" + getValueString() + "'>" + "\n"
        + getChild().toString() + "\n</phraseslop>";
  }
  public CharSequence toQueryString(EscapeQuerySyntax escapeSyntaxParser) {
    if (getChild() == null)
      return "";
    return getChild().toQueryString(escapeSyntaxParser) + "~"
        + getValueString();
  }
  @Override
  public QueryNode cloneTree() throws CloneNotSupportedException {
    PhraseSlopQueryNode clone = (PhraseSlopQueryNode) super.cloneTree();
    clone.value = this.value;
    return clone;
  }
  public CharSequence getField() {
    QueryNode child = getChild();
    if (child instanceof FieldableNode) {
      return ((FieldableNode) child).getField();
    }
    return null;
  }
  public void setField(CharSequence fieldName) {
    QueryNode child = getChild();
    if (child instanceof FieldableNode) {
      ((FieldableNode) child).setField(fieldName);
    }
  }
}
