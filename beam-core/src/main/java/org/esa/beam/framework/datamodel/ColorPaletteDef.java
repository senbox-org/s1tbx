/*
 * $Id: ColorPaletteDef.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.datamodel;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import org.esa.beam.util.Guardian;
import org.esa.beam.util.PropertyMap;

/**
 * The <code>ColorPaletteDef</code> class represents a curve that is used to transform the sample values of a
 * geo-physical band into color palette indexes.
 * <p/>
 * <p> This special implemnentation of a gradation curve also provides separate color values for each of the tie points
 * contained in the curve. This allows a better image interpretation because certain colors correspond to certain sample
 * values even if the curve points are used to create color gradient palettes.
 */
public class ColorPaletteDef {

	private final static String _PROPERTY_KEY_NUM_POINTS = "numPoints";
    private final static String _PROPERTY_KEY_COLOR = "color";
    private final static String _PROPERTY_KEY_SAMPLE = "sample";
    
    /**
     * this curve's points
     */
    private Vector _points;

    public ColorPaletteDef(double minSample, double maxSample) {
        this(minSample, 0.5F * (maxSample + minSample), maxSample);
    }

    public ColorPaletteDef(double minSample, double centerSample, double maxSample) {
        this(new ColorPaletteDef.Point[]{
            new ColorPaletteDef.Point(minSample, Color.black),
            new ColorPaletteDef.Point(centerSample, Color.gray),
            new ColorPaletteDef.Point(maxSample, Color.white)
        });
    }

    public ColorPaletteDef(Point[] points) {
        Guardian.assertNotNull("points", points);
        Guardian.assertGreaterThan("points.length", points.length, 1);
        _points = new Vector(points.length);
        for (int i = 0; i < points.length; i++) {
            _points.add(points[i]);
        }
    }

    public int getNumPoints() {
        return _points.size();
    }

    public Point getPointAt(int index) {
        return (Point) _points.get(index);
    }

    public Point getFirstPoint() {
        return (Point) _points.firstElement();
    }

    public Point getLastPoint() {
        return (Point) _points.lastElement();
    }

    public void insertPointAfter(int index, Point point) {
        _points.insertElementAt(point, index + 1);
    }

    /**
     * creates a new point between the point at the given index
     */
    public boolean createPointAfter(int index, Scaling scaling) {
        Point point1 = getPointAt(index);
        Point point2 = null;
        if (index < _points.indexOf(_points.lastElement())) {
            point2 = getPointAt(index + 1);
        }
        final Point newPoint;
        if (point2 != null) {
            final double max = Math.max(point1.getSample(), point2.getSample());
            final double min = Math.min(point1.getSample(), point2.getSample());
            final double middle;
            middle = scaling.scale(0.5 * (scaling.scaleInverse(min) + scaling.scaleInverse(max)));
            newPoint = new Point(middle, createCenterColor(point1, point2));
            insertPointAfter(index, newPoint);
            return true;
        }
        return false;
    }

    /**
     * creates the center color between the given two points
     */
    public static Color createCenterColor(Point p1, Point p2) {
        return new Color(0.5F * (p1.getColor().getRed() + p2.getColor().getRed()) / 255.0F,
                         0.5F * (p1.getColor().getGreen() + p2.getColor().getGreen()) / 255.0F,
                         0.5F * (p1.getColor().getBlue() + p2.getColor().getBlue()) / 255.0F);
    }


    public void removePointAt(int index) {
        check2PointsMinimum();
        _points.remove(index);
    }

    public void addPoint(Point point) {
        _points.add(point);
    }

    public Point[] getPoints() {
        Point[] points = new Point[getNumPoints()];
        for (int i = 0; i < getNumPoints(); i++) {
            points[i] = getPointAt(i);
        }
        return points;
    }

