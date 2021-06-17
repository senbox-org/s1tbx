/*
 * Copyright (C) 2020 by Microwave Remote Sensing Lab, IITBombay http://www.mrslab.in
 *
 * Authored by: Subhadip Dey
 * Email: sdey2307@gmail.com
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

/*
    Reference:
    Dey, S., Bhattacharya, A., Frery, A.C., López-Martínez, C. and Rao, Y.S.,
    2021. A Model-free Four Component Scattering Power Decomposition for 
    Polarimetric SAR Data. IEEE Journal of Selected Topics in Applied Earth 
    Observations and Remote Sensing, 14, pp.3887-3902.
*/

package org.csa.rstb.polarimetric.gpf.decompositions;

import org.csa.rstb.polarimetric.gpf.decompositions.EigenDecomposition;
import org.csa.rstb.polarimetric.gpf.support.QuadPolProcessor;
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
 * Perform MF4CF decomposition for given tile.
 */
public class MF4CF extends DecompositionBase implements Decomposition, QuadPolProcessor {

    public MF4CF(final PolBandUtils.PolSourceBand[] srcBandList, final MATRIX sourceProductType,
                  final int windowSize, final int srcImageWidth, final int srcImageHeight) {
        super(srcBandList, sourceProductType, windowSize, windowSize, srcImageWidth, srcImageHeight);
    }

    public String getSuffix() {
        return "_MF4CF";
    }

    /**
     * Return the list of band names for the target product
     */
    public String[] getTargetBandNames() {
        return new String[]{"MF4CF_even_r", "MF4CF_diffused_g", "MF4CF_odd_b", "MF4CF_helix_m", "MF4CF_theta_FP", "MF4CF_tau_FP"};
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
                if (targetBandName.contains("MF4CF_even_r")) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.R);
                } else if (targetBandName.contains("MF4CF_diffused_g")) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.G);
                } else if (targetBandName.contains("MF4CF_odd_b")) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.B);
                } else if (targetBandName.contains("MF4CF_helix_m")) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.M);
                } else if (targetBandName.contains("MF4CF_theta_FP")) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.A);
                } else if (targetBandName.contains("MF4CF_tau_FP")) {
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
            getQuadPolDataBuffer(op, bandList.srcBands, sourceRectangle, sourceProductType, sourceTiles, dataBuffers);

            final TileIndex srcIndex = new TileIndex(sourceTiles[0]);
            final double nodatavalue = bandList.srcBands[0].getNoDataValue();

            double T11, T22, T33, det_T3, trace_T3, Ps, Pd, Pv, Pc, theta_deg, tau_deg;

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
                        c3ToT3(Cr, Ci, Tr, Ti);

                    } else if (sourceProductType == MATRIX.T3) {

                        getMeanCoherencyMatrix(x, y, halfWindowSizeX, halfWindowSizeY,
                                sourceImageWidth, sourceImageHeight, sourceProductType, srcIndex, dataBuffers, Tr, Ti);
                    }

                    final VDD data = getMF4CFDecomposition(Tr, Ti);

                    //Ps = scaleDb(data.ps, bandList.spanMin, bandList.spanMax);
                    //Pd = scaleDb(data.pd, bandList.spanMin, bandList.spanMax);
                    //Pv = scaleDb(data.pv, bandList.spanMin, bandList.spanMax);
                    Ps = data.ps;
                    Pd = data.pd;
                    Pv = data.pv;
                    Pc = data.pc;
                    theta_deg = data.theta_fp;
                    tau_deg = data.tau_fp;

                    // save Pd as red, Pv as green and Ps as blue
                    for (TargetInfo target : targetInfo) {

                        if (target.colour == TargetBandColour.R) {
                            target.dataBuffer.setElemFloatAt(trgIndex.getIndex(x), (float) Pd);
                        } else if (target.colour == TargetBandColour.G) {
                            target.dataBuffer.setElemFloatAt(trgIndex.getIndex(x), (float) Pv);
                        } else if (target.colour == TargetBandColour.B) {
                            target.dataBuffer.setElemFloatAt(trgIndex.getIndex(x), (float) Ps);
                        } else if (target.colour == TargetBandColour.M) {
                            target.dataBuffer.setElemFloatAt(trgIndex.getIndex(x), (float) Pc);
                        } else if (target.colour == TargetBandColour.A) {
                            target.dataBuffer.setElemFloatAt(trgIndex.getIndex(x), (float) theta_deg);
                        } else {
                            target.dataBuffer.setElemFloatAt(trgIndex.getIndex(x), (float) tau_deg);
                        }
                    }
                }
            }
        }
    }

    public static VDD getMF4CFDecomposition(final double[][] Tr, final double[][] Ti) {

        double T11, T22, T33, det_T3, trace_T3, Ps, Pd, Pv, Pc, theta_deg, tau_deg; 

        T11 = Tr[0][0];
        T22 = Tr[1][1];
        T33 = Tr[2][2];
        final double T23i = Ti[1][2];

        final double[][] EigenVectRe = new double[3][3];
        final double[][] EigenVectIm = new double[3][3];
        final double[] EigenVal = new double[3];
        EigenDecomposition.eigenDecomposition(3, Tr, Ti, EigenVectRe, EigenVectIm, EigenVal);

        trace_T3 = EigenVal[0] + EigenVal[1] + EigenVal[2];
        det_T3 = EigenVal[0] * EigenVal[1] * EigenVal[2];

        final double m1 = Math.sqrt(1-(27*(det_T3/(Math.pow(trace_T3,3))))); // 3D Barakat degree of polarization
        final double h = (T11 - T22 - T33);
        final double g = (T22 + T33);
        final double val = (m1 * trace_T3 * h) / (T11 * g + Math.pow(m1,2) * Math.pow(trace_T3,2));
        final double theta_rad = Math.atan(val);

        theta_deg = Math.atan(val)*180/Math.PI;

        final double k11_f = (T11 + T22 + T33)/2;
        final double k44_f = (-T11 + T22 + T33)/2;
        final double k14_f = T23i;
        final double val2 = Math.abs(k14_f)/(k11_f);
        final double tau_rad = Math.atan(val2);

        tau_deg = Math.atan(val2)*180/Math.PI;

        Pc = m1 * trace_T3 * (Math.sin(2*tau_rad));
        Pv = trace_T3 * (1 - m1);
        final double res_pow = trace_T3 - (Pc + Pv);
        Ps = (res_pow/2) * (1 + Math.sin(2*theta_rad));
        Pd = (res_pow/2) * (1 - Math.sin(2*theta_rad));

        return new VDD(Pv, Pd, Ps, Pc, theta_deg, tau_deg);
    }

    public static class VDD {
        public final double pv;
        public final double pd;
        public final double ps;
        public final double pc;
        public final double theta_fp;
        public final double tau_fp;

        public VDD(final double pv, final double pd, final double ps, final double pc, final double theta_fp, final double tau_fp) {
            this.pd = pd;
            this.ps = ps;
            this.pv = pv;
            this.pc = pc;
            this.theta_fp = theta_fp;
            this.tau_fp = tau_fp;
        }
    }
}
