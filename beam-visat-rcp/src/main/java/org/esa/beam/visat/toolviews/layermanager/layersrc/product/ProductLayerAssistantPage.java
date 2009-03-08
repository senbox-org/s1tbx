package org.esa.beam.visat.toolviews.layermanager.layersrc.product;


import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.ImageLayer;
import com.jidesoft.tree.AbstractTreeModel;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.assistant.AbstractAppAssistantPage;
import org.esa.beam.framework.ui.assistant.AppAssistantPageContext;
import org.esa.beam.glevel.BandImageMultiLevelSource;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ProductLayerAssistantPage extends AbstractAppAssistantPage {

    private JTree tree;

    public ProductLayerAssistantPage() {
        super("Select Band / Tie-Point Grid");
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

        BandImageMultiLevelSource bandImageMultiLevelSource = BandImageMultiLevelSource.create(rasterDataNode,
                                                                                               ProgressMonitor.NULL);
        final ImageLayer imageLayer = new ImageLayer(bandImageMultiLevelSource);

        imageLayer.setName(rasterDataNode.getName());
        imageLayer.setVisible(true);

        Layer rootLayer = getAppPageContext().getAppContext().getSelectedProductSceneView().getRootLayer();
        rootLayer.getChildren().add(0, imageLayer);

        final LayerDataHandler layerDataHandler = new LayerDataHandler(rasterDataNode, imageLayer);
        rasterDataNode.getProduct().addProductNodeListener(layerDataHandler);
        rootLayer.addListener(layerDataHandler);

        return true;
    }

    static class CompatibleNodeList {
        final String name;
        final List<RasterDataNode> rasterDataNodes;

        CompatibleNodeList(String name, List<RasterDataNode> rasterDataNodes) {
            this.name = name;
            this.rasterDataNodes = rasterDataNodes;
        }
    }

    @Override
    protected Component createLayerPageComponent(AppAssistantPageContext context) {

        ProductTreeModel model = createTreeModel(context.getAppContext());
        tree = new JTree(model);
        tree.setEditable(false);
        tree.setShowsRootHandles(true);
        tree.setRootVisible(false);
        tree.setCellRenderer(new MyDefaultTreeCellRenderer());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.getSelectionModel().addTreeSelectionListener(new MyListSelectionListener());

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

    private ProductTreeModel createTreeModel(AppContext ctx) {
        Product product0 = ctx.getSelectedProductSceneView().getProduct();
        RasterDataNode raster = ctx.getSelectedProductSceneView().getRaster();
        GeoCoding geoCoding = raster.getGeoCoding();
        CoordinateReferenceSystem modelCRS = geoCoding != null ? geoCoding.getModelCRS() : null;

        ArrayList<CompatibleNodeList> compatibleNodeLists = new ArrayList<CompatibleNodeList>(3);

        List<RasterDataNode> compatibleNodes = new ArrayList<RasterDataNode>();
        compatibleNodes.addAll(Arrays.asList(product0.getBands()));
        compatibleNodes.addAll(Arrays.asList(product0.getTiePointGrids()));
        if (!compatibleNodes.isEmpty()) {
            compatibleNodeLists.add(new CompatibleNodeList(product0.getDisplayName(), compatibleNodes));
        }

        if (modelCRS != null) {
            final ProductManager productManager = ctx.getProductManager();
            final Product[] products = productManager.getProducts();
            for (int i = 0; i < products.length; i++) {
                Product product1 = products[i];
                if (product1 == product0) {
                    continue;
                }
                compatibleNodes = new ArrayList<RasterDataNode>();
                collectCompatibleRasterDataNodes(product1.getBands(), modelCRS, compatibleNodes);
                collectCompatibleRasterDataNodes(product1.getTiePointGrids(), modelCRS, compatibleNodes);
                if (!compatibleNodes.isEmpty()) {
                    compatibleNodeLists.add(new CompatibleNodeList(product1.getDisplayName(), compatibleNodes));
                }
            }
        }

        return new ProductTreeModel(compatibleNodeLists);
    }

    private void collectCompatibleRasterDataNodes(RasterDataNode[] bands, CoordinateReferenceSystem crs, Collection<RasterDataNode> rasterDataNodes) {
        for (RasterDataNode node: bands) {
            GeoCoding geoCoding = node.getGeoCoding();
            if (geoCoding != null && node.getGeoCoding().getModelCRS().equals(crs)) {
                rasterDataNodes.add(node);
            }
        }
    }

    private static class MyDefaultTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (value instanceof CompatibleNodeList) {
                label.setText(MessageFormat.format("<html><b>{0}</b></html>", ((CompatibleNodeList) value).name));
            } else if (value instanceof Band) {
                label.setText(MessageFormat.format("<html><b>{0}</b></html>", ((Band) value).getName()));
            } else if (value instanceof TiePointGrid) {
                label.setText(MessageFormat.format("<html><b>{0}</b> (Tie-point grid)</html>", ((TiePointGrid) value).getName()));
            }
            return label;
        }
    }

    private class MyListSelectionListener implements TreeSelectionListener {
        public void valueChanged(TreeSelectionEvent e) {
            getAppPageContext().updateState();
        }
    }

    private static class ProductTreeModel extends AbstractTreeModel {
        private final List<CompatibleNodeList> compatibleNodeLists;

        public ProductTreeModel(List<CompatibleNodeList> compatibleNodeLists) {
            this.compatibleNodeLists = compatibleNodeLists;
        }

        public Object getRoot() {
            return compatibleNodeLists;
        }

        public Object getChild(Object parent, int index) {
            if (parent == compatibleNodeLists) {
                return compatibleNodeLists.get(index);
            } else if (parent instanceof CompatibleNodeList) {
                return ((CompatibleNodeList) parent).rasterDataNodes.get(index);
            }
            return null;
        }

        public int getChildCount(Object parent) {
            if (parent == compatibleNodeLists) {
                return compatibleNodeLists.size();
            } else if (parent instanceof CompatibleNodeList) {
                return ((CompatibleNodeList) parent).rasterDataNodes.size();
            }
            return 0;
        }

        public boolean isLeaf(Object node) {
            return node instanceof RasterDataNode;
        }

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