    public Iterator getIterator() {
        return _points.iterator();
    }

    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (other instanceof ColorPaletteDef) {
            ColorPaletteDef gradationCurve = (ColorPaletteDef) other;
            return gradationCurve._points.equals(_points);
        } else {
            return false;
        }
    }

    public Object clone() {
        return createDeepCopy();
    }

    public ColorPaletteDef createDeepCopy() {
        ColorPaletteDef.Point[] points = getPoints();
        for (int i = 0; i < points.length; i++) {
            points[i] = points[i].createClone();
        }
        return new ColorPaletteDef(points);
    }
    
    /**
     * Loads a color palette definition from the given file
     *
     * @param file the file
     *
     * @return the color palette definition, never null
     *
     * @throws IOException if an I/O error occurs
     */
    public static ColorPaletteDef loadColorPaletteDef(File file) throws IOException {
    	final PropertyMap propertyMap = new PropertyMap();
        propertyMap.load(file); // Overwrite existing values
        final int numPoints = propertyMap.getPropertyInt(_PROPERTY_KEY_NUM_POINTS);
        if (numPoints < 2) {
            throw new IOException("The selected file contains less than\n" +
                            "two color points.");
        }
        final ColorPaletteDef.Point[] points = new ColorPaletteDef.Point[numPoints];
        double lastSample = 0;
        for (int i = 0; i < points.length; i++) {
            final ColorPaletteDef.Point point = new ColorPaletteDef.Point();
            final Color color = propertyMap.getPropertyColor(_PROPERTY_KEY_COLOR + i);
            double sample = propertyMap.getPropertyDouble(_PROPERTY_KEY_SAMPLE + i);
            if (i > 0 && sample < lastSample) {
                sample = lastSample + 1.0;
            }
            point.setColor(color);
            point.setSample(sample);
            points[i] = point;
            lastSample = sample;
        }
        return new ColorPaletteDef(points);
    }
    
    /**
     * Stores this color palette definition in the given file
     *
     * @param file the file
     *
     * @throws IOException if an I/O error occurs
     */
    public static void storeColorPaletteDef(ColorPaletteDef colorPaletteDef, File file) throws IOException {
    	final ColorPaletteDef.Point[] points = colorPaletteDef.getPoints();
        final PropertyMap propertyMap = new PropertyMap();
        final int numPoints = points.length;
        propertyMap.setPropertyInt(_PROPERTY_KEY_NUM_POINTS, numPoints);
        for (int i = 0; i < numPoints; i++) {
            propertyMap.setPropertyColor(_PROPERTY_KEY_COLOR + i, points[i].getColor());
            propertyMap.setPropertyDouble(_PROPERTY_KEY_SAMPLE + i, points[i].getSample());
        }
        propertyMap.store(file, "BEAM Color Palette Definition File"); /*I18N*/
    }

    private void check2PointsMinimum() {
        if (getNumPoints() == 2) {
            throw new IllegalStateException("gradation curve must at least have 2 points");
        }
    }

    /**
     * Releases all of the resources used by this color palette definition and all of its owned children. Its primary
     * use is to allow the garbage collector to perform a vanilla job.
     * <p/>
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>dispose()</code> are undefined.
     * <p/>
     * <p>Overrides of this method should always call <code>super.dispose();</code> after disposing this instance.
     */
    public void dispose() {
        if (_points != null) {
            _points.removeAllElements();
            _points = null;
        }
    }

    public static class Point {

        private double _sample;
        private Color _color;

        public Point() {
            this(0, Color.black);
        }

        public Point(double sample, Color color) {
            _sample = sample;
            _color = color;
        }

        public double getSample() {
            return _sample;
        }

        public void setSample(double sample) {
            _sample = sample;
        }

        public Color getColor() {
            return _color;
        }

        public void setColor(Color color) {
            _color = color;
        }

        public boolean equals(Object other) {
            if (other == this) {
                return true;
            } else if (other instanceof Point) {
                Point p = (Point) other;
                if (p.getSample() != getSample()) {
                    return false;
                }
                return p.getColor().equals(getColor());
            } else {
                return false;
            }
        }

        public Object clone() {
            return createClone();
        }

        public Point createClone() {
            return new Point(getSample(), getColor());
        }

    }
}
