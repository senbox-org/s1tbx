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
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.*;
import java.util.Map;

/**
 * Perform Krogager decomposition for given tile.
 */
public class Krogager extends DecompositionBase implements Decomposition, QuadPolProcessor {

    private final static String K_S = "ks_b";
    private final static String K_D = "kd_r";
    private final static String K_H = "kh_g";


    public Krogager(final PolBandUtils.PolSourceBand[] srcBandList, final MATRIX sourceProductType,
                    final int windowSize, final int srcImageWidth, final int srcImageHeight) {
        super(srcBandList, sourceProductType, windowSize, windowSize, srcImageWidth, srcImageHeight);
    }

    public String getSuffix() {
        return "_Krogager";
    }

    /**
     * Return the list of band names for the target product
     *
     * @return list of band names
     */
    public String[] getTargetBandNames() {
        return new String[]{K_D, K_H, K_S};

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
                if (targetBandName.contains(K_D)) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.R);
                } else if (targetBandName.contains(K_H)) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.G);
                } else if (targetBandName.contains(K_S)) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.B);
                }
                ++j;
            }
            final TileIndex trgIndex = new TileIndex(targetInfo[0].tile);

            final double[][] Tr = new double[3][3];
            final double[][] Ti = new double[3][3];

//            if (!bandList.spanMinMaxSet) {
//                setSpanMinMax(op, bandList);
//            }

            final Tile[] sourceTiles = new Tile[bandList.srcBands.length];
            final ProductData[] dataBuffers = new ProductData[bandList.srcBands.length];
            final Rectangle sourceRectangle = getSourceRectangle(x0, y0, w, h);
            getQuadPolDataBuffer(op, bandList.srcBands, sourceRectangle, sourceProductType, sourceTiles, dataBuffers);

            final TileIndex srcIndex = new TileIndex(sourceTiles[0]);
            final double nodatavalue = bandList.srcBands[0].getNoDataValue();

            double kd, kh, ks;
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

                    final KDD data = getKrogagerDecomposition(Tr, Ti);

//                    kd = 10.0 * Math.log10(data.kd);
//                    kh = 10.0 * Math.log10(data.kh);
//                    ks = 10.0 * Math.log10(data.ks);

                    kd = data.kd;
                    kh = data.kh;
                    ks = data.ks;

//                    kd = scaleDb(data.kd, bandList.spanMin, bandList.spanMax);
//                    kh = scaleDb(data.kh, bandList.spanMin, bandList.spanMax);
//                    ks = scaleDb(data.ks, bandList.spanMin, bandList.spanMax);

                    final int idx = trgIndex.getIndex(x);
                    for (TargetInfo target : targetInfo) {
                        if (target.colour == TargetBandColour.R) {
                            target.dataBuffer.setElemFloatAt(idx, (float) kd);
                        } else if (target.colour == TargetBandColour.G) {
                            target.dataBuffer.setElemFloatAt(idx, (float) kh);
                        } else if (target.colour == TargetBandColour.B) {
                            target.dataBuffer.setElemFloatAt(idx, (float) ks);
                        }
                    }
                }
            }
        }
    }

    public static KDD getKrogagerDecomposition(final double[][] Tr, final double[][] Ti) {

        final double A0 = 0.5 * Tr[0][0];
        final double B0 = 0.5 * (Tr[1][1] + Tr[2][2]);
        final double F = Ti[1][2];

        return new KDD(A0, B0, F);
    }

    public static class KDD {
        public final double kd;
        public final double kh;
        public final double ks;

        public KDD(final double A0, final double B0, final double F) {
            this.ks = FastMath.sqrt(A0);
            this.kd = FastMath.sqrt(B0 - FastMath.abs(F));
            this.kh = FastMath.sqrt(B0 + FastMath.abs(F)) - FastMath.sqrt(B0 - FastMath.abs(F));
        }
    }
}
