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

package org.esa.beam.pet;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.converters.ArrayConverter;
import com.bc.ceres.binding.converters.FloatConverter;
import org.esa.beam.framework.datamodel.GeoPos;

public class GeoPosConverter implements Converter<GeoPos> {

    @Override
    public Class<GeoPos> getValueType() {
        return GeoPos.class;
    }

    @Override
    public GeoPos parse(String text) throws ConversionException {
        if (text.isEmpty()) {
            return null;
        }

        final float[] array;
        try {
            array = (float[]) new ArrayConverter(float[].class, new FloatConverter()).parse(text);
        } catch (ConversionException e) {
            throw new ConversionException(
                    String.format("Cannot parse coordinate '%s'", text), e);
        }

        if (array.length != 2) {
            throw new ConversionException(String.format("Cannot parse coordinate '%s'", text));
        }

        final float lat = array[0];
        final float lon = array[1];

        try {
            return new GeoPos(lat, lon);
        } catch (Exception e) {
            throw new ConversionException(String.format("Cannot parse coordinate '%s'", text), e);
        }
    }

    @Override
    public String format(GeoPos geoPos) {
        if (geoPos == null) {
            return "";
        }
        
        return String.format("%f,%f", geoPos.lat, geoPos.lon);
    }
}
