package org.apache.batik.dom.traversal;
import org.apache.batik.dom.AbstractNode;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;
public class DOMTreeWalker implements TreeWalker {
    protected Node root;
    protected int whatToShow;
    protected NodeFilter filter;
    protected boolean expandEntityReferences;
    protected Node currentNode;
    public DOMTreeWalker(Node n, int what, NodeFilter nf, boolean exp) {
        root = n;
        whatToShow = what;
        filter = nf;
        expandEntityReferences = exp;
        currentNode = root;
    }
    public Node getRoot() {
        return root;
    }
    public int getWhatToShow() {
        return whatToShow;
    }
    public NodeFilter getFilter() {
        return filter;
    }
    public boolean getExpandEntityReferences() {
        return expandEntityReferences;
    }
    public Node getCurrentNode() {
        return currentNode;
    }
    public void setCurrentNode(Node n) {
        if (n == null) {
            throw ((AbstractNode)root).createDOMException
                (DOMException.NOT_SUPPORTED_ERR,
                 "null.current.node",  null);
        }
        currentNode = n;
    }
    public Node parentNode() {
        Node result = parentNode(currentNode);
        if (result != null) {
            currentNode = result;
        }
        return result;
    }
    public Node firstChild() {
        Node result = firstChild(currentNode);
        if (result != null) {
            currentNode = result;
        }
        return result;
    }
    public Node lastChild() {
        Node result = lastChild(currentNode);
        if (result != null) {
            currentNode = result;
        }
        return result;
    }
    public Node previousSibling() {
        Node result = previousSibling(currentNode, root);
        if (result != null) {
            currentNode = result;
        }
        return result;
    }
    public Node nextSibling() {
        Node result = nextSibling(currentNode, root);
        if (result != null) {
            currentNode = result;
        }
        return result;
    }
    public Node previousNode() {
        Node result = previousSibling(currentNode, root);
        if (result == null) {
            result = parentNode(currentNode);
            if (result != null) {
                currentNode = result;
            }
            return result;
        }
        Node n = lastChild(result);
        Node last = n;
        while (n != null) {
            last = n;
            n = lastChild(last);
        }
        return currentNode = (last != null) ? last : result;
    }
    public Node nextNode() {
        Node result;
        if ((result = firstChild(currentNode)) != null) {
            return currentNode = result;
        }
        if ((result = nextSibling(currentNode, root)) != null) {
            return currentNode = result;
        }
        Node parent = currentNode;
        for (;;) {
            parent = parentNode(parent);
            if (parent == null) {
                return null;
            }
            if ((result = nextSibling(parent, root)) != null) {
                return currentNode = result;
            }
        }
    }
    protected Node parentNode(Node n) {
        if (n == root) {
            return null;
        }
        Node result = n;
        for (;;) {
            result = result.getParentNode();
            if (result == null) {
                return null;
            }
            if ((whatToShow & (1 << result.getNodeType() - 1)) != 0) {
                if (filter == null ||
                    filter.acceptNode(result) == NodeFilter.FILTER_ACCEPT) {
                    return result;
                }
            }
        }
    }
    protected Node firstChild(Node n) {
        if (n.getNodeType() == Node.ENTITY_REFERENCE_NODE &&
            !expandEntityReferences) {
            return null;
        }
        Node result = n.getFirstChild();
        if (result == null) {
            return null;
        }
        switch (acceptNode(result)) {
        case NodeFilter.FILTER_ACCEPT:
            return result;
        case NodeFilter.FILTER_SKIP:
            Node t = firstChild(result);
            if (t != null) {
                return t;
            }
        default: 
            return nextSibling(result, n);
        }
    }
    protected Node lastChild(Node n) {
        if (n.getNodeType() == Node.ENTITY_REFERENCE_NODE &&
            !expandEntityReferences) {
            return null;
        }
        Node result = n.getLastChild();
        if (result == null) {
            return null;
        }
        switch (acceptNode(result)) {
        case NodeFilter.FILTER_ACCEPT:
            return result;
        case NodeFilter.FILTER_SKIP:
            Node t = lastChild(result);
            if (t != null) {
                return t;
            }
        default: 
            return previousSibling(result, n);
        }
    }
    protected Node previousSibling(Node n, Node root) {
        while (true) {
            if (n == root) {
                return null;
            }
            Node result = n.getPreviousSibling();
            if (result == null) {
                result = n.getParentNode();
                if (result == null || result == root) {
                    return null;
                }
                if (acceptNode(result) == NodeFilter.FILTER_SKIP) {
                    n = result;
                    continue;
                }
                return null;
            }
            switch (acceptNode(result)) {
            case NodeFilter.FILTER_ACCEPT:
                return result;
            case NodeFilter.FILTER_SKIP:
                Node t = lastChild(result);
                if (t != null) {
                    return t;
                }
            default: 
                n = result;
                continue;
            }
        }
    }
    protected Node nextSibling(Node n, Node root) {
        while (true) {
            if (n == root) {
                return null;
            }
            Node result = n.getNextSibling();
            if (result == null) {
                result = n.getParentNode();
                if (result == null || result == root) {
                    return null;
                }
                if (acceptNode(result) == NodeFilter.FILTER_SKIP) {
                    n = result;
                    continue;
                }
                return null;
            }
            switch (acceptNode(result)) {
            case NodeFilter.FILTER_ACCEPT:
                return result;
            case NodeFilter.FILTER_SKIP:
                Node t = firstChild(result);
                if (t != null) {
                    return t;
                }
            default: 
                n = result;
                continue;
            }
        }
    }
    protected short acceptNode(Node n) {
        if ((whatToShow & (1 << n.getNodeType() - 1)) != 0) {
            if (filter == null) {
                return NodeFilter.FILTER_ACCEPT;
            } else {
                return filter.acceptNode(n);
            }
        } else {
            return NodeFilter.FILTER_SKIP;
        }
    }
}
