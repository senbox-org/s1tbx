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
package org.csa.rstb.gpf.classifiers;

import org.csa.rstb.gpf.PolOpUtils;
import org.csa.rstb.gpf.PolarimetricClassificationOp;
import org.csa.rstb.gpf.decompositions.hAAlpha;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.util.SystemUtils;
import org.esa.nest.gpf.PolBandUtils;
import org.esa.nest.gpf.TileIndex;

import java.awt.*;
import java.util.Map;

/**
    Implements CloudePottier Classifier
 */
public class CloudePottier extends PolClassifierBase implements PolClassifier {

    /* H, A and alpha decision boundaries */
    static final double Alpha1 = 55.0;
    static final double Alpha2 = 50.0;
    static final double Alpha3 = 48.0;
    static final double Alpha4 = 42.0;
    static final double Alpha5 = 40.0;
    static final double H1 = 0.9;
    static final double H2 = 0.5;
    static final double A1 = 0.5;            //todo A1 is never used?

    private static final String H_ALPHA_CLASS = "H_alpha_class";
    private final boolean useLeeHAlphaPlaneDefinition;

    public CloudePottier(final PolBandUtils.MATRIX sourceProductType,
                         final int sourceWidth, final int sourceHeight, final int winSize,
                         final Map<Band, PolBandUtils.QuadSourceBand> srcbandMap) {
        super(sourceProductType, sourceWidth, sourceHeight, winSize, srcbandMap);

        useLeeHAlphaPlaneDefinition = Boolean.getBoolean(SystemUtils.getApplicationContextId()+
                                                             ".useLeeHAlphaPlaneDefinition");
    }

    /**
        Return the band name for the target product
        @return band name
     */
    public String getTargetBandName() {
        return H_ALPHA_CLASS;
    }

    /**
     * returns the number of classes
     * @return num classes
     */
    public int getNumClasses() {
        return 9;
    }

    /**
     * Perform decomposition for given tile.
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param op the polarimetric decomposition operator
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the filtered value.
     */
    public void computeTile(final Band targetBand, final Tile targetTile, final PolarimetricClassificationOp op) {
        final Rectangle targetRectangle = targetTile.getRectangle();
        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w  = targetRectangle.width;
        final int h  = targetRectangle.height;
        final int maxY = y0 + h;
        final int maxX = x0 + w;
        final ProductData targetData = targetTile.getDataBuffer();
        final TileIndex trgIndex = new TileIndex(targetTile);
        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        final PolBandUtils.QuadSourceBand srcBandList = bandMap.get(targetBand);

        final Rectangle sourceRectangle = getSourceRectangle(x0, y0, w, h);
        final Tile[] sourceTiles = new Tile[srcBandList.srcBands.length];
        final ProductData[] dataBuffers = new ProductData[srcBandList.srcBands.length];
        for (int i = 0; i < srcBandList.srcBands.length; ++i) {
            sourceTiles[i] = op.getSourceTile(srcBandList.srcBands[i], sourceRectangle);
            dataBuffers[i] = sourceTiles[i].getDataBuffer();
        }
        final TileIndex srcIndex = new TileIndex(sourceTiles[0]);
        
        final double[][] Tr = new double[3][3];
        final double[][] Ti = new double[3][3];
        final int noDataValue = 0;
        for(int y = y0; y < maxY; ++y) {
            trgIndex.calculateStride(y);
            for(int x = x0; x < maxX; ++x) {

                PolOpUtils.getMeanCoherencyMatrix(x, y, halfWindowSize, srcWidth, srcHeight,
                                                  sourceProductType, srcIndex, dataBuffers, Tr, Ti);

                final hAAlpha.HAAlpha data = hAAlpha.computeHAAlpha(Tr, Ti);

                if (!Double.isNaN(data.entropy) && !Double.isNaN(data.anisotropy) && !Double.isNaN(data.alpha)) {
                    targetData.setElemIntAt(trgIndex.getIndex(x),
                            getZoneIndex(data.entropy, data.alpha, useLeeHAlphaPlaneDefinition));
                } else {
                    targetData.setElemIntAt(trgIndex.getIndex(x), noDataValue);
                }
            }
        }
    }

    /**
     * Compute zone index (1 to 9) for given entropy and alpha
     * @param entropy The entropy
     * @param alpha The alpha
     * @param useLeeHAlphaPlaneDefinition Use Lee's H-Alpha plane definition if true, otherwise use PolSARPro definition
     * @return The zone index
     */
    static int getZoneIndex(final double entropy, final double alpha, final boolean useLeeHAlphaPlaneDefinition) {

        if (useLeeHAlphaPlaneDefinition) {

            if (entropy > H1 && alpha > Alpha1) { // zone 1
                return 1;
            } else if (entropy > H1 && alpha <= Alpha1 && alpha > Alpha5) {  // zone 2
                return 2;
            } else if (entropy > H1 && alpha <= Alpha5) {  // zone 3
                return 3;
            } else if (entropy <= H1 && entropy > H2 && alpha > Alpha2) {  // zone 4
                return 4;
            } else if (entropy <= H1 && entropy > H2 && alpha <= Alpha2 && alpha > Alpha5) {  // zone 5
                return 5;
            } else if (entropy <= H1 && entropy > H2 && alpha <= Alpha5) {  // zone 6
                return 6;
            } else if (entropy <= H2 && alpha > Alpha3) {  // zone 7
                return 7;
            } else if (entropy <= H2 && alpha <= Alpha3 && alpha > Alpha4) {  // zone 8
                return 8;
            } else if (entropy <= H2 && alpha <= Alpha4) {  // zone 9
                return 9;
            }

        } else { // PolSARPro definition

            if (entropy > H1 && alpha > Alpha1) { // zone 7
                return 7;
            } else if (entropy > H1 && alpha <= Alpha1 && alpha > Alpha5) {  // zone 8
                return 8;
            } else if (entropy > H1 && alpha <= Alpha5) {  // zone 9
                return 9;
            } else if (entropy <= H1 && entropy > H2 && alpha > Alpha2) {  // zone 4
                return 4;
            } else if (entropy <= H1 && entropy > H2 && alpha <= Alpha2 && alpha > Alpha5) {  // zone 5
                return 5;
            } else if (entropy <= H1 && entropy > H2 && alpha <= Alpha5) {  // zone 6
                return 6;
            } else if (entropy <= H2 && alpha > Alpha3) {  // zone 1
                return 1;
            } else if (entropy <= H2 && alpha <= Alpha3 && alpha > Alpha4) {  // zone 2
                return 2;
            } else if (entropy <= H2 && alpha <= Alpha4) {  // zone 3
                return 3;
            }
        }
        return 0;
    }

}
