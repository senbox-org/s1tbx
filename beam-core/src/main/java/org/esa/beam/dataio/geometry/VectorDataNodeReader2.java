/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.dataio.geometry;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.thoughtworks.xstream.core.util.OrderRetainingMap;
import com.vividsolutions.jts.geom.Geometry;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.FeatureUtils;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.converters.JavaTypeConverter;
import org.esa.beam.util.io.CsvReader;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.logging.BeamLogManager;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;

public class VectorDataNodeReader2 {

    private final String vectorDataNodeName;
    private final GeoCoding geoCoding;
    private final Product product;
    private final FeatureUtils.FeatureCrsProvider crsProvider;
    private final InterpretationStrategy interpretationStrategy;
    private final CsvReader reader;

    private static final String[] LONGITUDE_IDENTIFIERS = new String[]{"lon", "long", "longitude"};
    private static final String[] LATITUDE_IDENTIFIERS = new String[]{"lat", "latitude"};
    private static final String[] GEOMETRY_IDENTIFIERS = new String[]{"geometry", "geom", "the_geom"};
    private SimpleFeatureType featureType;

    VectorDataNodeReader2(String vectorDataNodeName, Product product, Reader reader, FeatureUtils.FeatureCrsProvider crsProvider) throws IOException {
        this.product = product;
        this.crsProvider = crsProvider;
        this.geoCoding = product.getGeoCoding();
        this.vectorDataNodeName = vectorDataNodeName;
        this.reader = new CsvReader(reader, new char[]{VectorDataNodeIO.DELIMITER_CHAR}, true, "#");
        this.interpretationStrategy = createInterpretationStrategy();
    }

    public static VectorDataNode read(String name, Reader reader, Product product, FeatureUtils.FeatureCrsProvider crsProvider, CoordinateReferenceSystem modelCrs, ProgressMonitor pm) throws IOException {
        return new VectorDataNodeReader2(name, product, reader, crsProvider).read(modelCrs, pm);
    }

    VectorDataNode read(CoordinateReferenceSystem modelCrs, ProgressMonitor pm) throws IOException {
        final String name = FileUtils.getFilenameWithoutExtension(vectorDataNodeName);
        Map<String, String> properties = readProperties();
        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = readFeatures();

        CoordinateReferenceSystem featureCrs = featureCollection.getSchema().getCoordinateReferenceSystem();
        final Geometry clipGeometry = FeatureUtils.createGeoBoundaryPolygon(product);
        FeatureCollection<SimpleFeatureType, SimpleFeature> clippedCollection
                = FeatureUtils.clipCollection(featureCollection,
                                              featureCrs,
                                              clipGeometry,
                                              DefaultGeographicCRS.WGS84,
                                              null,
                                              modelCrs,
                                              SubProgressMonitor.create(pm, 80));

        final List<PlacemarkDescriptor> placemarkDescriptors = PlacemarkDescriptorRegistry.getInstance().getPlacemarkDescriptors(featureType);
        final PlacemarkDescriptor placemarkDescriptor;
        if (placemarkDescriptors.isEmpty()) {
            placemarkDescriptor = PlacemarkDescriptorRegistry.getInstance().getPlacemarkDescriptor(GeometryDescriptor.class);
        } else {
            placemarkDescriptor = placemarkDescriptors.get(0);
        }
        VectorDataNode vectorDataNode = new VectorDataNode(name, clippedCollection, placemarkDescriptor);
        if (properties.containsKey(ProductNode.PROPERTY_NAME_DESCRIPTION)) {
            vectorDataNode.setDescription(properties.get(ProductNode.PROPERTY_NAME_DESCRIPTION));
        }
        if (properties.containsKey(VectorDataNodeIO.PROPERTY_NAME_DEFAULT_CSS)) {
            vectorDataNode.setDefaultStyleCss(properties.get(VectorDataNodeIO.PROPERTY_NAME_DEFAULT_CSS));

        }
        clippedCollection.getSchema().getUserData().putAll(properties);
        return vectorDataNode;
    }

