package org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile;

import com.bc.ceres.glayer.Layer;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import org.esa.beam.framework.ui.assistant.AbstractAppAssistantPage;
import org.esa.beam.framework.ui.assistant.AppAssistantPageContext;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.DefaultMapContext;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.SLD;
import org.geotools.styling.SLDParser;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.FilterFactory;
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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.concurrent.ExecutionException;

class ShapefileAssistantPage2 extends AbstractAppAssistantPage {

    private static final org.geotools.styling.StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);
    private static final FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory(null);

    private final File file;
    private final FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection;
    private final ReferencedEnvelope featureSourceEnvelope;
    private final SimpleFeatureType schema;
    private final Style[] styles;

    private JComboBox styleList;
    private org.geotools.styling.Style selectedStyle;
    private JLabel mapCanvas;
    private JLabel infoLabel;
    private SwingWorker worker;
    private Throwable error;


    ShapefileAssistantPage2(File file,
                   final FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection,
                   final ReferencedEnvelope featureSourceEnvelope, final SimpleFeatureType schema,
                   final org.geotools.styling.Style[] styles) {
        super("Layer Preview");
        this.file = file;
        this.featureCollection = featureCollection;
        this.featureSourceEnvelope = featureSourceEnvelope;
        this.schema = schema;
        this.styles = styles.clone();
    }

    @Override
    public boolean validatePage() {
        return error == null;
    }

    @Override
    public boolean canFinish() {
        return true;
    }

    protected Component createLayerPageComponent(AppAssistantPageContext context) {
        mapCanvas = new JLabel();
        mapCanvas.setHorizontalTextPosition(SwingConstants.CENTER);
        mapCanvas.setVerticalTextPosition(SwingConstants.CENTER);
        infoLabel = new JLabel(featureSourceEnvelope.toString());

        if (styles.length > 0) {
            selectedStyle = styles[0];
        } else {
            selectedStyle = null;
        }
        styleList = new JComboBox(styles);
        styleList.setSelectedItem(selectedStyle);
        styleList.setRenderer(new MyDefaultListCellRenderer());
        styleList.addItemListener(new MyItemListener());

        JPanel panel2 = new JPanel(new BorderLayout(4, 4));
        panel2.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel2.add(new JLabel("Style:"), BorderLayout.WEST);
        panel2.add(styleList, BorderLayout.EAST);

        JPanel panel3 = new JPanel(new BorderLayout(4, 4));
        panel3.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel3.add(new JLabel("<html><b>" + file.getName() + "</b>"), BorderLayout.CENTER);
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

    @Override
    public boolean performFinish() {
        Style style = ShapefileLayer.createStyle(file, schema);
        ShapefileLayer shapefileLayer = new ShapefileLayer(featureCollection, style);

        shapefileLayer.setName(file.getName());
        shapefileLayer.setVisible(true);

        final Layer rootLayer = getAppPageContext().getAppContext().getSelectedProductSceneView().getRootLayer();
        rootLayer.getChildren().add(0, shapefileLayer);
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

        worker = new MySwingWorker(computeMapSize(), selectedStyle);
        worker.execute();
    }

    private Dimension computeMapSize() {
        final ReferencedEnvelope bbox = featureSourceEnvelope;
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

        public void itemStateChanged(ItemEvent e) {
            selectedStyle = (org.geotools.styling.Style) styleList.getSelectedItem();
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
                InternationalString title = style.getDescription().getTitle();
                text = title.toString();
            }
            label.setText(text);
            return label;
        }
    }

    private class MySwingWorker extends SwingWorker<BufferedImage, Object> {

        private final Dimension size;
        private final Style style;

        MySwingWorker(Dimension size, org.geotools.styling.Style style) {
            this.size = size;
            this.style = style;
        }

        @Override
        protected BufferedImage doInBackground() throws Exception {
            final CoordinateReferenceSystem crs = schema.getGeometryDescriptor().getCoordinateReferenceSystem();

            final DefaultMapContext mapContext = new DefaultMapContext(crs);
            mapContext.addLayer(featureCollection, style);
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
            getPageContext().updateState();
        }
    }


    static org.geotools.styling.Style[] createStyle(File file, FeatureType schema) {
        File sld = toSLDFile(file);
        if (sld.exists()) {
            final Style[] styles = createFromSLD(sld);
            if (styles.length > 0) {
                return styles;
            }
        }
        Class type = schema.getGeometryDescriptor().getType().getBinding();
        if (type.isAssignableFrom(Polygon.class)
            || type.isAssignableFrom(MultiPolygon.class)) {
            return new Style[]{createPolygonStyle()};
        } else if (type.isAssignableFrom(LineString.class)
                   || type.isAssignableFrom(MultiLineString.class)) {
            return new Style[]{createLineStyle()};
        } else {
            return new Style[]{createPointStyle()};
        }
    }

    // Figure out the URL for the "sld" file
    static File toSLDFile(File file) {
        String filename = file.getAbsolutePath();
        if (filename.endsWith(".shp") || filename.endsWith(".dbf")
            || filename.endsWith(".shx")) {
            filename = filename.substring(0, filename.length() - 4);
            filename += ".sld";
        } else if (filename.endsWith(".SHP") || filename.endsWith(".DBF")
                   || filename.endsWith(".SHX")) {
            filename = filename.substring(0, filename.length() - 4);
            filename += ".SLD";
        }
        return new File(filename);
    }

    static org.geotools.styling.Style[] createFromSLD(File sld) {
        try {
            SLDParser stylereader = new SLDParser(styleFactory, sld.toURI().toURL());
            return stylereader.readXML();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Style[0];
    }

    static org.geotools.styling.Style createPointStyle() {
        PointSymbolizer symbolizer = styleFactory.createPointSymbolizer();
        symbolizer.getGraphic().setSize(filterFactory.literal(1));

        Rule rule = styleFactory.createRule();
        rule.setSymbolizers(new Symbolizer[]{symbolizer});
        FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle();
        fts.setRules(new Rule[]{rule});

        org.geotools.styling.Style style = styleFactory.createStyle();
        style.addFeatureTypeStyle(fts);
        return style;
    }

    static org.geotools.styling.Style createLineStyle() {
        LineSymbolizer symbolizer = styleFactory.createLineSymbolizer();
        SLD.setLineColour(symbolizer, Color.BLUE);
        symbolizer.getStroke().setWidth(filterFactory.literal(1));
        symbolizer.getStroke().setColor(filterFactory.literal(Color.BLUE));

        Rule rule = styleFactory.createRule();
        rule.setSymbolizers(new Symbolizer[]{symbolizer});
        FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle();
        fts.setRules(new Rule[]{rule});

        org.geotools.styling.Style style = styleFactory.createStyle();
        style.addFeatureTypeStyle(fts);
        return style;
    }

    static org.geotools.styling.Style createPolygonStyle() {
        PolygonSymbolizer symbolizer = styleFactory.createPolygonSymbolizer();
        Fill fill = styleFactory.createFill(
                filterFactory.literal("#FFAA00"),
                filterFactory.literal(0.5)
        );
        symbolizer.setFill(fill);
        Rule rule = styleFactory.createRule();
        rule.setSymbolizers(new Symbolizer[]{symbolizer});
        FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle();
        fts.setRules(new Rule[]{rule});

        org.geotools.styling.Style style = styleFactory.createStyle();
        style.addFeatureTypeStyle(fts);
        return style;
    }

}