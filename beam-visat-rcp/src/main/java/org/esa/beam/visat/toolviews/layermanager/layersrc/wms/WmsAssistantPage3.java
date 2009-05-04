package org.esa.beam.visat.toolviews.layermanager.layersrc.wms;

import com.bc.ceres.glayer.swing.LayerCanvas;
import org.esa.beam.visat.toolviews.layermanager.layersrc.AbstractLayerSourceAssistantPage;
import org.esa.beam.visat.toolviews.layermanager.layersrc.LayerSourcePageContext;
import org.geotools.data.ows.CRSEnvelope;
import org.geotools.data.ows.Layer;
import org.opengis.layer.Style;
import org.opengis.util.InternationalString;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.concurrent.ExecutionException;

class WmsAssistantPage3 extends AbstractLayerSourceAssistantPage {

    private JComboBox styleList;
    private JLabel messageLabel;
    private WmsWorker previewWorker;
    private Throwable error;
    private JPanel mapPanel;

    WmsAssistantPage3() {
        super("Layer Preview");
    }

    @Override
    public boolean validatePage() {
        return error == null;
    }

    @Override
    public Component createPageComponent() {
        final LayerSourcePageContext context = getContext();
        Layer selectedLayer = (Layer) context.getPropertyValue(WmsLayerSource.PROPERTY_SELECTED_LAYER);
        JLabel infoLabel = new JLabel(WmsAssistantPage2.getLatLonBoundingBoxText(selectedLayer.getLatLonBoundingBox()));

        List<Style> styles = selectedLayer.getStyles();

        styleList = new JComboBox(styles.toArray(new Style[styles.size()]));
        styleList.setSelectedItem(context.getPropertyValue(WmsLayerSource.PROPERTY_SELECTED_STYLE));
        styleList.setRenderer(new StyleListCellRenderer());
        styleList.addItemListener(new StyleItemListener());

        JPanel panel2 = new JPanel(new BorderLayout(4, 4));
        panel2.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel2.add(new JLabel("Style:"), BorderLayout.WEST);
        panel2.add(styleList, BorderLayout.EAST);

        JPanel panel3 = new JPanel(new BorderLayout(4, 4));
        panel3.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel3.add(new JLabel(String.format("<html><b>%s</b></html>", selectedLayer.getTitle())), BorderLayout.CENTER);
        panel3.add(panel2, BorderLayout.EAST);

        mapPanel = new JPanel(new BorderLayout());
        messageLabel = new JLabel();
        messageLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        messageLabel.setVerticalTextPosition(SwingConstants.CENTER);
        mapPanel.add(messageLabel, BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel.add(panel3, BorderLayout.NORTH);
        panel.add(mapPanel, BorderLayout.CENTER);
        panel.add(infoLabel, BorderLayout.SOUTH);
        panel.addAncestorListener(new AncestorListener() {

            @Override
            public void ancestorAdded(AncestorEvent event) {
                if (mapPanel.getComponent(0) instanceof JLabel) {
                    updatePreview();
                }
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
                cancelPreviewWorker();
            }
        }
        );

        return panel;
    }

    @Override
    public boolean performFinish() {
        WmsLayerSource.insertWmsLayer(getContext());
        return true;
    }

    @Override
    public void performCancel() {
        cancelPreviewWorker();
        super.performCancel();
    }

    private void updatePreview() {
        cancelPreviewWorker();
        showMessage("<html><i>Loading map...</i></html>");

        CRSEnvelope crsEnvelope = (CRSEnvelope) getContext().getPropertyValue(WmsLayerSource.PROPERTY_CRS_ENVELOPE);
        previewWorker = new WmsPreviewWorker(getPreviewSize(crsEnvelope), getContext());
        previewWorker.execute();

        // todo - AppContext.addWorker(previewWorker);  (nf)
    }

    private void cancelPreviewWorker() {
        if (previewWorker != null && !previewWorker.isDone()) {
            try {
                previewWorker.cancel(true);
            } catch (Throwable ignore) {
                // ok
            }
        }
    }

    private Dimension getPreviewSize(CRSEnvelope crsEnvelope) {
        Dimension preferredSize = messageLabel.getSize();
        if (preferredSize.width == 0 || preferredSize.height == 0) {
            preferredSize = new Dimension(400, 200);
        }
        return getPreviewImageSize(preferredSize, crsEnvelope);
    }

    private Dimension getPreviewImageSize(Dimension preferredSize, CRSEnvelope crsEnvelope) {
        int width;
        int height;
        double ratio = (crsEnvelope.getMaxX() - crsEnvelope.getMinX()) / (crsEnvelope.getMaxY() - crsEnvelope.getMinY());
        if (ratio >= 1.0) {
            width = preferredSize.width;
            height = (int) Math.round(preferredSize.width / ratio);
        } else {
            width = (int) Math.round(preferredSize.height * ratio);
            height = preferredSize.height;
        }
        return new Dimension(width, height);
    }

    private void showMessage(String message) {
        messageLabel.setIcon(null);
        messageLabel.setText(message);
        mapPanel.removeAll();
        mapPanel.add(messageLabel, BorderLayout.CENTER);
        mapPanel.repaint();
    }

    private void showLayerCanvas(LayerCanvas canvas) {
        mapPanel.removeAll();
        mapPanel.add(canvas, BorderLayout.CENTER);
        mapPanel.validate();
    }

    private class StyleItemListener implements ItemListener {

        @Override
        public void itemStateChanged(ItemEvent e) {
            getContext().setPropertyValue(WmsLayerSource.PROPERTY_SELECTED_STYLE, styleList.getSelectedItem());
            getContext().updateState();
            updatePreview();
        }
    }

    private static class StyleListCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            String text = null;
            if (value != null) {
                Style style = (Style) value;
                InternationalString title = style.getTitle();
                text = title.toString();
            }
            label.setText(text);
            return label;
        }
    }

    private class WmsPreviewWorker extends WmsWorker {


        private WmsPreviewWorker(Dimension size, LayerSourcePageContext pageContext) {
            super(size, pageContext);
        }

        @Override
        protected void done() {
            try {
                error = null;
                final com.bc.ceres.glayer.Layer layer = get();
                final LayerCanvas layerCanvas = new LayerCanvas(layer);
                layerCanvas.getViewport().setModelYAxisDown(false);

                showLayerCanvas(layerCanvas);
            } catch (ExecutionException e) {
                error = e.getCause();
                showMessage(String.format("<html><b>Error:</b> <i>%s</i></html>", error.getMessage()));
                e.printStackTrace();
            } catch (InterruptedException ignored) {
                // ok
            } finally {
                getContext().updateState();
            }
        }

    }


}
