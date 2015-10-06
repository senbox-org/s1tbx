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

package org.esa.snap.core.util.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConverterRegistry;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;

public class JtsGeometryConverter implements Converter<Geometry> {

    @Override
    public Class<? extends Geometry> getValueType() {
        return Geometry.class;
    }

    @Override
    public Geometry parse(String text) throws ConversionException {
        if (text.isEmpty()) {
            return null;
        }
        try {
            return new WKTReader().read(text);
        } catch (ParseException e) {
            throw new ConversionException("Could not parse geometry.", e);
        }
    }

    @Override
    public String format(Geometry value) {
        if (value == null) {
            return "";
        }
        return new WKTWriter().write(value);
    }

    public static void registerConverter() {
        JtsGeometryConverter geometryConverter = new JtsGeometryConverter();
        ConverterRegistry.getInstance().setConverter(Geometry.class, geometryConverter);
        ConverterRegistry.getInstance().setConverter(Point.class, geometryConverter);
        ConverterRegistry.getInstance().setConverter(MultiPoint.class, geometryConverter);
        ConverterRegistry.getInstance().setConverter(LineString.class, geometryConverter);
        ConverterRegistry.getInstance().setConverter(MultiLineString.class, geometryConverter);
        ConverterRegistry.getInstance().setConverter(LinearRing.class, geometryConverter);
        ConverterRegistry.getInstance().setConverter(Polygon.class, geometryConverter);
        ConverterRegistry.getInstance().setConverter(MultiPolygon.class, geometryConverter);
        ConverterRegistry.getInstance().setConverter(GeometryCollection.class, geometryConverter);
    }
}
