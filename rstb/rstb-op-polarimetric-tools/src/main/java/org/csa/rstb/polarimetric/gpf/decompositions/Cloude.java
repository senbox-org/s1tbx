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

import org.csa.rstb.polarimetric.gpf.PolOpUtils;
import org.esa.s1tbx.commons.polsar.PolBandUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.Rectangle;
import java.util.Map;

/**
 * Perform Cloude decomposition for given tile.
 */
public class Cloude extends DecompositionBase implements Decomposition {

    public Cloude(final PolBandUtils.PolSourceBand[] srcBandList, final PolBandUtils.MATRIX sourceProductType,
                  final int windowSize, final int srcImageWidth, final int srcImageHeight) {
        super(srcBandList, sourceProductType, windowSize, windowSize, srcImageWidth, srcImageHeight);
    }

    public String getSuffix() {
        return "_Cloude";
    }

    /**
     * Return the list of band names for the target product
     *
     * @return list of band names
     */
    public String[] getTargetBandNames() {
        return new String[]{"Cloude_dbl_r", "Cloude_vol_g", "Cloude_surf_b"};
    }

    /**
     * Sets the unit for the new target band
     *
     * @param targetBandName the band name
     * @param targetBand     the new target band
     */
    public void setBandUnit(final String targetBandName, final Band targetBand) {
        targetBand.setUnit(Unit.INTENSITY_DB);
    }

    /**
     * Perform decomposition for given tile.
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed.
     * @param op              the polarimetric decomposition operator
     * @throws OperatorException If an error occurs during computation of the filtered value.
     */
    public void computeTile(final Map<Band, Tile> targetTiles, final Rectangle targetRectangle, final Operator op) {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int maxY = y0 + h;
        final int maxX = x0 + w;
        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        for (final PolBandUtils.PolSourceBand bandList : srcBandList) {

            final TargetInfo[] targetInfo = new TargetInfo[bandList.targetBands.length];
            int j = 0;
            for (Band targetBand : bandList.targetBands) {
                final String targetBandName = targetBand.getName();
                if (targetBandName.contains("Cloude_dbl_r")) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.R);
                } else if (targetBandName.contains("Cloude_vol_g")) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.G);
                } else if (targetBandName.contains("Cloude_surf_b")) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.B);
                }
                ++j;
            }
            final TileIndex trgIndex = new TileIndex(targetInfo[0].tile);

            final double[][] Tr = new double[3][3];
            final double[][] Ti = new double[3][3];

            final Tile[] sourceTiles = new Tile[bandList.srcBands.length];
            final ProductData[] dataBuffers = new ProductData[bandList.srcBands.length];
            final Rectangle sourceRectangle = getSourceRectangle(x0, y0, w, h);
            PolOpUtils.getDataBuffer(op, bandList.srcBands, sourceRectangle, sourceProductType, sourceTiles, dataBuffers);
            final TileIndex srcIndex = new TileIndex(sourceTiles[0]);

            final double[][] EigenVectRe = new double[3][3];
            final double[][] EigenVectIm = new double[3][3];
            final double[] EigenVal = new double[3];

            double v = 0.0;
            for (int y = y0; y < maxY; ++y) {
                trgIndex.calculateStride(y);
                for (int x = x0; x < maxX; ++x) {

                    PolOpUtils.getMeanCoherencyMatrix(x, y, halfWindowSizeX, halfWindowSizeY, sourceImageWidth, sourceImageHeight,
                            sourceProductType, srcIndex, dataBuffers, Tr, Ti);

                    PolOpUtils.eigenDecomposition(3, Tr, Ti, EigenVectRe, EigenVectIm, EigenVal);

                    final double t11 = EigenVal[0] * (EigenVectRe[0][0] * EigenVectRe[0][0] + EigenVectIm[0][0] * EigenVectIm[0][0]);
                    final double t22 = EigenVal[0] * (EigenVectRe[1][0] * EigenVectRe[1][0] + EigenVectIm[1][0] * EigenVectIm[1][0]);
                    final double t33 = EigenVal[0] * (EigenVectRe[2][0] * EigenVectRe[2][0] + EigenVectIm[2][0] * EigenVectIm[2][0]);

                    for (TargetInfo target : targetInfo) {

                        if (target.colour == TargetBandColour.R) {
                            v = t22;
                        } else if (target.colour == TargetBandColour.G) {
                            v = t33;
                        } else if (target.colour == TargetBandColour.B) {
                            v = t11;
                        }

                        if (v < PolOpUtils.EPS) {
                            v = PolOpUtils.EPS;
                        }
                        v = 10.0 * Math.log10(v);
                        target.dataBuffer.setElemFloatAt(trgIndex.getIndex(x), (float) v);
                    }
                }
            }
        }
    }

    public static RGB getCloudeDecomposition(final double[][] Tr, final double[][] Ti) {

        final double[][] EigenVectRe = new double[3][3];
        final double[][] EigenVectIm = new double[3][3];
        final double[] EigenVal = new double[3];

        PolOpUtils.eigenDecomposition(3, Tr, Ti, EigenVectRe, EigenVectIm, EigenVal);

        final double b = EigenVal[0] * (EigenVectRe[0][0] * EigenVectRe[0][0] + EigenVectIm[0][0] * EigenVectIm[0][0]);
        final double r = EigenVal[0] * (EigenVectRe[1][0] * EigenVectRe[1][0] + EigenVectIm[1][0] * EigenVectIm[1][0]);
        final double g = EigenVal[0] * (EigenVectRe[2][0] * EigenVectRe[2][0] + EigenVectIm[2][0] * EigenVectIm[2][0]);

        return new RGB(r, g, b);
    }

    public static class RGB {
        public final double r;
        public final double g;
        public final double b;

        public RGB(final double r, final double g, final double b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }
}
