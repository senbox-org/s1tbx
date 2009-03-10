package org.esa.beam.visat.toolviews.layermanager.layersrc.wms;

import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.assistant.AbstractAppAssistantPage;
import org.esa.beam.framework.ui.assistant.AppAssistantPageContext;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.geotools.data.ows.CRSEnvelope;
import org.geotools.data.ows.Layer;
import org.geotools.data.wms.WebMapServer;
import org.geotools.data.wms.request.GetMapRequest;
import org.geotools.data.wms.response.GetMapResponse;
import org.geotools.ows.ServiceException;
import org.opengis.layer.Style;
import org.opengis.util.InternationalString;

import javax.imageio.ImageIO;
import javax.media.jai.PlanarImage;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;

class WmsAssistantPage3 extends AbstractAppAssistantPage {

    private final WebMapServer wms;
    private final Layer layer;
    private final CRSEnvelope crsEnvelope;
    private JComboBox styleList;
    private Style selectedStyle;
    private JLabel previewCanvas;
    private SwingWorker previewWorker;
    private Throwable error;

    WmsAssistantPage3(WebMapServer wms, Layer layer, CRSEnvelope crsEnvelope) {
        super("Layer Preview");
        this.wms = wms;
        this.layer = layer;
        this.crsEnvelope = crsEnvelope;
    }

    @Override
    public boolean validatePage() {
        return error == null;
    }

    @Override
    public boolean performFinish() {
        cancelPreviewWorker();
        ProductSceneView view = getAppPageContext().getAppContext().getSelectedProductSceneView();
        RasterDataNode raster = view.getRaster();

        WmsLayerWorker layerWorker = new WmsLayerWorker(
                view.getRootLayer(), getFinalImageSize(raster),
                selectedStyle);
        layerWorker.execute();   // todo - don't close dialog before image is downloaded! (nf)
        return true;
    }

