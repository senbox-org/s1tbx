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

import com.bc.ceres.swing.TreeCellExtender;
import org.esa.beam.framework.datamodel.AbstractBand;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.ui.ExceptionHandler;
import org.esa.beam.framework.ui.PopupMenuFactory;
import org.esa.beam.framework.ui.PopupMenuHandler;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.command.CommandManager;
import org.esa.beam.framework.ui.command.CommandUIFactory;
import org.esa.beam.util.Guardian;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
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
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private static final String METADATA = "Metadata";
    private static final String BANDS = "Bands";
    private static final String VECTOR_DATA = "Geometries";
    private static final String TIE_POINT_GRIDS = "Tie-point grids";
    private static final String FLAG_CODINGS = "Flag codings";
    private static final String INDEX_CODINGS = "Index codings";
// Uncomment for debugging masks:
//    private static final String MASKS = "Masks";

    private final ProductNodeListener productNodeListener;

    /**
     * The listener list.
     */
    private List<ProductTreeListener> productTreeListenerList;

    /**
     * The manager used by the interface PopupManuFactory
     */
    private CommandManager commandManager;

    private CommandUIFactory commandUIFactory;
    private Set<ProductNode> activeProductNodes;
    private Map<ProductNode, Integer> openedProductNodes;

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
        productNodeListener = new ProductTreePNL();
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
        activeProductNodes = new HashSet<ProductNode>();
        openedProductNodes = new HashMap<ProductNode, Integer>();

        TreeCellExtender.equip(this);
    }

    /**
     * Selects the specified object in this tree's model. If the given object has no representation in the tree, the
     * current selection will not be changed.
     *
     * @param toSelect the object whose representation in the tree will be selected.
     */
    public void select(Object toSelect) {
        final TreePath path = getTreePath(toSelect);
        if (path != null) {
            setSelectionPath(path);
        }
    }

    public void expand(Object toExpand) {
        final TreePath path = getTreePath(toExpand);
        if (path != null) {
            makeVisible(path);
        }
    }

    /**
     * Notifies this product tree, that a product scene view has opened.
     *
     * @param view The view.
     * @deprecated since BEAM 4.7, use {@link #registerOpenedProductNodes(org.esa.beam.framework.datamodel.ProductNode...)} instead
     */
    @Deprecated
    public void sceneViewOpened(ProductSceneView view) {
        registerOpenedProductNodes(view.getRasters());
    }

    /**
     * Notifies this product tree, that a product scene view has closed.
     *
     * @param view The view.
     * @deprecated since BEAM 4.7, use {@link #deregisterOpenedProductNodes(org.esa.beam.framework.datamodel.ProductNode...)} instead
     */
    @Deprecated
    public void sceneViewClosed(ProductSceneView view) {
        deregisterOpenedProductNodes(view.getRasters());
    }

    /**
     * Registers "opened" product nodes, e.g. visible nodes, nodes currently edited, etc.
     * Opened product nodes may be diffently displayed.
     *
     * @param nodes The products nodes which are in process.
     * @see #registerActiveProductNodes(org.esa.beam.framework.datamodel.ProductNode...)
     */
    public void registerOpenedProductNodes(ProductNode... nodes) {
        for (ProductNode node : nodes) {
            Integer count = 1;
            if (openedProductNodes.containsKey(node)) {
                count += openedProductNodes.get(node);
            }
            openedProductNodes.put(node, count);
        }
        updateUI();
    }

    /**
     * Deregisters "opened" product nodes, e.g. visible nodes, nodes currently edited, etc.
     * Opened product nodes may be diffently displayed.
     *
     * @param nodes The products nodes which are in process.
     * @see #deregisterActiveProductNodes(org.esa.beam.framework.datamodel.ProductNode...) 
     */
    public void deregisterOpenedProductNodes(ProductNode... nodes) {
        boolean changed = false;
        for (ProductNode node : nodes) {
            int count = -1;
            if (openedProductNodes.containsKey(node)) {
                count += openedProductNodes.get(node);
            }
            if (count == 0) {
                openedProductNodes.remove(node);
                changed = true;
            } else if (count > 0) {
                openedProductNodes.put(node, count);
                changed = true;
            }
        }
        if (changed) {
            updateUI();
        }
    }

    public void registerActiveProductNodes(ProductNode... nodes) {
        if (activeProductNodes.addAll(Arrays.asList(nodes))) {
            updateUI();
        }
    }

    public void deregisterActiveProductNodes(ProductNode... nodes) {
        if (activeProductNodes.removeAll(Arrays.asList(nodes))) {
            updateUI();
        }
    }

    @Override
    public JPopupMenu createPopupMenu(final Component component) {
        return null;
    }

    @Override
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
     * @see org.esa.beam.framework.ui.product.ProductTreeListener
     */
    public void addProduct(Product product) {
        product.addProductNodeListener(productNodeListener);
        DefaultMutableTreeNode rootNode = getRootTNode();
        DefaultMutableTreeNode productTreeNode = createProductTreeNode(product);
        rootNode.add(productTreeNode);
        getTreeModel().nodesWereInserted(rootNode, new int[]{rootNode.getIndex(productTreeNode)});
        final TreePath productTreeNodePath = new TreePath(productTreeNode.getPath());
        expandPath(productTreeNodePath);
        scrollPathToVisible(productTreeNodePath);
        invalidate();
        doLayout();
        fireProductAdded(product);
    }

    /**
     * Removes a  product from this product tree component. The method fires a 'productRemoved' event to all listeners.
     *
     * @param product the product to be removed.
     * @see org.esa.beam.framework.ui.product.ProductTreeListener
     */
    public void removeProduct(Product product) {
        DefaultMutableTreeNode root = getRootTNode();
        final DefaultMutableTreeNode productTreeNode = getTNode(root, product);
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
    public void removeProductTreeListener(ProductTreeListener listener) {
        if (listener != null && productTreeListenerList != null) {
            productTreeListenerList.remove(listener);
        }
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

    /**
     * This method does not have any effect.
     * @param exceptionHandler is ignored
     *
     *@deprecated since BEAM 4.7, no replacement
     */
    @Deprecated
    public void setExceptionHandler(ExceptionHandler exceptionHandler) {
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
        if (tiePointGridGroupTreeNode != null) {
            productTreeNode.add(tiePointGridGroupTreeNode);
        }

// Uncomment for debugging masks:
//        productTreeNode.add(createMaskNodes(product));

        productTreeNode.add(createVectorDataNodes(product));
        productTreeNode.add(createBandNodes(product));

        return productTreeNode;
    }

    private DefaultMutableTreeNode createMetadataNodes(Product product) {
        DefaultMutableTreeNode rootTreeNode = new DefaultMutableTreeNode(METADATA);
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

    // todo - remove code duplication (nf 10.2009)

    private static DefaultMutableTreeNode createBandNodes(Product product) {
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(BANDS);
        for (int i = 0; i < product.getNumBands(); i++) {
            DefaultMutableTreeNode childTreeNode = new DefaultMutableTreeNode(product.getBandAt(i));
            treeNode.add(childTreeNode);
        }
        return treeNode;
    }

// Uncomment for debugging masks:
/*
    private static DefaultMutableTreeNode createMaskNodes(Product product) {
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(MASKS);
        ProductNodeGroup<Mask> productNodeGroup = product.getMaskGroup();
        for (int i = 0; i < productNodeGroup.getNodeCount(); i++) {
            DefaultMutableTreeNode childTreeNode = new DefaultMutableTreeNode(productNodeGroup.get(i));
            treeNode.add(childTreeNode);
        }
        return treeNode;
    }
*/

    private static DefaultMutableTreeNode createVectorDataNodes(Product product) {
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(VECTOR_DATA);
        ProductNodeGroup<VectorDataNode> vectorDataGroup = product.getVectorDataGroup();
        for (int i = 0; i < vectorDataGroup.getNodeCount(); i++) {
            DefaultMutableTreeNode childTreeNode = new DefaultMutableTreeNode(vectorDataGroup.get(i));
            treeNode.add(childTreeNode);
        }
        return treeNode;
    }

    private static DefaultMutableTreeNode createTiePointGridNodes(Product product) {
        if (product.getNumTiePointGrids() == 0) {
            return null;
        }
        DefaultMutableTreeNode tiePointGridGroupTreeNode = new DefaultMutableTreeNode(TIE_POINT_GRIDS);
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
        DefaultMutableTreeNode flagCodingsGroupTreeNode = new DefaultMutableTreeNode(FLAG_CODINGS);
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
        DefaultMutableTreeNode indexCodingsGroupTreeNode = new DefaultMutableTreeNode(INDEX_CODINGS);
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

        @Override
        public void valueChanged(TreeSelectionEvent event) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) getLastSelectedPathComponent();
            if (node != null) {
                fireNodeSelected(node.getUserObject(), 1);
            }
        }
    }

    private class PTCellRenderer extends DefaultTreeCellRenderer {

        ImageIcon productIcon;
        ImageIcon metadataIcon;
        ImageIcon tiePointGridVisibleIcon;
        ImageIcon tiePointGridInvisibleIcon;
        ImageIcon bandVisibleIcon;
        ImageIcon bandInvisibleIcon;
        ImageIcon bandVirtualVisibleIcon;
        ImageIcon bandVirtualInvisibleIcon;
        ImageIcon bandFlagsVisibleIcon;
        ImageIcon bandFlagsInvisibleIcon;
        ImageIcon bandIndexedVisibleIcon;
        ImageIcon bandIndexedInvisibleIcon;
        ImageIcon vectorDataIcon;

        Font normalFont;
        Font boldFont;

        // Uncomment for debugging masks:
        // ImageIcon maskIcon;

        private PTCellRenderer() {
            productIcon = UIUtils.loadImageIcon("icons/RsProduct16.gif");
            metadataIcon = UIUtils.loadImageIcon("icons/RsMetaData16.gif");
            bandVisibleIcon = UIUtils.loadImageIcon("icons/RsBandAsSwath16.gif");
            bandInvisibleIcon = UIUtils.loadImageIcon("icons/RsBandAsSwath16Disabled.gif");
            tiePointGridVisibleIcon = UIUtils.loadImageIcon("icons/RsBandAsTiePoint16.gif");
            tiePointGridInvisibleIcon = UIUtils.loadImageIcon("icons/RsBandAsTiePoint16Disabled.gif");
            bandIndexedVisibleIcon = UIUtils.loadImageIcon("icons/RsBandIndexes16.gif");
            bandIndexedInvisibleIcon = UIUtils.loadImageIcon("icons/RsBandIndexes16Disabled.gif");
            bandFlagsVisibleIcon = UIUtils.loadImageIcon("icons/RsBandFlags16.gif");
            bandFlagsInvisibleIcon = UIUtils.loadImageIcon("icons/RsBandFlags16Disabled.gif");
            bandVirtualVisibleIcon = UIUtils.loadImageIcon("icons/RsBandVirtual16.gif");
            bandVirtualInvisibleIcon = UIUtils.loadImageIcon("icons/RsBandVirtual16Disabled.gif");
            vectorDataIcon = UIUtils.loadImageIcon("icons/RsVectorData16.gif");
            // Uncomment for debugging masks:
            // maskIcon = UIUtils.loadImageIcon("icons/RsMask16.gif");
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

            if (normalFont == null) {
                normalFont = getFont();
                boldFont = getFont().deriveFont(Font.BOLD);
            }

            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) value;
            value = treeNode.getUserObject();

            setFont(normalFont);

            if (value instanceof Product[]) {
                this.setText("Open products");
                this.setToolTipText("Contains the list of open data products");
                this.setIcon(null);
            } else if (value instanceof ProductNode) {
                final ProductNode productNode = (ProductNode) value;
                final boolean active = activeProductNodes.contains(productNode);
                final boolean opened = openedProductNodes.containsKey(productNode);

                if (active) {
                    setFont(boldFont);
                }

                String text = productNode.getName();
                final StringBuilder toolTipBuffer = new StringBuilder(32);

                final String prefix = productNode.getProductRefString();
                if (prefix != null) {
                    toolTipBuffer.append(prefix);
                    toolTipBuffer.append(' ');
                }
                String str = productNode.getDescription();
                if (!(str == null || str.length() == 0)) {
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
                        if (opened) {
                            this.setIcon(bandVirtualVisibleIcon);
                        } else {
                            this.setIcon(bandVirtualInvisibleIcon);
                        }
// Uncomment for debugging masks:
/*
                    } else if (band instanceof Mask) {
                        toolTipBuffer.append(", type = ");
                        toolTipBuffer.append(((Mask) band).getImageType().getClass().getSimpleName());
                        this.setIcon(maskIcon);
*/
                    } else if (band.getFlagCoding() != null) {
                        if (opened) {
                            this.setIcon(bandFlagsVisibleIcon);
                        } else {
                            this.setIcon(bandFlagsInvisibleIcon);
                        }
                    } else if (band.getIndexCoding() != null) {
                        if (opened) {
                            this.setIcon(bandIndexedVisibleIcon);
                        } else {
                            this.setIcon(bandIndexedInvisibleIcon);
                        }
                    } else {
                        if (opened) {
                            this.setIcon(bandVisibleIcon);
                        } else {
                            this.setIcon(bandInvisibleIcon);
                        }
                    }
                    toolTipBuffer.append(", raster size = ");
                    toolTipBuffer.append(band.getRasterWidth());
                    toolTipBuffer.append(" x ");
                    toolTipBuffer.append(band.getRasterHeight());
                } else if (productNode instanceof TiePointGrid) {
                    TiePointGrid grid = (TiePointGrid) productNode;
                    if (opened) {
                        this.setIcon(tiePointGridVisibleIcon);
                    } else {
                        this.setIcon(tiePointGridInvisibleIcon);
                    }
                    toolTipBuffer.append(", raster size = ");
                    toolTipBuffer.append(grid.getRasterWidth());
                    toolTipBuffer.append(" x ");
                    toolTipBuffer.append(grid.getRasterHeight());
                } else if (productNode instanceof VectorDataNode) {
                    VectorDataNode grid = (VectorDataNode) productNode;
                    this.setIcon(vectorDataIcon);
                    toolTipBuffer.append(", type = ");
                    toolTipBuffer.append(grid.getFeatureType().getTypeName());
                    toolTipBuffer.append(", #features = ");
                    toolTipBuffer.append(grid.getFeatureCollection().size());
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
            int componentCountBefore = popup.getComponentCount();
            if (commandUIFactory != null) {
                commandUIFactory.addContextDependentMenuItems("metadataNode", popup);
            }
            addSeparatorIfAnyComponentsAdded(popup, componentCountBefore);

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
        } else if(context instanceof VectorDataNode) {
            if (commandUIFactory != null) {
                commandUIFactory.addContextDependentMenuItems("vectorDataNode", popup);
            }
        }

        return popup;
    }


    private static void addSeparatorIfAnyComponentsAdded(JPopupMenu popup, int componentCountBevore) {
        if (popup.getComponentCount() > componentCountBevore) {
            popup.addSeparator();
        }
    }

    private void fireProductAdded(Product product) {
        if (productTreeListenerList != null) {
            for (ProductTreeListener l : productTreeListenerList) {
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
                } else if (node instanceof VectorDataNode) {
                    final VectorDataNode vectorDataNode = (VectorDataNode) node;
                    if (l instanceof ProductTreeListener2) {
                        ProductTreeListener2 l2 = (ProductTreeListener2) l;
                        l2.vectorDataSelected(vectorDataNode, clickCount);
                    }
                }
                if (node instanceof ProductNode) {
                    if (l instanceof ProductTreeListener2) {
                        ProductTreeListener2 l2 = (ProductTreeListener2) l;
                        l2.productNodeSelected((ProductNode) node, clickCount);
                    }
                }
            }
        }
    }

    private TreePath getTreePath(Object userObject) {
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) getModel().getRoot();
        Enumeration enumeration = rootNode.depthFirstEnumeration();
        while (enumeration.hasMoreElements()) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) enumeration.nextElement();
            if (childNode.getUserObject() == userObject) {
                return new TreePath(childNode.getPath());
            }
        }
        return null;
    }

    private static DefaultMutableTreeNode getTNode(DefaultMutableTreeNode groupNode,
                                                   Object userObject) {
        Enumeration enumeration = groupNode.children();
        while (enumeration.hasMoreElements()) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) enumeration.nextElement();
            if (childNode.getUserObject() == userObject) {
                return childNode;
            }
        }
        return null;
    }

    private DefaultMutableTreeNode getRootTNode() {
        return (DefaultMutableTreeNode) getTreeModel().getRoot();
    }

    private DefaultTreeModel getTreeModel() {
        return (DefaultTreeModel) getModel();
    }

    private class ProductTreePNL implements ProductNodeListener {

        @Override
        public void nodeChanged(final ProductNodeEvent event) {
            final ProductNode sourceNode = event.getSourceNode();
            final TreePath path = getTreePath(sourceNode);
            if (path != null) {
                getModel().valueForPathChanged(path, sourceNode);
            }
        }

        @Override
        public void nodeDataChanged(final ProductNodeEvent event) {
            repaint();
        }

        @Override
        public void nodeAdded(final ProductNodeEvent event) {
            ProductNode group = event.getGroup();
            if (!isRootGroup(group)) {
                return;
            }
            ProductNode sourceNode = event.getSourceNode();
            DefaultMutableTreeNode rootTNode = getRootTNode();
            DefaultMutableTreeNode productTNode = getTNode(rootTNode, sourceNode.getProduct());
            DefaultMutableTreeNode groupTNode = getGroupTNode(sourceNode, productTNode);
            if (groupTNode == null) {
                return;
            }
            groupTNode.add(new DefaultMutableTreeNode(sourceNode));
            getTreeModel().nodeStructureChanged(groupTNode);
        }

        @Override
        public void nodeRemoved(final ProductNodeEvent event) {
            ProductNode group = event.getGroup();
            if (!isRootGroup(group)) {
                return;
            }
            ProductNode sourceNode = event.getSourceNode();
            openedProductNodes.remove(sourceNode);
            activeProductNodes.remove(sourceNode);

            DefaultMutableTreeNode rootTNode = getRootTNode();
            DefaultMutableTreeNode productTNode = getTNode(rootTNode, sourceNode.getProduct());
            DefaultMutableTreeNode groupTNode = getGroupTNode(sourceNode, productTNode);
            if (groupTNode == null) {
                return;
            }
            DefaultMutableTreeNode sourceTNode = getTNode(groupTNode, sourceNode);
            if (sourceTNode == null) {
                return;
            }
            TreePath path = getTreePath(sourceNode);
            boolean nodeIsSelected = getSelectionModel().isPathSelected(path);
            DefaultMutableTreeNode selectedTNode = null;
            if (nodeIsSelected) {
                int selectionIndex = groupTNode.getIndex(sourceTNode);
                int childCount = groupTNode.getChildCount();
                if (childCount - 1 == selectionIndex) {
                    if (childCount > 1) {
                        TreeNode toSelectTNode = groupTNode.getChildAt(selectionIndex - 1);
                        selectedTNode = (DefaultMutableTreeNode) toSelectTNode;
                    } else {
                        selectedTNode = productTNode;
                    }
                } else {
                    TreeNode toSelectTNode = groupTNode.getChildAt(selectionIndex + 1);
                    selectedTNode = (DefaultMutableTreeNode) toSelectTNode;
                }
            }

            groupTNode.remove(sourceTNode);
            getTreeModel().nodeStructureChanged(groupTNode);
            if (selectedTNode != null) {
                select(selectedTNode.getUserObject());
            }
        }
    }

    private DefaultMutableTreeNode getGroupTNode(ProductNode sourceNode, DefaultMutableTreeNode productTNode) {
        DefaultMutableTreeNode groupTNode = null;
        if (sourceNode instanceof Mask) {
            // Uncomment for debugging masks:
            // groupTNode = getTNode(productTNode, MASKS);
        } else if (sourceNode instanceof AbstractBand) {
            groupTNode = getTNode(productTNode, BANDS);
        } else if (sourceNode instanceof TiePointGrid) {
            groupTNode = getTNode(productTNode, TIE_POINT_GRIDS);
        } else if (sourceNode instanceof VectorDataNode) {
            groupTNode = getTNode(productTNode, VECTOR_DATA);
        } else if (sourceNode instanceof FlagCoding) {
            groupTNode = getTNode(productTNode, FLAG_CODINGS);
        } else if (sourceNode instanceof IndexCoding) {
            groupTNode = getTNode(productTNode, INDEX_CODINGS);
        }
        return groupTNode;
    }

    private boolean isRootGroup(ProductNode group) {
        return group != null && group.getOwner() == group.getProduct();
    }
}

