package org.apache.batik.dom.traversal;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.apache.batik.dom.AbstractDocument;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.TreeWalker;
public class TraversalSupport {
    protected List iterators;
    public TraversalSupport() {
    }
    public static TreeWalker createTreeWalker(AbstractDocument doc,
                                              Node root,
                                              int whatToShow, 
                                              NodeFilter filter, 
                                              boolean entityReferenceExpansion) {
        if (root == null) {
            throw doc.createDOMException
                (DOMException.NOT_SUPPORTED_ERR, "null.root",  null);
        }
        return new DOMTreeWalker(root, whatToShow, filter,
                                 entityReferenceExpansion);
    }
    public NodeIterator createNodeIterator(AbstractDocument doc,
                                           Node root,
                                           int whatToShow, 
                                           NodeFilter filter, 
                                           boolean entityReferenceExpansion)
        throws DOMException {
        if (root == null) {
            throw doc.createDOMException
                (DOMException.NOT_SUPPORTED_ERR, "null.root",  null);
        }
        NodeIterator result = new DOMNodeIterator(doc, root, whatToShow,
                                                  filter,
                                                  entityReferenceExpansion);
        if (iterators == null) {
            iterators = new LinkedList();
        }
        iterators.add(result);
        return result;
    }
    public void nodeToBeRemoved(Node removedNode) {
        if (iterators != null) {
            Iterator it = iterators.iterator();
            while (it.hasNext()) {
                ((DOMNodeIterator)it.next()).nodeToBeRemoved(removedNode);
            }
        }
    }
    public void detachNodeIterator(NodeIterator it) {
        iterators.remove(it);
    }
}
