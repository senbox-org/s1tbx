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
import org.apache.commons.math.util.FastMath;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;
import org.esa.nest.gpf.PolBandUtils;
import org.esa.nest.gpf.PolBandUtils.MATRIX;
import org.esa.nest.gpf.TileIndex;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Perform Touzi decomposition for given tile.
 */
public class Touzi extends DecompositionBase implements Decomposition {

    private final boolean outputTouziParamSet0;
    private final boolean outputTouziParamSet1;
    private final boolean outputTouziParamSet2;
    private final boolean outputTouziParamSet3;

    public Touzi(final PolBandUtils.QuadSourceBand[] srcBandList, final MATRIX sourceProductType,
                 final int windowSize, final int srcImageWidth, final int srcImageHeight,
                 final boolean outputTouziParamSet0,
                 final boolean outputTouziParamSet1,
                 final boolean outputTouziParamSet2,
                 final boolean outputTouziParamSet3) {
        super(srcBandList, sourceProductType, windowSize, srcImageWidth, srcImageHeight);

        this.outputTouziParamSet0 = outputTouziParamSet0;
        this.outputTouziParamSet1 = outputTouziParamSet1;
        this.outputTouziParamSet2 = outputTouziParamSet2;
        this.outputTouziParamSet3 = outputTouziParamSet3;
    }

    /**
        Return the list of band names for the target product
        @return list of band names
     */
    public String[] getTargetBandNames() {
        final List<String> targetBandNameList = new ArrayList<String>(4);

        if (!outputTouziParamSet0 && !outputTouziParamSet1 && !outputTouziParamSet2 && !outputTouziParamSet3) {
            throw new OperatorException("Please select decomposition parameters to output");
        }

        if (outputTouziParamSet0) {
            targetBandNameList.add("Psi");
            targetBandNameList.add("Tau");
            targetBandNameList.add("Alpha");
            targetBandNameList.add("Phi");
        }
        if (outputTouziParamSet1) {
            targetBandNameList.add("Psi1");
            targetBandNameList.add("Tau1");
            targetBandNameList.add("Alpha1");
            targetBandNameList.add("Phi1");
        }
        if (outputTouziParamSet2) {
            targetBandNameList.add("Psi2");
            targetBandNameList.add("Tau2");
            targetBandNameList.add("Alpha2");
            targetBandNameList.add("Phi2");
        }
        if (outputTouziParamSet3) {
            targetBandNameList.add("Psi3");
            targetBandNameList.add("Tau3");
            targetBandNameList.add("Alpha3");
            targetBandNameList.add("Phi3");
        }

        return targetBandNameList.toArray(new String[targetBandNameList.size()]);
    }

