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

package org.esa.snap.core.dataio.geometry;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import org.esa.snap.core.datamodel.PlacemarkDescriptor;
import org.esa.snap.core.datamodel.PointDescriptor;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.core.util.FeatureUtils;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.converters.JavaTypeConverter;
import org.esa.snap.core.util.io.CsvReader;
import org.esa.snap.core.util.io.FileUtils;
import org.geotools.data.DataUtilities;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reader that creates an instance of {@link VectorDataNode} for a given CSV (character separated values) input.
 * Clients need to specify:
 * <ul>
 * <li>a strategy for receiving the CRS the vector data is based on (given by an instance of
 * {@link FeatureUtils.FeatureCrsProvider})</li>
 * <li>a strategy for receiving the instance of {@link PlacemarkDescriptor} which is responsible for creating placemarks
 * from the vector data (given by an instance of {@link PlacemarkDescriptorProvider})</li>
 * <li>The model CRS of the target product</li>
 * </ul>
 * The vector data will be read, clipped according to the product's bounds, and projected onto the model CRS of the
 * target product.
 */
public class VectorDataNodeReader {

    private final String sourceName;
    private final Product product;
    private final FeatureUtils.FeatureCrsProvider crsProvider;
    private final PlacemarkDescriptorProvider placemarkDescriptorProvider;
    private final boolean convertToVertices;
    private final CsvReader reader;

    private static final String[] LONGITUDE_IDENTIFIERS = new String[]{"lon", "long", "longitude", "lon_IS"};
    private static final String[] LATITUDE_IDENTIFIERS = new String[]{"lat", "latitude", "lat_IS"};
    private static final String[] GEOMETRY_IDENTIFIERS = new String[]{"geometry", "geom", "the_geom"};

    private Map<String, String> properties;
    private InterpretationStrategy interpretationStrategy;

    private VectorDataNodeReader(String sourceName, Product product, Reader reader, FeatureUtils.FeatureCrsProvider crsProvider,
                                 PlacemarkDescriptorProvider placemarkDescriptorProvider, boolean convertToVertices, char delimiterChar) throws IOException {
        this.product = product;
        this.crsProvider = crsProvider;
        this.placemarkDescriptorProvider = placemarkDescriptorProvider;
        this.convertToVertices = convertToVertices;
        this.sourceName = sourceName;
        this.reader = new CsvReader(reader, new char[]{delimiterChar}, true, "#");
    }

    /**
     * Reads a {@link VectorDataNode} from the given input.
     *
     * @param sourceName                  The name of the data source; typically a file name.
     * @param reader                      A reader for the CSV data.
     * @param product                     The product the vector data will be added to.
     * @param crsProvider                 A strategy for receiving the CRS of the vector data.
     * @param placemarkDescriptorProvider A strategy for receiving the placemark descriptor.
     * @param modelCrs                    The model CRS of the target product.
     * @param delimiterChar               The separation character of the CSV data.
     * @param pm                          A progress monitor.
     * @return A {@link VectorDataNode} containing features according to the input data, or {@code null} if no
     *         placemark descriptor can be found.
     * @throws IOException if the vector data could not be read.
     */
    public static VectorDataNode read(String sourceName, Reader reader, Product product, FeatureUtils.FeatureCrsProvider crsProvider,
                                      PlacemarkDescriptorProvider placemarkDescriptorProvider, CoordinateReferenceSystem modelCrs,
                                      char delimiterChar, ProgressMonitor pm) throws IOException {
        return new VectorDataNodeReader(sourceName, product, reader, crsProvider, placemarkDescriptorProvider, true, delimiterChar).read(modelCrs, pm);
    }

    /**
     * Reads a {@link VectorDataNode} from the given input.
     *
     * @param sourceName                  The name of the data source; typically a file name.
     * @param reader                      A reader for the CSV data.
     * @param product                     The product the vector data will be added to.
     * @param crsProvider                 A strategy for receiving the CRS of the vector data.
     * @param placemarkDescriptorProvider A strategy for receiving the placemark descriptor.
     * @param modelCrs                    The model CRS of the target product.
     * @param delimiterChar               The separation character of the CSV data.
     * @param pm                          A progress monitor.
     * @return A {@link VectorDataNode} containing features according to the input data, or {@code null} if no
     *         placemark descriptor can be found.
     * @throws IOException if the vector data could not be read.
     */
    public static VectorDataNode read(String sourceName, Reader reader, Product product, FeatureUtils.FeatureCrsProvider crsProvider,
                                      PlacemarkDescriptorProvider placemarkDescriptorProvider, CoordinateReferenceSystem modelCrs,
                                      char delimiterChar, boolean convertToVertices, ProgressMonitor pm) throws IOException {
        return new VectorDataNodeReader(sourceName, product, reader, crsProvider, placemarkDescriptorProvider, convertToVertices, delimiterChar).read(modelCrs, pm);
    }

