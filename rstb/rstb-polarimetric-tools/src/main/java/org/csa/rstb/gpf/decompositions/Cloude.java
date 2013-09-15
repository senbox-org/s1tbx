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
import org.esa.beam.framework.gpf.Tile;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.gpf.PolBandUtils;
import org.esa.nest.gpf.TileIndex;

import java.awt.*;
import java.util.Map;

/**
 * Perform Cloude decomposition for given tile.
 */
public class Cloude extends DecompositionBase implements Decomposition {

    public Cloude(final PolBandUtils.QuadSourceBand[] srcBandList, final PolBandUtils.MATRIX sourceProductType,
                  final int windowSize, final int srcImageWidth, final int srcImageHeight) {
        super(srcBandList, sourceProductType, windowSize, srcImageWidth, srcImageHeight);
    }

    /**
        Return the list of band names for the target product
        @return list of band names
     */
    public String[] getTargetBandNames() {
        return new String[] { "Cloude_dbl_r", "Cloude_vol_g", "Cloude_surf_b" };
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
    public void computeTile(final Map<Band, Tile> targetTiles, final Rectangle targetRectangle, final Operator op) {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w  = targetRectangle.width;
        final int h  = targetRectangle.height;
        final int maxY = y0 + h;
        final int maxX = x0 + w;
        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        for(final PolBandUtils.QuadSourceBand bandList : srcBandList) {
 
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
            for (int i = 0; i < bandList.srcBands.length; ++i) {
                sourceTiles[i] = op.getSourceTile(bandList.srcBands[i], sourceRectangle);
                dataBuffers[i] = sourceTiles[i].getDataBuffer();
            }
            final TileIndex srcIndex = new TileIndex(sourceTiles[0]);
            
            final double[][] EigenVectRe = new double[3][3];
            final double[][] EigenVectIm = new double[3][3];
            final double[] EigenVal = new double[3];

            double v = 0.0;
            for(int y = y0; y < maxY; ++y) {
                trgIndex.calculateStride(y);
                for(int x = x0; x < maxX; ++x) {

                    PolOpUtils.getMeanCoherencyMatrix(x, y, halfWindowSize, sourceImageWidth, sourceImageHeight,
                            sourceProductType, srcIndex, dataBuffers, Tr, Ti);

                    PolOpUtils.eigenDecomposition(3, Tr, Ti, EigenVectRe, EigenVectIm, EigenVal);

                    final double t11 = EigenVal[0]*(EigenVectRe[0][0]*EigenVectRe[0][0] + EigenVectIm[0][0]*EigenVectIm[0][0]);
                    final double t22 = EigenVal[0]*(EigenVectRe[1][0]*EigenVectRe[1][0] + EigenVectIm[1][0]*EigenVectIm[1][0]);
                    final double t33 = EigenVal[0]*(EigenVectRe[2][0]*EigenVectRe[2][0] + EigenVectIm[2][0]*EigenVectIm[2][0]);

                    for (TargetInfo target : targetInfo){

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
                        v = 10.0*Math.log10(v);
                        target.dataBuffer.setElemFloatAt(trgIndex.getIndex(x), (float)v);
                    }
                }
            }
        }
    }
}