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

package org.esa.snap.statistics.output;

import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.util.FeatureUtils;
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

/**
 * Instances of <code>FeatureStatisticsWriter</code> enrich shapefiles or features with statistics.
 * <p>
 * This is done like this:
 * <ol>
 * <li>the original shapefile is being opened and its features are loaded</li>
 * <li>a new feature type is created on the basis of the feature type of the original shapefile</li>
 * <li>this new feature type is being extended by the statistical means that have been computed</li>
 * <li>for each original feature, a new feature is created</li>
 * <li>the statistical values are added to each new feature</li>
 * <li>the new features are made available by the {@link #getFeatures()}-method</li>
 * <li>within the {@link #finaliseOutput()}-method, the resulting feature type and features are written to a new shapefile</li>
 * </ol>
 * Instances are created by using one of the factory methods this class provides.
 *
 * @author Thomas Storm
 */
public class FeatureStatisticsWriter implements StatisticsOutputter {

    private static final double FILL_VALUE = -999.0;
    private final String targetShapefile;
    private final FeatureCollection<SimpleFeatureType, SimpleFeature> originalFeatures;
    private final SimpleFeatureType originalFeatureType;
    private final BandNameCreator bandNameCreator;

    private SimpleFeatureType updatedFeatureType;
    private final List<SimpleFeature> features;

    /**
     * Factory method for creating a new instance of <code>FeatureStatisticsWriter</code>. This method opens and reads
     * the original shapefile. Use this method if the shapefile has not yet been read.
     *
     * @param originalShapefile An URL pointing to the original shapefile that shall is to be enriched with statistics.
     * @param targetShapefile   A file path where the target shapefile shall be written to.
     * @param bandNameCreator   An instance of {@link BandNameCreator}.
     *
     * @return An instance of <code>FeatureStatisticsWriter</code>.
     */
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

    /**
     * Factory method for creating a new instance of <code>FeatureStatisticsWriter</code>. This method utilises the
     * original features. Use this method if the shapefile has already been read.
     *
     * @param originalFeatures The features to be enriched with statistics.
     * @param targetShapefile  A file path where the target shapefile shall be written to.
     * @param bandNameCreator  An instance of {@link org.esa.snap.statistics.output.BandNameCreator}.
     *
     * @return An instance of <code>FeatureStatisticsWriter</code>.
     */
    public static FeatureStatisticsWriter createFeatureStatisticsWriter(FeatureCollection<SimpleFeatureType, SimpleFeature> originalFeatures, String targetShapefile,
                                                                        BandNameCreator bandNameCreator) {
        return new FeatureStatisticsWriter(originalFeatures.getSchema(), targetShapefile, originalFeatures, bandNameCreator);
    }

    private FeatureStatisticsWriter(SimpleFeatureType originalFeatureType, String targetShapefile, FeatureCollection<SimpleFeatureType, SimpleFeature> originalFeatures,
                                    BandNameCreator bandNameCreator) {
        this.originalFeatureType = originalFeatureType;
        this.targetShapefile = targetShapefile;
        this.originalFeatures = originalFeatures;
        this.bandNameCreator = bandNameCreator;
        features = new ArrayList<SimpleFeature>();
    }

    /**
     * {@inheritDoc}
     *
     * @param statisticsOutputContext A context providing meta-information about the statistics.
     */
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

    /**
     * {@inheritDoc}
     * After calling this method, the updated features can be retrieved by using {@link #getFeatures()}.
     *
     * @param bandName   The name of the band the statistics have been computed for.
     * @param regionId   The id of the region the statistics have been computed for.
     * @param statistics The actual statistics as map. Keys are the algorithm names, values are the actual statistical values.
     */
    @Override
    public void addToOutput(String bandName, String regionId, Map<String, Number> statistics) {
        final SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(updatedFeatureType);
        final List<SimpleFeature> markedToRemove = new ArrayList<SimpleFeature>();
        final Map<String, SimpleFeature> markedToAdd = new HashMap<String, SimpleFeature>();
        for (SimpleFeature feature : features) {
            for (String algorithmName : statistics.keySet()) {
                final String name = bandNameCreator.createUniqueAttributeName(algorithmName, bandName);
                if (Util.getFeatureName(feature).equals(regionId)) {
                    SimpleFeature featureToUpdate;
                    if (markedToAdd.containsKey(regionId)) {
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
            SimpleFeature feature = featureIterator.next();
            for (String algorithmName : statistics.keySet()) {
                final String name = bandNameCreator.createUniqueAttributeName(algorithmName, bandName);
                final Number value = getValue(statistics, algorithmName, feature, regionId);
                feature = createUpdatedFeature(simpleFeatureBuilder, feature, name, value);
            }
            features.add(feature);
        }
    }

    private Number getValue(Map<String, Number> statistics, String algorithmName, SimpleFeature originalFeature, String regionId) {
        if (Util.getFeatureName(originalFeature).equals(regionId)) {
            return statistics.get(algorithmName);
        } else {
            return FILL_VALUE;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation writes the enriched features to the specified target shapefile.
     *
     *
     * @throws IOException If writing fails.
     */
    @Override
    public void finaliseOutput() throws IOException {
        EsriShapeFileWriter.write(features, new File(targetShapefile));
    }

    /**
     * Returns the features enriched with statistics.
     *
     * @return the features enriched with statistics.
     */
    public List<SimpleFeature> getFeatures() {
        return features;
    }

    /**
     * Returns the updated feature type.
     *
     * @return the updated feature type.
     */
    public SimpleFeatureType getUpdatedFeatureType() {
        return updatedFeatureType;
    }

    private static SimpleFeature createUpdatedFeature(SimpleFeatureBuilder builder, SimpleFeature baseFeature, String name, Number value) {
        builder.init(baseFeature);
        builder.set(name, value);
        return builder.buildFeature(baseFeature.getID());
    }
}
