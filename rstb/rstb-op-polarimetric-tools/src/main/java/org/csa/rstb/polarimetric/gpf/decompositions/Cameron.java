/*
 * Copyright (C) 2021 SkyWatch. https://www.skywatch.com
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

import org.csa.rstb.polarimetric.gpf.support.QuadPolProcessor;
import org.esa.s1tbx.commons.polsar.PolBandUtils;
import org.esa.s1tbx.commons.polsar.PolBandUtils.MATRIX;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.*;
import java.util.Map;

/**
 * Perform Cameron decomposition for given tile.
 */
public class Cameron extends DecompositionBase implements Decomposition, QuadPolProcessor {

    private final static String SPAN = "span";
    private final static String TAU = "tau";
    private final static String I_HH = "i_HH";
    private final static String Q_HH = "q_HH";
    private final static String I_HV = "i_HV";
    private final static String Q_HV = "q_HV";
    private final static String I_VH = "i_VH";
    private final static String Q_VH = "q_VH";
    private final static String I_VV = "i_VV";
    private final static String Q_VV = "q_VV";


    public Cameron(final PolBandUtils.PolSourceBand[] srcBandList, final MATRIX sourceProductType,
                    final int windowSize, final int srcImageWidth, final int srcImageHeight) {
        super(srcBandList, sourceProductType, windowSize, windowSize, srcImageWidth, srcImageHeight);
    }

    public String getSuffix() {
        return "_Cameron";
    }

    /**
     * Return the list of band names for the target product
     *
     * @return list of band names
     */
    public String[] getTargetBandNames() {
        return new String[]{I_HH, Q_HH, I_HV, Q_HV, I_VH, Q_VH, I_VV, Q_VV, SPAN, TAU};
    }

    /**
     * Sets the unit for the new target band
     *
     * @param targetBandName the band name
     * @param targetBand     the new target band
     */
    public void setBandUnit(final String targetBandName, final Band targetBand) {
        if (targetBandName.contains("i_")) {
            targetBand.setUnit(Unit.REAL);
        } else if (targetBandName.contains("q_")) {
            targetBand.setUnit(Unit.IMAGINARY);
        } else if (targetBandName.equals(SPAN)) {
            targetBand.setUnit(Unit.INTENSITY);
        } else if (targetBandName.equals(TAU)) {
            targetBand.setUnit(Unit.RADIANS);
        }
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

        final TileIndex trgIndex = new TileIndex(targetTiles.get(op.getTargetProduct().getBandAt(0)));

        final double[][] Sr = new double[3][3];
        final double[][] Si = new double[3][3];

        for (final PolBandUtils.PolSourceBand bandList : srcBandList) {

            final Tile[] sourceTiles = new Tile[bandList.srcBands.length];
            final ProductData[] dataBuffers = new ProductData[bandList.srcBands.length];
            final Rectangle sourceRectangle = getSourceRectangle(x0, y0, w, h);
            getQuadPolDataBuffer(op, bandList.srcBands, sourceRectangle, sourceProductType, sourceTiles, dataBuffers);

            final TileIndex srcIndex = new TileIndex(sourceTiles[0]);
            final double nodatavalue = bandList.srcBands[0].getNoDataValue();

            for (int y = y0; y < maxY; ++y) {
                trgIndex.calculateStride(y);
                srcIndex.calculateStride(y);
                for (int x = x0; x < maxX; ++x) {
                    boolean isNoData = isNoData(dataBuffers, srcIndex.getIndex(x), nodatavalue);
                    if (isNoData) {
                        for (final Band band : bandList.targetBands) {
                            targetTiles.get(band).getDataBuffer().setElemFloatAt(trgIndex.getIndex(x), (float) nodatavalue);
                        }
                        continue;
                    }

                    getComplexScatterMatrix(srcIndex.getIndex(x), dataBuffers, Sr, Si);

                    final CDD data = getKrogagerDecomposition(Sr, Si);

                    final int idx = trgIndex.getIndex(x);
                    for (final Band band : bandList.targetBands) {
                        final String targetBandName = band.getName();
                        final ProductData dataBuffer = targetTiles.get(band).getDataBuffer();

                        if (targetBandName.equals(I_HH))
                            dataBuffer.setElemFloatAt(idx, (float) data.iHH);
                        else if (targetBandName.equals(Q_HH))
                            dataBuffer.setElemFloatAt(idx, (float) data.qHH);
                        else if (targetBandName.equals(I_HV))
                            dataBuffer.setElemFloatAt(idx, (float) data.iHV);
                        else if (targetBandName.equals(Q_HV))
                            dataBuffer.setElemFloatAt(idx, (float) data.qHV);
                        else if (targetBandName.equals(I_VH))
                            dataBuffer.setElemFloatAt(idx, (float) data.iVH);
                        else if (targetBandName.equals(Q_VH))
                            dataBuffer.setElemFloatAt(idx, (float) data.qVH);
                        else if (targetBandName.equals(I_VV))
                            dataBuffer.setElemFloatAt(idx, (float) data.iVV);
                        else if (targetBandName.equals(Q_VV))
                            dataBuffer.setElemFloatAt(idx, (float) data.qVV);
                        else if (targetBandName.equals(SPAN))
                            dataBuffer.setElemFloatAt(idx, (float) data.span);
                        else if (targetBandName.equals(TAU))
                            dataBuffer.setElemFloatAt(idx, (float) data.tau);
                    }
                }
            }
        }
    }

