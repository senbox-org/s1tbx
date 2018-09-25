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

import org.csa.rstb.polarimetric.gpf.QuadPolProcessor;
import org.esa.s1tbx.commons.polsar.PolBandUtils;
import org.esa.s1tbx.commons.polsar.PolBandUtils.MATRIX;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.*;
import java.util.Map;

/**
 * Perform van Zyl decomposition for given tile.
 */
public class vanZyl extends DecompositionBase implements Decomposition, QuadPolProcessor {

    public vanZyl(final PolBandUtils.PolSourceBand[] srcBandList, final MATRIX sourceProductType,
                  final int windowSize, final int srcImageWidth, final int srcImageHeight) {
        super(srcBandList, sourceProductType, windowSize, windowSize, srcImageWidth, srcImageHeight);
    }

    public String getSuffix() {
        return "_VanZyl";
    }

    /**
     * Return the list of band names for the target product
     */
    public String[] getTargetBandNames() {
        return new String[]{"VanZyl_dbl_r", "VanZyl_vol_g", "VanZyl_surf_b"};
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
     * Compute min/max values of the Span image.
     *
     * @param op       the decomposition operator
     * @param bandList the src band list
     * @throws OperatorException when thread fails
     */
    private synchronized void setSpanMinMax(final Operator op, final PolBandUtils.PolSourceBand bandList)
            throws OperatorException {

        if (bandList.spanMinMaxSet) {
            return;
        }
        final DecompositionBase.MinMax span = computeSpanMinMax(op, sourceProductType, halfWindowSizeX, halfWindowSizeY, bandList);
        bandList.spanMin = span.min;
        bandList.spanMax = span.max;
        bandList.spanMinMaxSet = true;
    }

    /**
     * Perform decomposition for given tile.
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed.
     * @param op              the polarimetric decomposition operator
     * @throws OperatorException If an error occurs during computation of the filtered value.
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
                if (targetBandName.contains("VanZyl_dbl_r")) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.R);
                } else if (targetBandName.contains("VanZyl_vol_g")) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.G);
                } else if (targetBandName.contains("VanZyl_surf_b")) {
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
            getQuadPolDataBuffer(op, bandList.srcBands, sourceRectangle, sourceProductType, sourceTiles, dataBuffers);

            final TileIndex srcIndex = new TileIndex(sourceTiles[0]);
            final double nodatavalue = bandList.srcBands[0].getNoDataValue();

            double C11, C22, C33, ratio, HHHHv, VVVVv, HVHVv, HHVVvre, C13_re, C13_im, sq_rt, alp1, alp2, alp3, alpmin, FV;
            double alpha, mu, rhoRe, rhoIm, rho2, eta, delta, lambda1, lambda2, tmp1, tmp2;
            double Lambda1, Lambda2, AlphaRe, AlphaIm, BetaRe, BetaIm, Ps, Pd, Pv;
            for (int y = y0; y < maxY; ++y) {
                trgIndex.calculateStride(y);
                srcIndex.calculateStride(y);
                for (int x = x0; x < maxX; ++x) {
                    boolean isNoData = isNoData(dataBuffers, srcIndex.getIndex(x), nodatavalue);
                    if (isNoData) {
                        for (TargetInfo target : targetInfo) {
                            target.dataBuffer.setElemFloatAt(trgIndex.getIndex(x), (float) nodatavalue);
                        }
                        continue;
                    }

                    if (sourceProductType == MATRIX.FULL ||
                            sourceProductType == MATRIX.C3) {

                        getMeanCovarianceMatrix(x, y, halfWindowSizeX, halfWindowSizeY,
                                sourceProductType, sourceTiles, dataBuffers, Cr, Ci);

                    } else if (sourceProductType == MATRIX.T3) {

                        getMeanCoherencyMatrix(x, y, halfWindowSizeX, halfWindowSizeY,
                                sourceImageWidth, sourceImageHeight, sourceProductType, srcIndex, dataBuffers, Tr, Ti);

                        t3ToC3(Tr, Ti, Cr, Ci);
                    }

                    final VDD data = getVanZylDecomposition(Cr, Ci);

                    Ps = scaleDb(data.ps, bandList.spanMin, bandList.spanMax);
                    Pd = scaleDb(data.pd, bandList.spanMin, bandList.spanMax);
                    Pv = scaleDb(data.pv, bandList.spanMin, bandList.spanMax);

                    // save Pd as red, Pv as green and Ps as blue
                    for (TargetInfo target : targetInfo) {

                        if (target.colour == TargetBandColour.R) {
                            target.dataBuffer.setElemFloatAt(trgIndex.getIndex(x), (float) Pd);
                        } else if (target.colour == TargetBandColour.G) {
                            target.dataBuffer.setElemFloatAt(trgIndex.getIndex(x), (float) Pv);
                        } else if (target.colour == TargetBandColour.B) {
                            target.dataBuffer.setElemFloatAt(trgIndex.getIndex(x), (float) Ps);
                        }
                    }
                }
            }
        }
    }

    public static VDD getVanZylDecomposition(final double[][] Cr, final double[][] Ci) {

        double C11, C22, C33, ratio, HHHHv, VVVVv, HVHVv, HHVVvre, C13_re, C13_im, sq_rt, alp1, alp2, alp3, alpmin, FV;
        double alpha, mu, rhoRe, rhoIm, rho2, eta, delta, lambda1, lambda2, tmp1, tmp2;
        double Lambda1, Lambda2, AlphaRe, AlphaIm, BetaRe, BetaIm, Ps, Pd, Pv;

        C11 = Cr[0][0];
        C22 = Cr[1][1];
        C33 = Cr[2][2];
        C13_re = Cr[0][2];
        C13_im = Ci[0][2];

        ratio = 10.0 * Math.log10(C33 / C11);
        if (ratio <= -2.0) {
            HHHHv = 8.0;
            VVVVv = 3.0;
            HVHVv = 4.0;
            HHVVvre = 2.0;
        } else if (ratio > 2.0) {
            HHHHv = 3.0;
            VVVVv = 8.0;
            HVHVv = 4.0;
            HHVVvre = 2.0;
        } else {
            HHHHv = 3.0;
            VVVVv = 3.0;
            HVHVv = 2.0;
            HHVVvre = 1.0;
        }

        sq_rt = C11 * VVVVv + C33 * HHHHv - 2. * C13_re * HHVVvre;
        sq_rt = sq_rt * sq_rt - 4.0 * (HHVVvre * HHVVvre - HHHHv * VVVVv) * (C13_re * C13_re + C13_im * C13_im - C11 * C33);
        sq_rt = Math.sqrt(sq_rt + Constants.EPS);

        alp1 = 2.0 * C13_re * HHVVvre - (C11 * VVVVv + C33 * HHHHv) + sq_rt;
        alp1 = alp1 / 2.0 / (HHVVvre - HHHHv * VVVVv + Constants.EPS);

        alp2 = 2.0 * C13_re * HHVVvre - (C11 * VVVVv + C33 * HHHHv) - sq_rt;
        alp2 = alp2 / 2.0 / (HHVVvre - HHHHv * VVVVv + Constants.EPS);

        alp3 = C22 / HVHVv;

        alpmin = Math.min(Math.min(alp1, alp2), alp3);
        if (ratio <= -2.0) {
            FV = 15.0 * alpmin;
            C11 = C11 - 8.0 * alpmin;
            C33 = C33 - 3.0 * alpmin;
            C13_re = C13_re - 2.0 * alpmin;
        } else if (ratio > 2.0) {
            FV = 15.0 * alpmin;
            C11 = C11 - 3.0 * alpmin;
            C33 = C33 - 8.0 * alpmin;
            C13_re = C13_re - 2.0 * alpmin;
        } else {
            FV = 8.0 * alpmin;
            C11 = C11 - 3.0 * alpmin;
            C33 = C33 - 3.0 * alpmin;
            C13_re = C13_re - 1.0 * alpmin;
        }

        alpha = C11;
        mu = C33 / C11;
        eta = C22 / C11;
        rhoRe = C13_re / C11;
        rhoIm = C13_im / C11;
        rho2 = rhoRe * rhoRe + rhoIm * rhoIm;

        delta = Math.sqrt((1.0 - mu) * (1.0 - mu) + 4.0 * rho2);
        lambda1 = 0.5 * alpha * (1.0 + mu + delta);
        lambda2 = 0.5 * alpha * (1.0 + mu - delta);

        tmp1 = (mu - 1.0 + delta) * (mu - 1.0 + delta);
        tmp2 = tmp1 + 4.0 * rho2;
        Lambda1 = lambda1 * tmp1 / tmp2;

        tmp1 = (mu - 1.0 - delta) * (mu - 1.0 - delta);
        tmp2 = tmp1 + 4.0 * rho2;
        Lambda2 = lambda2 * tmp1 / tmp2;

        AlphaRe = 2.0 * rhoRe / (mu - 1.0 + delta);
        AlphaIm = 2.0 * rhoIm / (mu - 1.0 + delta);
        BetaRe = 2.0 * rhoRe / (mu - 1.0 - delta);
        BetaIm = 2.0 * rhoIm / (mu - 1.0 - delta);

        tmp1 = Lambda1 * ((1.0 + AlphaRe) * (1.0 + AlphaRe) + AlphaIm * AlphaIm);
        tmp2 = Lambda2 * ((1.0 - AlphaRe) * (1.0 - AlphaRe) + AlphaIm * AlphaIm);

        if (tmp1 > tmp2) {
            Ps = Lambda1 * (1 + AlphaRe * AlphaRe + AlphaIm * AlphaIm);
            Pd = Lambda2 * (1 + BetaRe * BetaRe + BetaIm * BetaIm);
        } else {
            Pd = Lambda1 * (1 + AlphaRe * AlphaRe + AlphaIm * AlphaIm);
            Ps = Lambda2 * (1 + BetaRe * BetaRe + BetaIm * BetaIm);
        }

        Pv = FV;

        return new VDD(Pv, Pd, Ps);
    }

    public static class VDD {
        public final double pv;
        public final double pd;
        public final double ps;

        public VDD(final double pv, final double pd, final double ps) {
            this.pd = pd;
            this.ps = ps;
            this.pv = pv;
        }
    }
}
