/*
 * $Id: PinPixelsGenerator.java,v 1.1 2007/04/19 10:16:11 marcop Exp $
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
package org.esa.beam.visat.actions.pin;

import com.bc.jexp.ParseException;
import com.bc.jexp.Term;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.dataop.barithm.RasterDataEvalEnv;
import org.esa.beam.framework.dataop.barithm.RasterDataLoop;

import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author Maximilian Aulinger
 */
class PinPixelsGenerator {

    private Product product;

    public PinPixelsGenerator(final Product product) {
        this.product = product;
    }

    /**
     * Returns all pixels that are located in a quadric region of size x size around the
     * <code>center</code> and are not filtered by the arithmetic expression.
     *
     * @param center     the center pixel of the region
     * @param size       the height/width of the region
     * @param expression an arithmetic expression for classifying pixel relevance
     *
     * @return a point-array providing the pixel coordinates
     *
     * @throws ParseException
     * @throws IOException
     */
    Point[] generateQuadricPixelRegion(final Point center, final int size, final String expression)
            throws ParseException, IOException {
        final ArrayList<Point> pixels = new ArrayList<Point>();
        final int productWidth = product.getSceneRasterWidth();
        final int productHeigt = product.getSceneRasterHeight();
        final int minX = computeMinX(center, size);
        final int minY = computeMinY(center, size);
        final int maxX = computeMaxX(center, productWidth, size);
        final int maxY = computeMaxY(center, productHeigt, size);
        final int regionWidth = maxX - minX + 1;
        final int regionHeight = maxY - minY + 1;

        final RasterDataLoop loop;
        final Term t;
        if (expression != null) {
            t = product.createTerm(expression);
            loop = new RasterDataLoop(minX, minY, regionWidth, regionHeight, new Term[]{t}, ProgressMonitor.NULL);
        } else {
            t = null;
            loop = new RasterDataLoop(minX, minY, regionWidth, regionHeight, new Term[]{}, ProgressMonitor.NULL);
        }

        loop.forEachPixel(new RasterDataLoop.Body() {
            public void eval(RasterDataEvalEnv env, int pixelIndex) {
                if (t == null || t.evalB(env)) {
                    pixels.add(new Point(env.getPixelX(), env.getPixelY()));
                }
            }
        });
        if (!pixels.isEmpty()) {
            return pixels.toArray(new Point[pixels.size()]);
        }
        return null;
    }

    /**
     * Returns all pixels that are located in a quadric region of size x size around the
     * <code>center</code>.
     *
     * @param center the center pixel of the region
     * @param size   the height/width of the region
     *
     * @return a point-array providing the pixel coordinates
     */
    Point[] generateQuadricPixelRegion(final Point center, final int size) {
        final ArrayList<Point> pixels = new ArrayList<Point>();
        final int productWidth = product.getSceneRasterWidth();
        final int productHeigt = product.getSceneRasterHeight();
        final int minX = computeMinX(center, size);
        final int minY = computeMinY(center, size);
        final int maxX = computeMaxX(center, productWidth, size);
        final int maxY = computeMaxY(center, productHeigt, size);

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                pixels.add(new Point(x, y));
            }
        }
        if (!pixels.isEmpty()) {
            return pixels.toArray(new Point[pixels.size()]);
        }
        return null;
    }

    /**
     * Returns the relecance information for the <code>point</code> array by using the
     * <code>expression</code>
     */
    Boolean[] getRelevanceInformation(Point[] points, String expression) throws IOException, ParseException {
        final ArrayList<Boolean> relevance = new ArrayList<Boolean>();

        int regionWidth = points[points.length - 1].x - points[0].x + 1;
        int regionHeight = points[points.length - 1].y - points[0].y + 1;

        final RasterDataLoop loop;
        final Term t;

        if (expression != null) {
            t = product.createTerm(expression);
            loop = new RasterDataLoop(points[0].x, points[0].y, regionWidth, regionHeight, new Term[]{t}, ProgressMonitor.NULL);
        } else {
            t = null;
            loop = new RasterDataLoop(points[0].x, points[0].y, regionWidth, regionHeight, new Term[]{}, ProgressMonitor.NULL);
        }

        loop.forEachPixel(new RasterDataLoop.Body() {
            public void eval(RasterDataEvalEnv env, int pixelIndex) {
                if (t == null || t.evalB(env)) {
                    relevance.add(new Boolean(true));
                } else {
                    relevance.add(new Boolean(false));
                }
            }
        });
        return relevance.toArray(new Boolean[relevance.size()]);
    }

    /**
     * Computes the x-coordinate of the pixel in the lower right corner of the region.
     */
    private int computeMaxX(final Point center, final int productWidth, final int size) {
        return computeMaxValue(center.x, size, productWidth);
    }

    /**
     * Computes the y-coordinate of the pixel in the lower right corner of the region.
     */
    private int computeMaxY(final Point center, final int productHeigt, final int size) {
        return computeMaxValue(center.y, size, productHeigt);
    }

    /**
     * Computes the x-coordinate of the pixel in the upper left corner of the region.
     */
    private int computeMinX(final Point center, final int size) {
        return computeMinValue(center.x, size, 0);
    }

    /**
     * Computes the y-coordinate of the pixel in the upper left corner of the region.
     */
    private int computeMinY(final Point center, final int size) {
        return computeMinValue(center.y, size, 0);
    }

    private int computeMaxValue(final int centerValue, final int size, final int maxValue) {
        int max = centerValue + (size - 1) / 2;
        if (max >= maxValue) {
            max = maxValue - 1;
        }
        return max;
    }

    private int computeMinValue(final int centerValue, final int size, final int minValue) {
        int min = centerValue - (size - 1) / 2;
        if (min < minValue) {
            min = minValue;
        }
        return min;
    }

}
