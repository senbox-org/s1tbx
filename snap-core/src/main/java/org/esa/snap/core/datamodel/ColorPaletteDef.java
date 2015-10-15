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
package org.esa.snap.core.datamodel;

import com.bc.ceres.core.Assert;
import org.esa.snap.core.util.DefaultPropertyMap;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.PropertyMap;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;

/**
 * The <code>ColorPaletteDef</code> class represents a curve that is used to transform the sample values of a
 * geo-physical band into color palette indexes.
 * <p> This special implementation of a gradation curve also provides separate color values for each of the tie points
 * contained in the curve. This allows a better image interpretation because certain colors correspond to certain sample
 * values even if the curve points are used to create color gradient palettes.
 */
public class ColorPaletteDef implements Cloneable  {

    private final static String _PROPERTY_KEY_NUM_POINTS = "numPoints";
    private final static String _PROPERTY_KEY_COLOR = "color";
    private final static String _PROPERTY_KEY_SAMPLE = "sample";
    private final static String _PROPERTY_KEY_AUTODISTRIBUTE = "autoDistribute";

    /**
     * this curve's points
     */
    private Vector<Point> points;
    private int numColors;
    private boolean discrete;
    private boolean autoDistribute;

    public ColorPaletteDef(double minSample, double maxSample) {
        this(new Point[]{
                    new Point(minSample, Color.BLACK),
                    new Point(maxSample, Color.WHITE)
        }, 256);
    }

    public ColorPaletteDef(double minSample, double centerSample, double maxSample) {
        this(new Point[]{
                    new Point(minSample, Color.BLACK),
                    new Point(centerSample, Color.GRAY),
                    new Point(maxSample, Color.WHITE)
        }, 256);
    }

    public ColorPaletteDef(Point[] points) {
        this(points, 256);
    }

    public ColorPaletteDef(Point[] points, int numColors) {
        Guardian.assertGreaterThan("numColors", numColors, 1);
        Guardian.assertNotNull("points", points);
        Guardian.assertGreaterThan("points.length", points.length, 1);
        this.numColors = numColors;
        this.points = new Vector<>(points.length);
        this.points.addAll(Arrays.asList(points));
        this.discrete = false;
    }

    public boolean isDiscrete() {
        return discrete;
    }

    public void setDiscrete(boolean discrete) {
        this.discrete = discrete;
    }

    public int getNumColors() {
        return numColors;
    }

    public void setNumColors(int numColors) {
        this.numColors = numColors;
    }

    public int getNumPoints() {
        return points.size();
    }

    public void setNumPoints(int numPoints) {
        while (getNumPoints() < numPoints) {
            addPoint(new Point(getMaxDisplaySample() + 1.0, Color.BLACK));
        }
        while (getNumPoints() > numPoints) {
            removePointAt(getNumPoints() - 1);
        }
    }

    /**
     * @return true, if all the colors in the palette definitions are fully opaque.
     * @since SNAP 0.5
     */
    public boolean isFullyOpaque() {
        for (Point point : points) {
            if (point.getColor().getAlpha() != 255) {
                return false;
            }
        }
        return true;
    }

    public boolean isAutoDistribute() {
        return autoDistribute;
    }

    public void setAutoDistribute(boolean autoDistribute) {
        this.autoDistribute = autoDistribute;
    }

    public Point getPointAt(int index) {
        return points.get(index);
    }

    public Point getFirstPoint() {
        return points.firstElement();
    }

    public Point getLastPoint() {
        return points.lastElement();
    }

    public double getMinDisplaySample() {
        return getFirstPoint().getSample();
    }

    public double getMaxDisplaySample() {
        return getLastPoint().getSample();
    }

    public void insertPointAfter(int index, Point point) {
        points.insertElementAt(point, index + 1);
    }

    /**
     * creates a new point between the point at the given index
     *
     * @param index   the index
     * @param scaling the scaling
     *
     * @return true, if a point has been inserted
     */
    public boolean createPointAfter(int index, Scaling scaling) {
        Point point1 = getPointAt(index);
        Point point2 = null;
        if (index < points.indexOf(points.lastElement())) {
            point2 = getPointAt(index + 1);
        }
        final Point newPoint;
        if (point2 != null) {
            final double max = Math.max(point1.getSample(), point2.getSample());
            final double min = Math.min(point1.getSample(), point2.getSample());
            final double middle;
            middle = scaling.scale(0.5 * (scaling.scaleInverse(min) + scaling.scaleInverse(max)));
            newPoint = new Point(middle, getCenterColor(point1.getColor(), point2.getColor()));
            insertPointAfter(index, newPoint);
            return true;
        }
        return false;
    }

    /**
     * Creates the center color between the given two colors.
     *
     * @param c1 1st color
     * @param c2 2nd color
     *
     * @return the center color
     */
    public static Color getCenterColor(Color c1, Color c2) {
        return new Color(0.5F * (c1.getRed() + c2.getRed()) / 255.0F,
                         0.5F * (c1.getGreen() + c2.getGreen()) / 255.0F,
                         0.5F * (c1.getBlue() + c2.getBlue()) / 255.0F,
                         0.5F * (c1.getAlpha() + c2.getAlpha()) / 255.0F);
    }


    public void removePointAt(int index) {
        check2PointsMinimum();
        points.remove(index);
    }

