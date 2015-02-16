package org.esa.beam.statistics.output;

import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class EsriShapeFileWriter {

    private static final String FILE_EXTENSION_SHAPEFILE = ".shp";

    static void write(List<SimpleFeature> features, File file) throws IOException {
        String basename = file.getName();
        if (basename.endsWith(FILE_EXTENSION_SHAPEFILE)) {
            basename = basename.substring(0, basename.length() - 4);
        }
        File shapefile = new File(file.getParentFile(), basename + FILE_EXTENSION_SHAPEFILE);

        ShapefileDataStoreFactory factory = new ShapefileDataStoreFactory();
        Map map = Collections.singletonMap("url", shapefile.toURI().toURL());
        DataStore dataStore = factory.createNewDataStore(map);
        SimpleFeature simpleFeature = features.get(0);
        SimpleFeatureType simpleFeatureType = simpleFeature.getType();
        final String typeName = simpleFeatureType.getName().getLocalPart();
        dataStore.createSchema(simpleFeatureType);
        FeatureStore<SimpleFeatureType, SimpleFeature> featureStore = (FeatureStore<SimpleFeatureType, SimpleFeature>) dataStore.getFeatureSource(typeName);
        DefaultTransaction transaction = new DefaultTransaction("X");
        featureStore.setTransaction(transaction);
        final FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = DataUtilities.collection(features);
        featureStore.addFeatures(featureCollection);
        try {
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            throw new IOException("Cannot write shapefile. Error:\n" + e.getMessage(), e);
        } finally {
            dataStore.dispose();
            transaction.close();
        }
    }
}
