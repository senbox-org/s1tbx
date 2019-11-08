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
import org.esa.snap.engine_utilities.gpf.OperatorUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
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
 * This class uses the Multi-polarization approach utilizing sigma values in both HH and VV polarizations from
 * either an AM or PM image.
 */

@OperatorMetadata(alias = "IEM-Multi-Pol-Inversion",
        category = "Radar/Soil Moisture",
        authors = "Cecilia Wong",
        description = "Performs IEM inversion using Multi-polarization approach")
public class IEMMultiPolInverOp extends IEMInverBase {

    // There is just one source image.
    // The bands that are applicable are: sigmaHH, sigmaVV, theta. sigma is the backscatter (in dB). theta is
    // Local Incidence Angle (LIA) (in degrees).
    private static final int NUM_SOURCE_IMAGES = 1;
    // The two LUT parameters are: rms and RDC.
    private static final int NUM_LUT_PARAMS = 2;
    // pol[0], pol[1] are the polarizations of the sigmas (both from one image, either AM or PM).
    // 0 means HH, 1 means VV
    private final int[] pol = new int[]{0, 1};    // HH VV
    // There must be exactly one image: an AM image or a PM image.
    // The bands that are applicable are: sigmaHH, sigmaVV, theta. sigma is the backscatter (in dB). theta is
    // Local Incidence Angle (LIA) (in degrees).
    // Some bands in a source product may not be used.
    @SourceProduct
    Product sourceProduct;
    // There is just one target product containing multiple bands one of which is the Real Dielectric Constant (RDC)
    // (unit for dielectric constant is Farad/m).
    // Other band is rms.
    // rms is RMS roughness height (in cm).
    @TargetProduct
    Product targetProduct;
    // The LUT has 4 columns: rms, RDC, sigmaHH, sigmaVV.
    @Parameter
    private File lutFile;
    @Parameter(description = "Optional rms in output", defaultValue = "false", label = "Output rms")
    private Boolean outputRMS = false;
    @Parameter(description = "RDC deviation threshold", defaultValue = "0.5", label = "RDC threshold")
    private Double thresholdRDC = 0.5;

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

            getSourceBands();

            initLUT(NUM_LUT_PARAMS);

            buildMyKDTreeMap();

            updateMetadata("Multi-Pol IEM");

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

        // Source tile has larger dimensions than target tile
        final Rectangle sourceTileRectangle = getExtendedRectangle(tx0, ty0, tw, th);

        KDTreeNearestNeighbours[][] nearestNeighbours =
                new KDTreeNearestNeighbours[sourceTileRectangle.width][sourceTileRectangle.height];

