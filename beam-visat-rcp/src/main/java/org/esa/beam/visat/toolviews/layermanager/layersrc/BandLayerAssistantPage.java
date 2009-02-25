package org.esa.beam.visat.toolviews.layermanager.layersrc;


import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.ui.assistant.AbstractAppAssistantPage;
import org.esa.beam.framework.ui.assistant.AppAssistantPageContext;
import org.esa.beam.glevel.BandImageMultiLevelSource;

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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class BandLayerAssistantPage extends AbstractAppAssistantPage {

    private JList list;

    public BandLayerAssistantPage() {
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

        RasterDataNode band = (RasterDataNode) list.getSelectedValue();
        BandImageMultiLevelSource bandImageMultiLevelSource = BandImageMultiLevelSource.create(band,
                                                                                               ProgressMonitor.NULL);
        ImageLayer imageLayer = new ImageLayer(bandImageMultiLevelSource);

        imageLayer.setName(band.getName());
        imageLayer.setVisible(true);

        Layer rootLayer = getAppPageContext().getAppContext().getSelectedProductSceneView().getRootLayer();
        rootLayer.getChildren().add(0, imageLayer);
        return true;
    }

    @Override
    protected Component createLayerPageComponent(AppAssistantPageContext context) {

        Product product = context.getAppContext().getSelectedProductSceneView().getProduct();

        Collection<RasterDataNode> rasterDataNodes = new ArrayList<RasterDataNode>();
        rasterDataNodes.addAll(Arrays.asList(product.getBands()));
        rasterDataNodes.addAll(Arrays.asList(product.getTiePointGrids()));

        list = new JList(rasterDataNodes.toArray());
        list.setCellRenderer(new MyDefaultListCellRenderer());
        list.addListSelectionListener(new MyListSelectionListener());

        JPanel panel = new JPanel(new BorderLayout(4, 4));
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
            final String text = MessageFormat.format("<html><b>{0}</b> {1}</html>", r.getName(),
                                                     r instanceof TiePointGrid ? " (Tie-point grid)" : " (Band)");
            label.setText(text);
            return label;
        }
    }

    private class MyListSelectionListener implements ListSelectionListener {

        public void valueChanged(ListSelectionEvent e) {
            getAppPageContext().updateState();
        }
    }
}