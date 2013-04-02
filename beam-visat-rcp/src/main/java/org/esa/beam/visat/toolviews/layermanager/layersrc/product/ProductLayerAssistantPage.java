/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.visat.toolviews.layermanager.layersrc.product;


import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.glayer.support.ImageLayer;
import com.jidesoft.tree.AbstractTreeModel;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.layer.AbstractLayerSourceAssistantPage;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glayer.RasterImageLayerType;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.ObjectUtils;
import org.geotools.referencing.CRS;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

class ProductLayerAssistantPage extends AbstractLayerSourceAssistantPage {

    private JTree tree;

    ProductLayerAssistantPage() {
        super("Select Band / Tie-Point Grid");
    }

    @Override
    public Component createPageComponent() {
        ProductTreeModel model = createTreeModel(getContext().getAppContext());
        tree = new JTree(model);
        tree.setEditable(false);
        tree.setShowsRootHandles(true);
        tree.setRootVisible(false);
        tree.setCellRenderer(new ProductNodeTreeCellRenderer());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.getSelectionModel().addTreeSelectionListener(new ProductNodeSelectionListener());

        List<CompatibleNodeList> nodeLists = model.compatibleNodeLists;
        for (CompatibleNodeList nodeList : nodeLists) {
            tree.expandPath(new TreePath(new Object[]{nodeLists, nodeList}));
        }

        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel.add(new JLabel("Compatible bands and tie-point grids:"), BorderLayout.NORTH);
        panel.add(new JScrollPane(tree), BorderLayout.CENTER);
        return panel;
    }

    @Override
    public boolean validatePage() {
        TreePath path = tree.getSelectionPath();
        return path != null && path.getLastPathComponent() instanceof RasterDataNode;
    }

    @Override
    public boolean hasNextPage() {
        return false;
    }

    @Override
    public boolean canFinish() {
        return true;
    }

    @Override
    public boolean performFinish() {
        final RasterDataNode rasterDataNode = (RasterDataNode) tree.getSelectionPath().getLastPathComponent();

        LayerType type = LayerTypeRegistry.getLayerType(RasterImageLayerType.class.getName());
        PropertySet configuration = type.createLayerConfig(getContext().getLayerContext());
        configuration.setValue(RasterImageLayerType.PROPERTY_NAME_RASTER, rasterDataNode);
        configuration.setValue(ImageLayer.PROPERTY_NAME_BORDER_SHOWN, false);
        configuration.setValue(ImageLayer.PROPERTY_NAME_BORDER_COLOR, ImageLayer.DEFAULT_BORDER_COLOR);
        configuration.setValue(ImageLayer.PROPERTY_NAME_BORDER_WIDTH, ImageLayer.DEFAULT_BORDER_WIDTH);
        configuration.setValue(ImageLayer.PROPERTY_NAME_PIXEL_BORDER_SHOWN, false);
        final ImageLayer imageLayer = (ImageLayer) type.createLayer(getContext().getLayerContext(),
                                                                    configuration);
        imageLayer.setName(rasterDataNode.getDisplayName());

        ProductSceneView sceneView = getContext().getAppContext().getSelectedProductSceneView();
        Layer rootLayer = sceneView.getRootLayer();
        rootLayer.getChildren().add(sceneView.getFirstImageLayerIndex(), imageLayer);

        final LayerDataHandler layerDataHandler = new LayerDataHandler(rasterDataNode, imageLayer);
        rasterDataNode.getProduct().addProductNodeListener(layerDataHandler);
        rootLayer.addListener(layerDataHandler);

        return true;
    }

    private static class CompatibleNodeList {

        private final String name;
        private final List<RasterDataNode> rasterDataNodes;

        CompatibleNodeList(String name, List<RasterDataNode> rasterDataNodes) {
            this.name = name;
            this.rasterDataNodes = rasterDataNodes;
        }
    }

    private ProductTreeModel createTreeModel(AppContext ctx) {
        Product selectedProduct = ctx.getSelectedProductSceneView().getProduct();

        ArrayList<CompatibleNodeList> compatibleNodeLists = new ArrayList<CompatibleNodeList>(3);
        List<RasterDataNode> compatibleNodes = new ArrayList<RasterDataNode>();
        compatibleNodes.addAll(Arrays.asList(selectedProduct.getBands()));
        compatibleNodes.addAll(Arrays.asList(selectedProduct.getTiePointGrids()));
        if (!compatibleNodes.isEmpty()) {
            compatibleNodeLists.add(new CompatibleNodeList(selectedProduct.getDisplayName(), compatibleNodes));
        }

        RasterDataNode raster = ctx.getSelectedProductSceneView().getRaster();
        GeoCoding geoCoding = raster.getGeoCoding();
        CoordinateReferenceSystem modelCRS = ImageManager.getModelCrs(geoCoding);
        final ProductManager productManager = ctx.getProductManager();
        final Product[] products = productManager.getProducts();
        for (Product product : products) {
            if (product == selectedProduct) {
                continue;
            }
            compatibleNodes = new ArrayList<RasterDataNode>();
            collectCompatibleRasterDataNodes(modelCRS, product.getBands(), compatibleNodes);
            collectCompatibleRasterDataNodes(modelCRS, product.getTiePointGrids(), compatibleNodes);
            if (!compatibleNodes.isEmpty()) {
                compatibleNodeLists.add(new CompatibleNodeList(product.getDisplayName(), compatibleNodes));
            }
        }
        return new ProductTreeModel(compatibleNodeLists);
    }

