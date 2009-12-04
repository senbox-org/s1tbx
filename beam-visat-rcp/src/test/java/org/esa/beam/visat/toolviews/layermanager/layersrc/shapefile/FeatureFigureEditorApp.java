package org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile;

import com.bc.ceres.swing.demo.FigureEditorApp;
import com.bc.ceres.swing.figure.FigureCollection;
import com.bc.ceres.swing.figure.FigureFactory;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import com.vividsolutions.jts.geom.Geometry;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import javax.swing.JOptionPane;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FeatureFigureEditorApp extends FigureEditorApp {

    private FeatureCollection featureCollection;

    public FeatureFigureEditorApp() {
        SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
        DefaultGeographicCRS crs = DefaultGeographicCRS.WGS84;
        ftb.setCRS(crs);
        ftb.setName("X");
        ftb.add("geometry", Geometry.class);
        ftb.setDefaultGeometry("geometry");
        SimpleFeatureType ft = ftb.buildFeatureType();
        this.featureCollection = new DefaultFeatureCollection("X", ft);
    }

    public static void main(String[] args) {
        run(new FeatureFigureEditorApp());
    }

    @Override
    protected FigureFactory getFigureFactory() {
        return new SimpleFeatureFigureFactory(featureCollection);
    }

    @Override
    protected void loadFigureCollection(File file, FigureCollection figureCollection) {
        try {
            FeatureSource<SimpleFeatureType, SimpleFeature> featureFeatureSource;
            FeatureCollection<SimpleFeatureType, SimpleFeature> featureTypeSimpleFeatureFeatureCollection;
            featureFeatureSource = getFeatureSource(file);
            featureTypeSimpleFeatureFeatureCollection = featureFeatureSource.getFeatures();
            Iterator<SimpleFeature> featureIterator = featureTypeSimpleFeatureFeatureCollection.iterator();
            int numFeatures = 0;
            while (featureIterator.hasNext()) {
                SimpleFeature simpleFeature = featureIterator.next();
                numFeatures++;
                System.out.printf("Loaded feature %d%n", numFeatures);
                DefaultFigureStyle figureStyle = SimpleFeatureFigureFactory.createDefaultStyle();
                figureCollection.addFigure(new SimpleFeatureFigure(simpleFeature, figureStyle));
            }
            System.out.printf("Done loading %d features%n", numFeatures);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(getFrame(), "Error: " + e.getMessage());
        }
    }

    @Override
    protected void storeFigureCollection(FigureCollection figureCollection, File file) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public static FeatureSource<SimpleFeatureType, SimpleFeature> getFeatureSource(File file) throws IOException {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(ShapefileDataStoreFactory.URLP.key, file.toURI().toURL());
        map.put(ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key, Boolean.TRUE);
        DataStore shapefileStore = DataStoreFinder.getDataStore(map);
        String typeName = shapefileStore.getTypeNames()[0]; // Shape files do only have one type name
        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;
        featureSource = shapefileStore.getFeatureSource(typeName);
        return featureSource;
    }

}
