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

import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.util.FeatureUtils;
import org.geotools.data.FeatureSource;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//todo Rename class and update class documentation
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
public class FeatureStatisticsWriter implements StatisticsOutputter {

    private final String targetShapefile;
    private final FeatureCollection<SimpleFeatureType, SimpleFeature> originalFeatures;
    private final SimpleFeatureType originalFeatureType;
    private final BandNameCreator bandNameCreator;

    private SimpleFeatureType updatedFeatureType;
    final List<SimpleFeature> features;

    public static FeatureStatisticsWriter createFeatureStatisticsWriter(URL originalShapefile, String targetShapefile, BandNameCreator bandNameCreator) {
        final FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;
        final FeatureCollection<SimpleFeatureType, SimpleFeature> features;
        try {
            featureSource = FeatureUtils.getFeatureSource(originalShapefile);
            features = featureSource.getFeatures();
        } catch (IOException e) {
            throw new OperatorException("Unable to initialise the output.", e);
        }
        return new FeatureStatisticsWriter(featureSource.getSchema(), targetShapefile, features, bandNameCreator);
    }

    public static FeatureStatisticsWriter createFeatureStatisticsWriter(SimpleFeatureType originalFeatureType, FeatureCollection<SimpleFeatureType, SimpleFeature> originalFeatures, String targetShapefile,
                                                                        BandNameCreator bandNameCreator) {
        return new FeatureStatisticsWriter(originalFeatureType, targetShapefile, originalFeatures, bandNameCreator);
    }

    protected FeatureStatisticsWriter(SimpleFeatureType originalFeatureType, String targetShapefile, FeatureCollection<SimpleFeatureType, SimpleFeature> originalFeatures,
                                      BandNameCreator bandNameCreator) {
        this.originalFeatureType = originalFeatureType;
        this.targetShapefile = targetShapefile;
        this.originalFeatures = originalFeatures;
        this.bandNameCreator = bandNameCreator;
        features = new ArrayList<SimpleFeature>();
    }

    @Override
    public void initialiseOutput(StatisticsOutputContext statisticsOutputContext) {
        Arrays.sort(statisticsOutputContext.algorithmNames);
        final SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.init(originalFeatureType);
        for (final String algorithmName : statisticsOutputContext.algorithmNames) {
            for (String bandName : statisticsOutputContext.bandNames) {
                final String attributeName = bandNameCreator.createUniqueAttributeName(algorithmName, bandName);
                if (originalFeatureType.getDescriptor(attributeName) == null) {
                    typeBuilder.add(attributeName, Double.class);
                }
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
                if (StatisticsOp.getFeatureName(feature).equals(regionId)) {
                    SimpleFeature featureToUpdate;
                    if (markedToAdd.get(regionId) != null) {
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
            if (StatisticsOp.getFeatureName(originalFeature).equals(regionId)) {
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
        EsriShapeFileWriter.write(features, new File(targetShapefile));
    }

    public List<SimpleFeature> getFeatures() {
        return features;
    }

    public SimpleFeatureType getUpdatedFeatureType() {
        return updatedFeatureType;
    }

    private static SimpleFeature createUpdatedFeature(SimpleFeatureBuilder builder, SimpleFeature baseFeature, String name, Number value) {
        builder.init(baseFeature);
        builder.set(name, value);
        return builder.buildFeature(baseFeature.getID());
    }
}
