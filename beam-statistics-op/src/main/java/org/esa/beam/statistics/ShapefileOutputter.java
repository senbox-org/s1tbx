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

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.ProgressMonitor;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    final List<SimpleFeature> features;

    ShapefileOutputter(URL originalShapefile, String targetShapefile) {
        this.originalShapefile = originalShapefile;
        this.targetShapefile = targetShapefile;
        features = new ArrayList<SimpleFeature>();
    }

    @Override
    public void initialiseOutput(Product[] sourceProducts, String[] bandNames, String[] algorithmNames, ProductData.UTC startDate, ProductData.UTC endDate, String[] regionIds) {
        try {
            final FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = FeatureUtils.getFeatureSource(originalShapefile);
            originalFeatures = featureSource.getFeatures();
            final SimpleFeatureType originalFeatureType = featureSource.getSchema();
            final SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
            typeBuilder.init(originalFeatureType);
            typeBuilder.setSuperType(originalFeatureType);
            for (int i = 0; i < algorithmNames.length; i++) {
                final String algorithmName = algorithmNames[i];
                final String bandName = bandNames[i];
                typeBuilder.add(createAttributeName(algorithmName, bandName), Double.class);
            }
            typeBuilder.setDefaultGeometry(originalFeatureType.getGeometryDescriptor().getLocalName());
            typeBuilder.setName(originalFeatureType.getName());
            updatedFeatureType = typeBuilder.buildFeatureType();
        } catch (IOException e) {
            throw new OperatorException("Unable to initialise the output.", e);
        }

    }

    @Override
    public void addToOutput(StatisticsOp.BandConfiguration bandConfiguration, String regionId, Map<String, Double> statistics) {
        for (SimpleFeature feature : features) {
            if (feature.getID().equals(regionId)) {
                final SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(updatedFeatureType);
                simpleFeatureBuilder.init(feature);
                final PropertySet propertySet = StatisticsUtils.createPropertySet(bandConfiguration);
                final String description = bandConfiguration.statisticsCalculatorDescriptor.getDescription(propertySet);
                simpleFeatureBuilder.set(createAttributeName(description, bandConfiguration.sourceBandName), statistics.get(description));
                final SimpleFeature newFeature = simpleFeatureBuilder.buildFeature(feature.getID());
                newFeature.setDefaultGeometry(feature.getDefaultGeometry());
                features.remove(feature);
                features.add(newFeature);
                return;
            }
        }

        final SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(updatedFeatureType);
        final PropertySet propertySet = StatisticsUtils.createPropertySet(bandConfiguration);
        final String description = bandConfiguration.statisticsCalculatorDescriptor.getDescription(propertySet);
        final FeatureIterator<SimpleFeature> featureIterator = originalFeatures.features();
        while (featureIterator.hasNext()) {
            final SimpleFeature originalFeature = featureIterator.next();
            if (originalFeature.getID().equals(regionId)) {
                simpleFeatureBuilder.init(originalFeature);
                final String name = createAttributeName(description, bandConfiguration.sourceBandName);
                simpleFeatureBuilder.set(name, statistics.get(description));
                final SimpleFeature feature = simpleFeatureBuilder.buildFeature(originalFeature.getID());
                feature.setDefaultGeometry(originalFeature.getDefaultGeometry());
                features.add(feature);
            }
        }
    }

    @Override
    public void finaliseOutput() throws IOException {
        final DefaultFeatureCollection fc = new DefaultFeatureCollection("some_id", updatedFeatureType);
        fc.addAll(features);
        final VectorDataNode vectorDataNode = new VectorDataNode("some_name", fc);
        final File targetFile = new File(targetShapefile);
        exportVectorDataNode(vectorDataNode, targetFile, ProgressMonitor.NULL);
    }

    // todo - add test
    private static String createAttributeName(String description, String sourceBandName) {
        String temp = description + "_" + sourceBandName;
        if (temp.length() > 10) {
            temp = temp.replace("a", "").replace("e", "").replace("i", "").replace("o", "").replace("u", "");
            // todo - log warning here...
        }
        if (temp.length() > 10) {
            // todo - ...and here
            temp = temp.substring(0, 9);
        }
        return temp;
    }

    private static void exportVectorDataNode(VectorDataNode vectorNode, File file, ProgressMonitor pm) throws
                                                                                                       IOException {


        Map<Class<?>, List<SimpleFeature>> featureListMap = createGeometryToFeaturesListMap(vectorNode);
        if (featureListMap.size() > 1) {
//            final String msg = "The selected geometry contains different types of shapes.\n" +
//                               "Each type of shape will be exported as a separate shapefile.";
//            VisatApp.getApp().showInfoDialog(DLG_TITLE, msg, ExportGeometryAction.class.getName() + ".exportInfo");
        }

        Set<Map.Entry<Class<?>, List<SimpleFeature>>> entries = featureListMap.entrySet();
        pm.beginTask("Writing ESRI Shapefiles...", featureListMap.size());
        try {
            for (Map.Entry<Class<?>, List<SimpleFeature>> entry : entries) {
                writeEsriShapefile(entry.getValue(), file);
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    private static Map<Class<?>, List<SimpleFeature>> createGeometryToFeaturesListMap(VectorDataNode vectorNode) {
        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = vectorNode.getFeatureCollection();

        Map<Class<?>, List<SimpleFeature>> featureListMap = new HashMap<Class<?>, List<SimpleFeature>>();
        final FeatureIterator<SimpleFeature> featureIterator = featureCollection.features();
        while (featureIterator.hasNext()) {
            SimpleFeature feature = featureIterator.next();
            Object defaultGeometry = feature.getDefaultGeometry();
            Class<?> geometryType = defaultGeometry.getClass();

            List<SimpleFeature> featureList = featureListMap.get(geometryType);
            if (featureList == null) {
                featureList = new ArrayList<SimpleFeature>();
                featureListMap.put(geometryType, featureList);
            }
            featureList.add(feature);
        }
        return featureListMap;
    }

    private static void writeEsriShapefile(List<SimpleFeature> features, File file) throws IOException {
        String basename = file.getName();
        if (basename.endsWith(FILE_EXTENSION_SHAPEFILE)) {
            basename = basename.substring(0, basename.length() - 4);
        }
        File file1 = new File(file.getParentFile(), basename + FILE_EXTENSION_SHAPEFILE);

        ShapefileDataStoreFactory factory = new ShapefileDataStoreFactory();
        Map map = Collections.singletonMap("url", file1.toURI().toURL());
        DataStore dataStore = factory.createNewDataStore(map);
        SimpleFeature simpleFeature = features.get(0);
        SimpleFeatureType simpleFeatureType = simpleFeature.getType();
        String typeName = simpleFeatureType.getName().getLocalPart();
        dataStore.createSchema(simpleFeatureType);
        FeatureStore<SimpleFeatureType, SimpleFeature> featureStore = (FeatureStore<SimpleFeatureType, SimpleFeature>) dataStore.getFeatureSource(
                typeName);
        DefaultTransaction transaction = new DefaultTransaction("X");
        featureStore.setTransaction(transaction);
        final FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = DataUtilities.collection(
                features);
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
