/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.csa.rstb.polarimetric.gpf.decompositions;

import org.apache.commons.math3.util.FastMath;
import org.csa.rstb.polarimetric.gpf.DualPolOpUtils;
import org.csa.rstb.polarimetric.gpf.PolOpUtils;
import org.esa.s1tbx.io.PolBandUtils;
import org.esa.snap.datamodel.Unit;
import org.esa.snap.eo.Constants;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.gpf.Operator;
import org.esa.snap.framework.gpf.Tile;
import org.esa.snap.gpf.TileIndex;

import java.awt.Rectangle;
import java.util.Map;

/**
 * Perform H-Alpha decomposition for given tile.
 * <p>
 * [1]	S. R. Cloude, D. G. Goodenough, H. Chen, "Compact Decomposition Theory",
 * IEEE Geoscience and Remote Sensing Letters, Vol. 9, No. 1, Jan. 2012.
 */
public class HAlphaC2 extends DecompositionBase implements Decomposition {

    public HAlphaC2(final PolBandUtils.PolSourceBand[] srcBandList, final PolBandUtils.MATRIX sourceProductType,
                    final int windowSize, final int srcImageWidth, final int srcImageHeight) {
        super(srcBandList, sourceProductType, windowSize, windowSize, srcImageWidth, srcImageHeight);
    }

    /**
     * Return the list of band names for the target product
     *
     * @return list of band names
     */
    public String[] getTargetBandNames() {
        return new String[]{"Entropy", "Anisotropy", "Alpha"};
    }

    /**
     * Sets the unit for the new target band
     *
     * @param targetBandName the band name
     * @param targetBand     the new target band
     */
    public void setBandUnit(final String targetBandName, final Band targetBand) {
        if (targetBandName.contains("Entropy")) {
            targetBand.setUnit("entropy");
        } else if (targetBandName.contains("Anisotropy")) {
            targetBand.setUnit("anisotropy");
        } else if (targetBandName.equals("Alpha")) {
            targetBand.setUnit(Unit.DEGREES);
        }
    }

    /**
     * Perform decomposition for given tile.
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed.
     * @param op              the polarimetric decomposition operator
     * @throws org.esa.snap.framework.gpf.OperatorException If an error occurs during computation of the filtered value.
     */
    public void computeTile(final Map<Band, Tile> targetTiles, final Rectangle targetRectangle, final Operator op) {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int maxY = y0 + h;
        final int maxX = x0 + w;
        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        final double[][] Cr = new double[2][2]; // real part of covariance matrix
        final double[][] Ci = new double[2][2]; // imaginary part of covariance matrix
        final Rectangle sourceRectangle = getSourceRectangle(x0, y0, w, h);

        for (final PolBandUtils.PolSourceBand bandList : srcBandList) {

            final TargetInfo[] targetInfo = new TargetInfo[bandList.targetBands.length];
            int j = 0;
            for (Band targetBand : bandList.targetBands) {
                final String targetBandName = targetBand.getName();
                if (targetBandName.contains("Entropy")) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.R);
                } else if (targetBandName.contains("Anisotropy")) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.G);
                } else if (targetBandName.contains("Alpha")) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.B);
                }
                ++j;
            }
            final TileIndex trgIndex = new TileIndex(targetInfo[0].tile);

            final Tile[] sourceTiles = new Tile[bandList.srcBands.length];
            final ProductData[] dataBuffers = new ProductData[bandList.srcBands.length];
            for (int i = 0; i < bandList.srcBands.length; i++) {
                sourceTiles[i] = op.getSourceTile(bandList.srcBands[i], sourceRectangle);
                dataBuffers[i] = sourceTiles[i].getDataBuffer();
            }
            double v = 0.0;

            for (int y = y0; y < maxY; ++y) {
                trgIndex.calculateStride(y);
                for (int x = x0; x < maxX; ++x) {
                    final int index = trgIndex.getIndex(x);

                    DualPolOpUtils.getMeanCovarianceMatrixC2(x, y, halfWindowSizeX, halfWindowSizeY, sourceImageWidth,
                                                             sourceImageHeight, sourceProductType, sourceTiles, dataBuffers, Cr, Ci);

                    HAAlpha data = computeHAAlphaByC2(Cr, Ci);

                    for (TargetInfo target : targetInfo) {

                        if (target.colour == TargetBandColour.R) {
                            v = data.entropy;
                        } else if (target.colour == TargetBandColour.G) {
                            v = data.anisotropy;
                        } else if (target.colour == TargetBandColour.B) {
                            v = data.alpha;
                        }

                        target.dataBuffer.setElemFloatAt(index, (float) v);
                    }
                }
            }
        }
    }

    public static HAAlpha computeHAAlphaByC2(final double[][] Cr, final double[][] Ci) {

        final HAAlpha data = new HAAlpha();
        final double[][] EigenVectRe = new double[2][2];
        final double[][] EigenVectIm = new double[2][2];
        final double[] EigenVal = new double[2];

        PolOpUtils.eigenDecomposition(2, Cr, Ci, EigenVectRe, EigenVectIm, EigenVal);

        final double sum = EigenVal[0] + EigenVal[1];
        final double[] p = {EigenVal[0] / sum, EigenVal[1] / sum};
        data.entropy = -(p[0] * Math.log(p[0] + Constants.EPS) + p[1] * Math.log(p[1] + Constants.EPS)) / Math.log(2);
        data.anisotropy = (p[0] - p[1]) / (p[0] + p[1] + Constants.EPS);

        final double alpha = FastMath.acos(norm(EigenVectRe[0][0], EigenVectIm[0][0])) * Constants.RTOD;
        //final double alpha2 = FastMath.acos(norm(EigenVectRe[0][1], EigenVectIm[0][1]))*Constants.RTOD;
        data.alpha = p[0] * alpha + p[1] * (Constants.HALF_PI - alpha);

        return data;
    }

    private static double norm(final double real, final double imag) {
        return Math.sqrt(real * real + imag * imag);
    }

    public static class HAAlpha {
        public double entropy;
        public double anisotropy;
        public double alpha;
    }
}
