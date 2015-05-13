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
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.gpf.Operator;
import org.esa.snap.framework.gpf.OperatorException;
import org.esa.snap.framework.gpf.Tile;
import org.esa.s1tbx.dataio.PolBandUtils;
import org.esa.snap.datamodel.Unit;
import org.esa.snap.gpf.TileIndex;

import java.awt.*;
import java.util.Map;

/**
 * Perform Generalized Freeman-Durden decomposition for given tile.
 */
public class GeneralizedFreemanDurden extends DecompositionBase implements Decomposition {

    private int srcImageWidth = 0;
    private int srcImageHeight = 0;

    public GeneralizedFreemanDurden(final PolBandUtils.PolSourceBand[] srcBandList, final PolBandUtils.MATRIX sourceProductType,
                         final int windowSize, final int srcImageWidth, final int srcImageHeight) {
        super(srcBandList, sourceProductType, windowSize, windowSize, srcImageWidth, srcImageHeight);
        this.srcImageWidth = srcImageWidth;
        this.srcImageHeight = srcImageHeight;
    }

    /**
     * Return the list of band names for the target product
     */
    public String[] getTargetBandNames() {
        return new String[]{"Gen_Freeman_dbl_r", "Gen_Freeman_vol_g", "Gen_Freeman_surf_b"};
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
     * @throws org.esa.snap.framework.gpf.OperatorException If an error occurs during computation of the filtered value.
     */
    public void computeTile(final Map<Band, Tile> targetTiles, final Rectangle targetRectangle,
                            final Operator op) throws OperatorException {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int maxY = y0 + h;
        final int maxX = x0 + w;
        //System.out.println("freeman x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        for (final PolBandUtils.PolSourceBand bandList : srcBandList) {

            final TargetInfo[] targetInfo = new TargetInfo[bandList.targetBands.length];
            int j = 0;
            for (Band targetBand : bandList.targetBands) {
                final String targetBandName = targetBand.getName();
                if (targetBandName.contains("Gen_Freeman_dbl_r")) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.R);
                } else if (targetBandName.contains("Gen_Freeman_vol_g")) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.G);
                } else if (targetBandName.contains("Gen_Freeman_surf_b")) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.B);
                }
                ++j;
            }
            final TileIndex trgIndex = new TileIndex(targetInfo[0].tile);

            final double[][] Tr = new double[3][3];
            final double[][] Ti = new double[3][3];

            if (!bandList.spanMinMaxSet) {
                setSpanMinMax(op, bandList);
            }

            final Tile[] sourceTiles = new Tile[bandList.srcBands.length];
            final ProductData[] dataBuffers = new ProductData[bandList.srcBands.length];
            final Rectangle sourceRectangle = getSourceRectangle(x0, y0, w, h);
            for (int i = 0; i < bandList.srcBands.length; ++i) {
                sourceTiles[i] = op.getSourceTile(bandList.srcBands[i], sourceRectangle);
                dataBuffers[i] = sourceTiles[i].getDataBuffer();
            }
            final TileIndex srcIndex = new TileIndex(sourceTiles[0]);

            double pd, pv, ps;
            for (int y = y0; y < maxY; ++y) {
                trgIndex.calculateStride(y);
                for (int x = x0; x < maxX; ++x) {

                    PolOpUtils.getMeanCoherencyMatrix(x, y, halfWindowSizeX, halfWindowSizeY,srcImageWidth,
                            srcImageHeight, sourceProductType, srcIndex, dataBuffers, Tr, Ti);

                    final FDD data = getGeneralizedFreemanDurdenDecomposition(Tr, Ti);

                    ps = scaleDb(data.ps, bandList.spanMin, bandList.spanMax);
                    pd = scaleDb(data.pd, bandList.spanMin, bandList.spanMax);
                    pv = scaleDb(data.pv, bandList.spanMin, bandList.spanMax);

                    // save Pd as red, Pv as green and Ps as blue
                    for (TargetInfo target : targetInfo) {

                        if (target.colour == TargetBandColour.R) {
                            target.dataBuffer.setElemFloatAt(trgIndex.getIndex(x), (float) pd);
                        } else if (target.colour == TargetBandColour.G) {
                            target.dataBuffer.setElemFloatAt(trgIndex.getIndex(x), (float) pv);
                        } else if (target.colour == TargetBandColour.B) {
                            target.dataBuffer.setElemFloatAt(trgIndex.getIndex(x), (float) ps);
                        }
                    }
                }
            }
        }
    }

    /**
     * Compute Perform Generalized Freeman-Durden decomposition for given covariance matrix C3
     *
     * @param Tr Real part of the coherency matrix
     * @param Ti Imaginary part of the coherency matrix
     * @return The Generalized Freeman-Durden decomposition result
     */
    public static FDD getGeneralizedFreemanDurdenDecomposition(final double[][] Tr, final double[][] Ti) {

        final double mv = Tr[2][2];
        final double tmp1 = Tr[0][0] + Tr[1][1] - 3*Tr[2][2];
        final double tmp2 = Tr[0][0] - Tr[1][1] - Tr[2][2];
        final double tmp3 = Math.sqrt(tmp2*tmp2 + 4.0*(Tr[0][1]*Tr[0][1] + Ti[0][1]*Ti[0][1]));
        final double ms = 0.5*(tmp1 + tmp3); // Note md and ms calculation is different from the given in the book.
        final double md = 0.5*(tmp1 - tmp3);

        /*final double alpha_d = Math.acos(Math.pow(1 + (Tr[0][1]*Tr[0][1] +
                Ti[0][1]*Ti[0][1])/Math.pow((Tr[1][1] - Tr[2][2] - md),2), -0.5));

        final double alpha_s = Math.acos(Math.pow(1 + (Tr[0][1]*Tr[0][1] +
                Ti[0][1]*Ti[0][1])/Math.pow((Tr[1][1] - Tr[2][2] - ms),2), -0.5));

        final double t11 = ms*Math.cos(alpha_s)*Math.cos(alpha_s) + md*Math.sin(alpha_s)*Math.sin(alpha_s) + 2.0*mv;
        final double tmp4 = t11 - Tr[0][0];*/

        return new FDD(4.0*mv, md, ms);
    }

    public static class FDD {
        public final double pv;
        public final double pd;
        public final double ps;

        public FDD(final double pv, final double pd, final double ps) {
            this.pd = pd;
            this.ps = ps;
            this.pv = pv;
        }
    }
}