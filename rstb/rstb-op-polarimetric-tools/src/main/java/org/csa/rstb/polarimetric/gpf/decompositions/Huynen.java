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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Perform Huynen decomposition for given tile.
 */
public class Huynen extends DecompositionBase implements Decomposition, QuadPolProcessor {

    private final boolean outputHuynenParamSet0;
    private final boolean outputHuynenParamSet1;

    private final static String TWO_A0 = "2A0_b";
    private final static String B0_PLUS_B = "B0_plus_B_r";
    private final static String B0_MINUS_B = "B0_minus_B_g";
    private final static String T11 = "T11";
    private final static String T12_REAL = "T12_real";
    private final static String T12_IMAG = "T12_image";
    private final static String T13_REAL = "T13_real";
    private final static String T13_IMAG = "T13_imag";
    private final static String T22 = "T22";
    private final static String T23_REAL = "T23_real";
    private final static String T23_IMAG = "T23_imag";
    private final static String T33 = "T33";


    public Huynen(final PolBandUtils.PolSourceBand[] srcBandList, final MATRIX sourceProductType,
                  final int windowSize, final int srcImageWidth, final int srcImageHeight,
                  final boolean outputHuynenParamSet0,
                  final boolean outputHuynenParamSet1) {
        super(srcBandList, sourceProductType, windowSize, windowSize, srcImageWidth, srcImageHeight);

        this.outputHuynenParamSet0 = outputHuynenParamSet0;
        this.outputHuynenParamSet1 = outputHuynenParamSet1;
    }

    public String getSuffix() {
        return "_Huynen";
    }

    /**
     * Return the list of band names for the target product
     *
     * @return list of band names
     */
    public String[] getTargetBandNames() {
        final List<String> targetBandNameList = new ArrayList<>(3);

        if (!outputHuynenParamSet0 && !outputHuynenParamSet1) {
            throw new OperatorException("Please select decomposition parameters to output");
        }

        if (outputHuynenParamSet0) {
            targetBandNameList.add(B0_PLUS_B);
            targetBandNameList.add(B0_MINUS_B);
            targetBandNameList.add(TWO_A0);
        }
        if (outputHuynenParamSet1) {
            targetBandNameList.add(T11);
            targetBandNameList.add(T12_REAL);
            targetBandNameList.add(T12_IMAG);
            targetBandNameList.add(T13_REAL);
            targetBandNameList.add(T13_IMAG);
            targetBandNameList.add(T22);
            targetBandNameList.add(T23_REAL);
            targetBandNameList.add(T23_IMAG);
            targetBandNameList.add(T33);
        }

        return targetBandNameList.toArray(new String[targetBandNameList.size()]);
    }

