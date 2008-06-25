/*
 * $Id: ProductTree.java,v 1.5 2006/12/11 10:14:25 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.ui.product;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.DialogProgressMonitor;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.ui.ExceptionHandler;
import org.esa.beam.framework.ui.PopupMenuFactory;
import org.esa.beam.framework.ui.PopupMenuHandler;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.command.CommandManager;
import org.esa.beam.framework.ui.command.CommandUIFactory;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.StringUtils;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * A tree-view component for multiple <code>Product</code>s. Clients can register one or more
 * <code>ProductTreeListener</code>s on this component.
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @version $Revision$ $Date$
 * @see org.esa.beam.framework.ui.product.ProductTreeListener
 * @see org.esa.beam.framework.datamodel.Product
 */
public class ProductTree extends JTree implements PopupMenuFactory {

    private final ProductNodeListener productNodeListener;

    /**
     * The listener list.
     */
    private List<ProductTreeListener> productTreeListenerList;

    /**
     * The manager used by the interface PopupManuFactory
     */
    private CommandManager commandManager;

    private ExceptionHandler exceptionHandler;
    private CommandUIFactory commandUIFactory;

    /**
     * Constructs a new single selection <code>ProductTree</code>.
     */
    public ProductTree() {
        this(false);
    }

    /**
     * Constructs a new <code>ProductTree</code> with the given selection mode.
     *
     * @param multipleSelect whether or not the tree is multiple selection capable
     */
    public ProductTree(final boolean multipleSelect) {
        productNodeListener = new ProductTreePTL();
        getSelectionModel().setSelectionMode(multipleSelect
                                             ? TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION
                                             : TreeSelectionModel.SINGLE_TREE_SELECTION);
        addTreeSelectionListener(new PTSelectionListener());
        addMouseListener(new PTMouseListener());
        setCellRenderer(new PTCellRenderer());
        setRootVisible(false);
        setShowsRootHandles(true);
        setToggleClickCount(2);
        setAutoscrolls(true);
        putClientProperty("JTree.lineStyle", "Angled");
        setProducts(new Product[0]);
        ToolTipManager.sharedInstance().registerComponent(this);

        final PopupMenuHandler popupMenuHandler = new PopupMenuHandler(this);
        addMouseListener(popupMenuHandler);
        addKeyListener(popupMenuHandler);
    }

    public JPopupMenu createPopupMenu(final Component component) {
        return null;
    }

