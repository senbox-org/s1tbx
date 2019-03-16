/*
 * Copyright (C) 2018 Skywatch Space Applications Inc. https://www.skywatch.co
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
package org.csa.rstb.polarimetric.gpf.decompositions_cp;

import org.csa.rstb.polarimetric.gpf.CompactPolProcessor;
import org.csa.rstb.polarimetric.gpf.StokesParameters;
import org.csa.rstb.polarimetric.gpf.decompositions.EigenDecomposition;
import org.csa.rstb.polarimetric.gpf.decompositions.HAlphaC2;
import org.esa.s1tbx.commons.polsar.PolBandUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.math.MathUtils;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.*;
import java.util.Map;

/**
 * Perform H-Alpha decomposition for given tile.
 * <p>
 * [1]	S. R. Cloude, D. G. Goodenough, H. Chen, "Compact Decomposition Theory",
 * IEEE Geoscience and Remote Sensing Letters, Vol. 9, No. 1, Jan. 2012.
 */
public class CP_HAlpha extends HAlphaC2 implements CompactPolProcessor {

    private final String compactMode;
    private final boolean computeAlphaByT3;
    private final boolean useRCMConvention;

    public CP_HAlpha(final PolBandUtils.PolSourceBand[] srcBandList, final PolBandUtils.MATRIX sourceProductType,
                     final String compactMode, final int windowSizeX, final int windowSizeY,
                     final boolean computeAlphaByT3, final int srcImageWidth, final int srcImageHeight) {
        super(srcBandList, sourceProductType, windowSizeX, windowSizeY, srcImageWidth, srcImageHeight);

        this.compactMode = compactMode;
        this.computeAlphaByT3 = computeAlphaByT3;
        this.useRCMConvention = PolBandUtils.useRCMConvention();
    }

    /**
     * Perform decomposition for given tile.
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed.
     * @param op              the polarimetric decomposition operator
     * @throws org.esa.snap.core.gpf.OperatorException If an error occurs during computation of the filtered value.
     */
    public void computeTile(final Map<Band, Tile> targetTiles, final Rectangle targetRectangle, final Operator op) {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int maxY = y0 + h;
        final int maxX = x0 + w;
        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

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

            final double[][] Cr = new double[2][2]; // real part of covariance matrix
            final double[][] Ci = new double[2][2]; // imaginary part of covariance matrix

            final Tile[] sourceTiles = new Tile[bandList.srcBands.length];
            final ProductData[] dataBuffers = new ProductData[bandList.srcBands.length];
            for (int i = 0; i < bandList.srcBands.length; i++) {
                sourceTiles[i] = op.getSourceTile(bandList.srcBands[i], sourceRectangle);
                dataBuffers[i] = sourceTiles[i].getDataBuffer();
            }
            double v = 0.0;

            if (computeAlphaByT3) {
                final double[] g = new double[4];       // Stokes vector

                for (int y = y0; y < maxY; ++y) {
                    trgIndex.calculateStride(y);
                    for (int x = x0; x < maxX; ++x) {
                        final int index = trgIndex.getIndex(x);

                        getMeanCovarianceMatrixC2(x, y, halfWindowSizeX, halfWindowSizeY, sourceImageWidth,
                                sourceImageHeight, sourceProductType, sourceTiles, dataBuffers, Cr, Ci);

                        StokesParameters.computeCompactPolStokesVector(Cr, Ci, g);

                        HAAlpha data = computeHAAlphaByT3(Cr, Ci, g, compactMode, useRCMConvention);

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

            } else { // computeAlphaByC2

                for (int y = y0; y < maxY; ++y) {
                    trgIndex.calculateStride(y);
                    for (int x = x0; x < maxX; ++x) {
                        final int index = trgIndex.getIndex(x);

                        getMeanCovarianceMatrixC2(x, y, halfWindowSizeX, halfWindowSizeY, sourceImageWidth,
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
    }

    public static HAlphaC2.HAAlpha computeHAAlphaByT3(
            final double[][] Cr, double[][] Ci, final double[] g, final String compactMode, final boolean useRCMConvention) {

        final HAlphaC2.HAAlpha data = new HAlphaC2.HAAlpha();
        final double[][] EigenVectRe = new double[2][2];
        final double[][] EigenVectIm = new double[2][2];
        final double[] EigenVal = new double[2];

        EigenDecomposition.eigenDecomposition(2, Cr, Ci, EigenVectRe, EigenVectIm, EigenVal);

        final double sum = EigenVal[0] + EigenVal[1];
        final double[] p = {EigenVal[0] / sum, EigenVal[1] / sum};
        data.entropy = -(p[0] * Math.log(p[0] + Constants.EPS) + p[1] * Math.log(p[1] + Constants.EPS)) / Math.log(2);
        data.anisotropy = (p[0] - p[1]) / (p[0] + p[1] + Constants.EPS);

        if (compactMode.equals(CompactPolProcessor.lch) && !useRCMConvention ||
                compactMode.equals(CompactPolProcessor.rch) && useRCMConvention) {
            data.alpha = 0.5 * Math.atan2(Math.sqrt(g[1] * g[1] + g[2] * g[2]), g[3]) * MathUtils.RTOD;   // LHC
        } else {
            data.alpha = 0.5 * Math.atan2(Math.sqrt(g[1] * g[1] + g[2] * g[2]), -g[3]) * MathUtils.RTOD;  // RHC
        }

        return data;
    }
}