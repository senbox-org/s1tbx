package org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.FileHistory;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.visat.toolviews.layermanager.layersrc.AbstractLayerSourceAssistantPage;
import org.esa.beam.visat.toolviews.layermanager.layersrc.FeatureCollectionClipper;
import org.esa.beam.visat.toolviews.layermanager.layersrc.HistoryComboBoxModel;
import org.esa.beam.visat.toolviews.layermanager.layersrc.LayerSourcePageContext;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
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

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class ShapefileAssistantPage1 extends AbstractLayerSourceAssistantPage {

    private static final String PROPERTY_LAST_FILE_PREFIX = "ShapefileAssistant.Shapefile.history";
    private static final String PROPERTY_LAST_DIR = "ShapefileAssistant.Shapefile.lastDir";
    private static final org.geotools.styling.StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);
    private static final FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory(null);

    private HistoryComboBoxModel fileHistoryModel;


    ShapefileAssistantPage1() {
        super("Select ESRI Shapefile");
    }

    @Override
    public Component createPageComponent() {
        GridBagConstraints gbc = new GridBagConstraints();
        final JPanel panel = new JPanel(new GridBagLayout());

        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;
        panel.add(new JLabel("Path to ESRI Shapefile (*.shp):"), gbc);

        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 1;
        final LayerSourcePageContext context = getContext();
        final PropertyMap preferences = context.getAppContext().getPreferences();
        final FileHistory fileHistory = new FileHistory(5, PROPERTY_LAST_FILE_PREFIX);
        fileHistory.initBy(preferences);
        fileHistoryModel = new HistoryComboBoxModel(fileHistory);
        JComboBox shapefileBox = new JComboBox(fileHistoryModel);
        shapefileBox.setEditable(true);
        shapefileBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                context.updateState();
            }
        });
        panel.add(shapefileBox, gbc);

        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        JButton button = new JButton("...");
        button.addActionListener(new ShpaeFilechooserActionListener());
        panel.add(button, gbc);

        return panel;
    }

    @Override
    public boolean validatePage() {
        if (fileHistoryModel != null) {
            String path = (String) fileHistoryModel.getSelectedItem();
            return path != null && !path.trim().isEmpty();
        }
        return false;
    }

    @Override
    public boolean hasNextPage() {
        return true;
    }

    @Override
    public AbstractLayerSourceAssistantPage getNextPage() {
        final LayerSourcePageContext context = getContext();
        fileHistoryModel.getHistory().copyInto(context.getAppContext().getPreferences());
        String path = (String) fileHistoryModel.getSelectedItem();
        if (path != null && !path.trim().isEmpty()) {
            try {
                Product targetProduct = context.getAppContext().getSelectedProductSceneView().getProduct();

                CoordinateReferenceSystem targetCrs = targetProduct.getGeoCoding().getModelCRS();

                File file = new File(path);
                Map<String, Object> map = new HashMap<String, Object>();
                map.put(ShapefileDataStoreFactory.URLP.key, file.toURI().toURL());
                map.put(ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key, Boolean.TRUE);
                DataStore shapefileStore = DataStoreFinder.getDataStore(map);

                String typeName = shapefileStore.getTypeNames()[0]; // Shapefiles do only have one type name
                FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;
                featureSource = shapefileStore.getFeatureSource(typeName);

                FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection;
                featureCollection = featureSource.getFeatures();

                Geometry clipGeometry = createProductGeometry(targetProduct);
                featureCollection = FeatureCollectionClipper.doOperation(featureCollection, clipGeometry, targetCrs);

                ReferencedEnvelope referencedEnvelope = new ReferencedEnvelope(featureCollection.getBounds(),
                                                                               targetCrs);
                Style[] styles = createStyle(file, featureCollection.getSchema());

                context.setPropertyValue(ShapefileLayerSource.PROPERTY_FILE_NAME, file.getName());
                context.setPropertyValue(ShapefileLayerSource.PROPERTY_FEATURE_COLLECTION, featureCollection);
                context.setPropertyValue(ShapefileLayerSource.PROPERTY_FEATURE_SOURCE_ENVELOPE, referencedEnvelope);
                context.setPropertyValue(ShapefileLayerSource.PROPERTY_STYLES, styles);
                if (styles.length > 0) {
                    context.setPropertyValue(ShapefileLayerSource.PROPERTY_SELECTED_STYLE, styles[0]);
                }
                return new ShapefileAssistantPage2();
            } catch (Exception e) {
                e.printStackTrace();
                context.showErrorDialog("Failed to load ESRI shapefile:\n" + e.getMessage());
            }
        }

        return null;
    }

    @Override
    public boolean canFinish() {
        return false;
    }

    private static Geometry createProductGeometry(Product targetProduct) {
        GeometryFactory gf = new GeometryFactory();
        GeoPos[] geoPoses = ProductUtils.createGeoBoundary(targetProduct, 100);
        Coordinate[] coordinates = new Coordinate[geoPoses.length + 1];
        for (int i = 0; i < geoPoses.length; i++) {
            GeoPos geoPose = geoPoses[i];
            coordinates[i] = new Coordinate(geoPose.lon, geoPose.lat);
        }
        coordinates[coordinates.length - 1] = coordinates[0];

        return gf.createPolygon(gf.createLinearRing(coordinates), null);
    }

    private static Style[] createStyle(File file, FeatureType schema) {
        File sld = toSLDFile(file);
        if (sld.exists()) {
            final Style[] styles = createFromSLD(sld);
            if (styles.length > 0) {
                return styles;
            }
        }
        Class<?> type = schema.getGeometryDescriptor().getType().getBinding();
        if (type.isAssignableFrom(Polygon.class)
            || type.isAssignableFrom(MultiPolygon.class)) {
            return new Style[]{createPolygonStyle()};
        } else if (type.isAssignableFrom(LineString.class)
                   || type.isAssignableFrom(MultiLineString.class)) {
            return new Style[]{createLineStyle()};
        } else {
            return new Style[]{createPointStyle()};
        }
    }// Figure out the URL for the "sld" file

    private static File toSLDFile(File file) {
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

    private static Style[] createFromSLD(File sld) {
        try {
            SLDParser stylereader = new SLDParser(styleFactory, sld.toURI().toURL());
            return stylereader.readXML();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Style[0];
    }

    private static Style createPointStyle() {
        PointSymbolizer symbolizer = styleFactory.createPointSymbolizer();
        symbolizer.getGraphic().setSize(filterFactory.literal(1));

        Rule rule = styleFactory.createRule();
        rule.setSymbolizers(new Symbolizer[]{symbolizer});
        FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle();
        fts.setRules(new Rule[]{rule});

        Style style = styleFactory.createStyle();
        style.addFeatureTypeStyle(fts);
        return style;
    }

    private static Style createLineStyle() {
        LineSymbolizer symbolizer = styleFactory.createLineSymbolizer();
        SLD.setLineColour(symbolizer, Color.BLUE);
        symbolizer.getStroke().setWidth(filterFactory.literal(1));
        symbolizer.getStroke().setColor(filterFactory.literal(Color.BLUE));

        Rule rule = styleFactory.createRule();
        rule.setSymbolizers(new Symbolizer[]{symbolizer});
        FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle();
        fts.setRules(new Rule[]{rule});

        Style style = styleFactory.createStyle();
        style.addFeatureTypeStyle(fts);
        return style;
    }

    private static Style createPolygonStyle() {
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

        Style style = styleFactory.createStyle();
        style.addFeatureTypeStyle(fts);
        return style;
    }

    private class ShpaeFilechooserActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setAcceptAllFileFilterUsed(false);
            final FileNameExtensionFilter shapefileFilter = new FileNameExtensionFilter("ESRI Shapefile", "shp");
            fileChooser.addChoosableFileFilter(shapefileFilter);
            fileChooser.setFileFilter(shapefileFilter);
            File lastDir = getLastDirectory();
            fileChooser.setCurrentDirectory(lastDir);
            LayerSourcePageContext pageContext = getContext();
            fileChooser.showOpenDialog(pageContext.getWindow());
            if (fileChooser.getSelectedFile() != null) {
                String filePath = fileChooser.getSelectedFile().getPath();
                fileHistoryModel.setSelectedItem(filePath);
                PropertyMap preferences = pageContext.getAppContext().getPreferences();
                preferences.setPropertyString(PROPERTY_LAST_DIR, fileChooser.getCurrentDirectory().getAbsolutePath());
                pageContext.updateState();
            }
        }

        private File getLastDirectory() {
            PropertyMap preferences = getContext().getAppContext().getPreferences();
            String dirPath = preferences.getPropertyString(PROPERTY_LAST_DIR, System.getProperty("user.home"));
            File lastDir = new File(dirPath);
            if (!lastDir.isDirectory()) {
                lastDir = new File(System.getProperty("user.home"));
            }
            return lastDir;
        }
    }
}
