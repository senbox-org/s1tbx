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

import com.bc.ceres.binding.Converter;

import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.converters.JavaTypeConverter;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

public class VectorDataNodeWriter {

    private static long id = System.nanoTime();

    public void write(VectorDataNode vectorDataNode, File file) throws IOException {
        FileWriter writer = new FileWriter(file);
        try {
            writeNodeProperties(vectorDataNode, writer);
            writeFeatures(vectorDataNode.getFeatureCollection(), writer);
        } finally {
            writer.close();
        }
    }

    public void writeFeatures(FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection, Writer writer) throws IOException {
        SimpleFeatureType simpleFeatureType = featureCollection.getSchema();
        writeFeatureType(simpleFeatureType, writer);
        writeFeatures0(featureCollection, writer);
    }

    private void writeNodeProperties(VectorDataNode vectorDataNode, Writer writer) throws IOException {
        String description = vectorDataNode.getDescription();
        if (StringUtils.isNotNullAndNotEmpty(description)) {
            writer.write("#" + ProductNode.PROPERTY_NAME_DESCRIPTION + "=" + description+"\n");
        }
        String defaultCSS = vectorDataNode.getDefaultCSS();
        if (StringUtils.isNotNullAndNotEmpty(defaultCSS)) {
            writer.write("#" + VectorDataNodeIO.PROPERTY_NAME_DEFAULT_CSS + "=" + defaultCSS+"\n");
        }
    }

    private void writeFeatures0(FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection, Writer writer) throws IOException {
        Converter[] converters = VectorDataNodeIO.getConverters(featureCollection.getSchema());
        Iterator<SimpleFeature> featureIterator = featureCollection.iterator();

        while (featureIterator.hasNext()) {
            SimpleFeature simpleFeature = featureIterator.next();

            String fid = simpleFeature.getID();
            if (fid == null || fid.isEmpty()) {
                fid = String.format("FID%s", Long.toHexString(id++));
            }
            writer.write(fid);

            List<Object> attributes = simpleFeature.getAttributes();
            for (int i = 0; i < attributes.size(); i++) {
                Object value = attributes.get(i);
                String text = VectorDataNodeIO.NULL_TEXT;
                if (value != null) {
                    Converter converter = converters[i];
                    text = converter.format(value);
                    text = VectorDataNodeIO.encodeTabString(text);
                }
                writer.write(VectorDataNodeIO.DELIMITER_CHAR);
                writer.write(text);
            }
            writer.write('\n');
        }
    }

    private void writeFeatureType(SimpleFeatureType simpleFeatureType, Writer writer) throws IOException {

        writer.write(simpleFeatureType.getTypeName());

        List<AttributeDescriptor> attributeDescriptors = simpleFeatureType.getAttributeDescriptors();
        JavaTypeConverter typeConverter = new JavaTypeConverter();
        for (int i = 0; i < attributeDescriptors.size(); i++) {
            AttributeDescriptor attributeDescriptor = attributeDescriptors.get(i);
            Class<?> binding = attributeDescriptor.getType().getBinding();
            String name = attributeDescriptor.getLocalName();
            String type = typeConverter.format(binding);

            writer.write(VectorDataNodeIO.DELIMITER_CHAR);
            writer.write(name);
            writer.write(':');
            writer.write(type);
        }
        writer.write('\n');
    }
}