    /**
     * Collects comment lines of the form "# &lt;name&gt; = &lt;value&gt;" until the first non-empty and non-comment line is found.
     *
     * @return All the property assignments found.
     * @throws java.io.IOException
     */
    Map<String, String> readProperties() throws IOException {
        LineNumberReader lineNumberReader = new LineNumberReader(reader);
        OrderRetainingMap properties = new OrderRetainingMap();
        String line;
        while ((line = lineNumberReader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("#")) {
                line = line.substring(1);
                int index = line.indexOf('=');
                if (index != -1) {
                    String name = line.substring(0, index).trim();
                    String value = line.substring(index + 1).trim();
                    if (StringUtils.isNotNullAndNotEmpty(name) &&
                            StringUtils.isNotNullAndNotEmpty(value)) {
                        properties.put(name, value);
                    }
                }
            } else if (!line.isEmpty()) {
                // First non-comment line reached, no more property assignments expected
                break;
            }
        }
        reader.reset();
        //noinspection unchecked
        return (Map<String, String>) properties;
    }

    FeatureCollection<SimpleFeatureType, SimpleFeature> readFeatures() throws IOException {
        featureType = readFeatureType();
        return readFeatures(featureType);
    }

    private InterpretationStrategy createInterpretationStrategy() throws IOException {
        reader.mark(1024 * 1024 * 10);
        String[] tokens = reader.readRecord();
        reader.reset();

        if (tokens == null) {
            throw new IOException(String.format("Invalid header in file '%s'", vectorDataNodeName));
        }

        int latIndex = -1;
        int lonIndex = -1;
        int geometryIndex = -1;

        boolean hasFeatureTypeName = false;
        boolean hasLatLon;

        String featureTypeName = null;
        String geometryName = null;
        for (int i = 0; i < tokens.length; i++) {
            if (i == 0 && tokens[0].startsWith("org.esa.beam")) {
                hasFeatureTypeName = true;
                featureTypeName = tokens[0];
            } else {
                String token = tokens[i];
                final int colonPos = token.indexOf(':');
                String attributeName;
                if (colonPos == -1) {
                    attributeName = token;
                } else if (colonPos == 0) {
                    throw new IOException(String.format("Missing name specifier in attribute descriptor '%s'", token));
                } else {
                    attributeName = token.substring(0, colonPos);
                }

                if (contains(LONGITUDE_IDENTIFIERS, attributeName)) {
                    lonIndex = i;
                } else if (contains(LATITUDE_IDENTIFIERS, attributeName)) {
                    latIndex = i;
                } else if (contains(GEOMETRY_IDENTIFIERS, attributeName)) {
                    geometryIndex = i;
                    geometryName = attributeName;
                }
            }
        }

        hasLatLon = latIndex != -1 && lonIndex != -1;
        if (geometryIndex == -1 && (latIndex == -1 || lonIndex == -1)) {
            throw new IOException("Neither lat/lon nor geometry column provided.");
        }

        if (hasLatLon && hasFeatureTypeName) {
            return new LatLonAndFeatureTypeStrategy(geoCoding, featureTypeName, latIndex, lonIndex);
        } else if (hasLatLon && !hasFeatureTypeName) {
            return new LatLonNoFeatureTypeStrategy(geoCoding, latIndex, lonIndex);
        } else if (!hasLatLon && hasFeatureTypeName) {
            return new GeometryAndFeatureTypeStrategy(geoCoding, geometryName, featureTypeName);
        } else if (!hasLatLon && !hasFeatureTypeName) {
            return new GeometryNoFeatureTypeStrategy(geoCoding, geometryName);
        }
        throw new IllegalStateException("Cannot come here");
    }


