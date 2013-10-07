/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.csa.rstb.gpf.decompositions;

import org.csa.rstb.gpf.PolOpUtils;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;
import org.esa.nest.gpf.OperatorUtils;
import org.esa.nest.gpf.PolBandUtils;
import org.esa.nest.gpf.StatusProgressMonitor;
import org.esa.nest.gpf.ThreadManager;

import java.awt.*;

/**
 * Base class for polarimetric decompositions
 */
public class DecompositionBase {

    protected PolBandUtils.QuadSourceBand[] srcBandList;
    protected final PolBandUtils.MATRIX sourceProductType;

    protected final int windowSize;
    protected final int halfWindowSize;
    protected final int sourceImageWidth;
    protected final int sourceImageHeight;

    public static enum TargetBandColour { R, G, B}

    public DecompositionBase(final PolBandUtils.QuadSourceBand[] srcBandList, final PolBandUtils.MATRIX sourceProductType,
                      final int windowSize, final int srcImageWidth, final int srcImageHeight) {
        this.srcBandList = srcBandList;
        this.sourceProductType = sourceProductType;
        this.windowSize = windowSize;
        this.sourceImageWidth = srcImageWidth;
        this.sourceImageHeight = srcImageHeight;
        halfWindowSize = windowSize / 2;
    }

    /**
     * Get source tile rectangle.
     * @param tx0 X coordinate for the upper left corner pixel in the target tile.
     * @param ty0 Y coordinate for the upper left corner pixel in the target tile.
     * @param tw The target tile width.
     * @param th The target tile height.
     * @return The source tile rectangle.
     */
    protected Rectangle getSourceRectangle(final int tx0, final int ty0, final int tw, final int th) {

        final int x0 = Math.max(0, tx0 - halfWindowSize);
        final int y0 = Math.max(0, ty0 - halfWindowSize);
        final int xMax = Math.min(tx0 + tw - 1 + halfWindowSize, sourceImageWidth - 1);
        final int yMax = Math.min(ty0 + th - 1 + halfWindowSize, sourceImageHeight - 1);
        final int w = xMax - x0 + 1;
        final int h = yMax - y0 + 1;
        return new Rectangle(x0, y0, w, h);
    }

    /**
     * Compute min/max values of the Span image.
     * @param op the decomposition operator
     * @param bandList the src band list
     * @return min max values
     * @throws org.esa.beam.framework.gpf.OperatorException when thread fails
     */
    public MinMax computeSpanMinMax(final Operator op, final PolBandUtils.QuadSourceBand bandList)
            throws OperatorException {
        final MinMax minMaxValue = new MinMax();
        final Dimension tileSize = new Dimension(256, 256);
        final Rectangle[] tileRectangles = OperatorUtils.getAllTileRectangles(op.getSourceProduct(), tileSize, 25);
        final double[][] Cr = new double[3][3];
        final double[][] Ci = new double[3][3];

        final StatusProgressMonitor status = new StatusProgressMonitor(tileRectangles.length,
                "Computing min max span... ");
        status.setAllowStdOut(false);

        try {
            final ThreadManager threadManager = new ThreadManager();

            int tileCnt = 0;
            for (final Rectangle rectangle : tileRectangles) {

                final Thread worker = new Thread() {

                    double span = 0.0;
                    final int xMax = rectangle.x + rectangle.width;
                    final int yMax = rectangle.y + rectangle.height;
                    /*
                    System.out.println("setSpan x0 = " + rectangle.x + ", y0 = " + rectangle.y +
                                       ", w = " + rectangle.width + ", h = " + rectangle.height);
                    */

                    final Tile[] sourceTiles = new Tile[bandList.srcBands.length];
                    final ProductData[] dataBuffers = new ProductData[bandList.srcBands.length];

                    @Override
                    public void run() {
                        try {

                        for (int i = 0; i < sourceTiles.length; ++i) {
                            sourceTiles[i] = op.getSourceTile(bandList.srcBands[i], rectangle);
                            dataBuffers[i] = sourceTiles[i].getDataBuffer();
                        }

                        for (int y = rectangle.y; y < yMax; ++y) {

                            for (int x = rectangle.x; x < xMax; ++x) {

                                PolOpUtils.getMeanCovarianceMatrix(x, y, halfWindowSize,
                                        sourceImageWidth, sourceImageHeight,
                                        sourceProductType, sourceTiles, dataBuffers, Cr, Ci);

                                span = Cr[0][0] + Cr[1][1] + Cr[2][2];

                                if (minMaxValue.min > span) {
                                    synchronized (minMaxValue) {
                                        minMaxValue.min = span;
                                    }
                                }
                                if (minMaxValue.max < span) {
                                    synchronized (minMaxValue) {
                                        minMaxValue.max = span;
                                    }
                                }
                            }
                        }
                        } catch(Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }
                };

                threadManager.add(worker);

                status.worked(tileCnt++);
            }

            threadManager.finish();

            if (minMaxValue.min < PolOpUtils.EPS) {
                minMaxValue.min = PolOpUtils.EPS;
            }

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(op.getId()+ " computeMinMaxSpan ", e);
        } finally {
            status.done();
        }
        return minMaxValue;
    }

    /**
     * Convert pixel value from linear scale to dB.
     * @param p The pixel value in linear scale.
     * @param spanMin span min
     * @param spanMax span max
     * @return The pixel value in dB.
     */
    protected static double scaleDb(double p, final double spanMin, final double spanMax) {

        if (p < spanMin) {
            p = spanMin;
        }

        if (p > spanMax) {
            p = spanMax;
        }
        return 10.0*Math.log10(p);
    }

    /**
     * Compute min/max values of the Span image.
     * @param op the decomposition operator
     * @param bandList the src band list
     * @throws org.esa.beam.framework.gpf.OperatorException when thread fails
     */
    protected synchronized void setSpanMinMax(final Operator op, final PolBandUtils.QuadSourceBand bandList)
            throws OperatorException {

        if (bandList.spanMinMaxSet) {
            return;
        }
        final MinMax span = computeSpanMinMax(op, bandList);
        bandList.spanMin = span.min;
        bandList.spanMax = span.max;
        bandList.spanMinMaxSet = true;
    }

    public static class MinMax {
        public double min = 1e+30;
        public double max = -min;
    }

    public static class TargetInfo {
        public final Tile tile;
        public final ProductData dataBuffer;
        public final TargetBandColour colour;
        public TargetInfo(final Tile tile, final TargetBandColour col) {
            this.tile = tile;
            this.dataBuffer = tile.getDataBuffer();
            this.colour = col;
        }
    }
}
