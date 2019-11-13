/*
 * Copyright (C) 2019 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.csa.rstb.soilmoisture.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.csa.rstb.soilmoisture.gpf.support.IEMInverBase;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;

import java.awt.*;
import java.io.File;
import java.util.Map;


/**
 * There is a strong correlation between soil dielectric and radar backscatter. By inverting the Integral
 * Equation Model (IEM), the real dielectric constant (RDC) can be obtained from the radar backscatter
 * coefficient.
 * There are three approaches:
 * (1) Hybrid
 * (2) Multi-polarization
 * (3) Multi-angle
 * <p>
 * This class uses the Hybrid approach utilizing sigma values in both HH and VV polarizations from
 * AM and PM images.
 */

@OperatorMetadata(alias = "IEM-Hybrid-Inversion",
        category = "Radar/Soil Moisture",
        description = "Performs IEM inversion using Hybrid approach")
public class IEMHybridInverOp extends IEMInverBase {

    // There is an AM source image and a PM source image.
    // The bands that are applicable are: sigmaHH, sigmaVV, theta. sigma is the backscatter (in dB). theta is
    // Local Incidence Angle (LIA) (in degrees).
    private static final int NUM_SOURCE_IMAGES = 2;
    // The three LUT parameters are: rms, cl and RDC.
    private static final int NUM_LUT_PARAMS = 3;
    // pol[0], pol[1] are the polarizations of the AM sigmas
    // pol[2], pol[3] are the polarizations of the PM sigmas
    // 0 means HH, 1 means VV
    private final int[] pol = new int[]{0, 1, 0, 1};    // HH VV HH VV
    // There must be exactly two images: an AM image and a PM image.
    // The bands that are applicable are: sigmaHH, sigmaVV, theta. sigma is the backscatter (in dB). theta is
    // Local Incidence Angle (LIA) (in degrees).
    // Some bands in a source product may not be used.
    // All images MUST be in one source product.
    @SourceProduct
    Product sourceProduct;
    // There is just one target product containing multiple bands one of which is the Real Dielectric Constant (RDC)
    // (unit for dielectric constant is Farad/m).
    // Other bands are rms and cl.
    // rms is RMS roughness height (in cm). cl is Roughness correlation length (in cm).
    @TargetProduct
    Product targetProduct;
    // The LUT has 5 columns: rms, cl, RDC, sigmaHH, sigmaVV.
    @Parameter
    private File lutFile;
    @Parameter(description = "Optional rms in output", defaultValue = "false", label = "Output rms")
    private Boolean outputRMS = false;
    @Parameter(description = "Optional cl in output", defaultValue = "false", label = "Output correlation length")
    private Boolean outputCL = false;
    @Parameter(description = "RDC deviation threshold", defaultValue = "0.5", label = "RDC threshold")
    private Double thresholdRDC = 0.5;
    private Band clBand = null; // target

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link Product} annotated with the
     * {@link TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            initBaseParams(sourceProduct, lutFile, outputRMS);

            setNumberOfImages(NUM_SOURCE_IMAGES);

            rdcThreshold = thresholdRDC;
            super.initialize();

            targetProduct = getTargetProductFromBase();

            if (outputCL) {

                clBand = targetProduct.addBand("cl", ProductData.TYPE_FLOAT64);
                clBand.setUnit("cm");
                clBand.setNoDataValue(INVALID_OUTPUT_VALUE);
                clBand.setNoDataValueUsed(true);
            }

            getSourceBands();

            initLUT(NUM_LUT_PARAMS);

            buildMyKDTreeMap();

            updateMetadata("Hybrid IEM");

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Called by the framework in order to compute the stack of tiles for the given target bands.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     * <p>This method shall never be called directly.
     * </p>
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed (same for all rasters in <code>targetRasters</code>).
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException if an error occurs during computation of the target rasters.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {

        final int tx0 = targetRectangle.x;
        final int ty0 = targetRectangle.y;
        final int tw = targetRectangle.width;
        final int th = targetRectangle.height;

        //System.out.println("tx0 = " + tx0 + " ty0 = " + ty0 + " tw = " + tw + " th = " + th);

        // Source tile has larger dimensions then the target tile.
        final Rectangle sourceTileRectangle = getExtendedRectangle(tx0, ty0, tw, th);

        KDTreeNearestNeighbours1[][] nearestNeighbours =
                new KDTreeNearestNeighbours1[sourceTileRectangle.width][sourceTileRectangle.height];

        try {
            Tile[] sigmaHHTile = new Tile[NUM_SOURCE_IMAGES];
            Tile[] sigmaVVTile = new Tile[NUM_SOURCE_IMAGES];
            Tile[] thetaTile = new Tile[NUM_SOURCE_IMAGES];

            ProductData[] sigmaHHData = new ProductData[NUM_SOURCE_IMAGES];
            ProductData[] sigmaVVData = new ProductData[NUM_SOURCE_IMAGES];
            ProductData[] thetaData = new ProductData[NUM_SOURCE_IMAGES];

            for (int i = 0; i < NUM_SOURCE_IMAGES; i++) {

                sigmaHHTile[i] = getSourceTile(sigmaHHBand[i], sourceTileRectangle);
                if (sigmaHHTile[i] == null) {
                    throw new OperatorException("Failed to get source tile sigma HH");
                }
                sigmaHHData[i] = sigmaHHTile[i].getDataBuffer();

                sigmaVVTile[i] = getSourceTile(sigmaVVBand[i], sourceTileRectangle);
                if (sigmaVVTile[i] == null) {
                    throw new OperatorException("Failed to get source tile sigma VV");
                }
                sigmaVVData[i] = sigmaVVTile[i].getDataBuffer();

                thetaTile[i] = getSourceTile(thetaBand[i], sourceTileRectangle);
                if (thetaTile[i] == null) {
                    throw new OperatorException("Failed to get source tile theta");
                }
                thetaData[i] = thetaTile[i].getDataBuffer();
            }

            final Tile rmsTile = targetTiles.get(rmsBand);
            final Tile clTile = targetTiles.get(clBand);
            final Tile rdcTile = targetTiles.get(rdcBand);
            final Tile debugTile = (debugBand == null) ? null : targetTiles.get(debugBand);
            final Tile outlierTile = (outlierBand == null) ? null : targetTiles.get(outlierBand);
            final double[] sigma = new double[4];
            final double[] theta = new double[4];
            final double[][] result = new double[N][NUM_LUT_PARAMS];
            final double[][] resultSigma = new double[N][4];

            final int maxy = ty0 + th;
            final int maxx = tx0 + tw;

            // Process pixel by pixel in the tile
            for (int y = ty0; y < maxy; y++) { // loop through rows

                for (int x = tx0; x < maxx; x++) { // loop through columns

                    try {
                        final int index = rdcTile.getDataBufferIndex(x, y);

                        final double sigmaHH1 = sigmaHHData[0].getElemDoubleAt(sigmaHHTile[0].getDataBufferIndex(x, y));
                        final double sigmaVV1 = sigmaVVData[0].getElemDoubleAt(sigmaVVTile[0].getDataBufferIndex(x, y));
                        final double theta1 = thetaData[0].getElemDoubleAt(thetaTile[0].getDataBufferIndex(x, y));

                        final double sigmaHH2 = sigmaHHData[1].getElemDoubleAt(sigmaHHTile[1].getDataBufferIndex(x, y));
                        final double sigmaVV2 = sigmaVVData[1].getElemDoubleAt(sigmaVVTile[1].getDataBufferIndex(x, y));
                        final double theta2 = thetaData[1].getElemDoubleAt(thetaTile[1].getDataBufferIndex(x, y));

                        //System.out.println("sigmaHH1 = " + sigmaHH1 + " sigmaVV1 = " + sigmaVV1 + " theta1 = " + theta1);
                        //System.out.println("sigmaHH2 = " + sigmaHH2 + " sigmaVV2 = " + sigmaVV2 + " theta2 = " + theta2);

                        // result[.][0] is rms
                        // result[.][1] is cl
                        // result[.][2] is RDC
                        // resultSigma[.][0] is sigmaHH
                        // resultSigma[.][1] is sigmaVV
                        initResults(result, resultSigma);

                        // It is IMPORTANT that the values are not converted at this point because their
                        // validity is being checked against whatever the bands say is the "no data value".
                        try {
                            if (isValidSigmaHH(sigmaHH1, 0) && isValidSigmaVV(sigmaVV1, 0) && isValidTheta(theta1, 0) &&
                                    isValidSigmaHH(sigmaHH2, 1) && isValidSigmaVV(sigmaVV2, 1) && isValidTheta(theta2, 1)) {

                                sigma[0] = sigmaHHUnitIsDecibels[0] ? sigmaHH1 : toDecibels(sigmaHH1);
                                sigma[1] = sigmaVVUnitIsDecibels[0] ? sigmaVV1 : toDecibels(sigmaVV1);
                                sigma[2] = sigmaHHUnitIsDecibels[1] ? sigmaHH2 : toDecibels(sigmaHH2);
                                sigma[3] = sigmaVVUnitIsDecibels[1] ? sigmaVV2 : toDecibels(sigmaVV2);

                                theta[0] = theta1;
                                theta[1] = theta1;
                                theta[2] = theta2;
                                theta[3] = theta2;

                                try {
                                    searchLUTForN(sigma, theta, pol, result, resultSigma);
                                    nearestNeighbours[x - tx0][y - ty0] = setUpNearestNeighbours1(result, resultSigma);
                                } catch (Exception e) {
                                    SystemUtils.LOG.severe("wtf1" + e.getMessage());
                                }
                            }
                        } catch (Exception e) {
                            SystemUtils.LOG.severe("wtf2");
                        }

                        //System.out.println("results: " + result[0] + " " + result[1] + " " + result[2]);

                        if (N == 1) {
                            if (outputRMS) {
                                rmsTile.getDataBuffer().setElemDoubleAt(index, result[0][0]);
                            }

                            if (outputCL) {
                                clTile.getDataBuffer().setElemDoubleAt(index, result[0][1]);
                            }

                            try {
                                rdcTile.getDataBuffer().setElemDoubleAt(index, result[0][2]);
                            } catch (Exception e) {
                                SystemUtils.LOG.severe("wtf3");
                            }
                        }

                    } catch (Exception e) {
                        SystemUtils.LOG.severe("wtf4");
                    }
                }
            }

            if (N == 1) {
                return;
            }

            for (int y = ty0; y < maxy; y++) { // loop through rows

                for (int x = tx0; x < maxx; x++) { // loop through columns

                    final int index = rdcTile.getDataBufferIndex(x, y);
                    final int x0 = x - tx0;
                    final int y0 = y - ty0;
                    double rms = INVALID_OUTPUT_VALUE;
                    double rdc = INVALID_OUTPUT_VALUE;
                    double cl = INVALID_OUTPUT_VALUE;
                    int outlier = INVALID_OUTLIER_VALUE;

                    double[] outParmas = new double[4];
                    for (int i = 0; i < outParmas.length; i++) {
                        outParmas[i] = INVALID_OUTPUT_VALUE;
                    }

                    final KDTreeNearestNeighbours1 nn = nearestNeighbours[x0][y0];
                    if (nn != null) {

                        final int bestIdx = getBestKDTreeNeighbour(x0, y0, nearestNeighbours, outParmas);
                        if (bestIdx < 0) {
                            rms = outParmas[2];
                            cl = outParmas[3];
                            rdc = outParmas[1];
                            outlier = 2;
                        } else {
                            rms = nn.rms.get(bestIdx);
                            rdc = nn.rdc.get(bestIdx);
                            cl = nn.cl.get(bestIdx);
                            outlier = (bestIdx == 0) ? 0 : 1;
                        }

                        //printDebugMsg(x, y, rdc, bestIdx, sigma, nn);
                    }

                    if (outputRMS) {
                        rmsTile.getDataBuffer().setElemDoubleAt(index, rms);
                    }

                    if (outputCL) {
                        clTile.getDataBuffer().setElemDoubleAt(index, cl);
                    }

                    rdcTile.getDataBuffer().setElemDoubleAt(index, rdc);
                    if (outlierTile != null) {
                        outlierTile.getDataBuffer().setElemDoubleAt(index, outlier);
                    }
                    if (debugTile != null) {
                        debugTile.getDataBuffer().setElemDoubleAt(index, outParmas[0]);
                    }
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    protected void checkSourceBands() {

        for (int i = 0; i < NUM_SOURCE_IMAGES; i++) {

            if (sigmaHHBand[i] == null) {

                throw new OperatorException("No sigmaHHBand[" + i + ']');
            }

            if (sigmaVVBand[i] == null) {

                throw new OperatorException("No sigmaVVBand[" + i + ']');
            }

            if (thetaBand[i] == null) {

                throw new OperatorException("No thetaBand[" + i + ']');
            }
        }
    }

    private void buildMyKDTreeMap() {

        final double[] minThetas = new double[2];
        final double[] maxThetas = new double[2];

        getMinMaxIncidenceAngles(minThetas, maxThetas);

        //System.out.println("1st image: min theta = " + minThetas[0] + " max theta = " + maxThetas[0]);
        //System.out.println("2nd image: min theta = " + minThetas[1] + " max theta = " + maxThetas[1]);

        int minThetaInt1 = getAngleSectionIndex(minThetas[0]);
        int maxThetaInt1 = getAngleSectionIndex(maxThetas[0]);
        int minThetaInt2 = getAngleSectionIndex(minThetas[1]);
        int maxThetaInt2 = getAngleSectionIndex(maxThetas[1]);

        //System.out.println("minThetaInt1 = " + minThetaInt1 + " maxThetaInt = " + maxThetaInt1 + " minThetaInt2 = " + minThetaInt2 + " maxThetaInt2 = " + maxThetaInt2);

        if (minThetaInt1 < 0) {
            minThetaInt1 = 0;
        }

        if (maxThetaInt1 < 0) {
            maxThetaInt1 = NUM_ANGLE_SECTIONS - 1;
        }

        if (minThetaInt2 < 0) {
            minThetaInt2 = 0;
        }

        if (maxThetaInt2 < 0) {
            maxThetaInt2 = NUM_ANGLE_SECTIONS - 1;
        }

        final int[] sectionIdx1 = new int[maxThetaInt1 - minThetaInt1 + 1];
        final int[] sectionIdx2 = new int[maxThetaInt2 - minThetaInt2 + 1];

        for (int i = 0; i < sectionIdx1.length; i++) {

            sectionIdx1[i] = minThetaInt1 + i;
        }

        if (sectionIdx1[sectionIdx1.length - 1] != maxThetaInt1) {

            throw new OperatorException("Wrong theta sections 1 for kd tree ");
        }

        for (int i = 0; i < sectionIdx2.length; i++) {

            sectionIdx2[i] = minThetaInt2 + i;
        }

        if (sectionIdx2[sectionIdx2.length - 1] != maxThetaInt2) {

            throw new OperatorException("Wrong theta sections 2 for kd tree");
        }

        final KDTreeInfo[] infos = new KDTreeInfo[sectionIdx1.length * sectionIdx2.length];

        for (int i = 0; i < sectionIdx1.length; i++) { // loop through thetas from 1st image

            for (int j = 0; j < sectionIdx2.length; j++) { // loop through thetas from 2nd image

                final int[] sectionIdx = new int[4];

                // angle section from 1st image
                sectionIdx[0] = sectionIdx1[i];
                sectionIdx[1] = sectionIdx[0];

                // angle section from 2nd image
                sectionIdx[2] = sectionIdx2[j];
                sectionIdx[3] = sectionIdx[2];

                KDTreeInfo info = new KDTreeInfo(sectionIdx, pol);

                infos[i * sectionIdx2.length + j] = info;
            }
        }

        buildKDTreeMap(infos);
    }

    protected int convertToKDTreeMapIntKey(KDTreeInfo info) {
        final int[] sectionIdx = info.getSectionIdx();
        return sectionIdx[0] * 1000 + sectionIdx[2];
    }

    /**
     * Set the LUT file.
     * This function is used for unit test only.
     *
     * @param lutFilePath The full path to the LUT file.
     */
    public void setLUTFile(final String lutFilePath) {
        lutFile = new File(lutFilePath);
    }

    /**
     * Set the flag that controls whether optional bands are included in the output.
     * This function is used for unit test only.
     *
     * @param flag True means output all bands.
     */
    public void setOptionalOutputs(final boolean flag) {
        outputRMS = flag;
        outputCL = flag;
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.core.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(Map, Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(IEMHybridInverOp.class);
        }
    }
}