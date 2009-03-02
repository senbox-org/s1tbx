package org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.assistant.AbstractAppAssistantPage;
import org.esa.beam.framework.ui.assistant.AppAssistantPageContext;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.visat.toolviews.layermanager.layersrc.FeatureCollectionClipper;
import org.esa.beam.visat.toolviews.layermanager.layersrc.GeoCodingMathTransform;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultDerivedCRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.SingleCRS;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ShapefileAssistantPage extends AbstractAppAssistantPage {

    private JTextField shapefileBox;

    public ShapefileAssistantPage() {
        super("Select ESRI Shapefile");
    }

    @Override
    public boolean validatePage() {
        if (shapefileBox != null) {
            String path = shapefileBox.getText();
            return path != null && !path.trim().isEmpty();
        }

        return false;
    }

    @Override
    public boolean hasNextPage() {
        return true;
    }

    @Override
    public AbstractAppAssistantPage getNextPage(AppAssistantPageContext pageContext) {

        String path = shapefileBox.getText();
        if (path != null && !path.trim().isEmpty()) {
            try {
                Product targetProduct = getAppPageContext().getAppContext().getSelectedProductSceneView().getProduct();
                GeoCoding geoCoding = targetProduct.getGeoCoding();
                SingleCRS targetCrs = new DefaultDerivedCRS("xyz",
                                                          DefaultGeographicCRS.WGS84,
                                                          new GeoCodingMathTransform(geoCoding, GeoCodingMathTransform.Mode.G2P),
                                                          DefaultCartesianCS.DISPLAY);

                File file = new File(path);
                Map<String, Object> map = new HashMap<String, Object>();
                map.put(ShapefileDataStoreFactory.URLP.key, file.toURI().toURL());
                map.put(ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key, true);

                ShapefileDataStore shapefile = new ShapefileDataStoreFactory().createDataStore(map);

                FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = shapefile.getFeatureSource().getFeatures();

                GeometryFactory gf = new GeometryFactory();
                GeoPos[] geoPoses = ProductUtils.createGeoBoundary(targetProduct, 100);
                Coordinate[] coordinates = new Coordinate[geoPoses.length + 1];
                for (int i = 0; i < geoPoses.length; i++) {
                    GeoPos geoPose = geoPoses[i];
                    coordinates[i] = new Coordinate(geoPose.lon, geoPose.lat);
                }
                coordinates[coordinates.length-1] = coordinates[0];

                Geometry clipGeometry = gf.createPolygon(gf.createLinearRing(coordinates), null);
                // todo - reproject features to CRS WGS84 before
                featureCollection = FeatureCollectionClipper.doOperation(featureCollection, clipGeometry, targetCrs);

                ReferencedEnvelope referencedEnvelope = new ReferencedEnvelope(featureCollection.getBounds(), targetCrs);
                return new ShapefileAssistantPage2(file,
                                          featureCollection,
                                          referencedEnvelope,
                                          featureCollection.getSchema(),
                                          ShapefileAssistantPage2.createStyle(file, featureCollection.getSchema()));
            } catch (Exception e) {
                e.printStackTrace();
                getPageContext().showErrorDialog("Failed to load ESRI shapefile:\n" + e.getMessage());
            }
        }

        return null;
    }

    private void dumpAuthorityCodes(String authority) throws FactoryException {
        for (String code : CRS.getSupportedCodes(authority)) {
            if (!code.startsWith("EPSG")) {
                code = "EPSG:" + code;
            }
            try {
                CoordinateReferenceSystem crs = CRS.decode(code);
                System.out.println(code + " --> " + crs.getName());
            } catch (Throwable e) {
                System.out.println(code + " --> " + e.getMessage());
            }
        }
    }

    private static Coordinate[] createCircleCoords(double x0, double y0, double r) {
        final Coordinate[] coordinates = new Coordinate[360 + 1];
        for (int i = 0; i < coordinates.length; i++) {
            coordinates[i] = new Coordinate(x0 + r * Math.cos(Math.toRadians(i % 360)),
                                            y0 + r * Math.sin(Math.toRadians(i % 360)));
        }
        return coordinates;
    }


    @Override
    public boolean canFinish() {
        return false;
    }

    @Override
    protected Component createLayerPageComponent(AppAssistantPageContext context) {
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
        shapefileBox = new JTextField(
                "C:\\Dokumente und Einstellungen\\Marco Peters\\Eigene Dateien\\EOData\\ShapeFiles\\countries\\countries.shp");
        shapefileBox.setEditable(true);
        panel.add(shapefileBox, gbc);

        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        JButton button = new JButton("...");
        button.addActionListener(new MyActionListener());
        panel.add(button, gbc);

        return panel;
    }

    private class MyActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setAcceptAllFileFilterUsed(false);
            final FileNameExtensionFilter shapefileFilter = new FileNameExtensionFilter("ESRI Shapefile", "shp");
            fileChooser.addChoosableFileFilter(shapefileFilter);
            fileChooser.setFileFilter(shapefileFilter);
            fileChooser.showOpenDialog(getPageContext().getWindow());
            if (fileChooser.getSelectedFile() != null) {
                shapefileBox.setText(fileChooser.getSelectedFile().getPath());
                getPageContext().updateState();
            }
        }
    }
}