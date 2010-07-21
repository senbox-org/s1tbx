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

package org.esa.beam.dataio.chris.internal;

import com.bc.ceres.core.Assert;
import org.esa.beam.dataio.chris.Flags;

import java.awt.*;
import static java.lang.Math.sqrt;

/**
 * The class {@code DropoutCorrection} encapsulates the dropout correction
 * algorithm developed by Luis Gomez Chova.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class DropoutCorrection {

    /**
     * The dropout correction methods.
     */
    public enum Type {

        /**
         * This type includes the two neighboring pixels in along-track direction
         * only.
         */
        N2("2-Connected", new double[]{0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0}),
        /**
         * This type includes the two neighboring pixels in both along and across
         * track directions, giving a total of four neighboring pixels.
         */
        N4("4-Connected", new double[]{0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0}),
        /**
         * This type includes all eight surrounding pixels.
         */
        N8("8-Connected", new double[]{1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0});

        private final String name;
        private final double[] weights;

        private Type(String name, double[] weights) {
            this.name = name;
            this.weights = weights;
        }

        /**
         * Returns the pixel weights corresponding to this type.
         *
         * @return the pixel weights.
         */
        private double[] getWeights() {
            return weights;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final int M_WIDTH = 3;
    private static final int M_HEIGHT = 3;

    private static final short VALID = 0;
    private static final short DROPOUT = 1;
    private static final short SATURATED = 2;
    private static final short CORRECTED_DROPOUT = 256;

    private double[] weights;
    private boolean cosmetic;

    /**
     * Constructs the default instance of this class.
     */
    public DropoutCorrection() {
        this(Type.N4);
    }

    /**
     * Constructs an instance of this class which performs a non-cosmetic
     * dropout correction with the given neighborhood type.
     *
     * @param type the neighborhood type.
     */
    public DropoutCorrection(Type type) {
        this(type, false);
    }

    /**
     * Constructs an instance of this class.
     *
     * @param type     the neighborhood type.
     * @param cosmetic indicates if the dropout correction should be cosmetic only.
     *                 If {@code true} the mask data are not modified.
     */
    public DropoutCorrection(Type type, boolean cosmetic) {
        this.weights = type.getWeights();
        this.cosmetic = cosmetic;
    }

    /**
     * Computes the dropout correction for a given target rectangle.
     *
     * @param rciData         the RCI  raster data. The first array must hold the data which
     *                        are to be corrected.
     * @param maskData        the mask raster data. The first array must hold the data which
     *                        are to be corrected.
     * @param rasterWidth     the raster width.
     * @param rasterHeight    the raster height.
     * @param targetRectangle the target rectangle.
     *
     * @throws IllegalArgumentException if RCI and mask data arrays do not have the same length, or
     *                                  the target rectangle is larger than the raster.
     */
    public void compute(int[][] rciData, short[][] maskData, int rasterWidth, int rasterHeight,
                        Rectangle targetRectangle) {

        compute(rciData, maskData, new Rectangle(0, 0, rasterWidth, rasterHeight), 0, rasterWidth, rciData[0],
                maskData[0], targetRectangle, targetRectangle.x + targetRectangle.y * rasterWidth, rasterWidth);
    }

    /**
     * Compute the dropout correction for a given target rectangle.
     *
     * @param sourceRciData   the source RCI  raster data. The first array must hold the data
     *                        which are to be corrected.
     * @param sourceMaskData  the source mask raster data. The first array must hold the data
     *                        which are to be corrected.
     * @param sourceRectangle the source rectangle.
     * @param sourceOffset    the source scanline offset.
     * @param sourceStride    the source scanline stride.
     * @param targetRciData   the target RCI  raster data. May be the same as
     *                        {@code sourceRciData[0]}.
     * @param targetMaskData  the target mask raster data. May be the same as
     *                        {@code sourceMaskData[0]}.
     * @param targetRectangle the target rectangle, which must be inside the source rectangle.
     * @param targetOffset    the target scanline offset.
     * @param targetStride    the target scanline stride.
     *
     * @throws IllegalArgumentException if RCI and mask data arrays do not have the same length, or
     *                                  the target rectangle is not inside the source rectangle.
     */
    public void compute(int[][] sourceRciData,
                        short[][] sourceMaskData,
                        Rectangle sourceRectangle,
                        int sourceOffset,
                        int sourceStride,
                        int[] targetRciData,
                        short[] targetMaskData,
                        Rectangle targetRectangle,
                        int targetOffset,
                        int targetStride) {
        Assert.argument(sourceRciData.length == sourceMaskData.length);
//        Assert.argument(targetRciData.length == targetMaskData.length); // JAI buffers do not have the same length!!
        Assert.argument(sourceRectangle.contains(targetRectangle));

        final double[] w = new double[weights.length];

        for (int ty = targetRectangle.y; ty < targetRectangle.y + targetRectangle.height; ++ty) {
            for (int tx = targetRectangle.x; tx < targetRectangle.x + targetRectangle.width; ++tx) {
                final int sxy = sourceOffset + (tx - sourceRectangle.x) + (ty - sourceRectangle.y) * sourceStride;
                final int txy = targetOffset + (tx - targetRectangle.x) + (ty - targetRectangle.y) * targetStride;

                targetRciData[txy] = sourceRciData[0][sxy];
                targetMaskData[txy] = sourceMaskData[0][sxy];

                if (sourceMaskData[0][sxy] == DROPOUT) {
                    double ws = 0.0;
                    double xc = 0.0;

                    double ws2 = 0.0;
                    double xc2 = 0.0;

                    for (int i = 0, y = ty - 1; i < M_HEIGHT; ++i, ++y) {
                        if (y < sourceRectangle.y || y >= sourceRectangle.y + sourceRectangle.height) {
                            continue;
                        }
                        for (int j = 0, x = tx - 1; j < M_WIDTH; ++j, ++x) {
                            if (x < sourceRectangle.x || x >= sourceRectangle.x + sourceRectangle.width) {
                                continue;
                            }
                            final int ij = i * M_WIDTH + j;
                            final int xy = sourceOffset + (x - sourceRectangle.x) + (y - sourceRectangle.y) * sourceStride;

                            if (weights[ij] != 0.0) {
                                switch (sourceMaskData[0][xy]) {
                                case VALID:
                                    w[ij] = weights[ij] * calculateWeight(sxy, xy, sourceRciData, sourceMaskData);
                                    ws += w[ij];
                                    xc += sourceRciData[0][xy] * w[ij];
                                    break;

                                case SATURATED:
                                    ws2 += weights[ij];
                                    xc2 += sourceRciData[0][xy] * weights[ij];
                                    break;
                                }
                            }
                        }
                    }
                    if (ws > 0.0) {
                        targetRciData[txy] = (int) (xc / ws);
                        if (!cosmetic) {
                            targetMaskData[txy] = CORRECTED_DROPOUT;
                        }
                    } else { // all neighbors are saturated
                        if (ws2 > 0.0) {
                            targetRciData[txy] = (int) (xc2 / ws2);
                            if (!cosmetic) {
                                targetMaskData[txy] = SATURATED;
                            }
                        } else { // all neighbors are dropouts
                            targetRciData[txy] = 0;
                        }
                    }
                }
            }
        }
    }

    private double calculateWeight(int index, int neighborIndex, int[][] rciData, short[][] maskData) {
        double sum = 0.0;
        int count = 0;

        for (int i = 1; i < rciData.length; ++i) {
            if (maskData[i][index] == VALID && maskData[i][neighborIndex] == VALID && rciData[i][index] != 0) {
                final double d = (rciData[i][index] - rciData[i][neighborIndex]);

                sum += d * d;
                ++count;
            }
        }

        return count > 0 ? 1.0 / (1.0E-52 + sqrt(sum / count)) : 1.0;
    }
}
