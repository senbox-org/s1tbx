package org.esa.beam.visat.toolviews.layermanager.layersrc.wms;

import org.esa.beam.framework.ui.assistant.AbstractAppAssistantPage;
import org.esa.beam.framework.ui.assistant.AppAssistantPageContext;
import org.geotools.data.ows.CRSEnvelope;
import org.geotools.data.ows.Layer;
import org.geotools.data.wms.WebMapServer;
import org.geotools.data.wms.request.GetMapRequest;
import org.geotools.data.wms.response.GetMapResponse;
import org.geotools.ows.ServiceException;
import org.opengis.layer.Style;
import org.opengis.util.InternationalString;

import javax.imageio.ImageIO;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;

class WmsPage3 extends AbstractAppAssistantPage {
    private final WebMapServer wms;
    private final Layer layer;
    private JComboBox styleList;
    private Style selectedStyle;
    private JLabel mapCanvas;
    private JLabel infoLabel;
    private SwingWorker worker;
    private Throwable error;

    WmsPage3(WebMapServer wms, Layer layer) {
        super("Layer Preview");
        this.wms = wms;
        this.layer = layer;
    }

    @Override
    public boolean validatePage() {
        return error == null;
    }

    @Override
    public boolean canFinish() {
        return true;
    }

    @Override
    protected Component createLayerPageComponent(AppAssistantPageContext context) {
        mapCanvas = new JLabel();
        mapCanvas.setHorizontalTextPosition(SwingConstants.CENTER);
        mapCanvas.setVerticalTextPosition(SwingConstants.CENTER);
        infoLabel = new JLabel(WmsPage2.getLatLonBoundingBoxText(layer.getLatLonBoundingBox()));

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
        panel3.add(new JLabel("<html><b>" + layer.getTitle() + "</b></html>"), BorderLayout.CENTER);
        panel3.add(panel2, BorderLayout.EAST);

        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel.add(panel3, BorderLayout.NORTH);
        panel.add(mapCanvas, BorderLayout.CENTER);
        panel.add(infoLabel, BorderLayout.SOUTH);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                updateMap();
            }
        });

        return panel;
    }

    private void updateMap() {
        if (worker != null && !worker.isDone()) {
            try {
                worker.cancel(true);
            } catch (Throwable ignore) {
                // ok
            }
        }
        mapCanvas.setText("<html><i>Loading map...</i></html>");
        mapCanvas.setIcon(null);

        worker = new MySwingWorker(computeMapSize(), selectedStyle);
        worker.execute();
    }

    private Dimension computeMapSize() {
        CRSEnvelope bbox = layer.getLatLonBoundingBox();
        double aspectRatio = (bbox.getMaxX() - bbox.getMinX()) / (bbox.getMaxY() - bbox.getMinY());
        Dimension preferredSize = mapCanvas.getSize();
        if (preferredSize.width == 0 || preferredSize.height == 0) {
            preferredSize = new Dimension(400, 200);
        }
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
        public void itemStateChanged(ItemEvent e) {
            selectedStyle = (Style) styleList.getSelectedItem();
            getPageContext().updateState();
            updateMap();
        }
    }

    private static class MyDefaultListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
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

    private class MySwingWorker extends SwingWorker<BufferedImage, Object> {
        private final Dimension size;
        private final Style style;

        private MySwingWorker(Dimension size, Style style) {
            this.size = size;
            this.style = style;
        }

        @Override
        protected BufferedImage doInBackground() throws Exception {
            GetMapRequest mapRequest = wms.createGetMapRequest();
            mapRequest.addLayer(layer, style);
            mapRequest.setTransparent(true);
            mapRequest.setSRS("EPSG:4326"); // = Geographic CRS
            mapRequest.setBBox(layer.getLatLonBoundingBox());
            mapRequest.setDimensions(size.width, size.height);
            mapRequest.setFormat("image/png");
            return downloadMapImage(mapRequest);
        }

        @Override
        protected void done() {
            try {
                error = null;
                BufferedImage image = get();
                ImageIcon icon = new ImageIcon(image);
                mapCanvas.setText(null);
                mapCanvas.setIcon(icon);
            } catch (ExecutionException e) {
                error = e.getCause();
                mapCanvas.setText("<html><b>Error:</b> <i>" + error.getMessage() + "</i></html>");
                mapCanvas.setIcon(null);
            } catch (InterruptedException e) {
                // ok
            }
            getPageContext().updateState();
        }
    }
}
