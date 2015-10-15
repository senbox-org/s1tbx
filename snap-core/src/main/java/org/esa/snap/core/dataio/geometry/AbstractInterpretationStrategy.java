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

package org.esa.snap.core.dataio.geometry;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConverterRegistry;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.IOException;

/**
 * @author Thomas Storm
 */
abstract class AbstractInterpretationStrategy implements InterpretationStrategy {

    protected void setAttributeValue(SimpleFeatureBuilder builder, SimpleFeatureType simpleFeatureType, int attributeIndex, String token) throws IOException, ConversionException {
        token = VectorDataNodeIO.decodeTabString(token);
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
    }

}
