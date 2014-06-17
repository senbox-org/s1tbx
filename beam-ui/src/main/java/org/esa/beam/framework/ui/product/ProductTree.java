/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.beam.framework.ui.product;

import com.bc.ceres.swing.TreeCellExtender;
import org.esa.beam.dataio.dimap.DimapProductReader;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.ui.PopupMenuFactory;
import org.esa.beam.framework.ui.PopupMenuHandler;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.command.CommandManager;
import org.esa.beam.framework.ui.command.CommandUIFactory;
import org.esa.beam.framework.ui.product.tree.AbstractTN;
import org.esa.beam.framework.ui.product.tree.ProductTreeModel;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
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


    private final Set<ProductNode> activeProductNodes;
    private final Map<ProductNode, Integer> openedProductNodes;
    private final ProductTree.ProductManagerListener productManagerListener;
    private final ProductTreeModelListener productTreeModelListener;

    private CommandManager commandManager;
    private CommandUIFactory commandUIFactory;
    private List<ProductTreeListener> productTreeListenerList;

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
        super((TreeModel) null);
        getSelectionModel().setSelectionMode(multipleSelect
                                                     ? TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION
                                                     : TreeSelectionModel.SINGLE_TREE_SELECTION);
        addTreeSelectionListener(new PTSelectionListener());
        addMouseListener(new PTMouseListener());
        setCellRenderer(new PTCellRenderer());
        setRootVisible(false);
        setShowsRootHandles(true);
        setDragEnabled(true);
        setTransferHandler(new TreeTransferHandler());
        setToggleClickCount(2);
        setAutoscrolls(true);
        putClientProperty("JTree.lineStyle", "Angled");
        ToolTipManager.sharedInstance().registerComponent(this);

        final PopupMenuHandler popupMenuHandler = new PopupMenuHandler(this);
        addMouseListener(popupMenuHandler);
        addKeyListener(popupMenuHandler);
        activeProductNodes = new HashSet<>();
        openedProductNodes = new HashMap<>();
        productManagerListener = new ProductManagerListener();
        productTreeModelListener = new ProductTreeModelListener(this);

        TreeCellExtender.equip(this);
        this.setDropTarget(new DropTarget(this,
                                          DnDConstants.ACTION_COPY_OR_MOVE,
                                          new ProductTreeDropTarget()));
    }

    @Override
    public ProductTreeModel getModel() {
        return (ProductTreeModel) super.getModel();
    }

    /**
     * Sets the <code>TreeModel</code> that will provide the data.
     *
     * @param newModel the <code>TreeModel</code> that is to provide the data. Must be an
     *                 instance of {@link org.esa.beam.framework.ui.product.tree.ProductTreeModel}.
     *                 description: The TreeModel that will provide the data.
     */
    @Override
    public void setModel(TreeModel newModel) {
        if (newModel != null && !(newModel instanceof ProductTreeModel)) {
            throw new IllegalStateException("newModel must be instance of ProductTreeModel");
        }

        if (getModel() != null) {
            ProductManager productManager = getModel().getProductManager();
            productManager.removeListener(productManagerListener);
            getModel().removeTreeModelListener(productTreeModelListener);
        }
        if (newModel != null) {
            ProductTreeModel ptm = (ProductTreeModel) newModel;
            ptm.getProductManager().addListener(productManagerListener);
            ptm.addTreeModelListener(productTreeModelListener);
        }
        super.setModel(newModel);
    }

    /**
     * Selects the specified object in this tree's model. If the given object has no representation in the tree, the
     * current selection will not be changed.
     *
     * @param toSelect the object whose representation in the tree will be selected.
     */
    public void select(Object toSelect) {
        final TreePath path = getModel().getTreePath(toSelect);
        if (path != null) {
            setSelectionPath(path);
        }
    }

    public void expand(Object toExpand) {
        final TreePath path = getModel().getTreePath(toExpand);
        if (path != null) {
            makeVisible(path);
        }
    }

    /**
     * Registers "opened" product nodes, e.g. visible nodes, nodes currently edited, etc.
     * Opened product nodes may be differently displayed.
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
            AbstractTN node = (AbstractTN) getLastSelectedPathComponent();
            if (node != null) {
                Object context = node.getContent();
                if (context != null) {
                    return createPopup(context);
                }
            }
        }
        return null;
    }

    /**
     * Adds a new product tree listener to this product tree component.
     *
     * @param listener the listener to be added.
     */
    public void addProductTreeListener(ProductTreeListener listener) {
        if (listener != null) {
            if (productTreeListenerList == null) {
                productTreeListenerList = new ArrayList<>();
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

    private class PTMouseListener extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent event) {
            int selRow = getRowForLocation(event.getX(), event.getY());
            if (selRow >= 0) {
                int clickCount = event.getClickCount();
                TreePath selPath = getPathForLocation(event.getX(), event.getY());
                if (selPath != null) {
                    AbstractTN node = (AbstractTN) selPath.getLastPathComponent();
                    fireNodeSelected(node.getContent(), clickCount);
                }
            }
        }
    }

    private class PTSelectionListener implements TreeSelectionListener {

        @Override
        public void valueChanged(TreeSelectionEvent event) {
            AbstractTN node = (AbstractTN) getLastSelectedPathComponent();
            if (node != null) {
                fireNodeSelected(node.getContent(), 1);
            }
        }
    }

    private class PTCellRenderer extends DefaultTreeCellRenderer {

        private ImageIcon productIcon, productIconModified, productIconOrigFormat;
        private ImageIcon metadataIcon;
        private ImageIcon tiePointGridVisibleIcon;
        private ImageIcon tiePointGridInvisibleIcon;
        private ImageIcon bandVisibleIcon;
        private ImageIcon bandInvisibleIcon;
        private ImageIcon bandVirtualVisibleIcon;
        private ImageIcon bandVirtualInvisibleIcon;
        private ImageIcon bandFlagsVisibleIcon;
        private ImageIcon bandFlagsInvisibleIcon;
        private ImageIcon bandIndexedVisibleIcon;
        private ImageIcon bandIndexedInvisibleIcon;
        private ImageIcon vectorDataIcon;

        private Font normalFont;
        private Font boldFont;

        // Uncomment for debugging masks:
        // ImageIcon maskIcon;

        private PTCellRenderer() {
            productIcon = UIUtils.loadImageIcon("icons/RsProduct16.gif");
            productIconModified = UIUtils.loadImageIcon("icons/RsProduct16-red.gif");
            productIconOrigFormat = UIUtils.loadImageIcon("icons/RsProduct16-yellow.gif");
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

            AbstractTN treeNode = (AbstractTN) value;
            value = treeNode.getContent();

            setFont(normalFont);

            if (value instanceof ProductNode) {
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

                    if(product.isModified())
                        this.setIcon(productIconModified);
                    else {
                        if(product.getProductReader() instanceof DimapProductReader)
                            this.setIcon(productIcon);
                        else
                            this.setIcon(productIconOrigFormat);
                    }
                    toolTipBuffer.append(", ");
                    toolTipBuffer.append(product.getSceneRasterWidth());
                    toolTipBuffer.append(" x ");
                    toolTipBuffer.append(product.getSceneRasterHeight());
                    toolTipBuffer.append(" pixels");
                } else if (productNode instanceof ProductNodeGroup) {
                    text = treeNode.getName();
                } else if (productNode instanceof MetadataElement) {
                    MetadataElement metadataElement = (MetadataElement) productNode;
                    if (metadataElement.getParentElement() != null || metadataElement instanceof SampleCoding) {
                        this.setIcon(metadataIcon);
                    } else {
                        text = treeNode.getName();
                    }
                } else if (productNode instanceof Band) {
                    Band band = (Band) productNode;

                    if (band.getSpectralWavelength() > 0.0) {
                        if (band.getSpectralWavelength() == Math.round(band.getSpectralWavelength())) {
                            text = String.format("%s (%d nm)", productNode.getName(), (int) band.getSpectralWavelength());
                        } else {
                            text = String.format("%s (%s nm)", productNode.getName(), String.valueOf(band.getSpectralWavelength()));
                        }

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
                    TiePointGrid tiePointGrid = (TiePointGrid) productNode;
                    if (opened) {
                        this.setIcon(tiePointGridVisibleIcon);
                    } else {
                        this.setIcon(tiePointGridInvisibleIcon);
                    }
                    toolTipBuffer.append(", raster size = ");
                    toolTipBuffer.append(tiePointGrid.getRasterWidth());
                    toolTipBuffer.append(" x ");
                    toolTipBuffer.append(tiePointGrid.getRasterHeight());
                } else if (productNode instanceof VectorDataNode) {
                    VectorDataNode vectorDataNode = (VectorDataNode) productNode;
                    this.setIcon(vectorDataIcon);
                    toolTipBuffer.append(", type = ");
                    toolTipBuffer.append(vectorDataNode.getFeatureType().getTypeName());
                    toolTipBuffer.append(", #features = ");
                    toolTipBuffer.append(vectorDataNode.getFeatureCollection().size());
                }
                this.setText(text);
                this.setToolTipText(toolTipBuffer.toString());
            }

            return this;
        }
    }

    public JPopupMenu createPopup(final Object context) {

        JPopupMenu popup = new JPopupMenu();

        if (context instanceof Product) {
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
        } else if (context instanceof VectorDataNode) {
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
                if (node instanceof Product) {
                    l.productSelected((Product) node, clickCount);
                } else if (node instanceof ProductNodeGroup) {
                    l.productSelected(((ProductNode) node).getProduct(), clickCount);
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

    private class ProductManagerListener implements ProductManager.Listener {

        @Override
        public void productAdded(ProductManager.Event event) {
            fireProductAdded(event.getProduct());
            TreePath treePath = getModel().getTreePath(event.getProduct());
            expandPath(treePath);
            Rectangle bounds = getPathBounds(treePath);
            if (bounds != null) {
                bounds.setRect(0, bounds.y, bounds.width, bounds.height);
                scrollRectToVisible(bounds);
            }
        }

        @Override
        public void productRemoved(ProductManager.Event event) {
            fireProductRemoved(event.getProduct());
        }
    }

// must have this listener and can not use the above ProductManagerListener, because of the order the listeners
// are called. When the above listener is called the new product is not yet in the model. 

    private static class ProductTreeModelListener implements TreeModelListener {

        private ProductTree tree;

        private ProductTreeModelListener(ProductTree tree) {
            this.tree = tree;
        }

        @Override
        public void treeNodesChanged(TreeModelEvent e) {
            tree.makeVisible(e.getTreePath());
            updateUi();
        }

        @Override
        public void treeNodesInserted(TreeModelEvent e) {
            updateUi();
        }

        @Override
        public void treeNodesRemoved(TreeModelEvent e) {
            int selectionRow = tree.getLeadSelectionRow();
            updateUi();
            while (selectionRow >= tree.getRowCount()) {
                selectionRow -= 1;
            }
            if (selectionRow >= 0) {
                tree.setSelectionInterval(selectionRow, selectionRow);
            }
        }

        @Override
        public void treeStructureChanged(TreeModelEvent e) {
            updateUi();
        }

        private void updateUi() {
            tree.invalidate();
            tree.doLayout();
            tree.updateUI();
        }

    }

    private class ProductTreeDropTarget extends DropTargetAdapter {

        private DataFlavor uriListFlavor;

        private ProductTreeDropTarget() {
            try {
                uriListFlavor = new DataFlavor("text/uri-list;class=java.lang.String");
            } catch (ClassNotFoundException ignore) {
            }
        }

        @Override
        public void dragEnter(DropTargetDragEvent dtde) {
            final int dropAction = dtde.getDropAction();
            if ((dropAction & DnDConstants.ACTION_COPY_OR_MOVE) != 0 &&
                    (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor) ||
                            uriListFlavor != null && dtde.isDataFlavorSupported(uriListFlavor))) {
                dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
            } else {
                dtde.rejectDrag();
            }
        }

        @Override
        public void drop(DropTargetDropEvent dtde) {
            dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
            boolean success = false;
            try {
                final Transferable transferable = dtde.getTransferable();
                @SuppressWarnings({"unchecked"})
                final List<File> fileList;
                if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    fileList = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                } else if (transferable.isDataFlavorSupported(uriListFlavor)) {
                    // on Unix another mimetype is used, see
                    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4899516
                    String data = (String) transferable.getTransferData(uriListFlavor);
                    fileList = textURIListToFileList(data);
                } else {
                    fileList = Collections.emptyList();
                }
                for (File file : fileList) {
                    ProductManager productManager = getModel().getProductManager();
                    if (!isProductAlreadyOpen(file, productManager)) {
                        final Product product = ProductIO.readProduct(file);
                        if (product != null) {
                            productManager.addProduct(product);
                            success = true;
                        }
                    }
                }
            } catch (UnsupportedFlavorException | IOException ignored) {
                // This can happen if the transferred object is not a real file on disk
                // but a virtual file (e.g. email from Thunderbird or Outlook), see
                // http://bugs.sun.com/view_bug.do?bug_id=6242241
                // http://bugs.sun.com/view_bug.do?bug_id=4808793
            }
            dtde.dropComplete(success);
        }

        private boolean isProductAlreadyOpen(File file, ProductManager productManager) {
            for (int i = 0; i < productManager.getProductCount(); i++) {
                final Product product = productManager.getProduct(i);
                final File productFile = product.getFileLocation();
                if (file.equals(productFile)) {
                    return true;
                }
            }
            return false;
        }

        private List<File> textURIListToFileList(String data) {
            List<File> list = new ArrayList<>(1);
            StringTokenizer st = new StringTokenizer(data, "\r\n");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                if (token.startsWith("#")) {
                    // the line is a comment (as per the RFC 2483)
                    continue;
                }
                try {
                    list.add(new File(new URI(token)));
                } catch (java.net.URISyntaxException | IllegalArgumentException ignore) {
                    // malformed URI
                    // or
                    // the URI is not a valid 'file:' URI

                }
            }
            return list;
        }
    }
	
    public static class TreeTransferHandler extends TransferHandler {

        @Override
        public boolean canImport(TransferHandler.TransferSupport info) {
            return false;
        }

        @Override
        public int getSourceActions(JComponent c) {
            return TransferHandler.COPY;
        }

        //Bundle up the selected items in a single list for export.
        //Each line is separated by a newline.
        @Override
        protected Transferable createTransferable(JComponent c) {
            final JTree tree = (JTree)c;
            final TreePath path = tree.getSelectionPath();

            final AbstractTN node = (AbstractTN) path.getLastPathComponent();
            final Object context = node.getContent();
            if (context != null) {
                if(context instanceof Product) {
                    final Product product = (Product)context;
                    if(product.getFileLocation() != null) {
                        return new StringSelection(product.getFileLocation().getAbsolutePath());
                    }
                }
            }
            return null;
        }
    }
}