    public JPopupMenu createPopupMenu(MouseEvent event) {
        TreePath selPath = getPathForLocation(event.getX(), event.getY());
        if (selPath != null) {
            setSelectionPath(selPath);
            DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode) getLastSelectedPathComponent();
            if (node != null) {
                Object context = node.getUserObject();
                if (context != null) {
                    return createPopup(context);
                }
            }
        }
        return null;
    }

    /**
     * Sets the products for this product tree component.
     *
     * @param products a <code>Product[]</code> with the products to be displayed, must not be null.
     */
    public void setProducts(Product[] products) {
        Guardian.assertNotNull("products", products);
        setModel(new DefaultTreeModel(createProductListTreeNode(products)));
    }

    /**
     * Adds a new product to this product tree component. The method fires a 'productAdded' event to all listeners.
     *
     * @param product the product to be added.
     *
     * @see org.esa.beam.framework.ui.product.ProductTreeListener
     */
    public void addProduct(Product product) {
        product.addProductNodeListener(productNodeListener);
        DefaultMutableTreeNode rootNode = getRootTreeNode();
        DefaultMutableTreeNode productTreeNode = createProductTreeNode(product);
        rootNode.add(productTreeNode);
        getTreeModel().nodesWereInserted(rootNode, new int[]{rootNode.getIndex(productTreeNode)});
//        final TreePath lastLeafPath = new TreePath(productTreeNode.getLastLeaf().getPath());
//        makeVisible(lastLeafPath);
        final TreePath productTreeNodePath = new TreePath(productTreeNode.getPath());
        expandPath(productTreeNodePath);
//        makeVisible(productTreeNodePath);
        scrollPathToVisible(productTreeNodePath);
        invalidate();
        doLayout();
        fireProductAdded(product);
    }

    /**
     * Selects the specified object in this tree's model. If the given object has no representation in the tree, the
     * current selection will not be changed.
     *
     * @param toSelect the object whose representation in the tree will be selected.
     */
    public void select(Object toSelect) {
        final TreePath path = findTreePathFor(toSelect);
        if (path != null) {
            setSelectionPath(path);
        }
    }

    /**
     * Removes a  product from this product tree component. The method fires a 'productRemoved' event to all listeners.
     *
     * @param product the product to be removed.
     *
     * @see org.esa.beam.framework.ui.product.ProductTreeListener
     */
    public void removeProduct(Product product) {
        DefaultMutableTreeNode root = getRootTreeNode();
        final DefaultMutableTreeNode productTreeNode = getTreeNodeFor(product, root);
        if (productTreeNode != null) {
            final int index = root.getIndex(productTreeNode);
            productTreeNode.removeFromParent();
            getTreeModel().nodesWereRemoved(root, new int[]{index}, new Object[]{productTreeNode});
            product.removeProductNodeListener(productNodeListener);
            fireProductRemoved(product);
        }
    }

    /**
     * Adds a new product tree listener to this product tree component.
     *
     * @param listener the listener to be added.
     */
    public void addProductTreeListener(ProductTreeListener listener) {
        if (listener != null) {
            if (productTreeListenerList == null) {
                productTreeListenerList = new ArrayList<ProductTreeListener>();
            }
            productTreeListenerList.add(listener);
        }
    }

    /**
     * Removes a product tree listener from this product tree component.
     *
     * @param listener the listener to be removed.
     */
    public boolean removeProductTreeListener(ProductTreeListener listener) {
        if (listener != null && productTreeListenerList != null) {
            return productTreeListenerList.remove(listener);
        }
        return false;
    }

    public void setCommandManager(CommandManager commandManager) {
        this.commandManager = commandManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public void setCommandUIFactory(CommandUIFactory commandUIFactory) {
        this.commandUIFactory = commandUIFactory;
    }

    public void setExceptionHandler(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    private DefaultMutableTreeNode createProductListTreeNode(Product[] products) {
        DefaultMutableTreeNode productListTreeNode = new DefaultMutableTreeNode(products);
        if (products != null) {
            for (final Product product : products) {
                if (product != null) {
                    DefaultMutableTreeNode childTreeNode = createProductTreeNode(product);
                    productListTreeNode.add(childTreeNode);
                }
            }
        }
        return productListTreeNode;
    }

    private DefaultMutableTreeNode createProductTreeNode(Product product) {
        DefaultMutableTreeNode productTreeNode = new DefaultMutableTreeNode(product);

        DefaultMutableTreeNode metadataGroupTreeNode = createMetadataNodes(product);
        productTreeNode.add(metadataGroupTreeNode);

        DefaultMutableTreeNode flagCodingsGroupTreeNode = createFlagCodingNodes(product);
        if (flagCodingsGroupTreeNode != null) {
            productTreeNode.add(flagCodingsGroupTreeNode);
        }

        DefaultMutableTreeNode indexCodingsGroupTreeNode = createIndexCodingNodes(product);
        if (indexCodingsGroupTreeNode != null) {
            productTreeNode.add(indexCodingsGroupTreeNode);
        }

        DefaultMutableTreeNode tiePointGridGroupTreeNode = createTiePointGridNodes(product);
        if(tiePointGridGroupTreeNode != null) {
            productTreeNode.add(tiePointGridGroupTreeNode);
        }

        DefaultMutableTreeNode bandsGroupTreeNode = createBandNodes(product);
        productTreeNode.add(bandsGroupTreeNode);

        return productTreeNode;
    }

    private DefaultMutableTreeNode createMetadataNodes(Product product) {
        DefaultMutableTreeNode rootTreeNode = new DefaultMutableTreeNode("Metadata");
        MetadataElement rootElement = product.getMetadataRoot();
        return createMetadataNodes(rootElement, rootTreeNode);
    }

    private DefaultMutableTreeNode createMetadataNodes(MetadataElement parentElement) {
        DefaultMutableTreeNode parentTreeNode = new DefaultMutableTreeNode(parentElement);
        return createMetadataNodes(parentElement, parentTreeNode);
    }

    private DefaultMutableTreeNode createMetadataNodes(MetadataElement parentElement,
                                                       DefaultMutableTreeNode parentTreeNode) {
        for (int i = 0; i < parentElement.getNumElements(); i++) {
            MetadataElement childElement = parentElement.getElementAt(i);
            DefaultMutableTreeNode childTreeNode;
            if (childElement.getNumElements() > 0) {
                childTreeNode = createMetadataNodes(childElement);
            } else {
                childTreeNode = new DefaultMutableTreeNode(childElement);
            }
            parentTreeNode.add(childTreeNode);
        }
        return parentTreeNode;
    }

    private static DefaultMutableTreeNode createBandNodes(Product product) {
        DefaultMutableTreeNode bandsGroupTreeNode = new DefaultMutableTreeNode("Bands");
        for (int i = 0; i < product.getNumBands(); i++) {
            DefaultMutableTreeNode childTreeNode = new DefaultMutableTreeNode(product.getBandAt(i));

            bandsGroupTreeNode.add(childTreeNode);
        }
        return bandsGroupTreeNode;
    }

    private static DefaultMutableTreeNode createTiePointGridNodes(Product product) {
        if (product.getNumTiePointGrids() == 0) {
            return null;
        }
        DefaultMutableTreeNode tiePointGridGroupTreeNode = new DefaultMutableTreeNode("Tie Point Grids");
        for (int i = 0; i < product.getNumTiePointGrids(); i++) {
            DefaultMutableTreeNode childTreeNode = new DefaultMutableTreeNode(product.getTiePointGridAt(i));
            tiePointGridGroupTreeNode.add(childTreeNode);
        }
        return tiePointGridGroupTreeNode;
    }

    private static DefaultMutableTreeNode createFlagCodingNodes(Product product) {
        ProductNodeGroup<FlagCoding> productNodeGroup = product.getFlagCodingGroup();
        if (productNodeGroup.getNodeCount() == 0) {
            return null;
        }
        DefaultMutableTreeNode flagCodingsGroupTreeNode = new DefaultMutableTreeNode("Flag Codings");
        for (int i = 0; i < productNodeGroup.getNodeCount(); i++) {
            DefaultMutableTreeNode childTreeNode = new DefaultMutableTreeNode(productNodeGroup.get(i));
            flagCodingsGroupTreeNode.add(childTreeNode);
        }
        return flagCodingsGroupTreeNode;
    }

    private static DefaultMutableTreeNode createIndexCodingNodes(Product product) {
        ProductNodeGroup<IndexCoding> productNodeGroup = product.getIndexCodingGroup();
        if (productNodeGroup.getNodeCount() == 0) {
            return null;
        }
        DefaultMutableTreeNode indexCodingsGroupTreeNode = new DefaultMutableTreeNode("Index Codings");
        for (int i = 0; i < productNodeGroup.getNodeCount(); i++) {
            DefaultMutableTreeNode childTreeNode = new DefaultMutableTreeNode(productNodeGroup.get(i));
            indexCodingsGroupTreeNode.add(childTreeNode);
        }
        return indexCodingsGroupTreeNode;
    }

    private class PTMouseListener extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent event) {
            int selRow = getRowForLocation(event.getX(), event.getY());
            if (selRow >= 0) {
                int clickCount = event.getClickCount();
                TreePath selPath = getPathForLocation(event.getX(), event.getY());
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) selPath.getLastPathComponent();
                fireNodeSelected(node.getUserObject(), clickCount);
            }
        }
    }

    private class PTSelectionListener implements TreeSelectionListener {

        public void valueChanged(TreeSelectionEvent event) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) getLastSelectedPathComponent();
            if (node != null) {
                fireNodeSelected(node.getUserObject(), 1);
            }
