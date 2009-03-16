package org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.assistant.AbstractAppAssistantPage;
import org.esa.beam.framework.ui.assistant.AppAssistantPageContext;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.visat.toolviews.layermanager.layersrc.FeatureCollectionClipper;
import org.esa.beam.visat.toolviews.layermanager.layersrc.HistoryComboBoxModel;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
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

    private static final String PROPERTY_LAST_FILE_PREFIX = "ShapefileAssistantPage.Shapefile.history";
    private HistoryComboBoxModel fileHistoryModel;

    public ShapefileAssistantPage() {
        super("Select ESRI Shapefile");
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
    public AbstractAppAssistantPage getNextPage(AppAssistantPageContext pageContext) {
        fileHistoryModel.saveHistory();
        String path = (String) fileHistoryModel.getSelectedItem();
        if (path != null && !path.trim().isEmpty()) {
            try {
                Product targetProduct = getAppPageContext().getAppContext().getSelectedProductSceneView().getProduct();

                CoordinateReferenceSystem targetCrs = targetProduct.getGeoCoding().getModelCRS();

                File file = new File(path);
                Map<String, Object> map = new HashMap<String, Object>();
                map.put(ShapefileDataStoreFactory.URLP.key, file.toURI().toURL());
                map.put(ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key, true);
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
                return new ShapefileAssistantPage2(file,
                                                   featureCollection,
                                                   referencedEnvelope,
                                                   featureCollection.getSchema(),
                                                   ShapefileAssistantPage2.createStyle(file,
                                                                                       featureCollection.getSchema()));
            } catch (Exception e) {
                e.printStackTrace();
                getPageContext().showErrorDialog("Failed to load ESRI shapefile:\n" + e.getMessage());
            }
        }

        return null;
    }

    private Geometry createProductGeometry(Product targetProduct) {
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

        final PropertyMap preferences = context.getAppContext().getPreferences();
        fileHistoryModel = new HistoryComboBoxModel(preferences, PROPERTY_LAST_FILE_PREFIX, 5);
        JComboBox shapefileBox = new JComboBox(fileHistoryModel);
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

            if (fileHistoryModel.getSelectedItem() != null) {
                File file = new File((String) fileHistoryModel.getSelectedItem());
                if (file.isFile()) {
                    fileChooser.setCurrentDirectory(file.getParentFile());
                }
            }

            fileChooser.showOpenDialog(getPageContext().getWindow());
            if (fileChooser.getSelectedFile() != null) {
                String filePath = fileChooser.getSelectedFile().getPath();
                fileHistoryModel.setSelectedItem(filePath);
                getPageContext().updateState();
            }
        }

    }

}