    @Override
    protected Component createLayerPageComponent(AppAssistantPageContext context) {
        previewCanvas = new JLabel();
        previewCanvas.setHorizontalTextPosition(SwingConstants.CENTER);
        previewCanvas.setVerticalTextPosition(SwingConstants.CENTER);
        JLabel infoLabel = new JLabel(WmsAssistantPage2.getLatLonBoundingBoxText(layer.getLatLonBoundingBox()));

        List styles = layer.getStyles();
        if (!styles.isEmpty()) {
            selectedStyle = (Style) styles.get(0);
        } else {
            selectedStyle = null;
        }
        styleList = new JComboBox(styles.toArray(new Style[styles.size()]));
        styleList.setSelectedItem(selectedStyle);
        styleList.setRenderer(new MyDefaultListCellRenderer());
        styleList.addItemListener(new MyItemListener());

        JPanel panel2 = new JPanel(new BorderLayout(4, 4));
        panel2.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel2.add(new JLabel("Style:"), BorderLayout.WEST);
        panel2.add(styleList, BorderLayout.EAST);

        JPanel panel3 = new JPanel(new BorderLayout(4, 4));
        panel3.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel3.add(new JLabel(String.format("<html><b>%s</b></html>", layer.getTitle())), BorderLayout.CENTER);
        panel3.add(panel2, BorderLayout.EAST);

        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel.add(panel3, BorderLayout.NORTH);
        panel.add(previewCanvas, BorderLayout.CENTER);
        panel.add(infoLabel, BorderLayout.SOUTH);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                updatePreview();
            }
        });

        return panel;
    }

    private void updatePreview() {
        cancelPreviewWorker();
        previewCanvas.setText("<html><i>Loading map...</i></html>");
        previewCanvas.setIcon(null);

        previewWorker = new WmsPreviewWorker(getPreviewSize(), selectedStyle);
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

    private Dimension getFinalImageSize(RasterDataNode raster) {
        int width, height;
        double ratio = raster.getSceneRasterWidth() / (double) raster.getSceneRasterHeight();
        if (ratio >= 1.0) {
            width = Math.min(1280, raster.getSceneRasterWidth());
            height = (int) Math.round(width / ratio);
        } else {
            height = Math.min(1280, raster.getSceneRasterHeight());
            width = (int) Math.round(height * ratio);
        }
        return new Dimension(width, height);
    }

    private BufferedImage downloadWmsImage(GetMapRequest mapRequest) throws IOException, ServiceException {
        GetMapResponse mapResponse = wms.issueRequest(mapRequest);
        InputStream inputStream = mapResponse.getInputStream();
        try {
            return ImageIO.read(inputStream);
        } finally {
            inputStream.close();
        }
    }


    private class MyItemListener implements ItemListener {

        @Override
        public void itemStateChanged(ItemEvent e) {
            selectedStyle = (Style) styleList.getSelectedItem();
            getPageContext().updateState();
            updatePreview();
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

    private abstract class WmsWorker extends SwingWorker<BufferedImage, Object> {

        private final Dimension size;
        private final Style style;

        private WmsWorker(Dimension size, Style style) {
            this.size = size;
            this.style = style;
        }

        @Override
        protected BufferedImage doInBackground() throws Exception {
            GetMapRequest mapRequest = wms.createGetMapRequest();
            mapRequest.addLayer(layer, style);
            mapRequest.setTransparent(true);
            mapRequest.setDimensions(size.width, size.height);
            mapRequest.setSRS(crsEnvelope.getEPSGCode()); // e.g. "EPSG:4326" = Geographic CRS
            mapRequest.setBBox(crsEnvelope); // todo - adjust crsEnvelope to exactly match dimensions w x h (nf)
            mapRequest.setFormat("image/png");
            return downloadWmsImage(mapRequest);
        }
    }

    private class WmsPreviewWorker extends WmsWorker {

        private WmsPreviewWorker(Dimension size, Style style) {
            super(size, style);
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
            getPageContext().updateState();
        }

    }

    private class WmsLayerWorker extends WmsWorker {

        private final com.bc.ceres.glayer.Layer rootLayer;
        private JDialog dialog;
        private JProgressBar progressBar;

        private WmsLayerWorker(com.bc.ceres.glayer.Layer rootLayer,
                               Dimension size,
                               Style style) {
            super(size, style);
            this.rootLayer = rootLayer;
            progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            dialog = new JDialog(getAppPageContext().getWindow(), "Loading image from WMS...", Dialog.ModalityType.DOCUMENT_MODAL);
            dialog.getContentPane().add(progressBar, BorderLayout.SOUTH);
            dialog.pack();
        }

        @Override
        protected BufferedImage doInBackground() throws Exception {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    Rectangle parentBounds = getPageContext().getWindow().getBounds();
                    Rectangle bounds = dialog.getBounds();
                    dialog.setLocation(parentBounds.x + (parentBounds.width - bounds.width) / 2,
                                       parentBounds.y + (parentBounds.height - bounds.height) / 2);
                    dialog.setVisible(true);
                }
            });

            return super.doInBackground();
        }

        @Override
        protected void done() {
            dialog.dispose();

            try {
                error = null;
                BufferedImage image = get();
                try {
                    ProductSceneView sceneView = getAppPageContext().getAppContext().getSelectedProductSceneView();
                    AffineTransform i2mTransform = sceneView.getRaster().getGeoCoding().getGridToModelTransform();
                    ImageLayer imageLayer = new ImageLayer(PlanarImage.wrapRenderedImage(image), i2mTransform);
                    imageLayer.setName(layer.getName());
                    rootLayer.getChildren().add(sceneView.getFirstImageLayerIndex(), imageLayer);
                } catch (Exception e) {
                    getPageContext().showErrorDialog(e.getMessage());
                }

            } catch (ExecutionException e) {
                getPageContext().showErrorDialog(
                        String.format("Error while expecting WMS response:\n%s", e.getCause().getMessage()));
            } catch (InterruptedException ignored) {
                // ok
            }
        }

    }
}
