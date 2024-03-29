package org.apache.xalan.trace;
public interface TraceListener extends java.util.EventListener
{
  public void trace(TracerEvent ev);
  public void selected(SelectionEvent ev) throws javax.xml.transform.TransformerException;
  public void generated(GenerateEvent ev);
}
