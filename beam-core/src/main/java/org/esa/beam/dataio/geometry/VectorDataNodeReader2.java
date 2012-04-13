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
import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConverterRegistry;
import com.thoughtworks.xstream.core.util.OrderRetainingMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.converters.JavaTypeConverter;
import org.esa.beam.util.io.CsvReader;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.logging.BeamLogManager;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.Map;

public class VectorDataNodeReader2 {

    private final String location;
    private final CoordinateReferenceSystem modelCrs;
    private final GeoCoding geoCoding;
    private int latIndex = -1;
    private int lonIndex = -1;

    public VectorDataNodeReader2(GeoCoding geoCoding, String path, CoordinateReferenceSystem modelCrs) {
        this.geoCoding = geoCoding;
        this.location = path;
        this.modelCrs = modelCrs;
    }

    public static VectorDataNode read(File file, CoordinateReferenceSystem modelCrs, GeoCoding geoCoding) throws IOException {
        return new VectorDataNodeReader2(geoCoding, file.getPath(), modelCrs).read(file);
    }

    public VectorDataNode read(File file) throws IOException {
        final String name = FileUtils.getFilenameWithoutExtension(file);
        Reader headerReader = new FileReader(file);
        Reader featureReader = new FileReader(file);
        try {
            return read(name, headerReader, featureReader);
        } finally {
            headerReader.close();
            featureReader.close();
        }
    }

    private VectorDataNode read(String name, Reader headerReader, Reader featureReader) throws IOException {
        Map<String, String> properties = readProperties(headerReader);
        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = readFeatures(featureReader);
        VectorDataNode vectorDataNode = new VectorDataNode(name, featureCollection);
        if (properties.containsKey(ProductNode.PROPERTY_NAME_DESCRIPTION)) {
            vectorDataNode.setDescription(properties.get(ProductNode.PROPERTY_NAME_DESCRIPTION));
        }
        if (properties.containsKey(VectorDataNodeIO.PROPERTY_NAME_DEFAULT_CSS)) {
            vectorDataNode.setDefaultStyleCss(properties.get(VectorDataNodeIO.PROPERTY_NAME_DEFAULT_CSS));
        }
        return vectorDataNode;
    }

    /**
     * Collects comment lines of the form "# &lt;name&gt; = &lt;value&gt;" until the first non-empty and non-comment line is found.
     * Always closes the reader finally.
     *
     * @param reader A reader
     *
     * @return All the property assignments found.
     *
     * @throws java.io.IOException
     */
    Map<String, String> readProperties(Reader reader) throws IOException {
        LineNumberReader lineNumberReader = new LineNumberReader(reader);
        OrderRetainingMap properties = new OrderRetainingMap();
        try {
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
        } finally {
            lineNumberReader.close();
        }
        //noinspection unchecked
        return (Map<String, String>) properties;
    }

    public FeatureCollection<SimpleFeatureType, SimpleFeature> readFeatures(Reader reader) throws IOException {
        CsvReader csvReader = new CsvReader(reader, new char[]{VectorDataNodeIO.DELIMITER_CHAR}, true, "#");
        SimpleFeatureType featureType = readFeatureType(csvReader);
        return readFeatures(csvReader, featureType);
    }


