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
import org.esa.nest.gpf.TileIndex;

import java.awt.*;
import java.util.Map;

/**
 * Perform Yamaguchi decomposition for given tile.
 */
public class Yamaguchi extends DecompositionBase implements Decomposition {

    public Yamaguchi(final PolBandUtils.QuadSourceBand[] srcBandList, final PolBandUtils.MATRIX sourceProductType,
                     final int windowSize, final int srcImageWidth, final int srcImageHeight) {
        super(srcBandList, sourceProductType, windowSize, srcImageWidth, srcImageHeight);
    }

    /**
        Return the list of band names for the target product
     */
    public String[] getTargetBandNames() {
        return new String[] { "Yamaguchi_dbl_r", "Yamaguchi_vol_g", "Yamaguchi_surf_b", "Yamaguchi_hlx" };
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
                if (targetBandName.contains("Yamaguchi_dbl_r")) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.R);
                } else if (targetBandName.contains("Yamaguchi_vol_g")) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.G);
                } else if (targetBandName.contains("Yamaguchi_surf_b")) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.B);
                } else if (targetBandName.contains("Yamaguchi_hlx")) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), null);
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

            double ratio, d, cR, cI, c0, s, pd, pv, ps, pc, span, k1, k2, k3;
            for(int y = y0; y < maxY; ++y) {
                trgIndex.calculateStride(y);
                for(int x = x0; x < maxX; ++x) {

                    PolOpUtils.getMeanCovarianceMatrix(x, y, halfWindowSize, sourceImageWidth, sourceImageHeight,
                                                       sourceProductType, sourceTiles, dataBuffers, Cr, Ci);

                    PolOpUtils.c3ToT3(Cr, Ci, Tr, Ti);

                    span = Tr[0][0] + Tr[1][1] + Tr[2][2];
                    pc = 2*Math.abs(Ti[1][2]);
                    ratio = 10*Math.log10(Cr[2][2] / Cr[0][0]);

                    if (ratio <= -2) {
                        k1 = 1.0/6.0;
                        k2 = 7.0/30.0;
                        k3 = 4.0/15.0;
                    } else if (ratio > 2) {
                        k1 = -1.0/6.0;
                        k2 = 7.0/30.0;
                        k3 = 4.0/15.0;
                    } else { // -2 < ratio <= 2
                        k1 = 0.0;
                        k2 = 1.0/4.0;
                        k3 = 1.0/4.0;
                    }

                    pv = (Tr[2][2] - 0.5*pc) / k3;

                    if (pv <= 0) { // Freeman-Durden 3 component decomposition
                        pc = 0;
                        final FreemanDurden.FDD data = FreemanDurden.getFreemanDurdenDecomposition(Cr, Ci);
                        ps = data.ps;
                        pd = data.pd;
                        pv = data.pv;

                    } else { // Yamaguchi 4 component decomposition

                        s = Tr[0][0] - 0.5*pv;
                        d = Tr[1][1] - k2*pv - 0.5*pc;
                        cR = Tr[0][1] - k1*pv;
                        cI = Ti[0][1];

                        if (pv + pc < span) {

                            c0 = Cr[0][2] - 0.5*Cr[1][1] + 0.5*pc;
                            if (c0 < 0) {
                                ps = s - (cR*cR + cI*cI)/d;
                                pd = d + (cR*cR + cI*cI)/d;
                            } else {
                                ps = s + (cR*cR + cI*cI)/s;
                                pd = d - (cR*cR + cI*cI)/s;
                            }

                            if (ps > 0 && pd < 0) {
                                pd = 0;
                                ps = span - pv - pc;
                            } else if (ps < 0 && pd > 0) {
                                ps = 0;
                                pd = span - pv - pc;
                            } else if (ps < 0 && pd < 0) {
                                ps = 0;
                                pd = 0;
                                pv = span - pc;
                            }

                        } else {
                            ps = 0.0;
                            pd = 0.0;
                            pv = span - pc;
                        }
                    }

                    ps = scaleDb(ps, bandList.spanMin, bandList.spanMax);
                    pd = scaleDb(pd, bandList.spanMin, bandList.spanMax);
                    pv = scaleDb(pv, bandList.spanMin, bandList.spanMax);
                    pc = scaleDb(pc, bandList.spanMin, bandList.spanMax);

                    // save Pd as red, Pv as green and Ps as blue
                    for (TargetInfo target : targetInfo){

                        if (target.colour == TargetBandColour.R) {
                            target.dataBuffer.setElemFloatAt(trgIndex.getIndex(x), (float)pd);
                        } else if (target.colour == TargetBandColour.G) {
                            target.dataBuffer.setElemFloatAt(trgIndex.getIndex(x), (float)pv);
                        } else if (target.colour == TargetBandColour.B) {
                            target.dataBuffer.setElemFloatAt(trgIndex.getIndex(x), (float)ps);
                        } else {
                            target.dataBuffer.setElemFloatAt(trgIndex.getIndex(x), (float)pc);
                        }
                    }
                }
            }
        }
    }
}