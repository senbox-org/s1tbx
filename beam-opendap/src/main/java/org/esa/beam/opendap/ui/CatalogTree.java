package org.esa.beam.opendap.ui;

import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.opendap.datamodel.OpendapLeaf;
import org.esa.beam.opendap.utils.OpendapUtils;
import org.esa.beam.util.logging.BeamLogManager;
import thredds.catalog.InvAccess;
import thredds.catalog.InvCatalogRef;
import thredds.catalog.InvDataset;
import thredds.catalog.ServiceType;

import javax.swing.JTree;
import javax.swing.SwingWorker;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.awt.Cursor;
import java.io.InputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class CatalogTree {

    private final JTree jTree;
    private final HashMap<OpendapLeaf, MutableTreeNode> leafToParentNode = new HashMap<OpendapLeaf, MutableTreeNode>();
    private final Set<CatalogTreeListener> catalogTreeListeners = new HashSet<CatalogTreeListener>();
    private final AppContext appContext;
    private final UIContext uiContext;

    public CatalogTree(final LeafSelectionListener leafSelectionListener, AppContext appContext, UIContext uiContext) {
        this.appContext = appContext;
        this.uiContext = uiContext;
        jTree = new JTree();
        jTree.setRootVisible(false);
        ((DefaultTreeModel) jTree.getModel()).setRoot(CatalogTreeUtils.createRootNode());
        CatalogTreeUtils.addCellRenderer(jTree);
        addWillExpandListener();
        CatalogTreeUtils.addTreeSelectionListener(jTree, leafSelectionListener);
    }

    public Component getComponent() {
        return jTree;
    }

    public void setNewRootDatasets(List<InvDataset> rootDatasets) {
        final DefaultTreeModel model = (DefaultTreeModel) jTree.getModel();
        final DefaultMutableTreeNode rootNode = CatalogTreeUtils.createRootNode();
        model.setRoot(rootNode);
        appendToNode(jTree, rootDatasets, rootNode, true);
        fireCatalogElementsInsertionFinished();
        expandPath(rootNode);
        leafToParentNode.clear();
    }

    void addWillExpandListener() {
        jTree.addTreeWillExpandListener(new TreeWillExpandListener() {

            @Override
            public void treeWillExpand(final TreeExpansionEvent event) throws ExpandVetoException {
                new SwingWorker<Void, Void>() {

                    @Override
                    protected Void doInBackground() throws Exception {
                        uiContext.updateStatusBar("Loading subtree...");
                        uiContext.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        Object lastPathComponent = event.getPath().getLastPathComponent();
                        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) lastPathComponent;
                        DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(0);
                        if (CatalogTreeUtils.isCatalogReferenceNode(child)) {
                            resolveCatalogReferenceNode(child, true);
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                        uiContext.updateStatusBar("Ready.");
                        uiContext.setCursor(Cursor.getDefaultCursor());
                    }

                }.execute();
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
            }
        });
    }

    public void resolveCatalogReferenceNode(DefaultMutableTreeNode catalogReferenceNode, boolean expandPath) {
        final DefaultTreeModel model = (DefaultTreeModel) jTree.getModel();
        final DefaultMutableTreeNode parent = (DefaultMutableTreeNode) catalogReferenceNode.getParent();
        model.removeNodeFromParent(catalogReferenceNode);
        try {
            List<InvDataset> catalogDatasets = CatalogTreeUtils.getCatalogDatasets(catalogReferenceNode);
            insertCatalogElements(catalogDatasets, parent, expandPath);
        } catch (Exception e) {
            String msg = MessageFormat.format("Unable to completely resolve catalog. Reason: {0}", e.getMessage());
            BeamLogManager.getSystemLogger().warning(msg);
            appContext.handleError(msg, e);
        }
    }

    void insertCatalogElements(InputStream catalogIS, URI catalogBaseUri, DefaultMutableTreeNode parent,
                               boolean expandPath) {
        final List<InvDataset> catalogDatasets = CatalogTreeUtils.getCatalogDatasets(catalogIS, catalogBaseUri);
        insertCatalogElements(catalogDatasets, parent, expandPath);
    }


    void appendToNode(final JTree jTree, List<InvDataset> datasets, MutableTreeNode parentNode, boolean goDeeper) {
        for (InvDataset dataset : datasets) {
            final MutableTreeNode deeperParent;
            if (!goDeeper || !CatalogTreeUtils.isHyraxId(dataset.getID())) {
                appendToNode(jTree, dataset, parentNode);
                if (parentNode.getChildCount() == 0) {
                    continue;
                }
                deeperParent = (MutableTreeNode) parentNode.getChildAt(parentNode.getChildCount() - 1);
            } else {
                deeperParent = parentNode;
            }
            if (goDeeper && !(dataset instanceof InvCatalogRef)) {
                appendToNode(jTree, dataset.getDatasets(), deeperParent, false);
            }
        }
    }

    void appendLeafNode(MutableTreeNode parentNode, DefaultTreeModel treeModel, InvDataset dataset) {
        final OpendapLeaf leaf = new OpendapLeaf(dataset.getName(), dataset);
        final int fileSize = (int) (OpendapUtils.getDataSizeInBytes(leaf) / 1024);
        leaf.setFileSize(fileSize);
        final InvAccess dapAccess = dataset.getAccess(ServiceType.OPENDAP);
        if (dapAccess != null) {
            leaf.setDapAccess(true);
            leaf.setDapUri(dapAccess.getStandardUrlName());
        }
        final InvAccess fileAccess = dataset.getAccess(ServiceType.FILE);
        if (fileAccess != null) {
            leaf.setFileAccess(true);
            leaf.setFileUri(fileAccess.getStandardUrlName());
        } else {
            final InvAccess serverAccess = dataset.getAccess(ServiceType.HTTPServer);
            if (serverAccess != null) {
                leaf.setFileAccess(true);
                leaf.setFileUri(serverAccess.getStandardUrlName());
            }
        }
        final boolean hasNestedDatasets = dataset.hasNestedDatasets();
        appendDataNodeToParent(parentNode, treeModel, leaf);
        fireLeafAdded(leaf, hasNestedDatasets);
    }

    MutableTreeNode getNode(OpendapLeaf leaf) {
        final MutableTreeNode node = getNode(jTree.getModel(), jTree.getModel().getRoot(), leaf);
        if (node == null) {
            throw new IllegalStateException("node of leaf '" + leaf.toString() + "' is null.");
        }
        return node;
    }

    OpendapLeaf[] getLeaves() {
        final Set<OpendapLeaf> leafs = new HashSet<OpendapLeaf>();
        getLeaves(jTree.getModel().getRoot(), jTree.getModel(), leafs);
        leafs.addAll(leafToParentNode.keySet());
        return leafs.toArray(new OpendapLeaf[leafs.size()]);
    }

    void setLeafVisible(OpendapLeaf leaf, boolean visible) {
        if (visible) {
            setLeafVisible(leaf);
        } else {
            setLeafInvisible(leaf);
        }
    }

    void addCatalogTreeListener(CatalogTreeListener listener) {
        catalogTreeListeners.add(listener);
    }

    OpendapLeaf getSelectedLeaf() {
        if (jTree.isSelectionEmpty()) {
            return null;
        }
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) jTree.getAnchorSelectionPath().getLastPathComponent();
        return (OpendapLeaf) selectedNode.getUserObject();
    }

    private void insertCatalogElements(List<InvDataset> catalogDatasets, DefaultMutableTreeNode parent, boolean expandPath) {
        appendToNode(jTree, catalogDatasets, parent, true);
        fireCatalogElementsInsertionFinished();
        if (expandPath) {
            expandPath(parent);
        }
    }

    private void appendToNode(JTree jTree, InvDataset dataset, MutableTreeNode parentNode) {
        final DefaultTreeModel treeModel = (DefaultTreeModel) jTree.getModel();
        if (dataset instanceof InvCatalogRef) {
            CatalogTreeUtils.appendCatalogNode(parentNode, treeModel, (InvCatalogRef) dataset);
        } else {
            appendLeafNode(parentNode, treeModel, dataset);
        }
    }

    private void appendDataNodeToParent(MutableTreeNode parentNode, DefaultTreeModel treeModel, OpendapLeaf leaf) {
        final DefaultMutableTreeNode leafNode = new DefaultMutableTreeNode(leaf);
        treeModel.insertNodeInto(leafNode, parentNode, parentNode.getChildCount());
    }

    private void expandPath(DefaultMutableTreeNode node) {
        jTree.expandPath(new TreePath(node.getPath()));
    }


    private MutableTreeNode getNode(TreeModel model, Object node, OpendapLeaf leaf) {
        for (int i = 0; i < model.getChildCount(node); i++) {
            final DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) (model.getChild(node, i));
            if (childNode.getUserObject() == leaf) {
                return childNode;
            } else {
                final MutableTreeNode temp = getNode(model, model.getChild(node, i), leaf);
                if (temp != null) {
                    return temp;
                }
            }
        }
        return null;
    }

    private void fireLeafAdded(OpendapLeaf leaf, boolean hasNestedDatasets) {
        for (CatalogTreeListener catalogTreeListener : catalogTreeListeners) {
            catalogTreeListener.leafAdded(leaf, hasNestedDatasets);
        }
    }

    private void fireCatalogElementsInsertionFinished() {
        for (CatalogTreeListener catalogTreeListener : catalogTreeListeners) {
            catalogTreeListener.catalogElementsInsertionFinished();
        }
    }

    private void setLeafVisible(OpendapLeaf leaf) {
        boolean leafIsRemovedFromTree = leafToParentNode.containsKey(leaf);
        if (leafIsRemovedFromTree) {
            appendDataNodeToParent(leafToParentNode.get(leaf), (DefaultTreeModel) jTree.getModel(), leaf);
            leafToParentNode.remove(leaf);
        }
    }

    private void setLeafInvisible(OpendapLeaf leaf) {
        boolean leafIsRemovedFromTree = leafToParentNode.containsKey(leaf);
        if (!leafIsRemovedFromTree) {
            final MutableTreeNode node = getNode(leaf);
            final DefaultTreeModel model = (DefaultTreeModel) jTree.getModel();
            leafToParentNode.put(leaf, (MutableTreeNode) node.getParent());
            model.removeNodeFromParent(node);
        }
    }

    private void getLeaves(Object node, TreeModel model, Set<OpendapLeaf> result) {
        for (int i = 0; i < model.getChildCount(node); i++) {
            if (model.isLeaf(model.getChild(node, i))) {
                final DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) (model.getChild(node, i));
                if (CatalogTreeUtils.isDapNode(childNode) || CatalogTreeUtils.isFileNode(childNode)) {
                    result.add((OpendapLeaf) childNode.getUserObject());
                }
            } else {
                getLeaves(model.getChild(node, i), model, result);
            }
        }
    }

    static interface LeafSelectionListener {

        void dapLeafSelected(OpendapLeaf leaf);

        void fileLeafSelected(OpendapLeaf leaf);

        void leafSelectionChanged(boolean isSelected, OpendapLeaf dapObject);
    }

    static interface CatalogTreeListener {

        void leafAdded(OpendapLeaf leaf, boolean hasNestedDatasets);

        void catalogElementsInsertionFinished();
    }

    static interface UIContext {

        void setCursor(Cursor cursor);

        void updateStatusBar(String text);
    }

}
