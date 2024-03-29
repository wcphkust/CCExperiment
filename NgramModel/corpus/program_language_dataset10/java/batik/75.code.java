package org.apache.batik.apps.svgbrowser;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.Autoscroll;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;
import java.util.Iterator;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.EventListenerList;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.apache.batik.dom.util.DOMUtilities;
import org.apache.batik.apps.svgbrowser.DOMViewer.NodeInfo;
import org.w3c.dom.Node;
public class DOMDocumentTree extends JTree implements Autoscroll {
    protected EventListenerList eventListeners = new EventListenerList();
    protected Insets autoscrollInsets = new Insets(20, 20, 20, 20);
    protected Insets scrollUnits = new Insets(25, 25, 25, 25);
    protected DOMDocumentTreeController controller;
    public DOMDocumentTree(TreeNode root, DOMDocumentTreeController controller) {
        super(root);
        this.controller = controller;
        new TreeDragSource(this, DnDConstants.ACTION_COPY_OR_MOVE);
        new DropTarget(this, new TreeDropTargetListener(this));
    }
    public class TreeDragSource implements DragSourceListener,
                                           DragGestureListener {
        protected DragSource source;
        protected DragGestureRecognizer recognizer;
        protected TransferableTreeNode transferable;
        protected DOMDocumentTree sourceTree;
        public TreeDragSource(DOMDocumentTree tree, int actions) {
            sourceTree = tree;
            source = new DragSource();
            recognizer =
                source.createDefaultDragGestureRecognizer(sourceTree, actions,
                                                          this);
        }
        public void dragGestureRecognized(DragGestureEvent dge) {
            if (!controller.isDNDSupported()) {
                return;
            }
            TreePath[] paths = sourceTree.getSelectionPaths();
            if (paths == null) {
                return;
            }
            ArrayList nodeList = new ArrayList();
            for (int i = 0; i < paths.length; i++) {
                TreePath path = paths[i];
                if (path.getPathCount() > 1) {
                    DefaultMutableTreeNode node =
                        (DefaultMutableTreeNode) path.getLastPathComponent();
                    Node associatedNode = getDomNodeFromTreeNode(node);
                    if (associatedNode != null) {
                        nodeList.add(associatedNode);
                    }
                }
            }
            if (nodeList.isEmpty()) {
                return;
            }
            transferable = new TransferableTreeNode(new TransferData(nodeList));
            source.startDrag(dge, null, transferable, this);
        }
        public void dragEnter(DragSourceDragEvent dsde) {
        }
        public void dragExit(DragSourceEvent dse) {
        }
        public void dragOver(DragSourceDragEvent dsde) {
        }
        public void dropActionChanged(DragSourceDragEvent dsde) {
        }
        public void dragDropEnd(DragSourceDropEvent dsde) {
        }
    }
    public class TreeDropTargetListener implements DropTargetListener {
        private static final int BEFORE = 1;
        private static final int AFTER = 2;
        private static final int CURRENT = 3;
        private TransferData transferData;
        private Component originalGlassPane;
        private int visualTipOffset = 5;
        private int visualTipThickness = 2;
        private int positionIndicator;
        private Point startPoint;
        private Point endPoint;
        protected JPanel visualTipGlassPane = new JPanel() {
            public void paint(Graphics g) {
                g.setColor(UIManager.getColor("Tree.selectionBackground"));
                if (startPoint == null || endPoint == null) {
                    return;
                }
                int x1 = startPoint.x;
                int x2 = endPoint.x;
                int y1 = startPoint.y;
                int start = -visualTipThickness / 2;
                start += visualTipThickness % 2 == 0 ? 1 : 0;
                for (int i = start; i <= visualTipThickness / 2; i++) {
                    g.drawLine(x1 + 2, y1 + i, x2 - 2, y1 + i);
                }
            }
        };
        private Timer expandControlTimer;
        private int expandTimeout = 1500;
        private TreePath dragOverTreePath;
        private TreePath treePathToExpand;
        public TreeDropTargetListener(DOMDocumentTree tree) {
            addOnAutoscrollListener(tree);
        }
        public void dragEnter(DropTargetDragEvent dtde) {
            JTree tree = (JTree) dtde.getDropTargetContext().getComponent();
            JRootPane rootPane = tree.getRootPane();
            originalGlassPane = rootPane.getGlassPane();
            rootPane.setGlassPane(visualTipGlassPane);
            visualTipGlassPane.setOpaque(false);
            visualTipGlassPane.setVisible(true);
            updateVisualTipLine(tree, null);
            try {
                Transferable transferable =
                    new DropTargetDropEvent(dtde.getDropTargetContext(),
                                            dtde.getLocation(), 0, 0)
                        .getTransferable();
                DataFlavor[] flavors = transferable.getTransferDataFlavors();
                for (int i = 0; i < flavors.length; i++) {
                    if (transferable.isDataFlavorSupported(flavors[i])) {
                        transferData = (TransferData) transferable
                                .getTransferData(flavors[i]);
                        return;
                    }
                }
            } catch (UnsupportedFlavorException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public void dragOver(DropTargetDragEvent dtde) {
            JTree tree = (JTree) dtde.getDropTargetContext().getComponent();
            TreeNode targetTreeNode = getNode(dtde);
            if (targetTreeNode != null) {
                updatePositionIndicator(dtde);
                Point p = dtde.getLocation();
                TreePath currentPath = tree.getPathForLocation(p.x, p.y);
                TreePath parentPath = getParentPathForPosition(currentPath);
                TreeNode parentNode = getNodeForPath(parentPath);
                TreePath nextSiblingPath =
                    getSiblingPathForPosition(currentPath);
                TreeNode nextSiblingNode = getNodeForPath(nextSiblingPath);
                Node potentialParent =
                    getDomNodeFromTreeNode((DefaultMutableTreeNode) parentNode);
                Node potentialSibling =
                    getDomNodeFromTreeNode
                        ((DefaultMutableTreeNode) nextSiblingNode);
                if (DOMUtilities.canAppendAny(transferData.getNodeList(),
                                              potentialParent)
                        && !transferData.getNodeList()
                            .contains(potentialSibling)) {
                    dtde.acceptDrag(dtde.getDropAction());
                    updateVisualTipLine(tree, currentPath);
                    dragOverTreePath = currentPath;
                    if (!tree.isExpanded(currentPath)) {
                        scheduleExpand(currentPath, tree);
                    }
                } else {
                    dtde.rejectDrag();
                }
            } else {
                dtde.rejectDrag();
            }
        }
        public void dropActionChanged(DropTargetDragEvent dtde) {
        }
        public void drop(DropTargetDropEvent dtde) {
            Point p = dtde.getLocation();
            DropTargetContext dtc = dtde.getDropTargetContext();
            JTree tree = (JTree) dtc.getComponent();
            setOriginalGlassPane(tree);
            dragOverTreePath = null;
            TreePath currentPath = tree.getPathForLocation(p.x, p.y);
            DefaultMutableTreeNode parent =
                (DefaultMutableTreeNode) getNodeForPath
                    (getParentPathForPosition(currentPath));
            Node dropTargetNode = getDomNodeFromTreeNode(parent);
            DefaultMutableTreeNode sibling =
                (DefaultMutableTreeNode)
                    getNodeForPath(getSiblingPathForPosition(currentPath));
            Node siblingNode = getDomNodeFromTreeNode(sibling);
            if (this.transferData != null) {
                ArrayList nodelist =
                    getNodeListForParent(this.transferData.getNodeList(),
                                         dropTargetNode);
                fireDropCompleted
                    (new DOMDocumentTreeEvent
                        (new DropCompletedInfo
                            (dropTargetNode, siblingNode, nodelist)));
                dtde.dropComplete(true);
                return;
            }
            dtde.rejectDrop();
        }
        public void dragExit(DropTargetEvent dte) {
            setOriginalGlassPane
                ((JTree) dte.getDropTargetContext().getComponent());
            dragOverTreePath = null;
        }
        private void updatePositionIndicator(DropTargetDragEvent dtde) {
            Point p = dtde.getLocation();
            DropTargetContext dtc = dtde.getDropTargetContext();
            JTree tree = (JTree) dtc.getComponent();
            TreePath currentPath = tree.getPathForLocation(p.x, p.y);
            Rectangle bounds = tree.getPathBounds(currentPath);
            if (p.y <= bounds.y + visualTipOffset) {
                positionIndicator = BEFORE;
            }
            else if (p.y >= bounds.y + bounds.height - visualTipOffset) {
                positionIndicator = AFTER;
            }
            else {
                positionIndicator = CURRENT;
            }
        }
        private TreePath getParentPathForPosition(TreePath currentPath) {
            if (currentPath == null) {
                return null;
            }
            TreePath parentPath = null;
            if (positionIndicator == AFTER) {
                parentPath = currentPath.getParentPath();
            } else if (positionIndicator == BEFORE) {
                parentPath = currentPath.getParentPath();
            } else if (positionIndicator == CURRENT) {
                parentPath = currentPath;
            }
            return parentPath;
        }
        private TreePath getSiblingPathForPosition(TreePath currentPath) {
            TreePath parentPath = getParentPathForPosition(currentPath);
            TreePath nextSiblingPath = null;
            if (positionIndicator == AFTER) {
                TreeNode parentNode = getNodeForPath(parentPath);
                TreeNode currentNode = getNodeForPath(currentPath);
                if (parentPath != null && parentNode != null
                        && currentNode != null) {
                    int siblingIndex = parentNode.getIndex(currentNode) + 1;
                    if (parentNode.getChildCount() > siblingIndex) {
                        nextSiblingPath =
                            parentPath.pathByAddingChild
                                (parentNode.getChildAt(siblingIndex));
                    }
                }
            } else if (positionIndicator == BEFORE) {
                nextSiblingPath = currentPath;
            } else if (positionIndicator == CURRENT) {
                nextSiblingPath = null;
            }
            return nextSiblingPath;
        }
        private TreeNode getNodeForPath(TreePath path) {
            if (path == null || path.getLastPathComponent() == null) {
                return null;
            }
            return (TreeNode) path.getLastPathComponent();
        }
        private TreeNode getNode(DropTargetDragEvent dtde) {
            Point p = dtde.getLocation();
            DropTargetContext dtc = dtde.getDropTargetContext();
            JTree tree = (JTree) dtc.getComponent();
            TreePath path = tree.getPathForLocation(p.x, p.y);
            if (path == null || path.getLastPathComponent() == null) {
                return null;
            }
            return (TreeNode) path.getLastPathComponent();
        }
        private void updateVisualTipLine(JTree tree, TreePath path) {
            if (path == null) {
                startPoint = null;
                endPoint = null;
            } else {
                Rectangle bounds = tree.getPathBounds(path);
                if (positionIndicator == BEFORE) {
                    startPoint = bounds.getLocation();
                    endPoint = new Point(startPoint.x + bounds.width,
                            startPoint.y);
                } else if (positionIndicator == AFTER) {
                    startPoint = new Point(bounds.x, bounds.y + bounds.height);
                    endPoint = new Point(startPoint.x + bounds.width,
                            startPoint.y);
                    positionIndicator = AFTER;
                } else if (positionIndicator == CURRENT) {
                    startPoint = null;
                    endPoint = null;
                }
                if (startPoint != null && endPoint != null) {
                    startPoint = SwingUtilities.convertPoint(tree, startPoint,
                            visualTipGlassPane);
                    endPoint = SwingUtilities.convertPoint(tree, endPoint,
                            visualTipGlassPane);
                }
            }
            visualTipGlassPane.getRootPane().repaint();
        }
        private void addOnAutoscrollListener(DOMDocumentTree tree) {
            tree.addListener(new DOMDocumentTreeAdapter() {
                public void onAutoscroll(DOMDocumentTreeEvent event) {
                    startPoint = null;
                    endPoint = null;
                }
            });
        }
        private void setOriginalGlassPane(JTree tree) {
            JRootPane rootPane = tree.getRootPane();
            rootPane.setGlassPane(originalGlassPane);
            originalGlassPane.setVisible(false);
            rootPane.repaint();
        }
        private void scheduleExpand(TreePath treePath, JTree tree) {
            if (treePath != treePathToExpand) {
                getExpandTreeTimer(tree).stop();
                treePathToExpand = treePath;
                getExpandTreeTimer(tree).start();
            }
        }
        private Timer getExpandTreeTimer(final JTree tree) {
            if (expandControlTimer == null) {
                expandControlTimer = new Timer(expandTimeout,
                        new ActionListener() {
                            public void actionPerformed(ActionEvent arg0) {
                                if (treePathToExpand != null
                                        && treePathToExpand == dragOverTreePath) {
                                    tree.expandPath(treePathToExpand);
                                }
                                getExpandTreeTimer(tree).stop();
                            }
                        });
            }
            return expandControlTimer;
        }
    }
    public static class TransferableTreeNode implements Transferable {
        protected static final DataFlavor NODE_FLAVOR =
            new DataFlavor(TransferData.class, "TransferData");
        protected static final DataFlavor[] FLAVORS =
            new DataFlavor[] { NODE_FLAVOR, DataFlavor.stringFlavor };
        protected TransferData data;
        public TransferableTreeNode(TransferData data) {
            this.data = data;
        }
        public synchronized DataFlavor[] getTransferDataFlavors() {
            return FLAVORS;
        }
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            for (int i = 0; i < FLAVORS.length; i++) {
                if (flavor.equals(FLAVORS[i])) {
                    return true;
                }
            }
            return false;
        }
        public synchronized Object getTransferData(DataFlavor flavor) {
            if (!isDataFlavorSupported(flavor)) {
                return null;
            }
            if (flavor.equals(NODE_FLAVOR)) {
                return data;
            } else if (flavor.equals(DataFlavor.stringFlavor)) {
                return data.getNodesAsXML();
            } else {
                return null;
            }
        }
    }
    public static class TransferData {
        protected ArrayList nodeList;
        public TransferData(ArrayList nodeList) {
            this.nodeList = nodeList;
        }
        public ArrayList getNodeList() {
            return nodeList;
        }
        public String getNodesAsXML() {
            String toReturn = "";
            Iterator iterator = nodeList.iterator();
            while (iterator.hasNext()) {
                Node node = (Node) iterator.next();
                toReturn += DOMUtilities.getXML(node);
            }
            return toReturn;
        }
    }
    public void autoscroll(Point point) {
        JViewport viewport =
            (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class,
                                                          this);
        if (viewport == null) {
            return;
        }
        Point viewportPos = viewport.getViewPosition();
        int viewHeight = viewport.getExtentSize().height;
        int viewWidth = viewport.getExtentSize().width;
        if ((point.y - viewportPos.y) < autoscrollInsets.top) {
            viewport.setViewPosition
                (new Point(viewportPos.x,
                           Math.max(viewportPos.y - scrollUnits.top, 0)));
            fireOnAutoscroll(new DOMDocumentTreeEvent(this));
        } else if ((viewportPos.y + viewHeight - point.y)
                    < autoscrollInsets.bottom) {
            viewport.setViewPosition
                (new Point(viewportPos.x,
                           Math.min(viewportPos.y + scrollUnits.bottom,
                                    getHeight() - viewHeight)));
            fireOnAutoscroll(new DOMDocumentTreeEvent(this));
        } else if ((point.x - viewportPos.x) < autoscrollInsets.left) {
            viewport.setViewPosition
                (new Point(Math.max(viewportPos.x - scrollUnits.left, 0),
                           viewportPos.y));
            fireOnAutoscroll(new DOMDocumentTreeEvent(this));
        } else if ((viewportPos.x + viewWidth - point.x)
                    < autoscrollInsets.right) {
            viewport.setViewPosition
                (new Point(Math.min(viewportPos.x + scrollUnits.right,
                                    getWidth() - viewWidth),
                           viewportPos.y));
            fireOnAutoscroll(new DOMDocumentTreeEvent(this));
        }
    }
    public Insets getAutoscrollInsets() {
        int topAndBottom = getHeight();
        int leftAndRight = getWidth();
        return new Insets
            (topAndBottom, leftAndRight, topAndBottom, leftAndRight);
    }
    public static class DOMDocumentTreeEvent extends EventObject {
        public DOMDocumentTreeEvent(Object source) {
            super(source);
        }
    }
    public static interface DOMDocumentTreeListener extends EventListener {
        void dropCompleted(DOMDocumentTreeEvent event);
        void onAutoscroll(DOMDocumentTreeEvent event);
    }
    public static class DOMDocumentTreeAdapter
            implements DOMDocumentTreeListener {
        public void dropCompleted(DOMDocumentTreeEvent event) {
        }
        public void onAutoscroll(DOMDocumentTreeEvent event) {
        }
    }
    public void addListener(DOMDocumentTreeListener listener) {
        eventListeners.add(DOMDocumentTreeListener.class, listener);
    }
    public void fireDropCompleted(DOMDocumentTreeEvent event) {
        Object[] listeners = eventListeners.getListenerList();
        int length = listeners.length;
        for (int i = 0; i < length; i += 2) {
            if (listeners[i] == DOMDocumentTreeListener.class) {
                ((DOMDocumentTreeListener) listeners[i + 1])
                        .dropCompleted(event);
            }
        }
    }
    public void fireOnAutoscroll(DOMDocumentTreeEvent event) {
        Object[] listeners = eventListeners.getListenerList();
        int length = listeners.length;
        for (int i = 0; i < length; i += 2) {
            if (listeners[i] == DOMDocumentTreeListener.class) {
                ((DOMDocumentTreeListener) listeners[i + 1])
                        .onAutoscroll(event);
            }
        }
    }
    public static class DropCompletedInfo {
        protected Node parent;
        protected ArrayList children;
        protected Node sibling;
        public DropCompletedInfo(Node parent, Node sibling,
                                 ArrayList children) {
            this.parent = parent;
            this.sibling = sibling;
            this.children = children;
        }
        public ArrayList getChildren() {
            return children;
        }
        public Node getParent() {
            return parent;
        }
        public Node getSibling() {
            return sibling;
        }
    }
    protected Node getDomNodeFromTreeNode(DefaultMutableTreeNode treeNode) {
        if (treeNode == null) {
            return null;
        }
        if (treeNode.getUserObject() instanceof NodeInfo) {
            return ((NodeInfo) treeNode.getUserObject()).getNode();
        }
        return null;
    }
    protected ArrayList getNodeListForParent(ArrayList potentialChildren,
                                             Node parentNode) {
        ArrayList children = new ArrayList();
        int n = potentialChildren.size();
        for (int i = 0; i < n; i++) {
            Node node = (Node) potentialChildren.get(i);
            if (DOMUtilities.canAppend(node, parentNode)) {
                children.add(node);
            }
        }
        return children;
    }
}
