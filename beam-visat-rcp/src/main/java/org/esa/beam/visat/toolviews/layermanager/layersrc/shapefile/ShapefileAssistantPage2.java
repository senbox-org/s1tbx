package org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile;

import com.bc.ceres.glayer.Layer;

import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.toolviews.layermanager.layersrc.AbstractLayerSourceAssistantPage;
import org.esa.beam.visat.toolviews.layermanager.layersrc.LayerSourcePageContext;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.DefaultMapContext;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.Style;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.InternationalString;

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

class ShapefileAssistantPage2 extends AbstractLayerSourceAssistantPage {

    private JComboBox styleList;
    private JLabel mapCanvas;
    private SwingWorker<BufferedImage, Object> worker;
    private Throwable error;

    ShapefileAssistantPage2() {
        super("Layer Preview");
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
        
        LayerSourcePageContext context = getContext();
        String fileName = (String) context.getPropertyValue(ShapefileLayerSource.PROPERTY_FILE_NAME);
        ReferencedEnvelope featureSourceEnvelope = (ReferencedEnvelope) context.getPropertyValue(ShapefileLayerSource.PROPERTY_FEATURE_SOURCE_ENVELOPE);
        Style[] styles = (Style[]) context.getPropertyValue(ShapefileLayerSource.PROPERTY_STYLES);
        Style selectedStyle = (Style) context.getPropertyValue(ShapefileLayerSource.PROPERTY_SELECTED_STYLE);

        JLabel infoLabel = new JLabel(featureSourceEnvelope.toString());

        styleList = new JComboBox(styles);
        styleList.setSelectedItem(selectedStyle);
        styleList.setRenderer(new StyleListCellRenderer());
        styleList.addItemListener(new StyleSelectionListener());

        JPanel panel2 = new JPanel(new BorderLayout(4, 4));
        panel2.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel2.add(new JLabel("Style:"), BorderLayout.WEST);
        panel2.add(styleList, BorderLayout.EAST);

        JPanel panel3 = new JPanel(new BorderLayout(4, 4));
        panel3.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel3.add(new JLabel(String.format("<html><b>%s</b>", fileName)), BorderLayout.CENTER);
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

    @Override
    public boolean performFinish() {
        LayerSourcePageContext context = getContext();
        String fileName = (String) context.getPropertyValue(ShapefileLayerSource.PROPERTY_FILE_NAME);
        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = 
            (FeatureCollection<SimpleFeatureType, SimpleFeature>) context.getPropertyValue(ShapefileLayerSource.PROPERTY_FEATURE_COLLECTION);
        Style selectedStyle = (Style) context.getPropertyValue(ShapefileLayerSource.PROPERTY_SELECTED_STYLE);
        
        FeatureLayer featureLayer = new FeatureLayer(featureCollection, selectedStyle);
        featureLayer.setName(fileName);
        featureLayer.setVisible(true);

        ProductSceneView sceneView = getContext().getAppContext().getSelectedProductSceneView();
        final Layer rootLayer = getContext().getLayerContext().getRootLayer();
        rootLayer.getChildren().add(sceneView.getFirstImageLayerIndex(), featureLayer);
        return true;
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
        getContext().getWindow().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        ReferencedEnvelope featureSourceEnvelope = (ReferencedEnvelope) getContext().getPropertyValue(ShapefileLayerSource.PROPERTY_FEATURE_SOURCE_ENVELOPE);
        worker = new ShapefileLoader(computeMapSize(featureSourceEnvelope));
        worker.execute();
    }

    private Dimension computeMapSize(ReferencedEnvelope bbox) {
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

    private class StyleSelectionListener implements ItemListener {

        @Override
        public void itemStateChanged(ItemEvent e) {
            LayerSourcePageContext context = getContext();
            context.setPropertyValue(ShapefileLayerSource.PROPERTY_SELECTED_STYLE, styleList.getSelectedItem());
            context.updateState();
            updateMap();
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
                InternationalString title = style.getDescription().getTitle();
                text = title.toString();
            }
            label.setText(text);
            return label;
        }
    }

    private class ShapefileLoader extends SwingWorker<BufferedImage, Object> {

        private final Dimension size;

        ShapefileLoader(Dimension size) {
            this.size = size;
        }

        @Override
        protected BufferedImage doInBackground() throws Exception {
            LayerSourcePageContext context = getContext();
            FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = 
                (FeatureCollection<SimpleFeatureType, SimpleFeature>) context.getPropertyValue(ShapefileLayerSource.PROPERTY_FEATURE_COLLECTION);
            ReferencedEnvelope featureSourceEnvelope = (ReferencedEnvelope) context.getPropertyValue(ShapefileLayerSource.PROPERTY_FEATURE_SOURCE_ENVELOPE);
            Style selectedStyle = (Style) context.getPropertyValue(ShapefileLayerSource.PROPERTY_SELECTED_STYLE);
            SimpleFeatureType schema = featureCollection.getSchema();
            
            final CoordinateReferenceSystem crs = schema.getGeometryDescriptor().getCoordinateReferenceSystem();

            final DefaultMapContext mapContext = new DefaultMapContext(crs);
            mapContext.addLayer(featureCollection, selectedStyle);
            final StreamingRenderer renderer = new StreamingRenderer();
            renderer.setContext(mapContext);
            final BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_4BYTE_ABGR);
            final Graphics2D graphics2D = image.createGraphics();
            try {
                renderer.paint(graphics2D, new Rectangle(0, 0, size.width, size.height), featureSourceEnvelope);
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