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

package org.esa.snap.core.util;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class AwtGeomToJtsGeomConverter {

    private final GeometryFactory geometryFactory;
    private final double flatness;

    public AwtGeomToJtsGeomConverter() {
        this(new GeometryFactory(), -1.0);
    }

    /**
     * Contructor.
     * @param geometryFactory The geometry factory.
     * @param flatness Used to decompose curved shapes into linear segments. If less than or equal to
     * zero, then actual flatness will be computed from shape bounds. 
     */
    public AwtGeomToJtsGeomConverter(GeometryFactory geometryFactory, double flatness) {
        this.geometryFactory = geometryFactory;
        this.flatness = flatness;
    }

    public Point createPoint(Point2D point) {
        return geometryFactory.createPoint(new Coordinate(point.getX(), point.getY()));
    }

    public MultiLineString createMultiLineString(Shape shape) {
        List<LineString> lineStringList = createLineStringList(shape);
        LineString[] lineStrings = lineStringList.toArray(new LineString[lineStringList.size()]);
        return geometryFactory.createMultiLineString(lineStrings);
    }

    public Polygon createPolygon(Shape shape) {
        List<LinearRing> linearRings = createLinearRingList(shape);
        LinearRing exteriorRing = linearRings.get(0);
        LinearRing[] interiorRings = null;
        if (linearRings.size() > 1) {
            List<LinearRing> subList = linearRings.subList(1, linearRings.size());
            interiorRings = subList.toArray(new LinearRing[subList.size()]);
        }
        return geometryFactory.createPolygon(exteriorRing, interiorRings);
    }

    public MultiPolygon createMultiPolygon(Shape shape) {
        List<LinearRing> linearRings = createLinearRingList(shape);
        Polygon[] polygons = new Polygon[linearRings.size()];
        for (int i = 0; i < linearRings.size(); i++) {
            LinearRing linearRing = linearRings.get(i);
            polygons[i] = geometryFactory.createPolygon(linearRing, null);
        }
        return geometryFactory.createMultiPolygon(polygons);
    }

    public List<LinearRing> createLinearRingList(Shape shape) {
        List<List<Coordinate>> pathList = createPathList(shape, true);
        List<LinearRing> linearRingList = new ArrayList<LinearRing>();
        for (List<Coordinate> path : pathList) {
            Coordinate[] pathCoordinates = path.toArray(new Coordinate[path.size()]);
            linearRingList.add(geometryFactory.createLinearRing(pathCoordinates));
        }
        return linearRingList;
    }

    public List<LineString> createLineStringList(Shape geometry) {
        List<List<Coordinate>> pathList = createPathList(geometry, false);
        List<LineString> strings = new ArrayList<LineString>();
        for (List<Coordinate> path : pathList) {
            strings.add(geometryFactory.createLineString(path.toArray(new Coordinate[path.size()])));
        }
        return strings;
    }

    public List<List<Coordinate>> createPathList(Shape shape, boolean forceClosedPaths) {
        return createPathList(shape, flatness, forceClosedPaths);
    }

    private List<List<Coordinate>> createPathList(Shape shape, double flatness, boolean forceClosedPaths) {
        List<Coordinate> path = new ArrayList<Coordinate>(16);
        List<List<Coordinate>> pathList = new ArrayList<List<Coordinate>>(4);
        PathIterator pathIterator;
        if (flatness <= 0.0) {
            Rectangle2D d = shape.getBounds2D();
            flatness = Math.max(d.getWidth(), d.getHeight()) / 100.0;
        }
        pathIterator = shape.getPathIterator(null, flatness);
        double[] seg = new double[6];
        int segType = -1;
        while (!pathIterator.isDone()) {
            segType = pathIterator.currentSegment(seg);
            if (segType == PathIterator.SEG_CLOSE) {
                collectPath(path, forceClosedPaths, pathList);
                path = new ArrayList<Coordinate>(16);
            } else {
                path.add(new Coordinate(seg[0], seg[1]));
            }
            pathIterator.next();
        }
        if (segType != PathIterator.SEG_CLOSE) {
            collectPath(path, forceClosedPaths, pathList);
        }
        return pathList;
    }

    private void collectPath(List<Coordinate> path, boolean forceClosedPaths, List<List<Coordinate>> pathList) {
        if (forceClosedPaths) {
            forcePathClosed(path);
        }
        pathList.add(path);
    }

    private void forcePathClosed(List<Coordinate> path) {
        Coordinate first = path.get(0);
        Coordinate last = path.get(path.size() - 1);
        if (!first.equals2D(last)) {
            path.add(new Coordinate(first.x, first.y));
        }
    }
}