//            DefaultMutableTreeNode node = (DefaultMutableTreeNode) getLastSelectedPathComponent();
//            if (node != null) {
//                Object userObject = node.getUserObject();
//                if (node.isLeaf()) {
//                    Debug.trace("Leaf: " + userObject);
//                } else {
//                    Debug.trace("Node: " + userObject);
//                }
//            }
        }
    }

    private static class PTCellRenderer extends DefaultTreeCellRenderer {

        ImageIcon productIcon;
        ImageIcon groupOpenIcon;
        ImageIcon groupClosedIcon;
        ImageIcon metadataIcon;
        ImageIcon bandAsSwathIcon;
        ImageIcon bandAsSwathIconUnloaded;
        ImageIcon bandAsGridIcon;
        ImageIcon bandAsGridIconUnloaded;
        ImageIcon bandFlagsIcon;
        ImageIcon bandFlagsIconUnloaded;
        ImageIcon bandIndexesIcon;
        ImageIcon bandIndexesIconUnloaded;
        ImageIcon bandVirtualIcon;
        ImageIcon bandVirtualIconUnloaded;

        public PTCellRenderer() {
            productIcon = UIUtils.loadImageIcon("icons/RsProduct16.gif");
            groupOpenIcon = UIUtils.loadImageIcon("icons/RsGroupOpen16.gif");
            groupClosedIcon = UIUtils.loadImageIcon("icons/RsGroupClosed16.gif");
            metadataIcon = UIUtils.loadImageIcon("icons/RsMetaData16.gif");
            bandAsSwathIcon = UIUtils.loadImageIcon("icons/RsBandAsSwath16.gif");
            bandAsSwathIconUnloaded = UIUtils.loadImageIcon("icons/RsBandAsSwath16Disabled.gif");
            bandAsGridIcon = UIUtils.loadImageIcon("icons/RsBandAsGrid16.gif");
            bandAsGridIconUnloaded = UIUtils.loadImageIcon("icons/RsBandAsGrid16Disabled.gif");
            bandIndexesIcon = UIUtils.loadImageIcon("icons/RsBandIndexes16.gif");
            bandIndexesIconUnloaded = UIUtils.loadImageIcon("icons/RsBandIndexes16Disabled.gif");
            bandFlagsIcon = UIUtils.loadImageIcon("icons/RsBandFlags16.gif");
            bandFlagsIconUnloaded = UIUtils.loadImageIcon("icons/RsBandFlags16Disabled.gif");
            bandVirtualIcon = UIUtils.loadImageIcon("icons/RsBandVirtual16.gif");
            bandVirtualIconUnloaded = UIUtils.loadImageIcon("icons/RsBandVirtual16Disabled.gif");
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree,
                                                      Object value,
                                                      boolean sel,
                                                      boolean expanded,
                                                      boolean leaf,
                                                      int row,
                                                      boolean hasFocus) {

            super.getTreeCellRendererComponent(tree,
                                               value,
                                               sel,
                                               expanded,
                                               leaf,
                                               row,
                                               hasFocus);

            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) value;
            value = treeNode.getUserObject();

            if (value instanceof Product[]) {
                this.setText("Open products");
                this.setToolTipText("Contains the list of open data products");
                this.setIcon(null);
            } else if (value instanceof String) {
                this.setIcon(groupClosedIcon);
            } else if (value instanceof ProductNode) {
                final ProductNode productNode = (ProductNode) value;
                String text = productNode.getName();
                final StringBuffer toolTipBuffer = new StringBuffer(32);

                final String prefix = productNode.getProductRefString();
                if (prefix != null) {
                    toolTipBuffer.append(prefix);
                    toolTipBuffer.append(' ');
                }
                if (!StringUtils.isNullOrEmpty(productNode.getDescription())) {
                    toolTipBuffer.append(productNode.getDescription());
                } else {
                    toolTipBuffer.append(productNode.getName());
                }

                if (productNode instanceof Product) {
                    text = productNode.getDisplayName();
                    Product product = (Product) productNode;
                    this.setIcon(productIcon);
                    toolTipBuffer.append(", ");
                    toolTipBuffer.append(product.getSceneRasterWidth());
                    toolTipBuffer.append(" x ");
                    toolTipBuffer.append(product.getSceneRasterHeight());
                    toolTipBuffer.append(" pixels");
                } else if (productNode instanceof MetadataElement) {
                    this.setIcon(metadataIcon);
                } else if (productNode instanceof Band) {
                    Band band = (Band) productNode;

                    if (band.getSpectralWavelength() > 0.0) {
                        toolTipBuffer.append(", wavelength = ");
                        toolTipBuffer.append(band.getSpectralWavelength());
                        toolTipBuffer.append(" nm, bandwidth = ");
                        toolTipBuffer.append(band.getSpectralBandwidth());
                        toolTipBuffer.append(" nm");
                    }

                    if (band instanceof VirtualBand) {
                        toolTipBuffer.append(", expr = ");
                        toolTipBuffer.append(((VirtualBand) band).getExpression());
                        if (band.hasRasterData()) {
                            this.setIcon(bandVirtualIcon);
                        } else {
                            this.setIcon(bandVirtualIconUnloaded);
                        }
                    } else if (band.getFlagCoding() != null) {
                        if (band.hasRasterData()) {
                            this.setIcon(bandFlagsIcon);
                        } else {
                            this.setIcon(bandFlagsIconUnloaded);
                        }
                    } else if (band.getIndexCoding() != null) {
                        if (band.hasRasterData()) {
                            this.setIcon(bandIndexesIcon);
                        } else {
                            this.setIcon(bandIndexesIconUnloaded);
                        }
                    } else {
                        if (band.hasRasterData()) {
                            this.setIcon(bandAsSwathIcon);
                        } else {
                            this.setIcon(bandAsSwathIconUnloaded);
                        }
                    }
                    toolTipBuffer.append(", raster size = ");
                    toolTipBuffer.append(band.getRasterWidth());
                    toolTipBuffer.append(" x ");
                    toolTipBuffer.append(band.getRasterHeight());
                } else if (productNode instanceof TiePointGrid) {
                    TiePointGrid grid = (TiePointGrid) productNode;
                    this.setIcon(bandAsSwathIcon);
                    toolTipBuffer.append(", raster size = ");
                    toolTipBuffer.append(grid.getRasterWidth());
                    toolTipBuffer.append(" x ");
                    toolTipBuffer.append(grid.getRasterHeight());
                }
                this.setText(text);
                this.setToolTipText(toolTipBuffer.toString());
            }

            return this;
        }
    }

    public JPopupMenu createPopup(final Object context) {

        JPopupMenu popup = new JPopupMenu();

        JMenuItem menuItem;

        if (context instanceof Product[]) {
// TODO
//            menuItem = new JMenuItem("New Product...");
//            popup.add(menuItem);
//
//            popup.addSeparator();
//
//            menuItem = new JMenuItem("Close All Products");
//            popup.add(menuItem);
//
//            menuItem = new JMenuItem("Safe All Products");
//            popup.add(menuItem);
        } else if (context instanceof Product) {
// TODO
//            menuItem = new JMenuItem("New Group...");
//            popup.add(menuItem);
//
//            menuItem = new JMenuItem("New Attribute...");
//            popup.add(menuItem);
            if (commandUIFactory != null) {
                commandUIFactory.addContextDependentMenuItems("product", popup);
            }
        } else if (context instanceof MetadataElement) {
// TODO
//            menuItem = new JMenuItem("New Group...");
//            popup.add(menuItem);
//
//            menuItem = new JMenuItem("New Attribute...");
//            popup.add(menuItem);

        } else if (context instanceof RasterDataNode) {

            if (context instanceof VirtualBand) {
                int componentCountBefore = popup.getComponentCount();
                if (commandUIFactory != null) {
                    commandUIFactory.addContextDependentMenuItems("virtualBand", popup);
                }
                addSeparatorIfAnyComponentsAdded(popup, componentCountBefore);
            }
            if (context instanceof TiePointGrid) {
                if (commandUIFactory != null) {
                    commandUIFactory.addContextDependentMenuItems("tiePointGrid", popup);
                }
            } else if (context instanceof Band) {
                final Band band = (Band) context;
                menuItem = createMenuItem("Load Data"); /*I18N*/
                menuItem.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        loadRasterData(band);
                    }
                });
                menuItem.setEnabled(!band.hasRasterData());
                popup.add(menuItem);

                menuItem = createMenuItem("Unload Data"); /*I18N*/
                menuItem.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        unloadRasterData(band);
                    }
                });
                menuItem.setEnabled(band.hasRasterData());
                popup.add(menuItem);

                popup.addSeparator();

                if (commandUIFactory != null) {
                    commandUIFactory.addContextDependentMenuItems("band", popup);
                }
            }
