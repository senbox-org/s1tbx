package org.esa.snap.statistics.output;

import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.opengis.feature.simple.SimpleFeature;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class EsriShapeFileWriter {

    private static final String FILE_EXTENSION_SHAPEFILE = ".shp";

    static void write(List<SimpleFeature> features, File file) throws IOException {
        if(features.isEmpty()) {
            return;
        }
        String basename = file.getName();
        if (basename.endsWith(FILE_EXTENSION_SHAPEFILE)) {
            basename = basename.substring(0, basename.length() - 4);
        }
        File shapefile = new File(file.getParentFile(), basename + FILE_EXTENSION_SHAPEFILE);

        ShapefileDataStoreFactory factory = new ShapefileDataStoreFactory();
        Map<String, Serializable> map = Collections.singletonMap("url", shapefile.toURI().toURL());
        ShapefileDataStore dataStore = (ShapefileDataStore) factory.createNewDataStore(map);
        SimpleFeature simpleFeature = features.get(0);
        dataStore.createSchema(simpleFeature.getType());
        SimpleFeatureSource featureSource = dataStore.getFeatureSource();
        if (featureSource instanceof SimpleFeatureStore) {
            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
            DefaultTransaction transaction = new DefaultTransaction("X");
            featureStore.setTransaction(transaction);
            featureStore.addFeatures(DataUtilities.collection(features));
            try {
                transaction.commit();
            } catch (Exception e) {
                transaction.rollback();
                throw new IOException("Cannot write shapefile. Error:\n" + e.getMessage(), e);
            } finally {
                dataStore.dispose();
                transaction.close();
            }
        } else {
            throw new IOException("Cannot write shapefile. Error:\n" + "Write access not supported for " + file);
        }
    }
}
