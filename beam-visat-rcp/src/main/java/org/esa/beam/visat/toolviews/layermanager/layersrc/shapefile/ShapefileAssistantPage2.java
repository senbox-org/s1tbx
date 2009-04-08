package org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile;

import com.bc.ceres.glayer.Layer;
import com.vividsolutions.jts.geom.Geometry;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.toolviews.layermanager.layersrc.AbstractLayerSourceAssistantPage;
import org.esa.beam.visat.toolviews.layermanager.layersrc.FeatureCollectionClipper;
import org.esa.beam.visat.toolviews.layermanager.layersrc.LayerSourcePageContext;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.DefaultMapContext;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.Style;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.InternationalString;

import javax.swing.DefaultComboBoxModel;
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
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

class ShapefileAssistantPage2 extends AbstractLayerSourceAssistantPage {

    private JComboBox styleList;
    private JLabel mapCanvas;
    private SwingWorker<BufferedImage, Object> worker;
    private boolean shapeFileLoaded;
    private JLabel infoLabel;

    ShapefileAssistantPage2() {
        super("Layer Preview");
        shapeFileLoaded = false;
    }

    @Override
    public boolean validatePage() {
        return shapeFileLoaded;
    }

    @Override
    public Component createPageComponent() {
        mapCanvas = new JLabel();
        mapCanvas.setHorizontalTextPosition(SwingConstants.CENTER);
        mapCanvas.setVerticalTextPosition(SwingConstants.CENTER);

        LayerSourcePageContext context = getContext();
        String filePath = (String) context.getPropertyValue(ShapefileLayerSource.PROPERTY_FILE_PATH);
        String fileName = new File(filePath).getName();

        infoLabel = new JLabel();

        styleList = new JComboBox();
        styleList.setRenderer(new StyleListCellRenderer());
        styleList.addItemListener(new StyleSelectionListener());
        styleList.setPreferredSize(new Dimension(100, styleList.getPreferredSize().height));

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
        String filePath = (String) context.getPropertyValue(ShapefileLayerSource.PROPERTY_FILE_PATH);
        String fileName = new File(filePath).getName();
        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection =
                (FeatureCollection<SimpleFeatureType, SimpleFeature>) context.getPropertyValue(
                        ShapefileLayerSource.PROPERTY_FEATURE_COLLECTION);
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
        final LayerSourcePageContext context = getContext();
        context.getWindow().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        shapeFileLoaded = false;
        context.updateState();

        worker = new ShapefileLoader();
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

        @Override
        protected BufferedImage doInBackground() throws Exception {
            LayerSourcePageContext context = getContext();

            Product targetProduct = context.getAppContext().getSelectedProductSceneView().getProduct();
            CoordinateReferenceSystem targetCrs = targetProduct.getGeoCoding().getModelCRS();
            final Geometry clipGeometry = ShapefileAssistantPage1.createProductGeometry(targetProduct);

            File file = new File((String) context.getPropertyValue(ShapefileLayerSource.PROPERTY_FILE_PATH));
            FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = getFeatureCollection(file,
                                                                                                         targetCrs,
                                                                                                         clipGeometry);
            ReferencedEnvelope refEnvelope = getFeatureSourceEnvelope(targetCrs, featureCollection);
            Style[] styles = getStyles(file, featureCollection);
            Style selectedStyle = getSelectedStyle(styles);


            SimpleFeatureType schema = featureCollection.getSchema();
            final CoordinateReferenceSystem crs = schema.getGeometryDescriptor().getCoordinateReferenceSystem();
            final DefaultMapContext mapContext = new DefaultMapContext(crs);
            mapContext.addLayer(featureCollection, selectedStyle);
            final StreamingRenderer renderer = new StreamingRenderer();
            renderer.setContext(mapContext);
            final Dimension size = computeMapSize(refEnvelope);
            final BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_4BYTE_ABGR);
            final Graphics2D graphics2D = image.createGraphics();
            try {
                renderer.paint(graphics2D, new Rectangle(0, 0, size.width, size.height), refEnvelope);
            } finally {
                graphics2D.dispose();
            }
            return image;
        }