    public void addPoint(Point point) {
        points.add(point);
    }

    public Point[] getPoints() {
        Point[] points = new Point[getNumPoints()];
        for (int i = 0; i < getNumPoints(); i++) {
            points[i] = getPointAt(i);
        }
        return points;
    }

    public void setPoints(Point[] points) {
        Assert.notNull(points);
        Assert.argument(points.length >= 2, "points.length >= 2");
        this.points.clear();
        this.points.addAll(Arrays.asList(points));
    }

    public Iterator getIterator() {
        return points.iterator();
    }

    @Override
    public final Object clone() {
        try {
            ColorPaletteDef def = (ColorPaletteDef) super.clone();
            Vector<Point> pointVector = new Vector<>();
            pointVector.setSize(points.size());
            for (int i = 0; i < points.size(); i++) {
                Point point = points.get(i);
                pointVector.set(i, point.createClone());
            }
            def.points = pointVector;
            def.numColors = numColors;
            def.discrete = discrete;
            def.autoDistribute = autoDistribute;
            return def;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public ColorPaletteDef createDeepCopy() {
        return (ColorPaletteDef) clone();
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
        final PropertyMap propertyMap = new DefaultPropertyMap();
        propertyMap.load(file.toPath()); // Overwrite existing values
        final int numPoints = propertyMap.getPropertyInt(_PROPERTY_KEY_NUM_POINTS);
        if (numPoints < 2) {
            throw new IOException("The selected file contains less than\n" +
                                  "two colour points.");
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
        ColorPaletteDef paletteDef = new ColorPaletteDef(points, 256);
        paletteDef.setAutoDistribute(propertyMap.getPropertyBool(_PROPERTY_KEY_AUTODISTRIBUTE, false));
        return paletteDef;
    }

    /**
     * Stores this color palette definition in the given file
     *
     * @param colorPaletteDef thje color palette definition
     * @param file            the file
     *
     * @throws IOException if an I/O error occurs
     */
    public static void storeColorPaletteDef(ColorPaletteDef colorPaletteDef, File file) throws IOException {
        final ColorPaletteDef.Point[] points = colorPaletteDef.getPoints();
        final PropertyMap propertyMap = new DefaultPropertyMap();
        final int numPoints = points.length;
        propertyMap.setPropertyInt(_PROPERTY_KEY_NUM_POINTS, numPoints);
        propertyMap.setPropertyBool(_PROPERTY_KEY_AUTODISTRIBUTE, colorPaletteDef.isAutoDistribute());
        for (int i = 0; i < numPoints; i++) {
            propertyMap.setPropertyColor(_PROPERTY_KEY_COLOR + i, points[i].getColor());
            propertyMap.setPropertyDouble(_PROPERTY_KEY_SAMPLE + i, points[i].getSample());
        }
        propertyMap.store(file.toPath(), "BEAM Colour Palette Definition File"); /*I18N*/
    }

    private void check2PointsMinimum() {
        if (getNumPoints() == 2) {
            throw new IllegalStateException("gradation curve must at least have 2 points");
        }
    }

    /**
     * Releases all of the resources used by this color palette definition and all of its owned children. Its primary
     * use is to allow the garbage collector to perform a vanilla job.
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>dispose()</code> are undefined.
     * <p>Overrides of this method should always call <code>super.dispose();</code> after disposing this instance.
     */
    public void dispose() {
        if (points != null) {
            points.removeAllElements();
            points = null;
        }
    }

    public Color[] getColors() {
        final Color[] colors = new Color[points.size()];
        for (int i = 0; i < points.size(); i++) {
            colors[i] = points.get(i).getColor();
        }
        return colors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ColorPaletteDef that = (ColorPaletteDef) o;

        if (autoDistribute != that.autoDistribute) {
            return false;
        }
        if (discrete != that.discrete) {
            return false;
        }
        if (numColors != that.numColors) {
            return false;
        }
        if (!points.equals(that.points)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = points.hashCode();
        result = 31 * result + numColors;
        result = 31 * result + (discrete ? 1 : 0);
        result = 31 * result + (autoDistribute ? 1 : 0);
        return result;
    }

    public static class Point implements Cloneable {

        private double sample;
        private Color color;
        private String label;

        public Point() {
            this(0, Color.black);
        }

        public Point(double sample, Color color) {
            this(sample, color, "");
        }

        public Point(double sample, Color color, String label) {
            this.sample = sample;
            this.color = color;
            this.label = label;
        }

        public double getSample() {
            return sample;
        }

        public void setSample(double sample) {
            this.sample = sample;
        }

        public Color getColor() {
            return color;
        }

        public void setColor(Color color) {
            this.color = color;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        @Override
        public final Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        public Point createClone() {
            return (Point) clone();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Point point = (Point) o;

            if (Double.compare(point.sample, sample) != 0) {
                return false;
            }
            if (color != null ? !color.equals(point.color) : point.color != null) {
                return false;
            }
            if (label != null ? !label.equals(point.label) : point.label != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            temp = Double.doubleToLongBits(sample);
            result = (int) (temp ^ (temp >>> 32));
            result = 31 * result + (color != null ? color.hashCode() : 0);
            result = 31 * result + (label != null ? label.hashCode() : 0);
            return result;
        }
    }
}
