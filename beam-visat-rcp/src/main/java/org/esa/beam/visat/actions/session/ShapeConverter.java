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

package org.esa.beam.visat.actions.session;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.util.ArrayList;

/**
 * A converter for {@link java.awt.Shape}s.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class ShapeConverter implements Converter {
    private final GeometryFactory geometryFactory;

    public ShapeConverter() {
        geometryFactory = new GeometryFactory();
    }

    @Override
    public Class getValueType() {
        return Shape.class;
    }

    @Override
    public Object parse(String text) throws ConversionException {
        try {
            Geometry geometry = new WKTReader(geometryFactory).read(text);
            if (geometry instanceof LineString) {
                LineString lineString = (LineString) geometry;
                // todo
                return null;
            } else if (geometry instanceof Polygon) {
                Polygon polygon = (Polygon) geometry;
                // todo
                return null;
            } else {
                throw new ConversionException("Failed to parse shape geometry WKT.");
            }
        } catch (ParseException e) {
            throw new ConversionException("Failed to parse shape geometry WKT.", e);
        }
    }

    @Override
    public String format(Object value) {
        Shape shape = (Shape) value;
        PathIterator pathIterator = shape.getPathIterator(null, 1.0);
        ArrayList<Coordinate> coordinates = new ArrayList<Coordinate>();
        ArrayList<Geometry> geometries = new ArrayList<Geometry>();
        double[] coord = new double[6];
        while (!pathIterator.isDone()) {
            int type = pathIterator.currentSegment(coord);
            if (type == PathIterator.SEG_MOVETO) {
                coordinatesToGeometry(coordinates, geometries);
                coordinates.add(new Coordinate(coord[0], coord[1]));
            } else if (type == PathIterator.SEG_LINETO) {
                coordinates.add(new Coordinate(coord[0], coord[1]));
            } else if (type == PathIterator.SEG_CLOSE) {
                if (coordinates.size() > 0) {
                    if (!coordinates.get(0).equals(coordinates.get(coordinates.size() - 1))) {
                        coordinates.add(coordinates.get(0));
                    }
                    coordinatesToGeometry(coordinates, geometries);
                }
            }
            pathIterator.next();
        }
        coordinatesToGeometry(coordinates, geometries);

        if (geometries.isEmpty()) {
            return "";
        }

        if (geometries.get(0) instanceof LinearRing) {
            ArrayList<LinearRing> holes = new ArrayList<LinearRing>();
            for (int i = 1; i < geometries.size(); i++) {
                Geometry geometry = geometries.get(i);
                if (geometry instanceof LinearRing) {
                    holes.add((LinearRing) geometry);
                }
            }
            if (holes.size() == geometries.size() - 1) {
                return geometryFactory.createPolygon((LinearRing) geometries.get(0),
                                                     holes.toArray(new LinearRing[holes.size()])).toText();
            }
        }
        if (geometries.size() == 1) {
            return geometries.get(0).toText();
        } else {
            return geometryFactory.createGeometryCollection(geometries.toArray(new Geometry[geometries.size()])).toText();
        }
    }

    private void coordinatesToGeometry(ArrayList<Coordinate> coordinates, ArrayList<Geometry> geometries) {
        if (coordinates.size() > 0) {
            if (coordinates.get(0).equals(coordinates.get(coordinates.size() - 1))) {
                LinearRing linearRing = geometryFactory.createLinearRing(coordinates.toArray(new Coordinate[coordinates.size()]));
                geometries.add(linearRing);
            } else {
                LineString lineString = geometryFactory.createLineString(coordinates.toArray(new Coordinate[coordinates.size()]));
                geometries.add(lineString);
            }
            coordinates.clear();
        }
    }
}
