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
import org.esa.nest.eo.Constants;

import java.awt.*;
import java.util.Map;

/**
 * Perform FreemanDurden decomposition for given tile.
 */
public class FreemanDurden extends DecompositionBase implements Decomposition {

    public FreemanDurden(final PolBandUtils.QuadSourceBand[] srcBandList, final PolBandUtils.MATRIX sourceProductType,
                         final int windowSize, final int srcImageWidth, final int srcImageHeight) {
        super(srcBandList, sourceProductType, windowSize, srcImageWidth, srcImageHeight);
    }

    /**
        Return the list of band names for the target product
     */
    public String[] getTargetBandNames() {
        return new String[] { "Freeman_dbl_r", "Freeman_vol_g", "Freeman_surf_b" };
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
                if (targetBandName.contains("Freeman_dbl_r")) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.R);
                } else if (targetBandName.contains("Freeman_vol_g")) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.G);
                } else if (targetBandName.contains("Freeman_surf_b")) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.B);
                }
                ++j;
            }
            final TileIndex trgIndex = new TileIndex(targetInfo[0].tile);

            final double[][] Cr = new double[3][3];
            final double[][] Ci = new double[3][3];

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

            double pd, pv, ps;
            for(int y = y0; y < maxY; ++y) {
                trgIndex.calculateStride(y);
                for(int x = x0; x < maxX; ++x) {

                    PolOpUtils.getMeanCovarianceMatrix(x, y, halfWindowSize, sourceImageWidth, sourceImageHeight,
                                                       sourceProductType, sourceTiles, dataBuffers, Cr, Ci);

                    final FDD data = getFreemanDurdenDecomposition(Cr, Ci);

                    ps = scaleDb(data.ps, bandList.spanMin, bandList.spanMax);
                    pd = scaleDb(data.pd, bandList.spanMin, bandList.spanMax);
                    pv = scaleDb(data.pv, bandList.spanMin, bandList.spanMax);

                    // save Pd as red, Pv as green and Ps as blue
                    for (TargetInfo target : targetInfo){

                        if (target.colour == TargetBandColour.R) {
                            target.dataBuffer.setElemFloatAt(trgIndex.getIndex(x), (float)pd);
                        } else if (target.colour == TargetBandColour.G) {
                            target.dataBuffer.setElemFloatAt(trgIndex.getIndex(x), (float)pv);
                        } else if (target.colour == TargetBandColour.B) {
                            target.dataBuffer.setElemFloatAt(trgIndex.getIndex(x), (float)ps);
                        }
                    }
                }
            }
        }
    }

    /**
     * Compute Perform Freeman-Durden decomposition for given covariance matrix C3
     * @param Cr Real part of the covariance matrix
     * @param Ci Imaginary part of the covariance matrix
     * @return The Freeman-Durden decomposition result
     */
    public static FDD getFreemanDurdenDecomposition(final double[][] Cr, final double[][] Ci) {

        double fd, fv, fs, pd, pv, ps, c11, c13Re, c13Im, c33, alphaRe, alphaIm, betaRe, betaIm;

        // compute fv from C22 and subtract fv from C11, c13, C33
        fv = 4.0*Cr[1][1];
        c11 = Cr[0][0] - fv*3.0/8.0;
        c13Re = Cr[0][2] - fv/8.0;
        c13Im = Ci[0][2];
        c33 = Cr[2][2] - fv*3.0/8.0;
        final double a1 = c11*c33;

        if (c11 <= Constants.EPS || c33 <= Constants.EPS) {
            fs = 0.0;
            fd = 0.0;
            alphaRe = 0.0;
            alphaIm = 0.0;
            betaRe = 0.0;
            betaIm = 0.0;

        } else {

            final double a2 = c13Re*c13Re + c13Im*c13Im;
            if (a1 < a2) {
                final double c13 = Math.sqrt(a2);
                c13Re = Math.sqrt(a1)*c13Re/c13;
                c13Im = Math.sqrt(a1)*c13Im/c13;
            }

            // get sign of Re(C13), if -ve, set beta = 1; else set alpha = -1
            if (c13Re < 0.0) {

                betaRe = 1.0;
                betaIm = 0.0;
                fs = (a1 - c13Re*c13Re - c13Im*c13Im) / (c11 + c33 - 2*c13Re);
                fd = c33 - fs;
                alphaRe = (c13Re - fs) / fd;
                alphaIm = c13Im / fd;

            } else {

                alphaRe = -1.0;
                alphaIm = 0.0;
                fd = (a1 - c13Re*c13Re - c13Im*c13Im) / (c11 + c33 + 2*c13Re);
                fs = c33 - fd;
                betaRe = (c13Re + fd) / fs;
                betaIm = c13Im / fs;
            }
        }

        // compute Ps, Pd and Pv
        ps = fs*(1 + betaRe*betaRe + betaIm*betaIm);
        pd = fd*(1 + alphaRe*alphaRe + alphaIm*alphaIm);
        pv = fv;
        return new FDD(pv,pd,ps);
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