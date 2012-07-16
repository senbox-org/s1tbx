/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.statistics;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.util.FeatureUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes the given output to a new shapefile.
 * It works like this:
 * 1) the original shapefile is being opened and its features are loaded
 * 2) the feature type of the original shapefile is being extended by the statistical means that have been computed
 * 3) for each feature, the statistical value is being added
 * 4) the resulting feature type and features are written as new shapefile
 *
 * @author Thomas Storm
 */
class ShapefileOutputter implements StatisticsOp.Outputter {

    private static final String FILE_EXTENSION_SHAPEFILE = ".shp";

    private final URL originalShapefile;
    private final String targetShapefile;
    private SimpleFeatureType updatedFeatureType;
    private FeatureCollection<SimpleFeatureType, SimpleFeature> originalFeatures;
    private BandNameCreator bandNameCreator;

    final List<SimpleFeature> features;

    ShapefileOutputter(URL originalShapefile, String targetShapefile, BandNameCreator bandNameCreator) {
        this.originalShapefile = originalShapefile;
        this.targetShapefile = targetShapefile;
        this.bandNameCreator = bandNameCreator;
        features = new ArrayList<SimpleFeature>();
    }

    @Override
    public void initialiseOutput(Product[] sourceProducts, String[] bandNames, String[] algorithmNames, ProductData.UTC startDate, ProductData.UTC endDate, String[] regionIds) {
        Arrays.sort(algorithmNames);
        final FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;
        try {
            featureSource = FeatureUtils.getFeatureSource(originalShapefile);
            originalFeatures = featureSource.getFeatures();
        } catch (IOException e) {
            throw new OperatorException("Unable to initialise the output.", e);
        }
        final SimpleFeatureType originalFeatureType = featureSource.getSchema();
        final SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.init(originalFeatureType);
        for (final String algorithmName : algorithmNames) {
            for (String bandName : bandNames) {
                final String attributeName = bandNameCreator.createUniqueAttributeName(algorithmName, bandName);
                typeBuilder.add(attributeName, Double.class);
            }
        }
        typeBuilder.setName(originalFeatureType.getName());
        updatedFeatureType = typeBuilder.buildFeatureType();

    }

    @Override
    public void addToOutput(String bandName, String regionId, Map<String, Number> statistics) {
        final SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(updatedFeatureType);
        final List<SimpleFeature> markedToRemove = new ArrayList<SimpleFeature>();
        final Map<String, SimpleFeature> markedToAdd = new HashMap<String, SimpleFeature>();
        for (SimpleFeature feature : features) {
            for (String algorithmName : statistics.keySet()) {
                final String name = bandNameCreator.createUniqueAttributeName(algorithmName, bandName);
                if (feature.getID().equals(regionId)) {
                    SimpleFeature featureToUpdate;
                    if(markedToAdd.get(regionId) != null) {
                        featureToUpdate = markedToAdd.get(regionId);
                    } else {
                        featureToUpdate = feature;
                    }
                    final SimpleFeature updatedFeature = createUpdatedFeature(simpleFeatureBuilder, featureToUpdate, name, statistics.get(algorithmName));
                    markedToRemove.add(feature);
                    markedToAdd.put(regionId, updatedFeature);
                }
            }
        }
        features.removeAll(markedToRemove);
        features.addAll(markedToAdd.values());
        if (!(markedToAdd.isEmpty() && markedToRemove.isEmpty())) {
            return;
        }

        final FeatureIterator<SimpleFeature> featureIterator = originalFeatures.features();
        while (featureIterator.hasNext()) {
            final SimpleFeature originalFeature = featureIterator.next();
            String featureName = StatisticsOp.getFeatureName(originalFeature);
            if (featureName.equals(regionId)) {
                SimpleFeature feature = originalFeature;
                for (String algorithmName : statistics.keySet()) {
                    final String name = bandNameCreator.createUniqueAttributeName(algorithmName, bandName);
                    feature = createUpdatedFeature(simpleFeatureBuilder, feature, name, statistics.get(algorithmName));
                }
                features.add(feature);
                return;
            }
        }
    }

    @Override
    public void finaliseOutput() throws IOException {
        final DefaultFeatureCollection fc = new DefaultFeatureCollection("some_id", updatedFeatureType);
        fc.addAll(features);
        final VectorDataNode vectorDataNode = new VectorDataNode("some_name", fc);
        final File targetFile = new File(targetShapefile);
        exportVectorDataNode(vectorDataNode, targetFile);
    }

    private static SimpleFeature createUpdatedFeature(SimpleFeatureBuilder builder, SimpleFeature baseFeature, String name, Number value) {
        builder.init(baseFeature);
        builder.set(name, value);
        return builder.buildFeature(baseFeature.getID());
    }

    private static void exportVectorDataNode(VectorDataNode vectorDataNode, File file) throws IOException {
        List<SimpleFeature> simpleFeatures = new ArrayList<SimpleFeature>();
        final FeatureIterator<SimpleFeature> featureIterator = vectorDataNode.getFeatureCollection().features();
        Class<?> geometryClass = null;
        while (featureIterator.hasNext()) {
            final SimpleFeature simpleFeature = featureIterator.next();
            if (geometryClass != null && !simpleFeature.getDefaultGeometry().getClass().equals(geometryClass)) {
                throw new IllegalStateException("Different geometry type within shapefile detected. Geometries must all be of same type.");
            }
            geometryClass = simpleFeature.getDefaultGeometry().getClass();
            simpleFeatures.add(simpleFeature);
        }
        writeEsriShapefile(simpleFeatures, file);
    }

    private static void writeEsriShapefile(List<SimpleFeature> features, File file) throws IOException {
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
        String typeName = simpleFeatureType.getName().getLocalPart();
        dataStore.createSchema(simpleFeatureType);
        FeatureStore<SimpleFeatureType, SimpleFeature> featureStore = (FeatureStore<SimpleFeatureType, SimpleFeature>) dataStore.getFeatureSource(typeName);
        DefaultTransaction transaction = new DefaultTransaction("X");
        featureStore.setTransaction(transaction);
        final FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = DataUtilities.collection(features);
        featureStore.addFeatures(featureCollection);
        try {
            transaction.commit();
        } catch (IOException e) {
            transaction.rollback();
            throw e;
        } finally {
            transaction.close();
        }
    }
}