    /**
     * Sets the unit for the new target band
     * @param targetBandName the band name
     * @param targetBand the new target band
     */
    public void setBandUnit(final String targetBandName, final Band targetBand) {
        targetBand.setUnit("rad");
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
                                    final Operator op) {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w  = targetRectangle.width;
        final int h  = targetRectangle.height;
        final int maxY = y0 + h;
        final int maxX = x0 + w;
        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        final TileIndex trgIndex = new TileIndex(targetTiles.get(op.getTargetProduct().getBandAt(0)));

        final double[][] Tr = new double[3][3];
        final double[][] Ti = new double[3][3];
        final double[][] EigenVectRe = new double[3][3];
        final double[][] EigenVectIm = new double[3][3];
        final double[] EigenVal = new double[3];

        final double[] psi = new double[3];
        final double[] tau = new double[3];
        final double[] alpha = new double[3];
        final double[] phi = new double[3];
        final double[] vr = new double[3];
        final double[] vi = new double[3];
        double p1, p2, p3, psiMean, tauMean, alphaMean, phiMean;
        double phase, c, s, tmp1r, tmp1i, tmp2r, tmp2i;

        for(final PolBandUtils.QuadSourceBand bandList : srcBandList) {

            final Tile[] sourceTiles = new Tile[bandList.srcBands.length];
            final ProductData[] dataBuffers = new ProductData[bandList.srcBands.length];
            final Rectangle sourceRectangle = getSourceRectangle(x0, y0, w, h);
            for (int i = 0; i < bandList.srcBands.length; ++i) {
                sourceTiles[i] = op.getSourceTile(bandList.srcBands[i], sourceRectangle);
                dataBuffers[i] = sourceTiles[i].getDataBuffer();
            }
            final TileIndex srcIndex = new TileIndex(sourceTiles[0]);
            
            for(int y = y0; y < maxY; ++y) {
                trgIndex.calculateStride(y);
                for(int x = x0; x < maxX; ++x) {
                    final int idx = trgIndex.getIndex(x);

                    PolOpUtils.getMeanCoherencyMatrix(x, y, halfWindowSize, sourceImageWidth, sourceImageHeight,
                                                      sourceProductType, srcIndex, dataBuffers, Tr, Ti);

                    PolOpUtils.eigenDecomposition(3, Tr, Ti, EigenVectRe, EigenVectIm, EigenVal);

                    for (int k = 0; k < 3; ++k) {
                        for (int l = 0; l < 3; ++l) {
                            vr[l] = EigenVectRe[l][k];
                            vi[l] = EigenVectIm[l][k];
                        }

                        phase = Math.atan2(vi[0], vr[0] + PolOpUtils.EPS);
                        c = FastMath.cos(phase);
                        s = FastMath.sin(phase);
                        for (int l = 0; l < 3; ++l) {
                            tmp1r = vr[l];
                            tmp1i = vi[l];
                            vr[l] = tmp1r*c + tmp1i*s;
                            vi[l] = tmp1i*c - tmp1r*s;
                        }

                        psi[k] = 0.5*Math.atan2(vr[2], vr[1] + PolOpUtils.EPS);

                        tmp1r = vr[1];
                        tmp1i = vi[1];
                        tmp2r = vr[2];
                        tmp2i = vi[2];
                        c = FastMath.cos(2.0*psi[k]);
                        s = FastMath.sin(2.0*psi[k]);
                        vr[1] =  tmp1r*c + tmp2r*s;
                        vi[1] =  tmp1i*c + tmp2i*s;
                        vr[2] = -tmp1r*s + tmp2r*c;
                        vi[2] = -tmp1i*s + tmp2i*c;

                        tau[k] = 0.5*Math.atan2(-vi[2], vr[0] + PolOpUtils.EPS);

                        phi[k] = Math.atan2(vi[1], vr[1] + PolOpUtils.EPS);

                        alpha[k] = Math.atan(Math.sqrt((vr[1]*vr[1] + vi[1]*vi[1]) / (vr[0]*vr[0] + vi[2]*vi[2])));

                        if ((psi[k] < -Math.PI/4.0) || (psi[k] > Math.PI/4.0)) {
                            tau[k] = -tau[k];
                            phi[k] = -phi[k];
                        }

                    }

                    final double sum = EigenVal[0] + EigenVal[1] + EigenVal[2];
                    p1 = EigenVal[0] / sum;
                    p2 = EigenVal[1] / sum;
                    p3 = EigenVal[2] / sum;

                    psiMean = p1*psi[0] + p2*psi[1] + p3*psi[2];
                    tauMean = p1*tau[0] + p2*tau[1] + p3*tau[2];
                    alphaMean = p1*alpha[0] + p2*alpha[1] + p3*alpha[2];
                    phiMean = p1*phi[0] + p2*phi[1] + p3*phi[2];

                    for(final Band band : bandList.targetBands) {
                        final String targetBandName = band.getName();
                        final ProductData dataBuffer = targetTiles.get(band).getDataBuffer();
                        if (outputTouziParamSet0) {
                            if(targetBandName.equals("Psi") || targetBandName.contains("Psi_"))
                                dataBuffer.setElemFloatAt(idx, (float)psiMean);
                            else if(targetBandName.equals("Tau") || targetBandName.contains("Tau_"))
                                dataBuffer.setElemFloatAt(idx, (float)tauMean);
                            else if(targetBandName.equals("Alpha") || targetBandName.contains("Alpha_"))
                                dataBuffer.setElemFloatAt(idx, (float)alphaMean);
                            else if(targetBandName.equals("Phi") || targetBandName.contains("Phi_"))
                                dataBuffer.setElemFloatAt(idx, (float)phiMean);
                        }
                        if (outputTouziParamSet1) {
                            if(targetBandName.contains("Psi1"))
                                dataBuffer.setElemFloatAt(idx, (float)psi[0]);
                            else if(targetBandName.contains("Tau1"))
                                dataBuffer.setElemFloatAt(idx, (float)tau[0]);
                            else if(targetBandName.contains("Alpha1"))
                                dataBuffer.setElemFloatAt(idx, (float)alpha[0]);
                            else if(targetBandName.contains("Phi1"))
                                dataBuffer.setElemFloatAt(idx, (float)phi[0]);
                        }
                        if (outputTouziParamSet2) {
                            if(targetBandName.contains("Psi2"))
                                dataBuffer.setElemFloatAt(idx, (float)psi[1]);
                            else if(targetBandName.contains("Tau2"))
                                dataBuffer.setElemFloatAt(idx, (float)tau[1]);
                            else if(targetBandName.contains("Alpha2"))
                                dataBuffer.setElemFloatAt(idx, (float)alpha[1]);
                            else if(targetBandName.contains("Phi2"))
                                dataBuffer.setElemFloatAt(idx, (float)phi[1]);
                        }
                        if (outputTouziParamSet3) {
                            if(targetBandName.contains("Psi3"))
                                dataBuffer.setElemFloatAt(idx, (float)psi[2]);
                            else if(targetBandName.contains("Tau3"))
                                dataBuffer.setElemFloatAt(idx, (float)tau[2]);
                            else if(targetBandName.contains("Alpha3"))
                                dataBuffer.setElemFloatAt(idx, (float)alpha[2]);
                            else if(targetBandName.contains("Phi3"))
                                dataBuffer.setElemFloatAt(idx, (float)phi[2]);
                        }
                    }

                }
            }
        }
    }
}