// TODO:
//            menuItem = new JMenuItem("New Attribute...");
//            popup.add(menuItem);
        } else if (context instanceof MetadataAttribute) {
// TODO:
//            menuItem = new JMenuItem("Open Attribute Editor...");
//            popup.add(menuItem);
        }

        return popup;
    }

    private void loadRasterData(final Band band) {
        if (!band.hasRasterData()) {
            final SwingWorker worker = new SwingWorker() {
                ProgressMonitor pm = new DialogProgressMonitor(UIUtils.getRootJFrame(ProductTree.this),
                                                               "Load Band Data", Dialog.ModalityType.APPLICATION_MODAL);

                @Override
                protected Object doInBackground() throws Exception {
                    try {
                        band.loadRasterData(pm);
                    } catch (IOException e1) {
                        if (exceptionHandler != null) {
                            exceptionHandler.handleException(
                                    new Exception("IOException occurred!\nCannot load raster data!"));
                        }
                    }
                    return null;
                }

                @Override
                public void done() {
                    if (pm.isCanceled()) {
                        band.unloadRasterData();
                    }
                }
            };
            worker.execute();
        }
    }

    private void unloadRasterData(final Band band) {
        if (band.isSynthetic()) {
            JOptionPane.showMessageDialog(this, "This is a synthetic band.\nIts data cannot be unloaded.");
            return;
        }
        if (band.hasRasterData()) {
            band.unloadRasterData();
        }
    }

    private static JMenuItem createMenuItem(final String text) {
        JMenuItem menuItem;
        menuItem = new JMenuItem(text);
        return menuItem;
    }


    private static void addSeparatorIfAnyComponentsAdded(JPopupMenu popup, int componentCountBevore) {
        if (popup.getComponentCount() > componentCountBevore) {
            popup.addSeparator();
        }
    }

    private void fireProductAdded(Product product) {
        if (productTreeListenerList != null) {
            for (ProductTreeListener a_productTreeListenerList : productTreeListenerList) {
                ProductTreeListener l = (ProductTreeListener) a_productTreeListenerList;
                l.productAdded(product);
            }
        }
    }

    private void fireProductRemoved(Product product) {
        if (productTreeListenerList != null) {
            for (ProductTreeListener l : productTreeListenerList) {
                l.productRemoved(product);
            }
        }
    }

    private void fireNodeSelected(Object node, int clickCount) {
        //Debug.trace("Selected Node: " + node);
        if (productTreeListenerList != null) {
            for (ProductTreeListener l : productTreeListenerList) {
                if (node instanceof Product[]) {
                    /* ? */
                } else if (node instanceof Product) {
                    l.productSelected((Product) node, clickCount);
                } else if (node instanceof MetadataElement) {
                    l.metadataElementSelected((MetadataElement) node, clickCount);
                } else if (node instanceof TiePointGrid) {
                    l.tiePointGridSelected((TiePointGrid) node, clickCount);
                } else if (node instanceof Band) {
                    l.bandSelected((Band) node, clickCount);
                } else if (node instanceof MetadataAttribute) {
                    /* ? */
                }
            }
        }
    }

    private static DefaultMutableTreeNode getTreeNodeFor(final Object o, final DefaultMutableTreeNode node) {
        final Enumeration e = node.children();
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode child = (DefaultMutableTreeNode) e.nextElement();
            if (child.getUserObject() == o) {
                return child;
            }
        }
        return null;
    }

    private DefaultMutableTreeNode getRootTreeNode() {
        return (DefaultMutableTreeNode) getTreeModel().getRoot();
    }

    private DefaultTreeModel getTreeModel() {
        return (DefaultTreeModel) getModel();
    }

    private TreePath findTreePathFor(final Object o) {
        final DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) getModel().getRoot();
        final Enumeration enumeration = rootNode.depthFirstEnumeration();
        while (enumeration.hasMoreElements()) {
            final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) enumeration.nextElement();
            if (treeNode.getUserObject() == o) {
                return new TreePath(treeNode.getPath());
            }
        }
        return null;
    }

    private class ProductTreePTL implements ProductNodeListener {

        public void nodeChanged(final ProductNodeEvent event) {
            final ProductNode sourceNode = event.getSourceNode();
            final TreePath path = findTreePathFor(sourceNode);
            if (path != null) {
                getModel().valueForPathChanged(path, sourceNode);
            }
        }

        public void nodeDataChanged(final ProductNodeEvent event) {
            repaint();
        }

        public void nodeAdded(final ProductNodeEvent event) {
            final ProductNode sourceNode = event.getSourceNode();
            if (sourceNode instanceof AbstractBand) {
                final DefaultMutableTreeNode rootTNode = getRootTreeNode();

                final Product product = sourceNode.getProduct();
                final DefaultMutableTreeNode productTNode = getTreeNodeFor(product, rootTNode);
                final DefaultMutableTreeNode bandGroupTNode = getTreeNodeFor("Bands", productTNode);
                bandGroupTNode.add(new DefaultMutableTreeNode(sourceNode));
                getTreeModel().nodeStructureChanged(bandGroupTNode);
            }
        }

        public void nodeRemoved(final ProductNodeEvent event) {
            final ProductNode sourceNode = event.getSourceNode();
            if (sourceNode instanceof AbstractBand) {
                final DefaultMutableTreeNode rootTNode = getRootTreeNode();

                final Product product = sourceNode.getProduct();
                final DefaultMutableTreeNode productTNode = getTreeNodeFor(product, rootTNode);
                final DefaultMutableTreeNode bandGroupTNode = getTreeNodeFor("Bands", productTNode);
                final DefaultMutableTreeNode sourceTNode = getTreeNodeFor(sourceNode, bandGroupTNode);
                if (sourceTNode != null) {
                    final TreePath path = findTreePathFor(sourceNode);
                    final boolean nodeIsSelected = getSelectionModel().isPathSelected(path);
                    final DefaultMutableTreeNode node;
                    if (nodeIsSelected) {
                        final int selectionIndex = bandGroupTNode.getIndex(sourceTNode);
                        final int childCount = bandGroupTNode.getChildCount();
                        if (childCount - 1 == selectionIndex) {
                            if (childCount > 1) {
                                final TreeNode toSelectTNode = bandGroupTNode.getChildAt(selectionIndex - 1);
                                node = (DefaultMutableTreeNode) toSelectTNode;
                            } else {
                                node = productTNode;
                            }
                        } else {
                            final TreeNode toSelectTNode = bandGroupTNode.getChildAt(selectionIndex + 1);
                            node = (DefaultMutableTreeNode) toSelectTNode;
                        }
                    } else {
                        node = null;
                    }

                    bandGroupTNode.remove(sourceTNode);
                    getTreeModel().nodeStructureChanged(bandGroupTNode);
                    if (node != null) {
                        select(node.getUserObject());
                    }
                }
            }
        }
    }
}