        @Override
        protected void done() {
            final LayerSourcePageContext context = getContext();
            context.getWindow().setCursor(Cursor.getDefaultCursor());

            try {
                BufferedImage image = get();
                ImageIcon icon = new ImageIcon(image);
                mapCanvas.setText(null);
                mapCanvas.setIcon(icon);
                ReferencedEnvelope refEnvelope = (ReferencedEnvelope) context.getPropertyValue(
                        ShapefileLayerSource.PROPERTY_FEATURE_SOURCE_ENVELOPE);
                infoLabel.setText(getLabelString(refEnvelope));

                Style[] styles = (Style[]) context.getPropertyValue(ShapefileLayerSource.PROPERTY_STYLES);
                Style selectedStyle = (Style) context.getPropertyValue(ShapefileLayerSource.PROPERTY_SELECTED_STYLE);
                styleList.setModel(new DefaultComboBoxModel(styles));
                styleList.setSelectedItem(selectedStyle);
                shapeFileLoaded = true;
            } catch (ExecutionException e) {
                final String errorMessage = MessageFormat.format("<html><b>Error:</b> <i>{0}</i></html>",
                                                                 e.getMessage());
                mapCanvas.setText(errorMessage);
                mapCanvas.setIcon(null);
            } catch (InterruptedException ignore) {
                // ok
            } finally {
                context.updateState();
            }
        }

        public String getLabelString(ReferencedEnvelope refEnvelope) {
            final String prefix = String.format("%s [", refEnvelope.getClass().getSimpleName());
            final StringBuilder buffer = new StringBuilder(prefix);
            final int dimension = refEnvelope.getDimension();

            for (int i = 0; i < dimension; i++) {
                if (i != 0) {
                    buffer.append(", ");
                }

                final String values = String.format("%.3f : %.3f", refEnvelope.getMinimum(i),
                                                    refEnvelope.getMaximum(i));
                buffer.append(values);
            }

            return buffer.append(']').toString();
        }

        private Style getSelectedStyle(Style[] styles) {
            LayerSourcePageContext context = getContext();
            Style selectedStyle;
            selectedStyle = (Style) context.getPropertyValue(ShapefileLayerSource.PROPERTY_SELECTED_STYLE);
            if (selectedStyle == null) {
                selectedStyle = styles[0];
                context.setPropertyValue(ShapefileLayerSource.PROPERTY_SELECTED_STYLE, styles[0]);
            }
            return selectedStyle;
        }

        private Style[] getStyles(File file, FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection) {
            LayerSourcePageContext context = getContext();
            Style[] styles;
            styles = (Style[]) context.getPropertyValue(ShapefileLayerSource.PROPERTY_STYLES);
            if (styles == null) {
                styles = ShapefileAssistantPage1.createStyle(file, featureCollection.getSchema());
                context.setPropertyValue(ShapefileLayerSource.PROPERTY_STYLES, styles);
            }
            return styles;
        }


        private ReferencedEnvelope getFeatureSourceEnvelope(CoordinateReferenceSystem targetCrs,
                                                            FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection) {
            ReferencedEnvelope refEnvelope;
            refEnvelope = (ReferencedEnvelope) getContext().getPropertyValue(
                    ShapefileLayerSource.PROPERTY_FEATURE_SOURCE_ENVELOPE);
            if (refEnvelope == null) {
                refEnvelope = new ReferencedEnvelope(featureCollection.getBounds(), targetCrs);
            }
            getContext().setPropertyValue(ShapefileLayerSource.PROPERTY_FEATURE_SOURCE_ENVELOPE, refEnvelope);
            return refEnvelope;
        }

        private FeatureCollection<SimpleFeatureType, SimpleFeature> getFeatureCollection(
                File file, CoordinateReferenceSystem targetCrs, Geometry clipGeometry) throws IOException {
            LayerSourcePageContext context = getContext();

            FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection;
            featureCollection = (FeatureCollection<SimpleFeatureType, SimpleFeature>) context.getPropertyValue(
                    ShapefileLayerSource.PROPERTY_FEATURE_COLLECTION);
            if (featureCollection != null) {
                return featureCollection;
            }

            FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = getFeatureSource(file);
            featureCollection = featureSource.getFeatures();

            featureCollection = FeatureCollectionClipper.doOperation(featureCollection, clipGeometry, targetCrs);
            context.setPropertyValue(ShapefileLayerSource.PROPERTY_FEATURE_COLLECTION, featureCollection);
            return featureCollection;
        }

        private FeatureSource<SimpleFeatureType, SimpleFeature> getFeatureSource(File file) throws IOException {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put(ShapefileDataStoreFactory.URLP.key, file.toURI().toURL());
            map.put(ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key, Boolean.TRUE);
            DataStore shapefileStore = DataStoreFinder.getDataStore(map);

            String typeName = shapefileStore.getTypeNames()[0]; // Shapefiles do only have one type name
            FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;
            featureSource = shapefileStore.getFeatureSource(typeName);
            return featureSource;
        }
    }
}