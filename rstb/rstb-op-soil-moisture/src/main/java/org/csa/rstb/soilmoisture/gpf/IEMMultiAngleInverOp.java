
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
 * This class uses the Multi-angle approach utilizing one sigma value (either HH or VV polarization) from
 * AM image and one sigma value (HH or VV) from PM image.
 * There are 4 possible combinations:
 * 1) HH1-HH2
 * 2) HH1-VV2
 * 3) VV1-VV2
 * 4) VV1-HH2
 * where HH1, VV1 are from AM and HH2, VV2 are from PM.
 */

@OperatorMetadata(alias = "IEM-Multi-Angle-Inversion",
        category = "Radar/Soil Moisture",
        authors = "Cecilia Wong",
        description = "Performs IEM inversion using Multi-angle approach")
public class IEMMultiAngleInverOp extends IEMInverBase {

    // There is an AM source image and a PM source image.
    // The bands that are applicable are: sigmaHH or sigmaVV, theta. sigma is the backscatter (in dB). theta is
    // Local Incidence Angle (LIA) (in degrees).
    private static final int NUM_SOURCE_IMAGES = 2;
    // The two LUT parameters are: rms and RDC.
    private static final int NUM_LUT_PARAMS = 2;
    // pol[0] is the polarization of the AM sigma
    // pol[1] is the polarization of the PM sigma
    // 0 means HH, 1 means VV
    final int[] pol = new int[NUM_SOURCE_IMAGES];
    // sigmaUnitIsDecibels[0] for the sigma in AM image (whether it is HH or VV whichever is applicable depending on
    // which of the 4 sigma polarization combinations is chosen by the user).
    // sigmaUnitIsDecibels[1] for the sigma in PM image.
    final boolean[] sigmaUnitIsDecibels = new boolean[NUM_SOURCE_IMAGES];
    // There must be exactly two images: an AM image and a PM image.
    // The bands that are applicable are: sigmaHH, sigmaVV, theta. sigma is the backscatter (in dB). theta is
    // Local Incidence Angle (LIA) (in degrees).
    // Some bands in a source product may not be used.
    // All images MUST be in one source product.
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
    // Users can choose which one of the 4 combinations of sigma polarizations.
    @Parameter(valueSet = {"HH1-HH2", "HH1-VV2", "VV1-VV2", "VV1-HH2"}, description = "Multi-Angle Polarizations",
            defaultValue = "HH1-HH2", label = "Polarizations")
    private String sigmaPol = "HH1-HH2";
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

        //System.out.println("sigmaPol = " + sigmaPol);

        switch (sigmaPol) {
            case "HH1-HH2":
                pol[0] = 0; // AM HH
                pol[1] = 0; // PM HH
                break;

            case "HH1-VV2":
                pol[0] = 0; // AM HH
                pol[1] = 1; // PM VV
                break;

            case "VV1-VV2":
                pol[0] = 1; // AM VV
                pol[1] = 1; // PM VV
                break;

            case "VV1-HH2":
                pol[0] = 1; // AM VV
                pol[1] = 0; // PM HH
                break;

            default:
                throw new OperatorException("Invalid Multi-Angle polarization: " + sigmaPol);
        }

