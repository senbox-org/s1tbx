/*
 * Copyright (C) 2015 by SkyWatch Space Applications Inc.
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

import org.apache.commons.math3.util.FastMath;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Perform Huynen decomposition for given tile.
 */
public class Huynen extends DecompositionBase implements Decomposition, QuadPolProcessor {

    private final static String TWO_A0 = "2A0_b";
    private final static String B0_PLUS_B = "B0_plus_B_r";
    private final static String B0_MINUS_B = "B0_minus_B_g";


    public Huynen(final PolBandUtils.PolSourceBand[] srcBandList, final MATRIX sourceProductType,
                 final int windowSize, final int srcImageWidth, final int srcImageHeight) {
        super(srcBandList, sourceProductType, windowSize, windowSize, srcImageWidth, srcImageHeight);
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
        targetBandNameList.add(TWO_A0);
        targetBandNameList.add(B0_PLUS_B);
        targetBandNameList.add(B0_MINUS_B);
        return targetBandNameList.toArray(new String[targetBandNameList.size()]);
    }

    /**
     * Sets the unit for the new target band
     *
     * @param targetBandName the band name
     * @param targetBand     the new target band
     */
    public void setBandUnit(final String targetBandName, final Band targetBand) {
        targetBand.setUnit(Unit.INTENSITY);
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

        for (final PolBandUtils.PolSourceBand bandList : srcBandList) {

            final TargetInfo[] targetInfo = new TargetInfo[bandList.targetBands.length];
            int j = 0;
            for (Band targetBand : bandList.targetBands) {
                final String targetBandName = targetBand.getName();
                if (targetBandName.contains(B0_PLUS_B)) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.R);
                } else if (targetBandName.contains(B0_MINUS_B)) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.G);
                } else if (targetBandName.contains(TWO_A0)) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.B);
                }
                ++j;
            }
            final TileIndex trgIndex = new TileIndex(targetInfo[0].tile);

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

            double two_A0, B0_plus_B, B0_minus_B;
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

                    getCoherencyMatrixT3(srcIndex.getIndex(x), sourceProductType, dataBuffers, Tr, Ti);

                    final HDD data = getHuynenDecomposition(Tr, Ti);

                    two_A0 = scaleDb(data.two_A0, bandList.spanMin, bandList.spanMax);
                    B0_plus_B = scaleDb(data.B0_plus_B, bandList.spanMin, bandList.spanMax);
                    B0_minus_B = scaleDb(data.B0_minus_B, bandList.spanMin, bandList.spanMax);

                    final int idx = trgIndex.getIndex(x);
                    for (TargetInfo target : targetInfo) {
                        if (target.colour == TargetBandColour.R) {
                            target.dataBuffer.setElemFloatAt(idx, (float) B0_plus_B);
                        } else if (target.colour == TargetBandColour.G) {
                            target.dataBuffer.setElemFloatAt(idx, (float) B0_minus_B);
                        } else if (target.colour == TargetBandColour.B) {
                            target.dataBuffer.setElemFloatAt(idx, (float) two_A0);
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
        final double B0 = (C*C + D*D + G*G + H*H) / (4.0*A0);
        final double B = (C*C + D*D - G*G - H*H) / (4.0*A0);

        return new HDD(A0, B0, B);
    }

    public static class HDD {
        public final double two_A0;
        public final double B0_plus_B;
        public final double B0_minus_B;

        public HDD(final double A0, final double B0, final double B) {
            this.two_A0 = 2.0 * A0;
            this.B0_plus_B = B0 + B;
            this.B0_minus_B = B0 - B;
        }
    }
}