    public static CDD getKrogagerDecomposition(final double[][] Sr, final double[][] Si) {

        final double sHHr = Sr[0][0];
        final double sHHi = Si[0][0];
        final double sHVr = 0.5 * (Sr[0][1] + Sr[1][0]);
        final double sHVi = 0.5 * (Si[0][1] + Si[1][0]);
        final double sVVr = Sr[1][1];
        final double sVVi = Si[1][1];

        final double sqrt2 = Math.sqrt(2.0);
        final double alphaR = (sHHr + sVVr) / sqrt2;
        final double alphaI = (sHHi + sVVi) / sqrt2;
        final double betaR  = (sHHr - sVVr) / sqrt2;
        final double betaI  = (sHHi - sVVi) / sqrt2;
        final double gammaR = sHVr * sqrt2;
        final double gammaI = sHVi * sqrt2;

        final double span = Math.sqrt(sHHr*sHHr + sHHi*sHHi + 2.0*sHVr*sHVr + 2.0*sHVi*sHVi + sVVr*sVVr + sVVi*sVVi);

        final double tmp1 = 2.0 * (betaR*gammaR + betaI*gammaI);
        final double tmp2 = betaR*betaR + betaI*betaI - gammaR*gammaR - gammaI*gammaI;
        final double tmp3 = Math.sqrt(tmp1*tmp1 + tmp2*tmp2);
        final double sinKai = tmp1 / tmp3;
        final double cosKai = tmp2 / tmp3;
        final double tmp4r = cosKai*(sHHr - sVVr) + 2.0*sinKai*sHVr;
        final double tmp4i = cosKai*(sHHi - sVVi) + 2.0*sinKai*sHVi;

        final double iHH = alphaR/sqrt2 + 0.5*cosKai*tmp4r;
        final double qHH = alphaI/sqrt2 + 0.5*cosKai*tmp4i;
        final double iHV = 0.5*sinKai*tmp4r;
        final double qHV = 0.5*sinKai*tmp4i;
        final double iVV = alphaR/sqrt2 - 0.5*cosKai*tmp4r;
        final double qVV = alphaI/sqrt2 - 0.5*cosKai*tmp4i;

        final double sMaxNorm = Math.sqrt(iHH*iHH + qHH*qHH + 2.0*iHV*iHV + 2.0*qHV*qHV + iVV*iVV + qVV*qVV);
        final double ssMaxR = sHHr*iHH - sHHi*qHH + 2.0*(sHVr*iHV - sHVi*qHV) + sVVr*iVV - sVVi*qVV;
        final double ssMaxI = sHHr*qHH + sHHi*iHH + 2.0*(sHVr*qHV + sHVi*iHV) + sVVr*qVV + sVVi*iVV;
        final double tau = Math.acos(Math.sqrt(ssMaxR * ssMaxR + ssMaxI * ssMaxI) / (span * sMaxNorm));

        return new CDD(iHH, qHH, iHV, qHV, iVV, qVV, span, tau);
    }

    public static class CDD {
        public final double iHH;
        public final double qHH;
        public final double iHV;
        public final double qHV;
        public final double iVH;
        public final double qVH;
        public final double iVV;
        public final double qVV;
        public final double span;
        public final double tau;

        public CDD(final double iHH, final double qHH, final double iHV, final double qHV, final double iVV,
                   final double qVV, final double span, final double tau) {
            this.iHH = iHH;
            this.qHH = qHH;
            this.iHV = iHV;
            this.qHV = qHV;
            this.iVH = iHV;
            this.qVH = qHV;
            this.iVV = iVV;
            this.qVV = qVV;
            this.span = span;
            this.tau = tau;
        }
    }
}
