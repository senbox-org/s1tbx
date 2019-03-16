/*
 * Copyright (C) 2018 Skywatch Space Applications Inc. https://www.skywatch.co
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
package org.csa.rstb.polarimetric.gpf.decompositions_cp;

import org.csa.rstb.polarimetric.gpf.CompactPolProcessor;
import org.csa.rstb.polarimetric.gpf.StokesParameters;
import org.apache.commons.math3.util.FastMath;
import org.csa.rstb.polarimetric.gpf.decompositions.Decomposition;
import org.csa.rstb.polarimetric.gpf.decompositions.DecompositionBase;
import org.esa.s1tbx.commons.polsar.PolBandUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.*;
import java.util.Map;

/**
 * Perform M-Delta decomposition for given tile.
 */
public class CP_MDelta extends DecompositionBase implements Decomposition, CompactPolProcessor {

    private final String compactMode;
    private final boolean useRCMConvention;

    private static final String RED = "MDelta_r";
    private static final String GREEN = "MDelta_g";
    private static final String BLUE = "MDelta_b";

    public CP_MDelta(final PolBandUtils.PolSourceBand[] srcBandList, final PolBandUtils.MATRIX sourceProductType,
                     final String compactMode, final int windowSizeX, final int windowSizeY, final int srcImageWidth,
                     final int srcImageHeight) {

        super(srcBandList, sourceProductType, windowSizeX, windowSizeY, srcImageWidth, srcImageHeight);

        this.compactMode = compactMode;
        useRCMConvention = PolBandUtils.useRCMConvention();
    }

    public String getSuffix() {
        return "_MDelta";
    }

    /**
     * Return the list of band names for the target product
     *
     * @return list of band names
     */
    public String[] getTargetBandNames() {
        return new String[]{RED, GREEN, BLUE};
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

    /**
     * Perform decomposition for given tile.
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed.
     * @param op              the polarimetric decomposition operator
     * @throws org.esa.snap.core.gpf.OperatorException If an error occurs during computation of the filtered value.
     */
    public void computeTile(final Map<Band, Tile> targetTiles, final Rectangle targetRectangle, final Operator op) {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int maxY = y0 + h;
        final int maxX = x0 + w;

        final Rectangle sourceRectangle = getSourceRectangle(x0, y0, w, h);
        for (final PolBandUtils.PolSourceBand bandList : srcBandList) {

            final TargetInfo[] targetInfo = new TargetInfo[bandList.targetBands.length];
            int j = 0;
            for (Band targetBand : bandList.targetBands) {
                final String targetBandName = targetBand.getName();
                if (targetBandName.contains(RED)) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.R);
                } else if (targetBandName.contains(GREEN)) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.G);
                } else if (targetBandName.contains(BLUE)) {
                    targetInfo[j] = new TargetInfo(targetTiles.get(targetBand), TargetBandColour.B);
                }
                ++j;
            }
            final TileIndex trgIndex = new TileIndex(targetInfo[0].tile);

            final double[][] Cr = new double[2][2]; // real part of covariance matrix
            final double[][] Ci = new double[2][2]; // imaginary part of covariance matrix
            final double[] g = new double[4];       // Stokes vector

            final Tile[] sourceTiles = new Tile[bandList.srcBands.length];
            final ProductData[] dataBuffers = new ProductData[bandList.srcBands.length];
            for (int i = 0; i < bandList.srcBands.length; i++) {
                sourceTiles[i] = op.getSourceTile(bandList.srcBands[i], sourceRectangle);
                dataBuffers[i] = sourceTiles[i].getDataBuffer();
            }
            double v = 0.0;
            for (int y = y0; y < maxY; ++y) {
                trgIndex.calculateStride(y);
                for (int x = x0; x < maxX; ++x) {
                    final int index = trgIndex.getIndex(x);

                    getMeanCovarianceMatrixC2(x, y, halfWindowSizeX, halfWindowSizeY, sourceImageWidth,
                            sourceImageHeight, sourceProductType, sourceTiles, dataBuffers, Cr, Ci);

                    StokesParameters.computeCompactPolStokesVector(Cr, Ci, g);

                    StokesParameters sp = StokesParameters.computeStokesParameters(g, compactMode, useRCMConvention);

                    for (TargetInfo target : targetInfo) {

                        if (target.colour == TargetBandColour.R) {
                            v = sp.DegreeOfPolarization * g[0] * (1 + FastMath.sin(sp.RelativePhase)) / 2.0;
                        } else if (target.colour == TargetBandColour.G) {
                            v = sp.DegreeOfDepolarization * g[0];
                        } else if (target.colour == TargetBandColour.B) {
                            v = sp.DegreeOfPolarization * g[0] * (1 - FastMath.sin(sp.RelativePhase)) / 2.0;
                        }

                        target.dataBuffer.setElemFloatAt(index, (float) v);
                    }
                }
            }
        }
    }
}