        try {
            final Tile[] sigmaHHTile = new Tile[NUM_SOURCE_IMAGES];
            final Tile[] sigmaVVTile = new Tile[NUM_SOURCE_IMAGES];
            final Tile[] thetaTile = new Tile[NUM_SOURCE_IMAGES];

            final ProductData[] sigmaHHData = new ProductData[NUM_SOURCE_IMAGES];
            final ProductData[] sigmaVVData = new ProductData[NUM_SOURCE_IMAGES];
            final ProductData[] thetaData = new ProductData[NUM_SOURCE_IMAGES];

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
            final Tile rdcTile = targetTiles.get(rdcBand);
            final Tile debugTile = (debugBand == null) ? null : targetTiles.get(debugBand);
            final Tile outlierTile = (outlierBand == null) ? null : targetTiles.get(outlierBand);
            final double[] sigma = new double[2];
            final double[] theta = new double[2];
            final double[][] result = new double[N][NUM_LUT_PARAMS];
            final double[][] resultSigma = new double[N][2];

            final int maxy = ty0 + th;
            final int maxx = tx0 + tw;

            // Process pixel by pixel in the tile
            for (int y = ty0; y < maxy; y++) { // loop through rows

                for (int x = tx0; x < maxx; x++) { // loop through columns

                    final int index = rdcTile.getDataBufferIndex(x, y);
                    final int srcIndex = sigmaHHTile[0].getDataBufferIndex(x, y);

                    final double sigmaHH1 = sigmaHHData[0].getElemDoubleAt(srcIndex);
                    final double sigmaVV1 = sigmaVVData[0].getElemDoubleAt(srcIndex);
                    final double theta1 = thetaData[0].getElemDoubleAt(srcIndex);

                    //System.out.println("sigmaHH1 = " + sigmaHH1 + " sigmaVV1 = " + sigmaVV1 + " theta1 = " + theta1);
                    //System.out.println("sigmaHH2 = " + sigmaHH2 + " sigmaVV2 = " + sigmaVV2 + " theta2 = " + theta2);

                    // result[.][0] is rms
                    // result[.][1] is RDC
                    // resultSigma[.][0] is sigmaHH
                    // resultSigma[.][1] is sigmaVV
                    initResults(result, resultSigma);

                    // It is IMPORTANT that the values are not converted at this point because their
                    // validity is being checked against whatever the bands say is the "no data value".
                    if (isValidSigmaHH(sigmaHH1, 0) && isValidSigmaVV(sigmaVV1, 0) && isValidTheta(theta1, 0)) {

                        sigma[0] = sigmaHHUnitIsDecibels[0] ? sigmaHH1 : toDecibels(sigmaHH1);
                        sigma[1] = sigmaVVUnitIsDecibels[0] ? sigmaVV1 : toDecibels(sigmaVV1);

                        theta[0] = theta1;
                        theta[1] = theta1;

                        searchLUTForN(sigma, theta, pol, result, resultSigma);
                        nearestNeighbours[x - tx0][y - ty0] = setUpNearestNeighbours(result, resultSigma);
                    }

                    //System.out.println("results: " + result[0] + " " + result[1]);

                    if (N == 1) {
                        if (outputRMS) {
                            rmsTile.getDataBuffer().setElemDoubleAt(index, result[0][0]);
                        }

                        rdcTile.getDataBuffer().setElemDoubleAt(index, result[0][1]);
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
                    int outlier = INVALID_OUTLIER_VALUE;

                    double[] outParmas = new double[3];
                    for (int i = 0; i < outParmas.length; i++) {
                        outParmas[i] = INVALID_OUTPUT_VALUE;
                    }

                    final KDTreeNearestNeighbours nn = nearestNeighbours[x0][y0];
                    if (nn != null) {

                        final int bestIdx = getBestKDTreeNeighbour(x0, y0, nearestNeighbours, outParmas);
                        if (bestIdx < 0) {
                            rms = outParmas[2];
                            rdc = outParmas[1];
                            outlier = 2;
                        } else {
                            rms = nn.rms.get(bestIdx);
                            rdc = nn.rdc.get(bestIdx);
                            outlier = (bestIdx == 0) ? 0 : 1;
                        }
                        //printDebugMsg(x, y, rdc, bestIdx, sigma, nn);
                    }

                    if (outputRMS) {
                        rmsTile.getDataBuffer().setElemDoubleAt(index, rms);
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

    private void buildMyKDTreeMap() throws OperatorException {

        final double[] minThetas = new double[1];
        final double[] maxThetas = new double[1];

        getMinMaxIncidenceAngles(minThetas, maxThetas);

        //System.out.println("min theta = " + minThetas[0] + " max theta = " + maxThetas[0]);

        int minThetaInt1 = getAngleSectionIndex(minThetas[0]);
        int maxThetaInt1 = getAngleSectionIndex(maxThetas[0]);

        //System.out.println("minThetaInt1 = " + minThetaInt1 + " maxThetaInt = " + maxThetaInt1);

        if (minThetaInt1 < 0) {
            minThetaInt1 = 0;
        }

        if (maxThetaInt1 < 0) {
            maxThetaInt1 = NUM_ANGLE_SECTIONS - 1;
        }

        final int[] sectionIdx1 = new int[maxThetaInt1 - minThetaInt1 + 1];

        for (int i = 0; i < sectionIdx1.length; i++) {

            sectionIdx1[i] = minThetaInt1 + i;
        }

        if (sectionIdx1[sectionIdx1.length - 1] != maxThetaInt1) {
            throw new OperatorException("Wrong theta sections 1 for kd tree ");
        }

        final KDTreeInfo[] infos = new KDTreeInfo[sectionIdx1.length];

        for (int i = 0; i < sectionIdx1.length; i++) {

            final int[] sectionIdx = new int[]{sectionIdx1[i], sectionIdx1[i]};
            KDTreeInfo info = new KDTreeInfo(sectionIdx, pol);

            infos[i] = info;
        }

        buildKDTreeMap(infos);
    }

    protected int convertToKDTreeMapIntKey(KDTreeInfo info) {

        final int[] sectionIdx = info.getSectionIdx();
        return sectionIdx[0];
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
    }

    /**
     * Compare the values read from CSV LUT and Matlab LUT files.
     * This function is used for unit test only.
     *
     * @param csvLUTFilePath    The full path to the CSV LUT file.
     * @param matlabLUTFilePath The full path to the Matlab LUT file.
     * @param epsilon           Precision for comparison.
     */
    public boolean compareCSVLUTWithMatlabLUT(final String csvLUTFilePath, final String matlabLUTFilePath, final double epsilon) throws IOException {

        return compareLUTs(csvLUTFilePath, matlabLUTFilePath, 2, epsilon);
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
            super(IEMMultiPolInverOp.class);
        }
    }
}