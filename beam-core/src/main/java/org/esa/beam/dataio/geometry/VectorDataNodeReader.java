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

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class VectorDataNodeReader {

    private final String location;
    private final CoordinateReferenceSystem modelCrs;

    public VectorDataNodeReader(String location, CoordinateReferenceSystem modelCrs) {
        this.location = location;
        this.modelCrs = modelCrs;
    }

    public static VectorDataNode read(File file, CoordinateReferenceSystem modelCrs) throws IOException {
        return new VectorDataNodeReader(file.getPath(), modelCrs).read(file);
    }

    public VectorDataNode read(File file) throws IOException {
        final String name = FileUtils.getFilenameWithoutExtension(file);
        FileReader headerReader = new FileReader(file);
        FileReader featureReader = new FileReader(file);
        try {
            return read(name, headerReader, featureReader);
        } finally {
            headerReader.close();
            featureReader.close();
        }
    }

    private VectorDataNode read(String name, Reader headerReader, Reader featureReader) throws IOException {
        Map<String, String> properties = readNodeProperties(headerReader);
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

    private Map<String, String> readNodeProperties(Reader reader) throws IOException {
        LineNumberReader lineNumberReader = new LineNumberReader(reader);
        Map<String, String> propertiesMap = new HashMap<String, String>();
        String line = lineNumberReader.readLine();
        while (line != null) {
            if (line.startsWith("#")) {
                line = line.substring(1);
                int index = line.indexOf('=');
                if (index != -1) {
                    String name = line.substring(0, index).trim();
                    String value = line.substring(index + 1).trim();
                    if (StringUtils.isNotNullAndNotEmpty(name) &&
                            StringUtils.isNotNullAndNotEmpty(value)) {
                        propertiesMap.put(name, value);
                    }
                }
            }
            line = lineNumberReader.readLine();
        }
        lineNumberReader.close();
        return propertiesMap;
    }

    public FeatureCollection<SimpleFeatureType, SimpleFeature> readFeatures(Reader reader) throws IOException {
        CsvReader csvReader = new CsvReader(reader, new char[]{VectorDataNodeIO.DELIMITER_CHAR}, true, "#");
        SimpleFeatureType type = readFeatureType(csvReader);
        return readFeatures(csvReader, type);
    }


    private FeatureCollection<SimpleFeatureType, SimpleFeature> readFeatures(CsvReader csvReader, SimpleFeatureType simpleFeatureType) throws IOException {
        Converter<?>[] converters = VectorDataNodeIO.getConverters(simpleFeatureType);

        DefaultFeatureCollection fc = new DefaultFeatureCollection("?", simpleFeatureType);
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(simpleFeatureType);
        while (true) {
            String[] tokens = csvReader.readRecord();
            if (tokens == null) {
                break;
            }
            final int expectedTokenCount = 1 + simpleFeatureType.getAttributeCount();
            if (tokens.length != expectedTokenCount) {
                BeamLogManager.getSystemLogger().warning(String.format("Problem in '%s': unexpected number of columns: expected %d, but got %d",
                                                                       location, expectedTokenCount, tokens.length));
                continue;
            }
            builder.reset();
            String fid = null;
            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i];
                if (i == 0) {
                    fid = token;
                } else {
                    token = VectorDataNodeIO.decodeTabString(token);
                    try {
                        Object value = null;
                        if (!VectorDataNodeIO.NULL_TEXT.equals(token)) {
                            value = converters[i - 1].parse(token);
                        }
                        builder.set(simpleFeatureType.getDescriptor(i - 1).getLocalName(), value);
                    } catch (ConversionException e) {
                        BeamLogManager.getSystemLogger().warning(String.format("Problem in '%s': %s",
                                                                               location, e.getMessage()));
                    }
                }
            }
            SimpleFeature simpleFeature = builder.buildFeature(fid);
            fc.add(simpleFeature);
        }
        return fc;
    }

    private SimpleFeatureType readFeatureType(CsvReader csvReader) throws IOException {
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
                builder.add(attributeName, attributeType);
            }
        }
        return builder.buildFeatureType();
    }

}
