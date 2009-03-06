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
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.JProgressBar;
import javax.swing.JDialog;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Dialog;
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
    private JLabel mapCanvas;
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
                view.getRootLayer(), new Dimension(raster.getSceneRasterWidth(), raster.getSceneRasterHeight()),
                selectedStyle);
        layerWorker.execute();
        return true;
    }

    @Override
    protected Component createLayerPageComponent(AppAssistantPageContext context) {
        mapCanvas = new JLabel();
        mapCanvas.setHorizontalTextPosition(SwingConstants.CENTER);
        mapCanvas.setVerticalTextPosition(SwingConstants.CENTER);
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
        panel.add(mapCanvas, BorderLayout.CENTER);
        panel.add(infoLabel, BorderLayout.SOUTH);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                updateMap();
            }
        });

        return panel;
    }

    private void updateMap() {
        cancelPreviewWorker();
        mapCanvas.setText("<html><i>Loading map...</i></html>");
        mapCanvas.setIcon(null);

        previewWorker = new WmsPreviewWorker(computeMapSize(), selectedStyle);
        previewWorker.execute();
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

    private Dimension computeMapSize() {
        Dimension preferredSize = mapCanvas.getSize();
        if (preferredSize.width == 0 || preferredSize.height == 0) {
            preferredSize = new Dimension(400, 200);
        }
        return computeMapSize(preferredSize);
    }

    private Dimension computeMapSize(Dimension preferredSize) {
        double aspectRatio = (crsEnvelope.getMaxX() - crsEnvelope.getMinX()) / (crsEnvelope.getMaxY() - crsEnvelope.getMinY());
        Dimension size;
        if (aspectRatio > 1.0) {
            size = new Dimension(preferredSize.width,
                                 (int) Math.round(preferredSize.width / aspectRatio));
        } else {
            size = new Dimension((int) Math.round(preferredSize.height * aspectRatio),
                                 preferredSize.height);
        }
        return size;
    }

    private BufferedImage downloadMapImage(GetMapRequest mapRequest) throws IOException, ServiceException {
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
            updateMap();
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
        private JDialog dialog;
        private JProgressBar progressBar;

        private WmsWorker(Dimension size, Style style) {
            this.size = size;
            this.style = style;
            progressBar = new JProgressBar();
            dialog = new JDialog(getAppPageContext().getWindow(), "Contacting WMS...", Dialog.ModalityType.APPLICATION_MODAL);
            dialog.add(progressBar);
            dialog.pack();
        }

        @Override
        protected BufferedImage doInBackground() throws Exception {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    dialog.setVisible(true);
                    progressBar.setIndeterminate(true);
                }
            });

            GetMapRequest mapRequest = wms.createGetMapRequest();
            mapRequest.addLayer(layer, style);
            mapRequest.setTransparent(true);
            mapRequest.setSRS(crsEnvelope.getEPSGCode()); // e.g. "EPSG:4326" = Geographic CRS
            mapRequest.setBBox(crsEnvelope);
            mapRequest.setDimensions(size.width, size.height);
            mapRequest.setFormat("image/png");
            return downloadMapImage(mapRequest);
        }

        @Override
        protected void done() {
            dialog.dispose();
            doneImpl();
        }

        protected abstract void doneImpl();
    }

    private class WmsPreviewWorker extends WmsWorker {

        private WmsPreviewWorker(Dimension size, Style style) {
            super(size, style);
        }

        @Override
        protected void doneImpl() {
            try {
                error = null;
                BufferedImage image = get();
                ImageIcon icon = new ImageIcon(image);
                mapCanvas.setText(null);
                mapCanvas.setIcon(icon);
            } catch (ExecutionException e) {
                error = e.getCause();
                mapCanvas.setText(String.format("<html><b>Error:</b> <i>%s</i></html>", error.getMessage()));
                mapCanvas.setIcon(null);
            } catch (InterruptedException ignored) {
                // ok
            }
            getPageContext().updateState();
        }

    }

    private class WmsLayerWorker extends WmsWorker {

        private final com.bc.ceres.glayer.Layer rootLayer;

        private WmsLayerWorker(com.bc.ceres.glayer.Layer rootLayer,
                               Dimension size,
                               Style style) {
            super(size, style);
            this.rootLayer = rootLayer;
        }

        @Override
        protected void doneImpl() {
            try {
                error = null;
                BufferedImage image = get();
                try {
// todo - giving the ImageLayer a BufferedImage results in the following exception (mp - 06.03.2009)
// Exception in thread "AWT-EventQueue-0" java.lang.ClassCastException: java.awt.image.BufferedImage cannot be cast to javax.media.jai.PlanarImage
//    at com.bc.ceres.glevel.support.ConcurrentMultiLevelRenderer.renderImpl(ConcurrentMultiLevelRenderer.java:66)
//    at com.bc.ceres.glevel.support.ConcurrentMultiLevelRenderer.renderImage(ConcurrentMultiLevelRenderer.java:56)
//    at com.bc.ceres.glayer.support.ImageLayer.renderLayer(ImageLayer.java:180)
                    AffineTransform i2mTransform = getAppPageContext().getAppContext().getSelectedProductSceneView().getRaster().getGeoCoding().getGridToModelTransform();
                    ImageLayer imageLayer = new ImageLayer(PlanarImage.wrapRenderedImage(image), i2mTransform);
                    imageLayer.setName(layer.getName());
                    rootLayer.getChildren().add(0, imageLayer);
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
