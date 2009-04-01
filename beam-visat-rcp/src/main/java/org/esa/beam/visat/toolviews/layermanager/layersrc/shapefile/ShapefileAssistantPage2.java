package org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile;

import com.bc.ceres.glayer.Layer;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.toolviews.layermanager.layersrc.AbstractLayerSourceAssistantPage;
import org.esa.beam.visat.toolviews.layermanager.layersrc.LayerSourcePageContext;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.DefaultMapContext;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.Style;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.InternationalString;

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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.text.MessageFormat;
import java.util.concurrent.ExecutionException;

class ShapefileAssistantPage2 extends AbstractLayerSourceAssistantPage {

    private JComboBox styleList;
    private JLabel mapCanvas;
    private SwingWorker worker;
    private Throwable error;
    private final ShapefileModel model;


    ShapefileAssistantPage2(ShapefileModel model) {
        super("Layer Preview");
        this.model = model;
    }

    @Override
    public boolean validatePage() {
        return error == null;
    }

    @Override
    public Component createPageComponent() {
        mapCanvas = new JLabel();
        mapCanvas.setHorizontalTextPosition(SwingConstants.CENTER);
        mapCanvas.setVerticalTextPosition(SwingConstants.CENTER);
        JLabel infoLabel = new JLabel(model.getFeatureSourceEnvelope().toString());

        styleList = new JComboBox(model.getStyles());
        styleList.setSelectedItem(model.getSelectedStyle());
        styleList.setRenderer(new MyDefaultListCellRenderer());
        styleList.addItemListener(new MyItemListener());

        JPanel panel2 = new JPanel(new BorderLayout(4, 4));
        panel2.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel2.add(new JLabel("Style:"), BorderLayout.WEST);
        panel2.add(styleList, BorderLayout.EAST);

        JPanel panel3 = new JPanel(new BorderLayout(4, 4));
        panel3.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel3.add(new JLabel(String.format("<html><b>%s</b>", model.getFile().getName())), BorderLayout.CENTER);
        panel3.add(panel2, BorderLayout.EAST);

        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel.add(panel3, BorderLayout.NORTH);
        panel.add(mapCanvas, BorderLayout.CENTER);
        panel.add(infoLabel, BorderLayout.SOUTH);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                updateMap(getContext());
            }
        });

        return panel;
    }


    @Override
    public boolean performFinish() {
        FeatureLayer featureLayer = new FeatureLayer(model.getFeatureCollection(), model.getSelectedStyle());
        featureLayer.setName(model.getFile().getName());
        featureLayer.setVisible(true);

        ProductSceneView sceneView = getContext().getAppContext().getSelectedProductSceneView();
        final Layer rootLayer = getContext().getLayerContext().getRootLayer();
        rootLayer.getChildren().add(sceneView.getFirstImageLayerIndex(), featureLayer);
        return true;
    }

    private void updateMap(LayerSourcePageContext pageContext) {
        if (worker != null && !worker.isDone()) {
            try {
                worker.cancel(true);
            } catch (Throwable ignore) {
                // ok
            }
        }
        mapCanvas.setText("<html><i>Loading map...</i></html>");
        mapCanvas.setIcon(null);
        pageContext.getWindow().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        worker = new ShapefileLoader(computeMapSize(), model.getSelectedStyle());
        worker.execute();
    }

    private Dimension computeMapSize() {
        final ReferencedEnvelope bbox = model.getFeatureSourceEnvelope();
        double aspectRatio = (bbox.getMaxX() - bbox.getMinX()) / (bbox.getMaxY() - bbox.getMinY());
        Dimension preferredSize = mapCanvas.getSize();
        if (preferredSize.width == 0 || preferredSize.height == 0) {
            preferredSize = new Dimension(400, 200);
        }
        if (aspectRatio > 1.0) {
            return new Dimension(preferredSize.width, (int) Math.round(preferredSize.width / aspectRatio));
        } else {
            return new Dimension((int) Math.round(preferredSize.height * aspectRatio), preferredSize.height);
        }
    }

    private class MyItemListener implements ItemListener {

        @Override
        public void itemStateChanged(ItemEvent e) {
            model.setSelectedStyle((org.geotools.styling.Style) styleList.getSelectedItem());
            getContext().updateState();
            updateMap(getContext());
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
                InternationalString title = style.getDescription().getTitle();
                text = title.toString();
            }
            label.setText(text);
            return label;
        }
    }

    private class ShapefileLoader extends SwingWorker<BufferedImage, Object> {

        private final Dimension size;
        private final Style style;

        ShapefileLoader(Dimension size, org.geotools.styling.Style style) {
            this.size = size;
            this.style = style;
        }

        @Override
        protected BufferedImage doInBackground() throws Exception {
            final CoordinateReferenceSystem crs = model.getSchema().getGeometryDescriptor().getCoordinateReferenceSystem();

            final DefaultMapContext mapContext = new DefaultMapContext(crs);
            mapContext.addLayer(model.getFeatureCollection(), style);
            final StreamingRenderer renderer = new StreamingRenderer();
            renderer.setContext(mapContext);
            final BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_4BYTE_ABGR);
            final Graphics2D graphics2D = image.createGraphics();
            try {
                renderer.paint(graphics2D, new Rectangle(0, 0, size.width, size.height),
                               model.getFeatureSourceEnvelope());
            } finally {
                graphics2D.dispose();
            }
            return image;
        }

        @Override
        protected void done() {
            getContext().getWindow().setCursor(Cursor.getDefaultCursor());

            try {
                error = null;
                BufferedImage image = get();
                ImageIcon icon = new ImageIcon(image);
                mapCanvas.setText(null);
                mapCanvas.setIcon(icon);
            } catch (ExecutionException e) {
                error = e.getCause();
                final String errorMessage = MessageFormat.format("<html><b>Error:</b> <i>{0}</i></html>",
                                                                 error.getMessage());
                mapCanvas.setText(errorMessage);
                mapCanvas.setIcon(null);
            } catch (InterruptedException ignore) {
                // ok
            }
            getContext().updateState();
        }
    }


}