        try {

            initBaseParams(sourceProduct, lutFile, outputRMS);

            setNumberOfImages(NUM_SOURCE_IMAGES);

            rdcThreshold = thresholdRDC;
            super.initialize();

            targetProduct = getTargetProductFromBase();

            getSourceBands();

            initLUT(NUM_LUT_PARAMS);

            buildMyKDTreeMap();

            for (int i = 0; i < sigmaUnitIsDecibels.length; i++) {

                sigmaUnitIsDecibels[i] = (pol[i] == 0) ? sigmaHHUnitIsDecibels[i] : sigmaVVUnitIsDecibels[i];
            }

            updateMetadata("Multi-Angle IEM");
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

        // Source tile has larger dimensions than the target tile.
        final Rectangle sourceTileRectangle = getExtendedRectangle(tx0, ty0, tw, th);

        KDTreeNearestNeighbours[][] nearestNeighbours =
                new KDTreeNearestNeighbours[sourceTileRectangle.width][sourceTileRectangle.height];

        try {
            Tile[] sigmaTile = new Tile[NUM_SOURCE_IMAGES];
            Tile[] thetaTile = new Tile[NUM_SOURCE_IMAGES];

            ProductData[] sigmaData = new ProductData[NUM_SOURCE_IMAGES];
            ProductData[] thetaData = new ProductData[NUM_SOURCE_IMAGES];

            for (int i = 0; i < NUM_SOURCE_IMAGES; i++) {

                if (pol[i] == 0) {

                    sigmaTile[i] = getSourceTile(sigmaHHBand[i], sourceTileRectangle);

                } else if (pol[i] == 1) {

                    sigmaTile[i] = getSourceTile(sigmaVVBand[i], sourceTileRectangle);

                } else {

                    throw new OperatorException("Invalid polarization: " + pol[i]);
                }

                if (sigmaTile[i] == null) {
                    throw new OperatorException("Failed to get source tile sigma for image " + i + "; pol = " + pol[i] + " (0 is HH, 1 is VV)");
                }
                sigmaData[i] = sigmaTile[i].getDataBuffer();

                thetaTile[i] = getSourceTile(thetaBand[i], sourceTileRectangle);
                if (thetaTile[i] == null) {
                    throw new OperatorException("Failed to get source tile theta for image " + i);
                }
                thetaData[i] = thetaTile[i].getDataBuffer();
            }

            final int maxy = ty0 + th;
            final int maxx = tx0 + tw;
            final double[][] result = new double[N][NUM_LUT_PARAMS];
            final double[] sigma = new double[NUM_SOURCE_IMAGES];
            final double[] theta = new double[NUM_SOURCE_IMAGES];
            final double[][] resultSigma = new double[N][2];

            final Tile rmsTile = targetTiles.get(rmsBand);
            final Tile rdcTile = targetTiles.get(rdcBand);
            final Tile debugTile = (debugBand == null) ? null : targetTiles.get(debugBand);
            final Tile outlierTile = (outlierBand == null) ? null : targetTiles.get(outlierBand);

            // Process pixel by pixel in the tile
            for (int y = ty0; y < maxy; y++) { // loop through rows

                for (int x = tx0; x < maxx; x++) { // loop through columns

                    final double sigma1 = sigmaData[0].getElemDoubleAt(sigmaTile[0].getDataBufferIndex(x, y));
                    final double theta1 = thetaData[0].getElemDoubleAt(thetaTile[0].getDataBufferIndex(x, y));

                    final double sigma2 = sigmaData[1].getElemDoubleAt(sigmaTile[1].getDataBufferIndex(x, y));
                    final double theta2 = thetaData[1].getElemDoubleAt(thetaTile[1].getDataBufferIndex(x, y));

                    //System.out.println("sigma1 = " + sigma1 + " theta1 = " + theta1 + " pol = " + pol[0] + " (0 is HH, 1 is VV)");
                    //System.out.println("sigma2 = " + sigma2 + " theta2 = " + theta2 + " pol = " + pol[1] + " (0 is HH, 1 is VV)");

                    // result[.][0] is rms
                    // result[.][1] is RDC
                    // resultSigma[.][0] is sigmaHH
                    // resultSigma[.][1] is sigmaVV
                    initResults(result, resultSigma);

                    // It is IMPORTANT that the values are not converted at this point because their
                    // validity is being checked against whatever the bands say is the "no data value".
                    if (isValidSigmaHH(sigma1, 0) && isValidTheta(theta1, 0) &&
                            isValidSigmaHH(sigma2, 1) && isValidTheta(theta2, 1)) {

                        sigma[0] = sigmaUnitIsDecibels[0] ? sigma1 : toDecibels(sigma1);
                        sigma[1] = sigmaUnitIsDecibels[1] ? sigma2 : toDecibels(sigma2);

                        theta[0] = theta1;
                        theta[1] = theta2;

                        searchLUTForN(sigma, theta, pol, result, resultSigma);
                        nearestNeighbours[x - tx0][y - ty0] = setUpNearestNeighbours(result, resultSigma);
                    }

                    //System.out.println("results: " + result[0] + " " + result[1]);
                    if (N == 1) {
                        if (outputRMS) {
                            rmsTile.getDataBuffer().setElemDoubleAt(rmsTile.getDataBufferIndex(x, y), result[0][0]);
                        }

                        rdcTile.getDataBuffer().setElemDoubleAt(rdcTile.getDataBufferIndex(x, y), result[0][1]);
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

            // pol[i] == 0 means HH
            if (pol[i] == 0 && sigmaHHBand[i] == null) {

                throw new OperatorException("No sigmaHHBand[" + i + "]");
            }

            // pol[i] == 1 means VV
            if (pol[i] == 1 && sigmaVVBand[i] == null) {

                throw new OperatorException("No sigmaVVBand[" + i + "]");
            }

            if (thetaBand[i] == null) {

                throw new OperatorException("No thetaBand[" + i + "]");
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

                final int[] sectionIdx = new int[2];
                sectionIdx[0] = sectionIdx1[i]; // angle section from 1st image
                sectionIdx[1] = sectionIdx2[j]; // angle section from 2nd image

                KDTreeInfo info = new KDTreeInfo(sectionIdx, pol);

                infos[i * sectionIdx2.length + j] = info;
            }
        }

        buildKDTreeMap(infos);
    }

    protected int convertToKDTreeMapIntKey(KDTreeInfo info) {

        final int[] sectionIdx = info.getSectionIdx();
        return sectionIdx[0] * 1000 + sectionIdx[1];
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
     * Set sigma polarizations. The possibilities are:  "HH1-HH2", "HH1-VV2", "VV1-VV2", "VV1-HH2".
     * This function is used for unit test only.
     *
     * @param sigmaPol The sigma polarizations.
     */
    public void setSigmaPol(final String sigmaPol) {
        this.sigmaPol = sigmaPol;
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
            super(IEMMultiAngleInverOp.class);
        }
    }
}