    private FeatureCollection<SimpleFeatureType, SimpleFeature> readFeatures(SimpleFeatureType simpleFeatureType) throws IOException {
        DefaultFeatureCollection fc = new DefaultFeatureCollection("?", simpleFeatureType);
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(simpleFeatureType);
        while (true) {
            String[] tokens = reader.readRecord();
            if (tokens == null) {
                break;
            }
            if (!isLineValid(simpleFeatureType, tokens)) {
                continue;
            }
            SimpleFeature simpleFeature = null;
            try {
                simpleFeature = interpretationStrategy.interpretLine(tokens, builder, simpleFeatureType);
            } catch (ConversionException e) {
                BeamLogManager.getSystemLogger().warning(String.format("Unable to parse %s: %s", vectorDataNodeName, e.getMessage()));
            } catch (TransformException e) {
                throw new IOException(e);
            }

            if (simpleFeature != null) {
                try {
                    interpretationStrategy.transformGeoPosToPixelPos(simpleFeature);
                } catch (TransformException e) {
                    throw new IOException(e);
                }
                fc.add(simpleFeature);
            }
        }
        return fc;
    }

    private boolean isLineValid(SimpleFeatureType simpleFeatureType, String[] tokens) {
        int expectedTokenCount = interpretationStrategy.getExpectedTokenCount(simpleFeatureType.getAttributeCount());
        if (tokens.length != expectedTokenCount) {
            BeamLogManager.getSystemLogger().warning(String.format("Problem in '%s': unexpected number of columns: expected %d, but got %d",
                                                                   vectorDataNodeName, expectedTokenCount, tokens.length));
            return false;
        }
        return true;
    }

    SimpleFeatureType readFeatureType() throws IOException {
        String[] tokens = reader.readRecord();
        if (tokens == null || tokens.length <= 1) {
            throw new IOException("Missing feature type definition in first line.");
        }
        reader.mark(1024 * 1024 * 10);
        return createFeatureType(tokens);
    }

    private SimpleFeatureType createFeatureType(String[] tokens) throws IOException {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        CoordinateReferenceSystem featureCrs = crsProvider.getFeatureCrs(product);
        if (featureCrs == null) {
            featureCrs = DefaultGeographicCRS.WGS84;
        }
        builder.setCRS(featureCrs);
        JavaTypeConverter jtc = new JavaTypeConverter();

        String[] firstRecord = reader.readRecord();
        if (firstRecord != null && firstRecord.length != tokens.length) {
            throw new IOException("First record and header have different column count.");
        }
        reader.reset();

        for (int i = interpretationStrategy.getStartColumn(); i < tokens.length; i++) {
            String token = tokens[i];
            final int colonPos = token.indexOf(':');
            String attributeTypeName;
            String attributeName;
            if (colonPos == 0) {
                throw new IOException(String.format("Missing name specifier in attribute descriptor '%s'", token));
            } else if (colonPos == -1) {
                attributeName = token;
                attributeTypeName = findAttributeTypeName(firstRecord == null ? "" : firstRecord[i]);
            } else {
                attributeTypeName = token.substring(colonPos + 1);
                attributeName = token.substring(0, colonPos);
            }

            Class<?> attributeType = getAttributeType(jtc, token, attributeTypeName);
            builder.add(attributeName, attributeType);
        }

        interpretationStrategy.setDefaultGeometry(builder);
        interpretationStrategy.setName(builder);

        return builder.buildFeatureType();
    }

    private String findAttributeTypeName(String entry) throws IOException {
        try {
            Double.parseDouble(entry);
            return "Double";
        } catch (NumberFormatException e) {
            // ok
        }
        return "String";
    }

    private Class<?> getAttributeType(JavaTypeConverter jtc, String token, String attributeTypeName) throws IOException {
        Class<?> attributeType;
        try {
            attributeType = jtc.parse(attributeTypeName);
        } catch (ConversionException e) {
            throw new IOException(
                    String.format("Unknown type in attribute descriptor '%s'", token), e);
        }
        return attributeType;
    }

    private static boolean contains(String[] possibleStrings, String s) {
        for (String possibleString : possibleStrings) {
            if (possibleString.toLowerCase().equals(s.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

}