package org.esa.beam.visat.toolviews.layermanager.layersrc.wms;

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.assistant.AbstractAppAssistantPage;
import org.esa.beam.framework.ui.assistant.AppAssistantPageContext;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.geotools.data.ows.CRSEnvelope;
import org.geotools.data.ows.Layer;
import org.opengis.layer.Style;
import org.opengis.util.InternationalString;

import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.ExecutionException;

class WmsAssistantPage3 extends AbstractAppAssistantPage {

    private JComboBox styleList;
    private JLabel previewCanvas;
    private SwingWorker<BufferedImage, Object> previewWorker;
    private Throwable error;
    private final WmsModel wmsModel;

    WmsAssistantPage3(WmsModel wmsModel) {
        super("Layer Preview");
        this.wmsModel = wmsModel;
    }

    @Override
    public boolean validatePage() {
        return error == null;
    }

    @Override
    public Component createLayerPageComponent(final AppAssistantPageContext context) {
        previewCanvas = new JLabel();
        previewCanvas.setHorizontalTextPosition(SwingConstants.CENTER);
        previewCanvas.setVerticalTextPosition(SwingConstants.CENTER);
        Layer selectedLayer = wmsModel.getSelectedLayer();
        JLabel infoLabel = new JLabel(WmsAssistantPage2.getLatLonBoundingBoxText(selectedLayer.getLatLonBoundingBox()));

        List<Style> styles = selectedLayer.getStyles();

        styleList = new JComboBox(styles.toArray(new Style[styles.size()]));
        styleList.setSelectedItem(wmsModel.getSelectedStyle());
        styleList.setRenderer(new MyDefaultListCellRenderer());
        styleList.addItemListener(new MyItemListener(context));

        JPanel panel2 = new JPanel(new BorderLayout(4, 4));
        panel2.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel2.add(new JLabel("Style:"), BorderLayout.WEST);
        panel2.add(styleList, BorderLayout.EAST);

        JPanel panel3 = new JPanel(new BorderLayout(4, 4));
        panel3.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel3.add(new JLabel(String.format("<html><b>%s</b></html>", selectedLayer.getTitle())), BorderLayout.CENTER);
        panel3.add(panel2, BorderLayout.EAST);

        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel.add(panel3, BorderLayout.NORTH);
        panel.add(previewCanvas, BorderLayout.CENTER);
        panel.add(infoLabel, BorderLayout.SOUTH);
        panel.addAncestorListener(new AncestorListener() {

            @Override
            public void ancestorAdded(AncestorEvent event) {
                if (previewCanvas.getIcon() == null) {
                    updatePreview(context);
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
    public boolean performFinish(AppAssistantPageContext pageContext) {
        ProductSceneView view = pageContext.getAppContext().getSelectedProductSceneView();
        RasterDataNode raster = view.getRaster();

        WmsLayerWorker layerWorker = new WmsLayerWorker(view.getRootLayer(),
                                                        raster,
                                                        wmsModel,
                                                        pageContext);
        layerWorker.execute();   // todo - don't close dialog before image is downloaded! (nf)
        return true;
    }

    private void updatePreview(AppAssistantPageContext pageContext) {
        cancelPreviewWorker();
        previewCanvas.setText("<html><i>Loading map...</i></html>");
        previewCanvas.setIcon(null);

        previewWorker = new WmsPreviewWorker(getPreviewSize(), wmsModel, pageContext);
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

    private Dimension getPreviewSize() {
        Dimension preferredSize = previewCanvas.getSize();
        if (preferredSize.width == 0 || preferredSize.height == 0) {
            preferredSize = new Dimension(400, 200);
        }
        return getPreviewImageSize(preferredSize);
    }

    private Dimension getPreviewImageSize(Dimension preferredSize) {
        int width, height;
        CRSEnvelope crsEnvelope = wmsModel.getCrsEnvelope();
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

    private class MyItemListener implements ItemListener {

        private final AppAssistantPageContext pageContext;

        public MyItemListener(AppAssistantPageContext pageContext) {
            this.pageContext = pageContext;
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            wmsModel.setSelectedStyle((Style) styleList.getSelectedItem());
            pageContext.updateState();
            updatePreview(pageContext);
        }
    }

    private static class MyDefaultListCellRenderer extends DefaultListCellRenderer {

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

        private final AppAssistantPageContext pageContext;

        private WmsPreviewWorker(Dimension size, WmsModel wmsModel, AppAssistantPageContext pageContext) {
            super(size, wmsModel);
            this.pageContext = pageContext;
        }

        @Override
        protected void done() {
            try {
                error = null;
                previewCanvas.setIcon(new ImageIcon(get()));
                previewCanvas.setText(null);
            } catch (ExecutionException e) {
                error = e.getCause();
                previewCanvas.setIcon(null);
                previewCanvas.setText(String.format("<html><b>Error:</b> <i>%s</i></html>", error.getMessage()));
            } catch (InterruptedException ignored) {
                // ok
            }
            pageContext.updateState();
        }

    }


}
