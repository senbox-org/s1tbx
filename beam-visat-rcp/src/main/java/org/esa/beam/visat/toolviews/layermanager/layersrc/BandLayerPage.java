package org.esa.beam.visat.toolviews.layermanager.layersrc;


import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.glevel.BandImageMultiLevelSource;
import org.esa.beam.visat.toolviews.layermanager.LayerPage;
import org.esa.beam.visat.toolviews.layermanager.LayerPageContext;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class BandLayerPage extends LayerPage {

    private static final String BAND_OVERLAYS_ID = "org.esa.beam.bandOverlays";
    private static final String BAND_OVERLAYS_NAME = "Band overlays";

    private JList list;

    public BandLayerPage() {
        super("Select Band");
    }

    @Override
    public boolean validatePage() {
        return list.getSelectedValue() != null;
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
        Layer bandOverlaysLayer = getLayerPageContext().getLayer(BAND_OVERLAYS_ID);
        if (bandOverlaysLayer == null) {
            bandOverlaysLayer = new Layer();
            bandOverlaysLayer.setId(BAND_OVERLAYS_ID);
            bandOverlaysLayer.setName(BAND_OVERLAYS_NAME);
            bandOverlaysLayer.setVisible(true);
            getLayerPageContext().getView().getRootLayer().getChildren().add(bandOverlaysLayer);
        }

        RasterDataNode band = (RasterDataNode) list.getSelectedValue();
        BandImageMultiLevelSource bandImageMultiLevelSource = BandImageMultiLevelSource.create(band,
                                                                                               ProgressMonitor.NULL);
        ImageLayer imageLayer = new ImageLayer(bandImageMultiLevelSource);

        imageLayer.setName(band.getName());
        imageLayer.setVisible(true);

        bandOverlaysLayer.getChildren().add(imageLayer);
        return true;
    }

    @Override
    protected Component createLayerPageComponent(LayerPageContext context) {

        Product product = context.getView().getProduct();

        Collection rasterDataNodes = new ArrayList();
        rasterDataNodes.addAll(Arrays.asList(product.getBands()));
        rasterDataNodes.addAll(Arrays.asList(product.getTiePointGrids()));

        list = new JList(rasterDataNodes.toArray());
        list.setCellRenderer(new MyDefaultListCellRenderer());
        list.addListSelectionListener(new MyListSelectionListener());

        final JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));

        panel.add(new JLabel("Available bands:"), BorderLayout.NORTH);
        panel.add(new JScrollPane(list), BorderLayout.CENTER);

        return panel;
    }

    private static class MyDefaultListCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            RasterDataNode r = (RasterDataNode) value;
            label.setText(r.getName() + (r instanceof TiePointGrid ? " (tie-point grid)" : " (band)"));
            return label;
        }
    }

    private class MyListSelectionListener implements ListSelectionListener {

        public void valueChanged(ListSelectionEvent e) {
            getLayerPageContext().updateState();
        }
    }
}