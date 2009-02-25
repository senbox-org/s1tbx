package org.esa.beam.visat.toolviews.layermanager.layersrc.wms;

import org.esa.beam.framework.ui.assistant.AbstractAppAssistantPage;
import org.esa.beam.framework.ui.assistant.AppAssistantPageContext;
import org.geotools.data.ows.CRSEnvelope;
import org.geotools.data.ows.Layer;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.data.wms.WebMapServer;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Component;
import java.text.MessageFormat;
import java.util.List;

class WmsPage2 extends AbstractAppAssistantPage {
    private final WebMapServer wms;
    private final WMSCapabilities wmsCapabilities;
    private JLabel infoLabel;
    private JTree layerTree;
    private Layer selectedLayer;

    WmsPage2(WebMapServer wms, WMSCapabilities wmsCapabilities) {
        super("Select Layer");
        this.wms = wms;
        this.wmsCapabilities = wmsCapabilities;
    }

    @Override
    public boolean canFinish() {
        return false;
    }

    @Override
    public AbstractAppAssistantPage getNextLayerPage() {
        return new WmsPage3(wms, selectedLayer);
    }

    @Override
    public boolean hasNextPage() {
        return true;
    }

    @Override
    public boolean validatePage() {
        return selectedLayer != null;
    }

    protected Component createLayerPageComponent(AppAssistantPageContext context) {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel.add(new JLabel("Available layers:"), BorderLayout.NORTH);

        layerTree = new JTree(createRootNode());
        layerTree.setRootVisible(false);
        layerTree.setShowsRootHandles(true);
        layerTree.setCellRenderer(new MyDefaultTreeCellRenderer());
        layerTree.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        layerTree.getSelectionModel().addTreeSelectionListener(new MyTreeSelectionListener());
        panel.add(new JScrollPane(layerTree), BorderLayout.CENTER);
        infoLabel = new JLabel(" ");
        panel.add(infoLabel, BorderLayout.SOUTH);
        return panel;
    }

    private DefaultMutableTreeNode createRootNode() {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Root", true);
        List<Layer> list = wmsCapabilities.getLayerList();
        if (list != null) {
            for (Layer layer : list) {
                rootNode.add(createChildNode(layer));
            }
        }
        return rootNode;
    }

    private DefaultMutableTreeNode createChildNode(Layer layer) {
        Layer[] children = layer.getChildren();
        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(layer, children.length > 0);
        for (Layer childLayer : children) {
            childNode.add(createChildNode(childLayer));
        }
        return childNode;
    }

    private static double roundDeg(double v) {
        return Math.round(100.0 * v) / 100.0;
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
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
            String text;
            if (userObject instanceof Layer) {
                Layer layer = (Layer) userObject;
                text = layer.getTitle();
                if (text == null) {
                    text = layer.getName();
                }
                if (text == null) {
                    text = layer.toString();
                }
                text = "<html><b>" + text + "</b>";
                Layer[] children = layer.getChildren();
                if (children.length > 1) {
                    text += " (" + children.length + " children)";
                } else if (children.length == 1) {
                    text += " (1 child)";
                }
                text += "</html>";
            } else {
                text = "<html><b>" + userObject + "</b></html>";
            }
            label.setText(text);
            return label;
        }
    }

    private class MyTreeSelectionListener implements TreeSelectionListener {
        public void valueChanged(TreeSelectionEvent e) {
            TreePath path = layerTree.getSelectionModel().getSelectionPath();
            selectedLayer = (Layer) ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
            if (selectedLayer != null) {
                infoLabel.setText(getLatLonBoundingBoxText(selectedLayer.getLatLonBoundingBox()));
            } else {
                infoLabel.setText("");
            }
            getPageContext().updateState();
        }

    }
}