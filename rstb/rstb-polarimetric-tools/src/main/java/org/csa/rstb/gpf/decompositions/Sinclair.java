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
 * Perform Sinclair decomposition for given tile.
 */
public class Sinclair extends DecompositionBase implements Decomposition {

    public Sinclair(final PolBandUtils.QuadSourceBand[] srcBandList, final PolBandUtils.MATRIX sourceProductType,
                    final int windowSize, final int srcImageWidth, final int srcImageHeight) {
        super(srcBandList, sourceProductType, windowSize, srcImageWidth, srcImageHeight);
    }

    /**
        Return the list of band names for the target product
        @return list of band names
     */
    public String[] getTargetBandNames() {
        return new String[] { "Sinclair_r", "Sinclair_g", "Sinclair_b" };
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
                if (targetBandName.contains("Sinclair_r")) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.R);
                } else if (targetBandName.contains("Sinclair_g")) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.G);
                } else if (targetBandName.contains("Sinclair_b")) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.B);
                }
                ++j;
            }
            final TileIndex trgIndex = new TileIndex(targetInfo[0].tile);

            final double[][] Sr = new double[2][2]; // real part of scatter matrix
            final double[][] Si = new double[2][2]; // imaginary part of scatter matrix
            final double[][] Cr = new double[3][3]; // real part of covariance matrix
            final double[][] Ci = new double[3][3]; // imaginary part of covariance matrix
            final double[][] Tr = new double[3][3]; // real part of coherency matrix
            final double[][] Ti = new double[3][3]; // imaginary part of coherency matrix

            final Tile[] sourceTiles = new Tile[bandList.srcBands.length];
            final ProductData[] dataBuffers = new ProductData[bandList.srcBands.length];
            for (int i = 0; i < bandList.srcBands.length; i++) {
                sourceTiles[i] = op.getSourceTile(bandList.srcBands[i], targetRectangle);
                dataBuffers[i] = sourceTiles[i].getDataBuffer();
            }
            double re = 0.0, im = 0.0, v = 0.0;
            for(int y = y0; y < maxY; ++y) {
                trgIndex.calculateStride(y);
                for(int x = x0; x < maxX; ++x) {
                    final int index = trgIndex.getIndex(x);

                    if (sourceProductType == PolBandUtils.MATRIX.FULL) {
                        PolOpUtils.getComplexScatterMatrix(index, dataBuffers, Sr, Si);

                        for (TargetInfo target : targetInfo){

                            if (target.colour == TargetBandColour.R) {
                                re = Sr[1][1];
                                im = Si[1][1];
                            } else if (target.colour == TargetBandColour.G) {
                                re = 0.5*(Sr[0][1] + Sr[1][0]);
                                im = 0.5*(Si[0][1] + Si[1][0]);
                            } else if (target.colour == TargetBandColour.B) {
                                re = Sr[0][0];
                                im = Si[0][0];
                            }

                            v = re*re + im*im;
                            if (v < PolOpUtils.EPS) {
                                v = PolOpUtils.EPS;
                            }
                            v = 10.0*Math.log10(v);
                            target.dataBuffer.setElemFloatAt(index, (float)v);
                        }

                    } else if (sourceProductType == PolBandUtils.MATRIX.C3) {

                        PolOpUtils.getCovarianceMatrixC3(index, dataBuffers, Cr, Ci);
                        for (TargetInfo target : targetInfo){

                            if (target.colour == TargetBandColour.R) { // C33
                                v = Cr[2][2];
                            } else if (target.colour == TargetBandColour.G) { // 0.5*C22
                                v = 0.5*Cr[1][1];
                            } else if (target.colour == TargetBandColour.B) { // C11
                                v = Cr[0][0];
                            }

                            if (v < PolOpUtils.EPS) {
                                v = PolOpUtils.EPS;
                            }
                            v = 10.0*Math.log10(v);
                            target.dataBuffer.setElemFloatAt(index, (float)v);
                        }

                    } else if (sourceProductType == PolBandUtils.MATRIX.T3) {

                        PolOpUtils.getCoherencyMatrixT3(index, dataBuffers, Tr, Ti);
                        for (TargetInfo target : targetInfo){

                            if (target.colour == TargetBandColour.R) { // 0.5*(T11+T22) - T12_real
                                v = 0.5*(Tr[0][0] + Tr[1][1]) - Tr[0][1];
                            } else if (target.colour == TargetBandColour.G) { // 0.5*T33
                                v = 0.5*Tr[2][2];
                            } else if (target.colour == TargetBandColour.B) { // 0.5*(T11+T22) + T12_real
                                v = 0.5*(Tr[0][0] + Tr[1][1]) + Tr[0][1];
                            }

                            if (v < PolOpUtils.EPS) {
                                v = PolOpUtils.EPS;
                            }
                            v = 10.0*Math.log10(v);
                            target.dataBuffer.setElemFloatAt(index, (float)v);
                        }
                    }

                }
            }
        }
    }
}