    VectorDataNode read(CoordinateReferenceSystem modelCrs, ProgressMonitor pm) throws IOException {
        String nodeName = FileUtils.getFilenameWithoutExtension(sourceName);
        pm.beginTask("Reading vector data node '" + nodeName + "'", 100);
        reader.mark(1024 * 1024 * 10);
        readProperties();
        pm.worked(5);
        interpretationStrategy = createInterpretationStrategy();
        pm.worked(5);
        final CoordinateReferenceSystem featureCrs = crsProvider.getFeatureCrs(product);
        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = readFeatures(featureCrs);
        pm.worked(45);

        final boolean needsClipping = !CRS.equalsIgnoreMetadata(featureCrs, modelCrs);
        final boolean clipToProductBounds = crsProvider.clipToProductBounds();
        FeatureCollection<SimpleFeatureType, SimpleFeature> clippedCollection;
        if (product.getSceneGeoCoding() != null && featureCollection.size() > 0 && (needsClipping || clipToProductBounds)) {
            final Geometry clipGeometry = FeatureUtils.createGeoBoundaryPolygon(product);
            clippedCollection = FeatureUtils.clipCollection(featureCollection,
                                                            null,
                                                            clipGeometry,
                                                            product.getSceneGeoCoding().getGeoCRS(),
                                                            null,
                                                            modelCrs,
                                                            SubProgressMonitor.create(pm, 45));
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

        if (convertToVertices && placemarkDescriptor instanceof PointDescriptor && clippedCollection.size() > 0) {
            clippedCollection = convertPointsToVertices(clippedCollection);
        }

        VectorDataNode vectorDataNode = new VectorDataNode(nodeName, clippedCollection, placemarkDescriptor);
        if (properties.containsKey(ProductNode.PROPERTY_NAME_DESCRIPTION)) {
            featureType.getUserData().put(ProductNode.PROPERTY_NAME_DESCRIPTION, properties.get(ProductNode.PROPERTY_NAME_DESCRIPTION));
            vectorDataNode.setDescription(properties.get(ProductNode.PROPERTY_NAME_DESCRIPTION));
        }
        if (properties.containsKey(VectorDataNodeIO.PROPERTY_NAME_DEFAULT_CSS)) {
            featureType.getUserData().put(VectorDataNodeIO.PROPERTY_NAME_DEFAULT_CSS, properties.get(VectorDataNodeIO.PROPERTY_NAME_DEFAULT_CSS));
            vectorDataNode.setDefaultStyleCss(properties.get(VectorDataNodeIO.PROPERTY_NAME_DEFAULT_CSS));
        }

        pm.done();
        return vectorDataNode;
    }

    /**
     * Collects comment lines of the form "# &lt;name&gt; = &lt;value&gt;" until the first non-empty and non-comment line is found.
     *
     * @throws java.io.IOException in case of an IO-error
     */
    private void readProperties() throws IOException {
        properties = new LinkedHashMap<>();
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
    }

    private FeatureCollection<SimpleFeatureType, SimpleFeature> readFeatures(CoordinateReferenceSystem featureCrs) throws IOException {
        final SimpleFeatureType featureType = readFeatureType(featureCrs);
        return readFeatures(featureType);
    }

    private InterpretationStrategy createInterpretationStrategy() throws IOException {
        reader.reset();
        String[] tokens = reader.readRecord();
        reader.reset();

        if (tokens == null) {
            throw new IOException(String.format("Invalid header in file '%s'", sourceName));
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
            if (i == 0 && (tokens[0].startsWith("org.esa.snap") || tokens[0].startsWith("org.esa.beam") /* for backward compatibility*/ )) {
                hasFeatureTypeName = true;
                featureTypeName = tokens[0];
            } else {
                String token = tokens[i];
                final int colonPos = token.indexOf(':');
                String attributeName;
                switch (colonPos) {
                    case -1:
                        attributeName = token;
                        break;
                    case 0:
                        throw new IOException(String.format("Missing name specifier in attribute descriptor '%s'", token));
                    default:
                        attributeName = token.substring(0, colonPos);
                        break;
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

        if (hasGeometry && !hasFeatureTypeName) {
            return new GeometryNoFeatureTypeStrategy(geometryName);
        } else if (hasGeometry && hasFeatureTypeName) {
            return new GeometryAndFeatureTypeStrategy(geometryName, featureTypeName);
        } else if (hasLatLon && !hasFeatureTypeName) {
            return new LatLonNoFeatureTypeStrategy(latIndex, lonIndex);
        } else if (hasLatLon && hasFeatureTypeName) {
            return new LatLonAndFeatureTypeStrategy(featureTypeName, latIndex, lonIndex);
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
                SystemUtils.LOG.warning(String.format("Unable to parse %s: %s", sourceName, e.getMessage()));
            } catch (TransformException e) {
                throw new IOException(e);
            }

            if (simpleFeature != null) {
                fc.add(simpleFeature);
            }
        }
        return fc;
    }

    private boolean isLineValid(SimpleFeatureType simpleFeatureType, String[] tokens) {
        int expectedTokenCount = interpretationStrategy.getExpectedTokenCount(simpleFeatureType.getAttributeCount());
        if (tokens.length != expectedTokenCount) {
            SystemUtils.LOG.warning(String.format("Problem in '%s': unexpected number of columns: expected %d, but got %d",
                                                  sourceName, expectedTokenCount, tokens.length));
            return false;
        }
        return true;
    }

    private SimpleFeatureType readFeatureType(CoordinateReferenceSystem featureCrs) throws IOException {
        String[] tokens = reader.readRecord();
        if (tokens == null || tokens.length <= 1) {
            throw new IOException("Missing feature type definition in first line.");
        }
        reader.mark(1024 * 1024 * 10);
        return createFeatureType(tokens, featureCrs);
    }

    private SimpleFeatureType createFeatureType(String[] tokens, CoordinateReferenceSystem featureCrs) throws IOException {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
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
            switch (colonPos) {
                case 0:
                    throw new IOException(String.format("Missing name specifier in attribute descriptor '%s'", token));
                case -1:
                    attributeName = token;
                    attributeTypeName = findAttributeTypeName(firstRecord == null ? "" : firstRecord[i]);
                    break;
                default:
                    attributeTypeName = token.substring(colonPos + 1);
                    attributeName = token.substring(0, colonPos);
                    break;
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
            //noinspection ResultOfMethodCallIgnored
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

    /**
     * Converts a {@link FeatureCollection} with features having single {@link Point} geometries to a new feature collection
     * which contains just one feature with either a {@link LineString} geometry or a {@link Polygon} geometry
     * (in case of a closed line string), with the single points being vertices of this new geometry.
     *
     * @param featureCollection The feature collection to be converted.
     * @return the new feature collection
     */
    static FeatureCollection<SimpleFeatureType, SimpleFeature> convertPointsToVertices(FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection) {
        final FeatureIterator<SimpleFeature> featureIterator = featureCollection.features();
        List<Coordinate> coordList = new ArrayList<>();
        while (featureIterator.hasNext()) {
            final SimpleFeature feature = featureIterator.next();
            final Point pt = (Point) feature.getDefaultGeometry();
            coordList.add(pt.getCoordinate());
        }

        if (coordList.size() >= 2) {
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
                SystemUtils.LOG.warning("Cannot create line/polygon geometry: " + e.getMessage() +
                                                " --> Will interpret points as they are.");
                return featureCollection;
            }

            DefaultFeatureCollection vertexCollection = new DefaultFeatureCollection(featureCollection.getID() + "_vertex", featureType);
            vertexCollection.add(feature);

            return vertexCollection;
        } else {
            return featureCollection;
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
     * A strategy for receiving an instance of {@link PlacemarkDescriptor}.
     */
    public interface PlacemarkDescriptorProvider {

        /**
         * Returns a placemark descriptor fitting to the input feature type.
         *
         * @param simpleFeatureType The feature type on which the placemark descriptor choice may be based upon.
         * @return An instance of {@link PlacemarkDescriptor}.
         */
        PlacemarkDescriptor getPlacemarkDescriptor(SimpleFeatureType simpleFeatureType);
    }

}
