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

import org.esa.snap.core.util.math.MathUtils;

import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.LinkedList;
import java.util.List;

/**
 * Instances of this class are used to rasterize the outline of a <code>java.awt.Shape</code>.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class ShapeRasterizer {

    private AffineTransform transform;
    private double flatness;
    private LineRasterizer lineRasterizer;

    /**
     * Constructs a new rasterizer with a no affine transformation, a flatness of <code>1.0</code> and a bresenham line
     * rasterizer.
     *
     * @see BresenhamLineRasterizer
     */
    public ShapeRasterizer() {
        this(null, 1.0, new BresenhamLineRasterizer());
    }

    /**
     * Constructs a new rasterizer with the specified affine transformation, flatness and line rasterizer.
     *
     * @param transform      the affine transformation to be applied before a shape is rasterized. Can be
     *                       <code>null</code>.
     * @param flatness       the flatness of the rasterized outlines to be created.
     * @param lineRasterizer the rasterizing algorithm
     */
    public ShapeRasterizer(AffineTransform transform, double flatness, LineRasterizer lineRasterizer) {
        this.transform = transform;
        this.flatness = flatness;
        this.lineRasterizer = lineRasterizer;
    }

    /**
     * Gets the affine transformation to be applied before a shape is rasterized.
     *
     * @return the affine transformation, can be <code>null</code>.
     */
    public AffineTransform getTransform() {
        return transform;
    }

    /**
     * Sets the affine transformation to be applied before a shape is rasterized.
     *
     * @param transform the affine transformation to be applied before a shape is rasterized. Can be <code>null</code>.
     */
    public void setTransform(AffineTransform transform) {
        this.transform = transform;
    }

    /**
     * Gets the flatness of the of the rasterized outlines to be created.
     *
     * @return the flatness.
     */
    public double getFlatness() {
        return flatness;
    }

    /**
     * Sets the flatness of the of the rasterized outlines to be created.
     *
     * @param flatness the flatness.
     */
    public void setFlatness(double flatness) {
        this.flatness = flatness;
    }

    /**
     * Sets the rasterizing algorithm.
     *
     * @return the rasterizing algorithm
     */
    public LineRasterizer getLineRasterizer() {
        return lineRasterizer;
    }

    /**
     * Sets the rasterizing algorithm.
     *
     * @param lineRasterizer the rasterizing algorithm
     */
    public void setLineRasterizer(LineRasterizer lineRasterizer) {
        this.lineRasterizer = lineRasterizer;
    }

    /**
     * Rasterizes the given shape.
     *
     * @param shape the shape to be rasterized
     * @return an array of points representing the rasterized shape outline
     */
    public Point2D[] rasterize(Shape shape) {
        return rasterize(getVertices(shape));
    }

    /**
     * Rasterizes the given shape given as a vertices array.
     *
     * @param vertices the shape to be rasterized given as vertices
     * @return an array of points representing the rasterized shape outline
     */
    public Point2D[] rasterize(final Point2D[] vertices) {
        return rasterize(vertices, null);
    }

    /**
     * Rasterizes the given shape given as a vertices array. The method also stores the indeices of the original
     * vertices in the given index array.
     *
     * @param vertices      the shape to be rasterized given as vertices
     * @param vertexIndexes if not <code>null</code>, the method stores the original vertex indices in this array
     * @return an array of points representing the rasterized shape outline
     */
    public Point2D[] rasterize(final Point2D[] vertices, final int[] vertexIndexes) {

        if (vertices == null || vertices.length <= 1) {
            return vertices;
        }

        if (vertexIndexes != null && vertexIndexes.length < vertices.length) {
            throw new IllegalArgumentException("size of 'vertexIndexes' less than 'vertices'");
        }

        final List<Point> list = new LinkedList<Point>();
        final Point lastPoint = new Point();

        final LinePixelVisitor visitor = new LinePixelVisitor() {

            public void visit(int x, int y) {
                if (list.size() == 0 || lastPoint.x != x || lastPoint.y != y) {
                    lastPoint.x = x;
                    lastPoint.y = y;
                    list.add(new Point(lastPoint));
                }
            }
        };

        int x0 = MathUtils.floorInt(vertices[0].getX());
        int y0 = MathUtils.floorInt(vertices[0].getY());
        if (vertexIndexes != null) {
            vertexIndexes[0] = 0;
        }
        for (int i = 1; i < vertices.length; i++) {
            int x1 = MathUtils.floorInt(vertices[i].getX());
            int y1 = MathUtils.floorInt(vertices[i].getY());
            lineRasterizer.rasterize(x0, y0, x1, y1, visitor);
            if (vertexIndexes != null) {
                vertexIndexes[i] = (list.size() > 0) ? list.size() - 1 : 0;
            }
            x0 = x1;
            y0 = y1;
        }

        return list.toArray(new Point[list.size()]);
    }

    /**
     * Converts the given shape into an array of vertices.
     *
     * @param shape the shape
     * @return the shape given as a vertices array
     */
    public Point2D[] getVertices(Shape shape) {

        if (shape == null) {
            return null;
        }

        final List<Point2D.Float> list = new LinkedList<Point2D.Float>();
        final float[] coordinates = new float[6];
        final PathIterator pathIterator = shape.getPathIterator(transform, flatness);

        float x1 = Integer.MAX_VALUE;
        float y1 = Integer.MAX_VALUE;
        while (!pathIterator.isDone()) {
            int type = pathIterator.currentSegment(coordinates);
            if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO) {
                float x0 = coordinates[0];
                float y0 = coordinates[1];
                if (x0 != x1 || y0 != y1) {
                    list.add(new Point2D.Float(x0, y0));
                }
                x1 = x0;
                y1 = y0;
            }
            pathIterator.next();
        }

        return list.toArray(new Point2D[list.size()]);
    }

    /**
     * Visits each pixel of a rasterized line. This interface is used by the <code>{@link ShapeRasterizer.LineRasterizer}</code>
     * interface.
     */
    public static interface LinePixelVisitor {

        /**
         * Visits the pixel at x, y.
         */
        void visit(int x, int y);
    }

    /**
     * An abstract representation of an algorithm used to rasterize lines.
     */
    public static interface LineRasterizer {

        /**
         * Rasterizes a line by visiting all pixels between the two line end-points.
         *
         * @param x0      x of the first end-point
         * @param y0      y of the first end-point
         * @param x1      x of the seconf end-point
         * @param y1      y of the seconf end-point
         * @param visitor the pixel visitor
         */
        void rasterize(int x0, int y0, int x1, int y1, LinePixelVisitor visitor);
    }

    /**
     * The <i>Bresenham Algorithm</i> is the default algorithm used to rasterize lines.
     */
    public static class BresenhamLineRasterizer implements LineRasterizer {

        public BresenhamLineRasterizer() {
        }

        public void rasterize(int x0, int y0, int x1, int y1, LinePixelVisitor visitor) {

            int dx = x1 - x0;
            int dy = y1 - y0;
            int stepy = 1;
            int stepx = 1;
            int fraction;

            if (dx < 0) {
                dx = -dx;
                stepx = -1;
            }

            if (dy < 0) {
                dy = -dy;
                stepy = -1;
            }

            dx <<= 1;   // dx is now 2*dx
            dy <<= 1;   // dy is now 2*dy

            visitor.visit(x0, y0);
            if (dx > dy) {
                fraction = dy - (dx >> 1); // same as 2*dy - dx
                while (x0 != x1) {
                    if (fraction >= 0) {
                        y0 += stepy;
                        fraction -= dx;   // same as fraction -= 2*dx
                    }
                    x0 += stepx;
                    fraction += dy;       // same as fraction -= 2*dy
                    visitor.visit(x0, y0);
                }
            } else {
                fraction = dx - (dy >> 1);
                while (y0 != y1) {
                    if (fraction >= 0) {
                        x0 += stepx;
                        fraction -= dy;
                    }
                    y0 += stepy;
                    fraction += dx;
                    visitor.visit(x0, y0);
                }
            }
        }
    }
}
