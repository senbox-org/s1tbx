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
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.gpf.PolBandUtils;
import org.esa.nest.gpf.PolBandUtils.MATRIX;
import org.esa.nest.gpf.TileIndex;

import java.awt.*;
import java.util.Map;

/**
 * Perform van Zyl decomposition for given tile.
 */
public class vanZyl extends DecompositionBase implements Decomposition {

    public vanZyl(final PolBandUtils.QuadSourceBand[] srcBandList, final MATRIX sourceProductType,
                  final int windowSize, final int srcImageWidth, final int srcImageHeight) {
        super(srcBandList, sourceProductType, windowSize, srcImageWidth, srcImageHeight);
    }

    /**
        Return the list of band names for the target product
     */
    public String[] getTargetBandNames() {
        return new String[] { "vanZyl_dbl_r", "vanZyl_vol_g", "vanZyl_surf_b" };
    }

    /**
     * Sets the unit for the new target band
     * @param targetBandName the band name
     * @param targetBand the new target band
     */
    public void setBandUnit(final String targetBandName, final Band targetBand) {
        targetBand.setUnit(Unit.INTENSITY_DB);
    }

    /**
     * Perform decomposition for given tile.
     * @param targetTiles The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed.
     * @param op the polarimetric decomposition operator
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the filtered value.
     */
    public void computeTile(final Map<Band, Tile> targetTiles, final Rectangle targetRectangle,
                                    final Operator op) throws OperatorException {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w  = targetRectangle.width;
        final int h  = targetRectangle.height;
        final int maxY = y0 + h;
        final int maxX = x0 + w;
        //System.out.println("freeman x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        for(final PolBandUtils.QuadSourceBand bandList : srcBandList) {

            final TargetInfo[] targetInfo = new TargetInfo[bandList.targetBands.length];
            int j = 0;
            for (Band targetBand : bandList.targetBands) {
                final String targetBandName = targetBand.getName();
                if (targetBandName.contains("vanZyl_dbl_r")) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.R);
                } else if (targetBandName.contains("vanZyl_vol_g")) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.G);
                } else if (targetBandName.contains("vanZyl_surf_b")) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.B);
                }
                ++j;
            }
            final TileIndex trgIndex = new TileIndex(targetInfo[0].tile);

            final double[][] Cr = new double[3][3];
            final double[][] Ci = new double[3][3];
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
            
            double alpha, mu, rhoRe, rhoIm, rho2, eta, delta, lambda1, lambda2, lambda3, fs, fd, fv, tmp1, tmp2;
            for(int y = y0; y < maxY; ++y) {
                trgIndex.calculateStride(y);
                for(int x = x0; x < maxX; ++x) {

                    if (sourceProductType == MATRIX.FULL ||
                        sourceProductType == MATRIX.C3) {

                        PolOpUtils.getMeanCovarianceMatrix(x, y, halfWindowSize, sourceImageWidth, sourceImageHeight,
                                                           sourceProductType, sourceTiles, dataBuffers, Cr, Ci);

                        PolOpUtils.c3ToT3(Cr, Ci, Tr, Ti);

                    } else if (sourceProductType == MATRIX.T3) {

                        PolOpUtils.getMeanCoherencyMatrix(x, y, halfWindowSize, sourceImageWidth, sourceImageHeight,
                                                          sourceProductType, srcIndex, dataBuffers, Tr, Ti);

                        PolOpUtils.t3ToC3(Tr, Ti, Cr, Ci);
                    }

                    alpha = Cr[0][0];
                    mu    = Cr[2][2] / Cr[0][0];
                    eta   = Cr[1][1] / Cr[0][0];
                    rhoRe = Cr[0][2] / Cr[0][0];
                    rhoIm = Ci[0][2] / Cr[0][0];
                    rho2 = rhoRe*rhoRe + rhoIm*rhoIm;

                    delta = Math.sqrt((1 - mu)*(1 - mu) + 4*rho2);
                    lambda1 = 0.5*alpha*(1 + mu + delta);
                    lambda2 = 0.5*alpha*(1 + mu - delta);
                    lambda3 = alpha*eta;

                    tmp1 = (mu - 1 + delta)*(mu - 1 + delta);
                    tmp2 = tmp1 + 4*rho2;
                    fs = lambda1*tmp1/tmp2;

                    tmp1 = (mu - 1 - delta)*(mu - 1 - delta);
                    tmp2 = tmp1 + 4*rho2;
                    fd = lambda2*tmp1/tmp2;
                    fv = lambda3;

                    fs = scaleDb(fs, bandList.spanMin, bandList.spanMax);
                    fd = scaleDb(fd, bandList.spanMin, bandList.spanMax);
                    fv = scaleDb(fv, bandList.spanMin, bandList.spanMax);

                    // save fd as red, fv as green and fs as blue
                    for (TargetInfo target : targetInfo){

                        if (target.colour == TargetBandColour.R) {
                            target.dataBuffer.setElemFloatAt(trgIndex.getIndex(x), (float)fd);
                        } else if (target.colour == TargetBandColour.G) {
                            target.dataBuffer.setElemFloatAt(trgIndex.getIndex(x), (float)fv);
                        } else if (target.colour == TargetBandColour.B) {
                            target.dataBuffer.setElemFloatAt(trgIndex.getIndex(x), (float)fs);
                        }
                    }
                }
            }
        }
    }
}