    private FeatureCollection<SimpleFeatureType, SimpleFeature> readFeatures(CsvReader csvReader, SimpleFeatureType simpleFeatureType) throws IOException {
        DefaultFeatureCollection fc = new DefaultFeatureCollection("?", simpleFeatureType);
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(simpleFeatureType);
        PixelPos pixelPos = new PixelPos();
        while (true) {
            String[] tokens = csvReader.readRecord();
            if (tokens == null) {
                break;
            }
            final int expectedTokenCount = 1 + simpleFeatureType.getAttributeCount() - 2;
            if (tokens.length != expectedTokenCount) {
                BeamLogManager.getSystemLogger().warning(String.format("Problem in '%s': unexpected number of columns: expected %d, but got %d",
                                                                       location, expectedTokenCount, tokens.length));
                continue;
            }
            builder.reset();
            String fid = null;
            double lat = 0;
            double lon = 0;
            int attributeIndex = 0;
            for (int columnIndex = 0; columnIndex < tokens.length; columnIndex++) {
                String token = tokens[columnIndex];
                if (columnIndex == 0) {
                    fid = token;
                } else if (columnIndex == latIndex) {
                    lat = Double.parseDouble(token);
                    attributeIndex++;
                } else if (columnIndex == lonIndex) {
                    lon = Double.parseDouble(token);
                    attributeIndex++;
                } else {
                    token = VectorDataNodeIO.decodeTabString(token);
                    try {
                        Object value = null;
                        if (!VectorDataNodeIO.NULL_TEXT.equals(token)) {
                            Class<?> attributeType = simpleFeatureType.getType(attributeIndex).getBinding();
                            ConverterRegistry converterRegistry = ConverterRegistry.getInstance();
                            Converter<?> converter = converterRegistry.getConverter(attributeType);
                            if (converter == null) {
                                throw new IOException(String.format("No converter for type %s found.", attributeType));
                            }
                            value = converter.parse(token);
                        }
                        builder.set(simpleFeatureType.getDescriptor(attributeIndex).getLocalName(), value);
                    } catch (ConversionException e) {
                        BeamLogManager.getSystemLogger().warning(String.format("Problem in '%s': %s",
                                                                               location, e.getMessage()));
                    }
                    attributeIndex++;
                }
            }
            builder.set("geoPos", new GeometryFactory().createPoint(new Coordinate(lon, lat)));
            geoCoding.getPixelPos(new GeoPos((float) lat, (float) lon), pixelPos);
            builder.set("pixelPos", new GeometryFactory().createPoint(new Coordinate(pixelPos.x, pixelPos.y)));
            SimpleFeature simpleFeature = builder.buildFeature(fid);
            fc.add(simpleFeature);
        }
        return fc;
    }

    SimpleFeatureType readFeatureType(CsvReader csvReader) throws IOException {
        String[] tokens = csvReader.readRecord();
        if (tokens == null || tokens.length <= 1) {
            throw new IOException("Missing feature type definition in first line.");
        }
        return createFeatureType(tokens);
    }

    private SimpleFeatureType createFeatureType(String[] tokens) throws IOException {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setCRS(modelCrs);
        JavaTypeConverter jtc = new JavaTypeConverter();
        for (int i = 0; i < tokens.length; i++) {
            if (i == 0) {
                builder.setName(tokens[0]);
            } else {
                String token = tokens[i];
                final int colonPos = token.indexOf(':');
                if (colonPos == -1) {
                    throw new IOException(String.format("Missing type specifier in attribute descriptor '%s'", token));
                } else if (colonPos == 0) {
                    throw new IOException(String.format("Missing name specifier in attribute descriptor '%s'", token));
                }
                String attributeName = token.substring(0, colonPos);
                String attributeTypeName = token.substring(colonPos + 1);
                Class<?> attributeType;
                try {
                    attributeType = jtc.parse(attributeTypeName);
                } catch (ConversionException e) {
                    throw new IOException(
                            String.format("Unknown type in attribute descriptor '%s'", token), e);
                }

                if (attributeName.toLowerCase().equals("lon") || attributeName.toLowerCase().equals("long") ||
                    attributeName.toLowerCase().equals("longitude")) {
                    lonIndex = i;
                } else if (attributeName.toLowerCase().equals("lat") ||
                           attributeName.toLowerCase().equals("latitude")) {
                    latIndex = i;
                }
                builder.add(attributeName, attributeType);
            }
        }
        builder.add("geoPos", Point.class);
        builder.add("pixelPos", Point.class);
        builder.setDefaultGeometry("pixelPos");
        return builder.buildFeatureType();
    }
}