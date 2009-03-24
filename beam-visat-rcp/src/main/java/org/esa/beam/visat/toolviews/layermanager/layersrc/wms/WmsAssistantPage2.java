package org.esa.beam.visat.toolviews.layermanager.layersrc.wms;

import com.jidesoft.tree.AbstractTreeModel;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.assistant.AbstractAppAssistantPage;
import org.esa.beam.framework.ui.assistant.AppAssistantPageContext;
import org.geotools.data.ows.CRSEnvelope;
import org.geotools.data.ows.Layer;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

class WmsAssistantPage2 extends AbstractAppAssistantPage {

    private JLabel infoLabel;
    private JTree layerTree;
    private CoordinateReferenceSystem modelCRS;
    private final WmsModel wmsModel;

    WmsAssistantPage2(WmsModel wmsModel) {
        super("Select Layer");
        this.wmsModel = wmsModel;
    }

    @Override
    public boolean canFinish() {
        return false;
    }

    @Override
    public AbstractAppAssistantPage getNextPage(AppAssistantPageContext pageContext) {
        String crsCode = getMatchingCRSCode(wmsModel.selectedLayer);
        if (crsCode == null) {
            pageContext.showErrorDialog("Coordinate system not supported.");
            return null;
        }
        RasterDataNode raster = pageContext.getAppContext().getSelectedProductSceneView().getRaster();
        GeoCoding geoCoding = raster.getGeoCoding();
        AffineTransform g2mTransform = geoCoding.getGridToModelTransform();
        Rectangle2D bounds = g2mTransform.createTransformedShape(
                new Rectangle(0, 0, raster.getSceneRasterWidth(), raster.getSceneRasterHeight())).getBounds2D();
        wmsModel.crsEnvelope = new CRSEnvelope(crsCode, bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(),
                                                  bounds.getMaxY());
        return new WmsAssistantPage3(wmsModel);
    }

    @Override
    public boolean hasNextPage() {
        return true;
    }

    @Override
    public boolean validatePage() {
        return wmsModel.selectedLayer != null;
    }

    @Override
    public Component createLayerPageComponent(AppAssistantPageContext context) {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel.add(new JLabel("Available layers:"), BorderLayout.NORTH);

        modelCRS = context.getAppContext().getSelectedProductSceneView().getRaster().getGeoCoding().getModelCRS();

        layerTree = new JTree(new WMSTreeModel(wmsModel.wmsCapabilities.getLayer()));
        layerTree.setRootVisible(false);
        layerTree.setShowsRootHandles(true);
        layerTree.setCellRenderer(new MyDefaultTreeCellRenderer());
        layerTree.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        layerTree.getSelectionModel().addTreeSelectionListener(new MyTreeSelectionListener(context));
        panel.add(new JScrollPane(layerTree), BorderLayout.CENTER);
        infoLabel = new JLabel(" ");
        panel.add(infoLabel, BorderLayout.SOUTH);
        return panel;
    }

    private static double roundDeg(double v) {
        return Math.round(100.0 * v) / 100.0;
    }

    private String getMatchingCRSCode(Layer layer) {
        Set<String> srsSet = layer.getSrs();
        if (modelCRS.equals(DefaultGeographicCRS.WGS84)) {
            if (srsSet.contains("EPSG:4326")) {
                return "EPSG:4326";
            }
        }
        String modelSRS = CRS.toSRS(modelCRS);
        if (modelSRS != null) {
            for (String srs : srsSet) {
                if (srs.equals(modelSRS)) {
                    return srs;
                }
            }
        }
//        for (Object srsObj : srsSet) {
//            String srs = (String) srsObj;
//            try {
//                CoordinateReferenceSystem crs = CRS.decode(srs);
//                MathTransform transform = CRS.findMathTransform(crs, modelCRS, true);
//                if (transform.isIdentity()) {
//                    return srs;
//                }
//            } catch (FactoryException e) {
//                System.out.println(MessageFormat.format("Warning: SRS ''{0}'' not found: {1}", srs, e.getMessage()));
//            }
//        }
        return null;
    }

    static String getLatLonBoundingBoxText(CRSEnvelope bbox) {
        if (bbox == null) {
            return "Lon = ?° ... ?°, Lat = ?° ... ?°";
        }
        return MessageFormat.format("Lon = {0}° ... {1}°, Lat = {2}° ... {3}°",
                                    roundDeg(bbox.getMinX()),
                                    roundDeg(bbox.getMaxX()),
                                    roundDeg(bbox.getMinY()),
                                    roundDeg(bbox.getMaxY()));
    }

    private static class MyDefaultTreeCellRenderer extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            String text;
            if (value instanceof Layer) {
                String title;
                Layer layer = (Layer) value;
                title = layer.getTitle();
                if (title == null) {
                    title = layer.getName();
                }
                if (title == null) {
                    title = layer.toString();
                }
                StringBuilder sb = new StringBuilder(String.format("<html><b>%s</b>", title));

                Layer[] children = layer.getChildren();
                if (children.length > 1) {
                    sb.append(String.format(" (%d children)", children.length));
                } else if (children.length == 1) {
                    sb.append(" (1 child)");
                }

                text = sb.append("</html>").toString();
            } else if (value instanceof WMSCapabilities) {
                WMSCapabilities capabilities = (WMSCapabilities) value;
                text = String.format("<html><b>%s</b></html>", capabilities.getService().getName());
            } else {
                text = String.format("<html><b>%s</b></html>", value);
            }
            label.setText(text);
            return label;
        }

    }

    private class MyTreeSelectionListener implements TreeSelectionListener {
        
        private final AppAssistantPageContext pageContext;
        
        public MyTreeSelectionListener(AppAssistantPageContext pageContext) {
            this.pageContext = pageContext;
        }
        
        @Override
        public void valueChanged(TreeSelectionEvent e) {
            TreePath path = layerTree.getSelectionModel().getSelectionPath();
            wmsModel.selectedLayer = (Layer) path.getLastPathComponent();
            if (wmsModel.selectedLayer != null) {
                infoLabel.setText(getLatLonBoundingBoxText(wmsModel.selectedLayer.getLatLonBoundingBox()));
            } else {
                infoLabel.setText("");
            }
            pageContext.updateState();
        }

    }

    private static class WMSTreeModel extends AbstractTreeModel {

        private Layer rootLayer;

        private WMSTreeModel(Layer rootLayer) {
            this.rootLayer = rootLayer;
        }

        @Override
        public Object getRoot() {
            return rootLayer;
        }

        @Override
        public Object getChild(Object parent, int index) {
            Layer layer = (Layer) parent;
            return layer.getChildren()[index];
        }

        @Override
        public int getChildCount(Object parent) {
            Layer layer = (Layer) parent;
            return layer.getChildren().length;
        }

        @Override
        public boolean isLeaf(Object node) {
            Layer layer = (Layer) node;
            return layer.getChildren() != null && layer.getChildren().length == 0;
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            Layer layer = (Layer) parent;
            int index = Arrays.binarySearch(layer.getChildren(), child);
            return index < 0 ? -1 : index;
        }
    }

}