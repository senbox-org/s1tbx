package org.esa.beam.opendap.ui;

import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.opendap.datamodel.CatalogNode;
import org.esa.beam.opendap.datamodel.OpendapLeaf;
import org.esa.beam.opendap.utils.OpendapUtils;
import org.esa.beam.util.logging.BeamLogManager;
import thredds.catalog.InvAccess;
import thredds.catalog.InvCatalogFactory;
import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvCatalogRef;
import thredds.catalog.InvDataset;
import thredds.catalog.ServiceType;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
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

    public CatalogTree(final LeafSelectionListener leafSelectionListener, AppContext appContext) {
        this.appContext = appContext;
        jTree = new JTree();
        jTree.setRootVisible(false);
        ((DefaultTreeModel) jTree.getModel()).setRoot(createRootNode());
        addCellRenderer(jTree);
        addWillExpandListener();
        addTreeSelectionListener(jTree, leafSelectionListener);
    }

    public Component getComponent() {
        return jTree;
    }

    public void setNewRootDatasets(List<InvDataset> rootDatasets) {
        final DefaultTreeModel model = (DefaultTreeModel) jTree.getModel();
        final DefaultMutableTreeNode rootNode = createRootNode();
        model.setRoot(rootNode);
        appendToNode(jTree, rootDatasets, rootNode, true);
        fireCatalogElementsInsertionFinished();
        expandPath(rootNode);
        leafToParentNode.clear();
    }

    static void addCellRenderer(final JTree jTree) {
        final ImageIcon dapIcon = UIUtils.loadImageIcon("/org/esa/beam/opendap/images/icons/DRsProduct16.png", CatalogTree.class);
        final ImageIcon fileIcon = UIUtils.loadImageIcon("/org/esa/beam/opendap/images/icons/FRsProduct16.png", CatalogTree.class);
        final ImageIcon standardIcon = UIUtils.loadImageIcon("/org/esa/beam/opendap/images/icons/NoAccess16.png", CatalogTree.class);
        jTree.setToolTipText(null);
        jTree.setCellRenderer(new DefaultTreeCellRenderer() {

            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                if (isDapNode(value)) {
                    setLeafIcon(dapIcon);
                    setToolTip((DefaultMutableTreeNode) value, tree);
                } else if (isFileNode(value)) {
                    setLeafIcon(fileIcon);
                    setToolTip((DefaultMutableTreeNode) value, tree);
                } else {
                    setLeafIcon(standardIcon);
                    tree.setToolTipText(null);
                }
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                return this;
            }

            private void setToolTip(DefaultMutableTreeNode value, JTree tree) {
                final int fileSize = ((OpendapLeaf)value.getUserObject()).getFileSize();
                tree.setToolTipText(OpendapUtils.format(fileSize / 1024.0) + " MB");
            }
        });
    }

    void addWillExpandListener() {
        jTree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                final Object lastPathComponent = event.getPath().getLastPathComponent();
                final DefaultMutableTreeNode parent = (DefaultMutableTreeNode) lastPathComponent;
                final DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(0);
                if (isCatalogReferenceNode(child)) {
                    resolveCatalogReferenceNode(child, true);
                }
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
            }
        });
    }

    public void resolveCatalogReferenceNode(DefaultMutableTreeNode catalogReferenceNode, boolean expandPath) {
        final CatalogNode catalogNode = (CatalogNode) catalogReferenceNode.getUserObject();
        final DefaultTreeModel model = (DefaultTreeModel) jTree.getModel();
        final DefaultMutableTreeNode parent = (DefaultMutableTreeNode) catalogReferenceNode.getParent();
        model.removeNodeFromParent(catalogReferenceNode);
        try {
            final URL catalogUrl = new URL(catalogNode.getCatalogUri());
            final URLConnection urlConnection = catalogUrl.openConnection();
            final InputStream inputStream = urlConnection.getInputStream();
            insertCatalogElements(inputStream, catalogUrl.toURI(), parent, expandPath);
        } catch (Exception e) {
            String msg = MessageFormat.format("Unable to completely resolve catalog. Reason: {0}", e.getMessage());
            BeamLogManager.getSystemLogger().warning(msg);
            appContext.handleError(msg, e);
        }
    }

    void insertCatalogElements(InputStream catalogIS, URI catalogBaseUri, DefaultMutableTreeNode parent,
                               boolean expandPath) {
        final InvCatalogFactory factory = InvCatalogFactory.getDefaultFactory(true);
        final InvCatalogImpl catalog = factory.readXML(catalogIS, catalogBaseUri);
        final List<InvDataset> catalogDatasets = catalog.getDatasets();
        appendToNode(jTree, catalogDatasets, parent, true);
        fireCatalogElementsInsertionFinished();
        if (expandPath) {
            expandPath(parent);
        }
    }

    static void addTreeSelectionListener(final JTree jTree, final LeafSelectionListener leafSelectionListener) {

        jTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {


                final TreePath[] paths = e.getPaths();
                for (TreePath path : paths) {
                    final DefaultMutableTreeNode lastPathComponent = (DefaultMutableTreeNode) path.getLastPathComponent();
                    final Object userObject = lastPathComponent.getUserObject();
                    if (!(userObject instanceof OpendapLeaf)) {
                        continue;
                    }
                    final OpendapLeaf dapObject = (OpendapLeaf) userObject;
                    leafSelectionListener.leafSelectionChanged(e.isAddedPath(path), dapObject);
                }

                TreePath path = e.getPath();
                final DefaultMutableTreeNode lastPathComponent = (DefaultMutableTreeNode) path.getLastPathComponent();
                final Object userObject = lastPathComponent.getUserObject();
                if (!(userObject instanceof OpendapLeaf)) {
                    return;
                }
                OpendapLeaf opendapLeaf = (OpendapLeaf) userObject;
                if (opendapLeaf.isDapAccess()) {
                    leafSelectionListener.dapLeafSelected(opendapLeaf);
                } else if (opendapLeaf.isFileAccess()) {
                    leafSelectionListener.fileLeafSelected(opendapLeaf);
                }
            }
        });
    }


    static boolean isDapNode(Object value) {
        if (value instanceof DefaultMutableTreeNode) {
            final Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
            return (userObject instanceof OpendapLeaf) && ((OpendapLeaf) userObject).isDapAccess();
        }
        return false;
    }

    static boolean isFileNode(Object value) {
        if (value instanceof DefaultMutableTreeNode) {
            final Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
            return (userObject instanceof OpendapLeaf) && ((OpendapLeaf) userObject).isFileAccess();
        }
        return false;
    }

    static boolean isCatalogReferenceNode(Object value) {
        if (value instanceof DefaultMutableTreeNode) {
            final Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
            return (userObject instanceof CatalogNode);
        }
        return false;
    }

    void appendToNode(final JTree jTree, List<InvDataset> datasets, MutableTreeNode parentNode, boolean goDeeper) {
        for (InvDataset dataset : datasets) {
            final MutableTreeNode deeperParent;
            if (!goDeeper || !isHyraxId(dataset.getID())) {
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

    private static boolean isHyraxId(String id) {
        return id != null && id.startsWith("/") && id.endsWith("/");
    }

    private void appendToNode(JTree jTree, InvDataset dataset, MutableTreeNode parentNode) {
        final DefaultTreeModel treeModel = (DefaultTreeModel) jTree.getModel();
        if (dataset instanceof InvCatalogRef) {
            appendCatalogNode(parentNode, treeModel, (InvCatalogRef) dataset);
        } else {
            appendLeafNode(parentNode, treeModel, dataset);
        }
    }

    static void appendCatalogNode(MutableTreeNode parentNode, DefaultTreeModel treeModel, InvCatalogRef catalogRef) {
        final DefaultMutableTreeNode catalogNode = new DefaultMutableTreeNode(catalogRef.getName());
        final String catalogPath = catalogRef.getURI().toASCIIString();
        final CatalogNode opendapNode = new CatalogNode(catalogPath, catalogRef);
        opendapNode.setCatalogUri(catalogPath);
        catalogNode.add(new DefaultMutableTreeNode(opendapNode));
        treeModel.insertNodeInto(catalogNode, parentNode, parentNode.getChildCount());
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

    private void appendDataNodeToParent(MutableTreeNode parentNode, DefaultTreeModel treeModel, OpendapLeaf leaf) {
        final DefaultMutableTreeNode leafNode = new DefaultMutableTreeNode(leaf);
        treeModel.insertNodeInto(leafNode, parentNode, parentNode.getChildCount());
    }

    private void expandPath(DefaultMutableTreeNode node) {
        jTree.expandPath(new TreePath(node.getPath()));
    }

    static DefaultMutableTreeNode createRootNode() {
        return new DefaultMutableTreeNode("root", true);
    }


    MutableTreeNode getNode(OpendapLeaf leaf) {
        final MutableTreeNode node = getNode(jTree.getModel(), jTree.getModel().getRoot(), leaf);
        if (node == null) {
            throw new IllegalStateException("node of leaf '" + leaf.toString() + "' is null.");
        }
        return node;
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

    OpendapLeaf[] getLeaves() {
        final Set<OpendapLeaf> leafs = new HashSet<OpendapLeaf>();
        getLeaves(jTree.getModel().getRoot(), jTree.getModel(), leafs);
        leafs.addAll(leafToParentNode.keySet());
        return leafs.toArray(new OpendapLeaf[leafs.size()]);
    }

    private void getLeaves(Object node, TreeModel model, Set<OpendapLeaf> result) {
        for (int i = 0; i < model.getChildCount(node); i++) {
            if (model.isLeaf(model.getChild(node, i))) {
                final DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) (model.getChild(node, i));
                if (isDapNode(childNode) || isFileNode(childNode)) {
                    result.add((OpendapLeaf) childNode.getUserObject());
                }
            } else {
                getLeaves(model.getChild(node, i), model, result);
            }
        }
    }

    void setLeafVisible(OpendapLeaf leaf, boolean visible) {
        if (visible) {
            setLeafVisible(leaf);
        } else {
            setLeafInvisible(leaf);
        }
    }

    private void setLeafVisible(OpendapLeaf leaf) {
        final boolean leafIsRemovedFromTree = leafToParentNode.containsKey(leaf);
        if (leafIsRemovedFromTree) {
            appendDataNodeToParent(leafToParentNode.get(leaf), (DefaultTreeModel) jTree.getModel(), leaf);
            leafToParentNode.remove(leaf);
        }
    }

    private void setLeafInvisible(OpendapLeaf leaf) {
        final boolean leafIsRemovedFromTree = leafToParentNode.containsKey(leaf);
        if (!leafIsRemovedFromTree) {
            final MutableTreeNode node = getNode(leaf);
            final DefaultTreeModel model = (DefaultTreeModel) jTree.getModel();
            leafToParentNode.put(leaf, (MutableTreeNode) node.getParent());
            model.removeNodeFromParent(node);
        }
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

    void addCatalogTreeListener(CatalogTreeListener listener) {
        catalogTreeListeners.add(listener);
    }

    OpendapLeaf getSelectedLeaf() {
        if(jTree.isSelectionEmpty()) {
            return null;
        }
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) jTree.getAnchorSelectionPath().getLastPathComponent();
        return (OpendapLeaf)selectedNode.getUserObject();
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
}