    /**
     * Sets the unit for the new target band
     *
     * @param targetBandName the band name
     * @param targetBand     the new target band
     */
    public void setBandUnit(final String targetBandName, final Band targetBand) {
        if (targetBandName.equals(B0_PLUS_B) || targetBandName.equals(B0_MINUS_B) || targetBandName.equals(TWO_A0)) {
            targetBand.setUnit(Unit.INTENSITY_DB);
        } else if (targetBandName.equals(T11) || targetBandName.equals(T22) || targetBandName.equals(T33) ||
                targetBandName.equals(T12_REAL) || targetBandName.equals(T13_REAL) || targetBandName.equals(T23_REAL)) {
            targetBand.setUnit(Unit.REAL);
        } else if (targetBandName.equals(T12_IMAG) || targetBandName.equals(T13_IMAG) || targetBandName.equals(T23_IMAG)) {
            targetBand.setUnit(Unit.IMAGINARY);
        }
    }

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
    public void computeTile(final Map<Band, Tile> targetTiles, final Rectangle targetRectangle, final Operator op) {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int maxY = y0 + h;
        final int maxX = x0 + w;
        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        final TileIndex trgIndex = new TileIndex(targetTiles.get(op.getTargetProduct().getBandAt(0)));

        final double[][] Tr = new double[3][3];
        final double[][] Ti = new double[3][3];

        for (final PolBandUtils.PolSourceBand bandList : srcBandList) {

//            if (outputHuynenParamSet0 && !bandList.spanMinMaxSet) {
//                setSpanMinMax(op, bandList);
//            }

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

                    getMeanCoherencyMatrix(x, y, halfWindowSizeX, halfWindowSizeY, sourceImageWidth, sourceImageHeight,
                            sourceProductType, srcIndex, dataBuffers, Tr, Ti);

//                    getCoherencyMatrixT3(srcIndex.getIndex(x), sourceProductType, dataBuffers, Tr, Ti);

                    final HDD data = getHuynenDecomposition(Tr, Ti);

                    final int idx = trgIndex.getIndex(x);
                    for (final Band band : bandList.targetBands) {
                        final String targetBandName = band.getName();
                        final ProductData dataBuffer = targetTiles.get(band).getDataBuffer();

                        if (outputHuynenParamSet0) {
                            if (targetBandName.equals(TWO_A0))
                                dataBuffer.setElemFloatAt(idx,
                                        (float) (10.0 * Math.log10(data.T11)));
//                                        (float) scaleDb(data.two_A0, bandList.spanMin, bandList.spanMax));
                            else if (targetBandName.equals(B0_PLUS_B))
                                dataBuffer.setElemFloatAt(idx,
                                        (float) (10.0 * Math.log10(data.T22)));
//                                        (float) scaleDb(data.B0_plus_B, bandList.spanMin, bandList.spanMax));
                            else if (targetBandName.equals(B0_MINUS_B))
                                dataBuffer.setElemFloatAt(idx,
                                        (float) (10.0 * Math.log10(data.T33)));
//                                        (float) scaleDb(data.B0_minus_B, bandList.spanMin, bandList.spanMax));
                        }

                        if (outputHuynenParamSet1) {
                            if (targetBandName.equals(T11))
                                dataBuffer.setElemFloatAt(idx, (float) data.T11);
                            else if (targetBandName.equals(T12_REAL))
                                dataBuffer.setElemFloatAt(idx, (float) data.T12_real);
                            else if (targetBandName.equals(T12_IMAG))
                                dataBuffer.setElemFloatAt(idx, (float) data.T12_imag);
                            else if (targetBandName.equals(T13_REAL))
                                dataBuffer.setElemFloatAt(idx, (float) data.T13_real);
                            else if (targetBandName.equals(T13_IMAG))
                                dataBuffer.setElemFloatAt(idx, (float) data.T13_imag);
                            else if (targetBandName.equals(T22))
                                dataBuffer.setElemFloatAt(idx, (float) data.T22);
                            else if (targetBandName.equals(T23_REAL))
                                dataBuffer.setElemFloatAt(idx, (float) data.T23_real);
                            else if (targetBandName.equals(T23_IMAG))
                                dataBuffer.setElemFloatAt(idx, (float) data.T23_imag);
                            else if (targetBandName.equals(T33))
                                dataBuffer.setElemFloatAt(idx, (float) data.T33);
                        }
                    }
                }
            }
        }
    }

    public static HDD getHuynenDecomposition(final double[][] Tr, final double[][] Ti) {

        final double A0 = 0.5 * Tr[0][0];
        final double C = Tr[0][1];
        final double D = -Ti[0][1];
        final double H = Tr[0][2];
        final double G = Ti[0][2];
        final double B0 = (C * C + D * D + G * G + H * H) / (4.0 * A0);
        final double B = (C * C + D * D - G * G - H * H) / (4.0 * A0);
        final double E = (C * H - D * G) / (2.0 * A0);
        final double F = (C * G + D * H) / (2.0 * A0);

        return new HDD(A0, B0, B, C, D, E, F, G, H);
    }

    public static class HDD {
        public final double T11;
        public final double T12_real;
        public final double T12_imag;
        public final double T13_real;
        public final double T13_imag;
        public final double T22;
        public final double T23_real;
        public final double T23_imag;
        public final double T33;

        public HDD(final double A0, final double B0, final double B, final double C, final double D,
                   final double E, final double F, final double G, final double H) {
            this.T11 = 2.0 * A0;
            this.T12_real = C;
            this.T12_imag = -D;
            this.T13_real = H;
            this.T13_imag = G;
            this.T22 = B0 + B;
            this.T23_real = E;
            this.T23_imag = F;
            this.T33 = B0 - B;
        }
    }
}