    private void collectCompatibleRasterDataNodes(CoordinateReferenceSystem thisCrs, RasterDataNode[] bands,
                                                  Collection<RasterDataNode> rasterDataNodes) {
        for (RasterDataNode node : bands) {
            CoordinateReferenceSystem otherCrs = ImageManager.getModelCrs(node.getGeoCoding());
            // For GeoTools, two CRS where unequal if the authorities of their CS only differ in version
            // This happened with the S-2 L1C CRS, namely an EPSG:32615. Here one authority's version was null,
            // the other "7.9". Extremely annoying to debug and find out :-(   (nf, Feb 2013)
            if (CRS.equalsIgnoreMetadata(thisCrs, otherCrs)
                    || haveCommonReferenceIdentifiers(thisCrs, otherCrs)) {
                rasterDataNodes.add(node);
            }
        }
    }

    private static boolean haveCommonReferenceIdentifiers(CoordinateReferenceSystem crs1, CoordinateReferenceSystem crs2) {
        Set<ReferenceIdentifier> identifiers1 = crs1.getIdentifiers();
        Set<ReferenceIdentifier> identifiers2 = crs2.getIdentifiers();
        // If a CRS does not have identifiers or if they have different number of identifiers
        // they cannot be equal.
        if (identifiers1 == null || identifiers1.isEmpty()
                || identifiers2 == null || identifiers2.isEmpty()
                || identifiers1.size() != identifiers2.size()) {
            return false;
        }
        // The two CRSs can only be equal if they have the same number of identifiers
        // and all of them are common to both.
        int eqCount = 0;
        for (ReferenceIdentifier refId1 : identifiers1) {
            for (ReferenceIdentifier refId2 : identifiers2) {
                if (compareRefIds(refId1, refId2)) {
                    eqCount++;
                    break;
                }
            }
        }
        return eqCount == identifiers1.size();
    }

    private static boolean compareRefIds(ReferenceIdentifier refId1, ReferenceIdentifier refId2) {
        return ObjectUtils.equalObjects(refId1.getCodeSpace(), refId2.getCodeSpace())
                && ObjectUtils.equalObjects(refId1.getCode(), refId2.getCode())
                && compareVersions(refId1.getVersion(), refId2.getVersion());
    }

    // Other than GeoTools, we compare versions only if given.
    // We interpret the case version==null, as not provided, hence all versions match  (nf, Feb 2013)
    private static boolean compareVersions(String version1, String version2) {
        return version1 == null || version2 == null || version1.equalsIgnoreCase(version2);
    }

    private static class ProductNodeTreeCellRenderer extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (value instanceof CompatibleNodeList) {
                label.setText(MessageFormat.format("<html><b>{0}</b></html>", ((CompatibleNodeList) value).name));
            } else if (value instanceof Band) {
                label.setText(MessageFormat.format("<html><b>{0}</b></html>", ((Band) value).getName()));
            } else if (value instanceof TiePointGrid) {
                label.setText(MessageFormat.format("<html><b>{0}</b> (Tie-point grid)</html>",
                                                   ((TiePointGrid) value).getName()));
            }
            return label;
        }
    }

    private class ProductNodeSelectionListener implements TreeSelectionListener {

        @Override
        public void valueChanged(TreeSelectionEvent e) {
            getContext().updateState();
        }
    }

    private static class ProductTreeModel extends AbstractTreeModel {

        private final List<CompatibleNodeList> compatibleNodeLists;

        private ProductTreeModel(List<CompatibleNodeList> compatibleNodeLists) {
            this.compatibleNodeLists = compatibleNodeLists;
        }

        @Override
        public Object getRoot() {
            return compatibleNodeLists;
        }

        @Override
        public Object getChild(Object parent, int index) {
            if (parent == compatibleNodeLists) {
                return compatibleNodeLists.get(index);
            } else if (parent instanceof CompatibleNodeList) {
                return ((CompatibleNodeList) parent).rasterDataNodes.get(index);
            }
            return null;
        }

        @Override
        public int getChildCount(Object parent) {
            if (parent == compatibleNodeLists) {
                return compatibleNodeLists.size();
            } else if (parent instanceof CompatibleNodeList) {
                return ((CompatibleNodeList) parent).rasterDataNodes.size();
            }
            return 0;
        }

        @Override
        public boolean isLeaf(Object node) {
            return node instanceof RasterDataNode;
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            if (parent == compatibleNodeLists) {
                return compatibleNodeLists.indexOf(child);
            } else if (parent instanceof CompatibleNodeList) {
                return ((CompatibleNodeList) parent).rasterDataNodes.indexOf(child);
            }
            return -1;
        }
    }
}
