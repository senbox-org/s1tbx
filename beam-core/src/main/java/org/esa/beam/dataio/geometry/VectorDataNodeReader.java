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
import com.vividsolutions.jts.geom.*;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.FeatureUtils;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.converters.JavaTypeConverter;
import org.esa.beam.util.io.CsvReader;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.logging.BeamLogManager;
import org.geotools.data.DataUtilities;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VectorDataNodeReader {

    private final String vectorDataNodeName;
    private final GeoCoding geoCoding;
    private final Product product;
    private final FeatureUtils.FeatureCrsProvider crsProvider;
    private final PlacemarkDescriptorProvider placemarkDescriptorProvider;
    private InterpretationStrategy interpretationStrategy;
    private final CsvReader reader;

    private static final String[] LONGITUDE_IDENTIFIERS = new String[]{"lon", "long", "longitude", "lon_IS"};
    private static final String[] LATITUDE_IDENTIFIERS = new String[]{"lat", "latitude", "lat_IS"};
    private static final String[] GEOMETRY_IDENTIFIERS = new String[]{"geometry", "geom", "the_geom"};
    private final char delimiterChar = VectorDataNodeIO.DEFAULT_DELIMITER_CHAR;

    private Map<String, String> properties;

    private VectorDataNodeReader(String vectorDataNodeName, Product product, Reader reader, FeatureUtils.FeatureCrsProvider crsProvider,
                                 PlacemarkDescriptorProvider placemarkDescriptorProvider, char delimiterChar) throws IOException {
        this.product = product;
        this.crsProvider = crsProvider;
        this.placemarkDescriptorProvider = placemarkDescriptorProvider;
        this.geoCoding = product.getGeoCoding();
        this.vectorDataNodeName = vectorDataNodeName;
        this.reader = new CsvReader(reader, new char[]{delimiterChar}, true, "#");
    }

    public static VectorDataNode read(String name, Reader reader, Product product, FeatureUtils.FeatureCrsProvider crsProvider,
                                      PlacemarkDescriptorProvider placemarkDescriptorProvider, CoordinateReferenceSystem modelCrs,
                                      char delimiterChar, ProgressMonitor pm) throws IOException {
        return new VectorDataNodeReader(name, product, reader, crsProvider, placemarkDescriptorProvider, delimiterChar).read(modelCrs, pm);
    }

    VectorDataNode read(CoordinateReferenceSystem modelCrs, ProgressMonitor pm) throws IOException {
        reader.mark(1024 * 1024 * 10);
        properties = readProperties();
        interpretationStrategy = createInterpretationStrategy();
        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = readFeatures();

        FeatureCollection<SimpleFeatureType, SimpleFeature> clippedCollection;
        if (product.getGeoCoding() != null) {
            final Geometry clipGeometry = FeatureUtils.createGeoBoundaryPolygon(product);
            clippedCollection = FeatureUtils.clipCollection(featureCollection,
                                                            null,
                                                            clipGeometry,
                                                            featureCollection.getSchema().getCoordinateReferenceSystem(),
                                                            null,
                                                            modelCrs,
                                                            SubProgressMonitor.create(pm, 80));
        } else {
            clippedCollection = featureCollection;
        }

        SimpleFeatureType featureType = clippedCollection.getSchema();
        featureType.getUserData().putAll(properties);
        final PlacemarkDescriptor placemarkDescriptor = placemarkDescriptorProvider.getPlacemarkDescriptor(featureType);
        if (placemarkDescriptor == null) {
            return null;
        }
        placemarkDescriptor.setUserDataOf(featureType);

        if (placemarkDescriptor instanceof PointDescriptor && clippedCollection.size() > 0) {
            clippedCollection = convertPointsToVertices(clippedCollection);
        }

        final String name = FileUtils.getFilenameWithoutExtension(vectorDataNodeName);
        VectorDataNode vectorDataNode = new VectorDataNode(name, clippedCollection, placemarkDescriptor);
        if (properties.containsKey(ProductNode.PROPERTY_NAME_DESCRIPTION)) {
            featureType.getUserData().put(ProductNode.PROPERTY_NAME_DESCRIPTION, properties.get(ProductNode.PROPERTY_NAME_DESCRIPTION));
            vectorDataNode.setDescription(properties.get(ProductNode.PROPERTY_NAME_DESCRIPTION));
        }
        if (properties.containsKey(VectorDataNodeIO.PROPERTY_NAME_DEFAULT_CSS)) {
            featureType.getUserData().put(VectorDataNodeIO.PROPERTY_NAME_DEFAULT_CSS, properties.get(VectorDataNodeIO.PROPERTY_NAME_DEFAULT_CSS));
            vectorDataNode.setDefaultStyleCss(properties.get(VectorDataNodeIO.PROPERTY_NAME_DEFAULT_CSS));
        }

        return vectorDataNode;
    }

    /**
     * Converts a {@link FeatureCollection} with features having single {@link Point} geometries to a new feature collection
     * which contains just one feature with either a {@link LineString} geometry or a {@link Polygon} geometry
     * (in case of a closed line string), with the single points being vertices of this new geometry.
     *
     * @param clippedCollection
     * @return the new feature collection
     */
    static FeatureCollection<SimpleFeatureType, SimpleFeature> convertPointsToVertices(FeatureCollection<SimpleFeatureType, SimpleFeature> clippedCollection) {
        final FeatureIterator<SimpleFeature> featureIterator = clippedCollection.features();
        List<Coordinate> coordList = new ArrayList<Coordinate>();
        while (featureIterator.hasNext()) {
            final SimpleFeature feature = featureIterator.next();
            final Point pt = (Point) feature.getDefaultGeometry();
            coordList.add(pt.getCoordinate());
        }

        if (coordList.size() > 0) {
            final GeometryFactory geometryFactory = new GeometryFactory();
            SimpleFeatureType featureType;
            SimpleFeatureBuilder featureBuilder;
            SimpleFeature feature;

            try {
                if (isClosedPolygon(coordList)) {
                    featureType = DataUtilities.createType("Geometry", "geometry:Polygon");
                    featureBuilder = new SimpleFeatureBuilder(featureType);
                    feature = featureBuilder.buildFeature(Integer.toString(coordList.size() + 1));
                    final LinearRing polygon = geometryFactory.createLinearRing(coordList.toArray(new Coordinate[coordList.size()]));
                    feature.setDefaultGeometry(polygon);
                } else {
                    featureType = DataUtilities.createType("Geometry", "geometry:LineString");
                    featureBuilder = new SimpleFeatureBuilder(featureType);
                    feature = featureBuilder.buildFeature(Integer.toString(coordList.size() + 1));
                    final LineString lineString = geometryFactory.createLineString(coordList.toArray(new Coordinate[coordList.size()]));
                    feature.setDefaultGeometry(lineString);
                }
            } catch (SchemaException e) {
                BeamLogManager.getSystemLogger().warning("Cannot create line/polygon geometry: " + e.getMessage() +
                                                                 " --> Will interpret points as they are.");
                return clippedCollection;
            }

            FeatureCollection<SimpleFeatureType, SimpleFeature> vertexCollection =
                    new DefaultFeatureCollection(clippedCollection.getID() + "_vertex", featureType);
            vertexCollection.add(feature);

            return vertexCollection;
        } else {
            return clippedCollection;
        }
    }

    static boolean isClosedPolygon(List<Coordinate> coordList) {
        final double firstX = coordList.get(0).x;
        final double firstY = coordList.get(0).y;
        final double lastX = coordList.get(coordList.size() - 1).x;
        final double lastY = coordList.get(coordList.size() - 1).y;
        return (firstX == lastX && firstY == lastY);
    }

    /**
     * Collects comment lines of the form "# &lt;name&gt; = &lt;value&gt;" until the first non-empty and non-comment line is found.
     *
     * @return All the property assignments found.
     * @throws java.io.IOException
     */
    Map<String, String> readProperties() throws IOException {
        OrderRetainingMap properties = new OrderRetainingMap();
        String line;
        while ((line = reader.readLine()) != null) {
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
                reader.mark(1024 * 1024 * 10);
            } else if (line.isEmpty()) {
                reader.mark(1024 * 1024 * 10);
            } else {
                // First non-comment line reached, no more property assignments expected
                break;
            }
        }
        //noinspection unchecked
        return (Map<String, String>) properties;
    }

    FeatureCollection<SimpleFeatureType, SimpleFeature> readFeatures() throws IOException {
        final SimpleFeatureType featureType = readFeatureType();
        return readFeatures(featureType);
    }

    private InterpretationStrategy createInterpretationStrategy() throws IOException {
        reader.reset();
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
        final String defaultGeometry = properties.get("defaultGeometry");
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

                if (defaultGeometry != null && attributeName.equals(defaultGeometry)) {
                    geometryIndex = i;
                    geometryName = attributeName;
                    break;
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

        boolean hasGeometry = geometryIndex != -1;
        hasLatLon = latIndex != -1 && lonIndex != -1;
        if (!hasGeometry && (latIndex == -1 || lonIndex == -1)) {
            throw new IOException("Neither lat/lon nor geometry column provided.");
        }

        if (!hasGeometry && geoCoding == null) {
            throw new IOException("No geometry provided in product without geo-coding.");
        }

        if (hasGeometry && !hasFeatureTypeName) {
            return new GeometryNoFeatureTypeStrategy(geoCoding, geometryName);
        } else if (hasGeometry && hasFeatureTypeName) {
            return new GeometryAndFeatureTypeStrategy(geoCoding, geometryName, featureTypeName);
        } else if (hasLatLon && !hasFeatureTypeName) {
            return new LatLonNoFeatureTypeStrategy(geoCoding, latIndex, lonIndex);
        } else if (hasLatLon && hasFeatureTypeName) {
            return new LatLonAndFeatureTypeStrategy(geoCoding, featureTypeName, latIndex, lonIndex);
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
        final CoordinateReferenceSystem featureCrs = crsProvider.getFeatureCrs(product);
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

        interpretationStrategy.setDefaultGeometry(properties.get("defaultGeometry"), featureCrs, builder);
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

    public interface PlacemarkDescriptorProvider {

        PlacemarkDescriptor getPlacemarkDescriptor(SimpleFeatureType simpleFeatureType);
    }

}