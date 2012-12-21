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
package org.esa.nest.util;

import Jama.Matrix;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.nest.gpf.OperatorUtils;

/**
 * Created by IntelliJ IDEA.
 * User: junlu
 * Date: Feb 10, 2009
 * Time: 5:31:11 PM
 * To change this template use File | Settings | File Templates.
 */
class QuadInterpolator {

    private final float subSamplinX;
    private final float subSamplinY;
    private final double[][] warpPolynomialCoef;
    private static final int warpPolynomialOrder = 2;

    public QuadInterpolator(final TiePointGrid tpg) {

        final int imageWidth = tpg.getSceneRasterWidth();
        subSamplinX = tpg.getSubSamplingX();
        subSamplinY = tpg.getSubSamplingY();
        final int width = tpg.getRasterWidth();
        final int height = tpg.getRasterHeight();
        final float[] tiePoints = tpg.getTiePoints();

        final String tiePointGridName = tpg.getName();
        boolean imageFlipped = false;
        if ((tiePointGridName.equals(OperatorUtils.TPG_INCIDENT_ANGLE) ||
             tiePointGridName.equals(OperatorUtils.TPG_SLANT_RANGE_TIME)) &&
            (tiePoints[0] > tiePoints[width - 1])) {
            imageFlipped = true;
        }

        final double[] sampleIndexArray = new double[width];
        for (int c = 0; c < width; c++) {
            if (imageFlipped) {
                sampleIndexArray[width - 1 - c] = imageWidth - 1 - Math.min(c*((int)subSamplinX - 1), imageWidth - 1);
            } else {
                sampleIndexArray[c] = Math.min(c*((int)subSamplinX - 1), imageWidth - 1);
            }
        }

        final Matrix A = MathUtils.createVandermondeMatrix(sampleIndexArray, warpPolynomialOrder);

        final double[] tiePointArray = new double[width];
        warpPolynomialCoef = new double[height][warpPolynomialOrder + 1];
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                tiePointArray[c] = (double)tiePoints[r*width + c];
            }
            final Matrix b = new Matrix(tiePointArray, width);
            final Matrix x = A.solve(b);
            warpPolynomialCoef[r] = x.getColumnPackedCopy();
        }
    }

    /**
     * Get pixel values for given rectangle.
     * @param x0 The x coordinate for the upper left corner of the rectangle.
     * @param y0 The y coordinate for the upper left corner of the rectangle.
     * @param w The width of the rectangle.
     * @param h The height of the rectangle.
     * @param pixels The pixel array.
     * @return The pixel array.
     */
    public float[] getPixelFloats(int x0, int y0, int w, int h, float[] pixels) {

        int k = 0;
        for (int y = y0; y < y0 + h; y++) {
            int r = (int) (y / subSamplinY);
            final double a0 = warpPolynomialCoef[r][0];
            final double a1 = warpPolynomialCoef[r][1];
            final double a2 = warpPolynomialCoef[r][2];
            for (int x = x0; x < x0 + w; x++) {
                pixels[k++] = (float)(a0 + a1*x + a2*x*x);
            }
        }
        return pixels;
    }
}
